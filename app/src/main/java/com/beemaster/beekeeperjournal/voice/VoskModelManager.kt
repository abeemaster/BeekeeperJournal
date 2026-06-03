// VoskModelManager.kt Це Менеджер або Репозиторій для моделі Vosk. Його головна мета — звільнити VoiceManager від важкої роботи з великим файлом моделі.
package com.beemaster.beekeeperjournal.voice

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.work.*
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.workers.ModelDownloadWorker
import com.beemaster.beekeeperjournal.workers.ModelUnpackWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton-клас, відповідальний за ініціалізацію, зберігання та керування
 * життєвим циклом моделі Vosk для розпізнавання мовлення.
 *
 * Інтегрований з WorkManager для асинхронного завантаження та розпакування
 * великої мовної моделі.
 */
@Singleton
class VoskModelManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    companion object {
        const val VOSK_WORK_CHAIN_TAG = "VoskModelSetupChain"
        // Назва критично важливого файлу Vosk для перевірки цілісності
        private const val VOSK_CRITICAL_FILE = "model.conf"
    }

    private val TAG = "VoskModelManager"

    private var voskModel: Model? = null
    private val voskModelReadyListeners = mutableListOf<() -> Unit>()
    private val modelDirPath = File(context.filesDir, Constants.VOSK_MODEL_DIR_NAME).absolutePath

    private var progressUpdateCallback: ((state: String, progress: Int) -> Unit)? = null

    private var workInfoObserver: androidx.lifecycle.Observer<WorkInfo?>? = null

    val isModelReady: Boolean
        get() = voskModel != null

    init {
        if (!isEmulator()) {
            // Ініціалізація Vosk: встановлення рівня логування
            try {
                LibVosk.setLogLevel(LogLevel.INFO)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to set Vosk log level: ${e.message}")
            }
        } else {
            Log.i(TAG, "Vosk initialization skipped: Emulator detected")
        }
    }

    /**
     * Визначає, чи запущено додаток на емуляторі.
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }

    /**
     * Перевіряє, чи тека моделі існує і містить необхідний критичний файл.
     */
    private fun isVoskModelIntegrityOk(modelDir: File): Boolean {
        // Vosk вимагає наявності model.conf
        return modelDir.exists() && File(modelDir, VOSK_CRITICAL_FILE).exists()
    }


    /**
     * Повертає модель Vosk.
     * @return Об'єкт Model або null, якщо модель ще не готова.
     */
    fun getModel(): Model? = voskModel

    /**
     * Додає слухача, який буде викликаний, коли модель Vosk буде готова.
     */
    fun addModelReadyListener(listener: () -> Unit) {
        if (isModelReady) {
            Handler(Looper.getMainLooper()).post { listener.invoke() }
        } else {
            voskModelReadyListeners.add(listener)
        }
    }

    private fun notifyModelReady() {
        Log.d(TAG, "notifyModelReady: Notifying ${voskModelReadyListeners.size} listeners.")
        Handler(Looper.getMainLooper()).post {
            voskModelReadyListeners.forEach { it.invoke() }
            voskModelReadyListeners.clear()
        }
    }

    /**
     * Встановлює зворотний виклик для оновлення прогресу в діалоговому вікні.
     * @param callback Функція, яка приймає стан (String) та прогрес (Int 0-100, або -1 для невизначеного).
     */
    fun setProgressUpdateCallback(callback: ((state: String, progress: Int) -> Unit)?) {
        this.progressUpdateCallback = callback
    }

    private fun reportProgress(state: String, progress: Int) {
        Handler(Looper.getMainLooper()).post {
            progressUpdateCallback?.invoke(state, progress)
        }
    }

    /**
     * Перевіряє наявність моделі та, за потреби, запускає ланцюжок WorkManager для завантаження/розпакування.
     */
    fun checkAndStartModelDownload() {
        if (isEmulator()) {
            Log.w(TAG, "Download skipped: Vosk is disabled on emulators.")
            reportProgress("Голосовий ввід недоступний на емуляторі", 0)
            notifyModelReady()
            return
        }
        if (isModelReady) {
            notifyModelReady()
            return
        }

        val modelDir = File(modelDirPath)

        // ВИПРАВЛЕНО: Використовуємо надійну перевірку цілісності моделі
        if (isVoskModelIntegrityOk(modelDir)) {
            Log.i(TAG, "Модель Vosk знайдена локально і цілісна. Завантаження в пам'ять...")
            reportProgress("Модель знайдена локально. Завантаження...", -1)
            loadModelFromPath(modelDirPath)
            return
        }

        // Якщо тека існує, але не цілісна (наприклад, корумпована або вкладена),
        // ми видаляємо її, щоб WorkManager почав роботу з чистого листа.
        if (modelDir.exists() && modelDir.isDirectory) {
            Log.w(TAG, "Папка моделі існує, але не містить критичного файлу. Видаляємо для повторного завантаження/розпакування.")
            modelDir.deleteRecursively()
        }


        Log.i(TAG, "Модель Vosk не знайдена. Запуск WorkManager для завантаження...")
        reportProgress("Модель на пристрої не знайдена. Запуск завантаження...", -1)
        startVoskWorkChain()
    }

    /**
     * Допоміжна функція для рекурсивного видалення вмісту директорії
     */
    private fun File.deleteRecursively(): Boolean {
        var success = true
        if (isDirectory) {
            listFiles()?.forEach {
                if (!it.deleteRecursively()) success = false
            }
        }
        if (success) {
            success = delete()
        }
        return success
    }


    /**
     * Запускає ланцюжок воркерів: Завантаження -> Розпакування.
     */
    private fun startVoskWorkChain() {
        // 1. Створюємо запит на завантаження
        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(VOSK_WORK_CHAIN_TAG)
            .build()

        // 2. Створюємо запит на розпакування (залежить від успішного завантаження)
        val unzipRequest = OneTimeWorkRequestBuilder<ModelUnpackWorker>()
            .addTag(VOSK_WORK_CHAIN_TAG)
            .build()

        // 3. Формуємо ланцюжок та ставимо його в чергу
        val workContinuation = workManager.beginUniqueWork(
            VOSK_WORK_CHAIN_TAG,
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
            .then(unzipRequest)

        workContinuation.enqueue()

        // 4. Починаємо спостерігати за станом останнього воркера (ModelUnpackWorker)
        observeUnzipWorkerStatus(unzipRequest.id)
    }

    /**
     * Спостерігає за станом виконання ModelUnpackWorker.
     */
    private fun observeUnzipWorkerStatus(unzipWorkerId: java.util.UUID) {
        // Отримуємо LiveData для стану воркера
        val workInfoLiveData = workManager.getWorkInfoByIdLiveData(unzipWorkerId)

        // Створюємо Observer, який буде реагувати на зміни
        workInfoObserver = androidx.lifecycle.Observer { workInfo ->
            if (workInfo == null) return@Observer

            // Перевіряємо, чи був WorkInfo для ModelDownloadWorker, щоб відобразити прогрес
            val isUnpackWorker = workInfo.id == unzipWorkerId

            Log.d(TAG, "Worker State: ${workInfo.state}")

            when (workInfo.state) {
                WorkInfo.State.RUNNING -> {
                    // ВИПРАВЛЕНО: Використовуємо уніфікований ключ прогресу з Constants
                    val progress = workInfo.progress.getInt(Constants.WORK_KEY_PROGRESS, -1)
                    val stateText = if (isUnpackWorker) "Розпакування моделі..." else "Завантаження моделі..."
                    reportProgress(stateText, progress)
                }
                WorkInfo.State.SUCCEEDED -> {
                    val modelPath = workInfo.outputData.getString(Constants.WORK_KEY_MODEL_UNZIPPED_PATH)
                    if (modelPath != null) {
                        Log.i(TAG, "WorkManager завершено успішно. Шлях моделі: $modelPath")
                        // ВИПРАВЛЕНО: Завантажуємо модель після успішного розпакування
                        loadModelFromPath(modelPath)
                    } else {
                        Log.e(TAG, "WorkManager SUCCEEDED, але шлях до моделі відсутній.")
                        reportProgress("Помилка ініціалізації після завантаження.", 0)
                        notifyModelReady()
                    }
                    // Видаляємо спостерігача, щоб уникнути витоку пам'яті
                    workInfoLiveData.removeObserver(workInfoObserver!!)
                    workInfoObserver = null
                }
                WorkInfo.State.FAILED -> {
                    Log.e(TAG, "WorkManager FAILED.")
                    reportProgress("Помилка завантаження або розпакування моделі.", 0)
                    // Видаляємо спостерігача
                    workInfoLiveData.removeObserver(workInfoObserver!!)
                    workInfoObserver = null
                    notifyModelReady()
                }
                else -> {
                    // ENQUEUED, BLOCKED, CANCELLED
                }
            }
        }

        // Починаємо спостереження, використовуючи observeForever для синглтона.
        workInfoLiveData.observeForever(workInfoObserver!!)
    }

    /**
     * Завантажує модель Vosk у пам'ять з вказаного шляху.
     */
    private fun loadModelFromPath(path: String) {
        Thread {
            try {
                if (voskModel != null) {
                    Log.d(TAG, "Model already initialized. Skipping readModel.")
                    return@Thread
                }

                reportProgress("Ініціалізація моделі...", -1)

                voskModel = Model(path)

                reportProgress("Модель Vosk готова!", 100)
                Log.d(TAG, "loadModelFromPath: Vosk model successfully loaded from $path.")
                notifyModelReady()
            } catch (e: Exception) {
                Log.e(TAG, "loadModelFromPath: Error loading Vosk model from $path.", e)
                reportProgress("Помилка завантаження: ${e.message}", 0)
                notifyModelReady()
            }
        }.start()
    }

    /**
     * Метод, який Activity викликатиме для повторної спроби або первинного запуску
     */
    fun startModelSetup() {
        checkAndStartModelDownload()
    }

    /**
     * Звільняє ресурси моделі Vosk.
     */
    fun release() {
        Log.d(TAG, "release: Releasing Vosk model resources.")
        voskModel?.close()
        voskModel = null
        // Зупиняємо будь-яку роботу, що виконується
        workManager.cancelUniqueWork(VOSK_WORK_CHAIN_TAG)

        // Видаляємо спостерігача, якщо він ще активний і прив'язаний
        workInfoObserver?.let { observer ->
            // Примітка: Без прямої посилання на LiveData тут, ми покладаємося на те,
            // що він буде видалений в SUCCEEDED/FAILED. Це безпечніше, ніж спроба примусового видалення.
        }
    }
}
package com.beemaster.beekeeperjournal.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.beemaster.beekeeperjournal.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Воркер для завантаження Vosk-моделі з Інтернету.
 * Використовує Foreground Service для відображення системного індикатора прогресу в рядку стану.
 */
class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val TAG = "ModelDownloadWorker"
        // Нові константи для сповіщення
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download_channel"
        private const val CHANNEL_NAME = "Завантаження моделей"
    }

    // --- 1. Створення каналу сповіщень (потрібно для Android 8.0+) ---
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // Використовуємо IMPORTANCE_LOW, оскільки це сповіщення про прогрес, яке не має турбувати звуком
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- 2. Створення об'єкта ForegroundInfo для підняття Worker до рівня Service ---
    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        // Створення початкового сповіщення з нескінченним прогресом
        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setContentTitle("Завантаження моделі Vosk")
            .setContentText("Ініціалізація...")
            .setSmallIcon(android.R.drawable.stat_sys_download) // Системна іконка стрілочки
            .setProgress(100, 0, true) // Нескінченний прогрес під час ініціалізації
            .setOngoing(true) // Робить сповіщення постійним (це
            // ознака Foreground Service)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Оголошуємо, що це робота переднього плану. Це миттєво відобразить сповіщення в системі.
        // Це має бути викликано протягом перших 10 секунд роботи Worker.
        try {
            setForeground(getForegroundInfo())
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Cannot set Foreground Service: ${e.message}")
            return@withContext Result.failure()
        }


        val modelUrl = Constants.VOSK_MODEL_URL
        val zipFileName = Constants.VOSK_MODEL_ZIP_NAME
        // Завантажуємо в кеш, оскільки це тимчасовий файл
        val zipFile = File(applicationContext.cacheDir, zipFileName)

        // 1. Перевірка, чи модель вже завантажена
        if (zipFile.exists() && zipFile.length() > 0) {
            Log.i(TAG, "Model ZIP already exists. Skipping download.")
            // Фінальне сповіщення
            showFinalNotification("Модель вже завантажена. Перехід до розпакування...", isSuccess = true)
            // Повертаємо Result.success, щоб ModelUnpackWorker міг продовжити
            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_ZIP_NAME to zipFileName
            ))
        }

        try {
            Log.d(TAG, "Starting download from: $modelUrl")

            // 2. Встановлення з'єднання
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Server returned HTTP ${connection.responseCode}")
                // Фінальне сповіщення
                showFinalNotification("Помилка сервера: ${connection.responseCode}", isSuccess = false)
                return@withContext Result.failure()
            }

            // 3. Відстеження прогресу
            val contentLength = connection.contentLength.toLong()
            var totalBytesRead: Long = 0
            val buffer = ByteArray(Constants.BUFFER_SIZE)
            var bytesRead: Int
            var lastProgress = -1

            connection.inputStream.use { input ->
                // Використовуємо .apply { createNewFile() } для створення файлу, якщо він не існує
                FileOutputStream(zipFile.apply { createNewFile() }).use { output ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        // Перевірка на зупинку воркера
                        if (isStopped) {
                            zipFile.delete()
                            // Фінальне сповіщення про скасування
                            showFinalNotification("Завантаження скасовано.", isSuccess = false)
                            return@withContext Result.failure() // Скасовано
                        }

                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Оновлення прогресу
                        if (contentLength > 0) {
                            val currentProgress = (totalBytesRead * 100 / contentLength).toInt()
                            if (currentProgress > lastProgress) {
                                // Оновлюємо системне сповіщення (те, що бачить користувач у шторці)
                                updateNotification(currentProgress)

                                // Публікуємо прогрес для WorkManager (для внутрішнього відстеження)
                                // ВИПРАВЛЕНО: Використовуємо Constants.WORK_KEY_PROGRESS
                                setProgress(workDataOf(Constants.WORK_KEY_PROGRESS to currentProgress))
                                lastProgress = currentProgress
                            }
                        }
                    }
                }
            }

            // 4. Фінальна перевірка
            if (zipFile.exists() && zipFile.length() > 0) {
                Log.i(TAG, "Download successful. File size: ${zipFile.length()} bytes")

                // Фінальне сповіщення про успіх
                showFinalNotification("Завантаження моделі завершено. Початок розпакування...", isSuccess = true)

                // Встановлюємо прогрес на 100% після успішного завершення
                setProgress(workDataOf(Constants.WORK_KEY_PROGRESS to 100))

                // Повертаємо Result.success, щоб WorkManager запустив наступний воркер (ModelUnpackWorker)
                return@withContext Result.success(workDataOf(
                    Constants.WORK_KEY_MODEL_ZIP_NAME to zipFileName
                ))
            } else {
                Log.e(TAG, "Download failed: File is missing or empty after operation.")
                zipFile.delete()
                showFinalNotification("Помилка: файл відсутній або порожній.", isSuccess = false)
                return@withContext Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model from $modelUrl", e)
            zipFile.delete()
            showFinalNotification("Помилка завантаження: ${e.message}", isSuccess = false)
            return@withContext Result.retry()
        }
    }

    /**
     * Оновлює системне сповіщення про прогрес завантаження.
     */
    private fun updateNotification(progress: Int) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setContentTitle("Завантаження моделі Vosk")
            .setContentText("Прогрес: $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false) // Встановлюємо фактичний прогрес
            .setOngoing(true) // Залишаємо його постійним під час роботи
            .setOnlyAlertOnce(true) // Не видавати звук/вібрацію при кожному оновленні
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Відображає фінальне сповіщення (успіх або помилка) і скасовує постійний індикатор.
     */
    private fun showFinalNotification(message: String, isSuccess: Boolean) {
        // Ми не скасовуємо постійний індикатор, але оновлюємо його
        // WorkManager автоматично скасує його після завершення Worker.
        val finalNotification = NotificationCompat.Builder(
            applicationContext,
            CHANNEL_ID
        )
            .setContentTitle(if (isSuccess) "Завантаження завершено" else "Помилка завантаження")
            .setContentText(message)
            .setSmallIcon(
                if (isSuccess) android.R.drawable.stat_sys_download_done
                else android.R.drawable.ic_dialog_alert
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Середній пріоритет для результату
            .setAutoCancel(true) // Дозволити користувачеві прибрати його
            .build()

        // Замінюємо постійне сповіщення про прогрес на фінальне
        notificationManager.notify(NOTIFICATION_ID, finalNotification)
    }
}
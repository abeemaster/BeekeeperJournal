package com.beemaster.beekeeperjournal.voice

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.beemaster.beekeeperjournal.workers.ModelDownloadWorker
import com.beemaster.beekeeperjournal.workers.ModelUnpackWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton-клас, відповідальний за запуск та керування процесом
 * завантаження та розпакування моделі Vosk через WorkManager.
 */
@Singleton
class VoskModelDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    // Фікс Unresolved reference 'TAG' та визначення робочих констант
    companion object {
        private const val TAG = "VoskModelDownloader"
        private const val WORK_CHAIN_NAME = "vosk_model_download_unpack_chain"
    }

    /**
     * Запускає ланцюжок WorkManager для завантаження та розпакування моделі Vosk.
     * Використовує ExistingWorkPolicy.KEEP, щоб не перезапускати, якщо вже запущено.
     */
    fun startModelDownloadAndUnpack() {
        Log.d(TAG, "Ініціалізація завантаження моделі Vosk через WorkManager.")

        // 1. Створюємо запит на завантаження
        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .addTag(ModelDownloadWorker.TAG)
            .build()

        // 2. Створюємо запит на розпакування
        val unpackRequest = OneTimeWorkRequestBuilder<ModelUnpackWorker>()
            .addTag(ModelUnpackWorker.TAG)
            .build()

        // 3. Запускаємо ланцюжок WorkManager: Завантаження -> Розпакування
        WorkManager.getInstance(context)
            .beginUniqueWork(
                WORK_CHAIN_NAME,
                ExistingWorkPolicy.KEEP, // Не перезапускати, якщо робота вже йде
                downloadRequest
            )
            .then(unpackRequest)
            .enqueue()

        Log.d(TAG, "Ланцюжок завантаження моделі успішно додано в чергу WorkManager.")
    }

    /**
     * Повертає стан роботи WorkManager.
     */
    fun getWorkInfoLiveData() = WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkLiveData(WORK_CHAIN_NAME)
}
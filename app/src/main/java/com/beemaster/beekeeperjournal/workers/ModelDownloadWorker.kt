package com.beemaster.beekeeperjournal.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.beemaster.beekeeperjournal.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Воркер для завантаження Vosk-моделі з Інтернету.
 * Зберігає ZIP-файл у кеш-директорії (cacheDir).
 */
class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        // Константа, що вирішує помилку "Unresolved reference 'TAG'"
        const val TAG = "ModelDownloadWorker"
        const val PROGRESS_KEY = "Progress"
    }

    private val TAG = "ModelDownloadWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = Constants.VOSK_MODEL_URL
        val zipFileName = Constants.VOSK_MODEL_ZIP_NAME
        // Використовуємо cacheDir для тимчасового зберігання великого ZIP-файлу
        val zipFile = File(applicationContext.cacheDir, zipFileName)

        // 1. Перевірка, чи модель вже завантажена
        if (zipFile.exists() && zipFile.length() > 0) {
            Log.i(TAG, "Model ZIP already exists. Skipping download.")
            // Передаємо ім'я файлу наступному воркеру
            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_ZIP_NAME to zipFileName
            ))
        }

        try {
            Log.d(TAG, "Starting download from: $modelUrl")

            // 2. Виконання завантаження
            URL(modelUrl).openStream().use { input ->
                FileOutputStream(zipFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 3. Фінальна перевірка
            if (zipFile.exists() && zipFile.length() > 0) {
                Log.i(TAG, "Download successful. File saved to: ${zipFile.absolutePath}")
                // Передаємо ім'я файлу наступному воркеру
                return@withContext Result.success(workDataOf(
                    Constants.WORK_KEY_MODEL_ZIP_NAME to zipFileName
                ))
            } else {
                // Якщо файл порожній або не існує після завантаження
                Log.e(TAG, "Download failed: File is missing or empty.")
                return@withContext Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading model from $modelUrl", e)
            // Видаляємо частково завантажений файл у разі помилки
            zipFile.delete()
            return@withContext Result.retry() // Спробувати пізніше
        }
    }
}
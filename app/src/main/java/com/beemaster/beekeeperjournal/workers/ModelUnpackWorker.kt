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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Воркер для розпакування завантаженого ZIP-файлу Vosk-моделі
 * та її розміщення у постійній директорії програми (filesDir).
 * Також очищує ZIP-файл з кешу.
 */
class ModelUnpackWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        // Константа, що вирішує помилку "Unresolved reference 'TAG'"
        const val TAG = "ModelUnpackWorker"
        const val MODEL_NAME = "vosk-model-small-uk-v3-small"
    }

    private val TAG = "ModelUnpackWorker"
    private val BUFFER_SIZE = 4096

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Отримання імені ZIP-файлу з вхідних даних від попереднього воркера
        val zipFileName = inputData.getString(Constants.WORK_KEY_MODEL_ZIP_NAME)
        val unpackedDirName = Constants.VOSK_MODEL_UNPACKED_NAME

        if (zipFileName.isNullOrEmpty()) {
            Log.e(TAG, "Помилка вхідних даних: Відсутнє ім'я ZIP-файлу моделі.")
            return@withContext Result.failure()
        }

        // Файл, який потрібно розпакувати (знаходиться в cacheDir)
        val cacheZipFile = File(applicationContext.cacheDir, zipFileName)
        // Цільова директорія для розпакування (знаходиться в filesDir)
        val targetDir = File(applicationContext.filesDir, unpackedDirName)

        // 1. Перевірка, чи модель вже розпакована (перевіряємо, чи директорія існує і не порожня)
        if (targetDir.exists() && targetDir.isDirectory && targetDir.listFiles()?.isNotEmpty() == true) {
            Log.i(TAG, "Директорія моделі вже існує і не порожня. Пропуск розпакування.")
            // Повертаємо успіх, оскільки робота вже виконана
            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_UNPACKED_NAME to unpackedDirName
            ))
        }

        // 2. Перевірка наявності ZIP-файлу
        if (!cacheZipFile.exists()) {
            Log.e(TAG, "ZIP-файл не знайдено в кеші: ${cacheZipFile.absolutePath}")
            // Це критична помилка, оскільки попередній крок не зміг надати файл
            return@withContext Result.failure()
        }

        try {
            Log.d(TAG, "Початок розпакування ${cacheZipFile.name} у ${targetDir.absolutePath}")

            // Створення цільової директорії, якщо вона не існує
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // Виконання розпакування
            ZipInputStream(FileInputStream(cacheZipFile)).use { zipInputStream ->
                var zipEntry = zipInputStream.nextEntry
                val buffer = ByteArray(BUFFER_SIZE)

                while (zipEntry != null) {
                    val newFile = File(targetDir, zipEntry.name)

                    if (zipEntry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        // Запис файлу, переконавшись, що батьківські директорії існують
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var count: Int
                            while (zipInputStream.read(buffer, 0, BUFFER_SIZE).also { count = it } != -1) {
                                fos.write(buffer, 0, count)
                            }
                        }
                    }
                    zipEntry = zipInputStream.nextEntry
                }
            }

            Log.i(TAG, "Розпакування успішне. Модель готова в: ${targetDir.absolutePath}")

            // 3. Очищення: Видалення тимчасового ZIP-файлу з кешу
            if (cacheZipFile.delete()) {
                Log.d(TAG, "Очищення успішне: Видалено ${cacheZipFile.name}")
            } else {
                Log.w(TAG, "Попередження очищення: Не вдалося видалити ${cacheZipFile.name}")
            }

            // 4. Успішне завершення, передача імені директорії
            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_UNPACKED_NAME to unpackedDirName
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Помилка при розпакуванні моделі: ${e.message}", e)
            // Видалення частково розпакованої директорії у разі помилки
            targetDir.deleteRecursively()
            return@withContext Result.failure()
        }
    }
}
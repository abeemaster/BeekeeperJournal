package com.beemaster.beekeeperjournal.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.beemaster.beekeeperjournal.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Воркер для розпакування завантаженого ZIP-файлу Vosk-моделі.
 * Ця версія ВИПРАВЛЯЄ проблему подвійного вкладення папок, видаляючи
 * кореневу папку з імені файлу ZIP-архіву, що дозволяє Vosk правильно знайти файли.
 */
class ModelUnpackWorker( // Використовуємо New в назві класу, щоб уникнути конфліктів
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "ModelUnzipWorkerNew"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val zipFileName = inputData.getString(Constants.WORK_KEY_MODEL_ZIP_NAME)
        if (zipFileName.isNullOrEmpty()) {
            Log.e(TAG, "Input data missing: ${Constants.WORK_KEY_MODEL_ZIP_NAME}")
            return@withContext Result.failure()
        }

        val zipFile = File(applicationContext.cacheDir, zipFileName)
        // Цільова директорія: /files/vosk-model (однорівневе розпакування)
        val targetDir = File(applicationContext.filesDir, Constants.VOSK_MODEL_DIR_NAME)

        if (!zipFile.exists() || zipFile.length() == 0L) {
            Log.e(TAG, "ZIP file not found or empty: ${zipFile.absolutePath}")
            return@withContext Result.failure()
        }

        if (targetDir.exists() && targetDir.listFiles()?.isNotEmpty() == true) {
            Log.i(TAG, "Model directory already exists and is not empty. Skipping unzip.")
            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_UNZIPPED_PATH to targetDir.absolutePath
            ))
        }

        Log.d(TAG, "Starting unzip operation for: ${zipFile.name} to ${targetDir.absolutePath}")

        if (!targetDir.exists() && !targetDir.mkdirs()) {
            Log.e(TAG, "Failed to create target directory: ${targetDir.absolutePath}")
            return@withContext Result.failure()
        }

        var processedFiles = 0

        try {
            zipFile.inputStream().use { fileInput ->
                ZipInputStream(fileInput).use { zipInput ->

                    // --- ВИПРАВЛЕНА ЛОГІКА ШЛЯХІВ: КРОК 1 (ВИЗНАЧЕННЯ КОРЕНЯ) ---
                    var rootDirName: String? = null
                    val entries = mutableListOf<java.util.zip.ZipEntry>()

                    // Перший прохід: зчитуємо всі записи та визначаємо кореневу папку
                    // Створюємо новий потік для цього, щоб не порушити основний zipInput
                    val tempZipInput = ZipInputStream(zipFile.inputStream())
                    var tempEntry = tempZipInput.nextEntry
                    while (tempEntry != null) {
                        entries.add(tempEntry)
                        if (rootDirName == null) {
                            val entryName = tempEntry.name
                            val firstSeparator = entryName.indexOf('/')
                            if (firstSeparator > 0) {
                                rootDirName = entryName.substring(0, firstSeparator)
                            } else if (tempEntry.isDirectory && entryName.endsWith('/')) {
                                rootDirName = entryName.trimEnd('/')
                            }
                        }
                        tempEntry = tempZipInput.nextEntry
                    }
                    tempZipInput.close()

                    Log.d(TAG, "Identified potential root directory: $rootDirName")

                    // Другий прохід: фактична обробка та розпакування
                    // Потрібно знову відкрити ZIP, оскільки перший потік було закрито
                    val secondFileInput = zipFile.inputStream()
                    val secondZipInput = ZipInputStream(secondFileInput)

                    var currentZipEntry = secondZipInput.nextEntry

                    while (currentZipEntry != null) {
                        if (isStopped) {
                            Log.w(TAG, "Unzip cancelled.")
                            targetDir.deleteRecursively()
                            secondZipInput.close()
                            secondFileInput.close()
                            return@withContext Result.failure()
                        }

                        // --- ВИПРАВЛЕНА ЛОГІКА ШЛЯХІВ: КРОК 2 (КОРЕКЦІЯ ШЛЯХУ) ---
                        var entryName = currentZipEntry.name
                        if (rootDirName != null && entryName.startsWith("$rootDirName/")) {
                            // Видаляємо назву кореневої папки з шляху
                            entryName = entryName.substring(rootDirName.length + 1)
                        }

                        // Якщо шлях став порожнім, це сама коренева папка, пропускаємо її
                        if (entryName.isEmpty()) {
                            currentZipEntry = secondZipInput.nextEntry
                            continue
                        }

                        // Створення файлу/папки в targetDir, використовуючи СКОРИГОВАНИЙ entryName
                        val newFile = File(targetDir, entryName)
                        Log.v(TAG, "Processing entry: $entryName")

                        // Перевірка Zip Slip Attack
                        if (!newFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                            secondZipInput.close()
                            secondFileInput.close()
                            throw SecurityException("Zip Slip Attack detected: Entry is outside of the target directory.")
                        }

                        // Створення батьківських директорій, якщо необхідно
                        val parentDir = newFile.parentFile
                        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                            secondZipInput.close()
                            secondFileInput.close()
                            throw Exception("Parent directory creation failed for ${newFile.parent}")
                        }


                        if (currentZipEntry.isDirectory) {
                            if (!newFile.isDirectory && !newFile.mkdirs()) {
                                secondZipInput.close()
                                secondFileInput.close()
                                throw Exception("Failed to create directory $newFile")
                            }
                        } else {
                            // Запис файлу
                            FileOutputStream(newFile).use { fileOutput ->
                                BufferedOutputStream(fileOutput, Constants.BUFFER_SIZE).use { bufferOut ->
                                    val buffer = ByteArray(Constants.BUFFER_SIZE)
                                    var read: Int
                                    while (secondZipInput.read(buffer).also { read = it } != -1) {
                                        bufferOut.write(buffer, 0, read)
                                    }
                                }
                            }
                            processedFiles++
                        }

                        currentZipEntry = secondZipInput.nextEntry
                    }
                    secondZipInput.close()
                    secondFileInput.close()
                }
            }

            Log.i(TAG, "Unzip successful. Total files processed: $processedFiles. ZIP file deleted.")
            zipFile.delete()

            return@withContext Result.success(workDataOf(
                Constants.WORK_KEY_MODEL_UNZIPPED_PATH to targetDir.absolutePath
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error during unzip operation: ${e.message}", e)
            // Видаляємо невдало розпаковані файли
            targetDir.deleteRecursively()
            return@withContext Result.failure()
        }
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
}
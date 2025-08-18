// DataSynchronizer.kt

package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupData(
    val hiveList: List<HiveData>,
    val notes: List<Note>
)

class DataSynchronizer(
    private val context: Context,
    private val createBackupFileLauncher: ActivityResultLauncher<Intent>,
    private val openBackupFileLauncher: ActivityResultLauncher<Intent>,
    private val pickFolderLauncher: ActivityResultLauncher<Intent>
) {

    private val gson = Gson()
    private val notesFileName = "notes.json"
    private val hiveDataFileName = "hives.json"


    companion object {
        const val TAG = "DataSynchronizer"
        private const val BACKUP_FILE_MIME_TYPE = "application/json"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }

    fun showSyncOptionsDialog() {
        val dialogView = (context as Activity).layoutInflater.inflate(R.layout.dialog_sync_options, null)
        val dialogTitleTextView: TextView = dialogView.findViewById(R.id.dialogTitle)
        val backupCard: MaterialCardView = dialogView.findViewById(R.id.backupCard)
        val restoreCard: MaterialCardView = dialogView.findViewById(R.id.restoreCard)
        val exportCsvCard: MaterialCardView = dialogView.findViewById(R.id.exportCsvCard)

        dialogTitleTextView.text = "Опції синхронізації"

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        backupCard.setOnClickListener {
            dialog.dismiss()
            backupDataLocally()
        }

        restoreCard.setOnClickListener {
            dialog.dismiss()
            restoreDataLocally()
        }

        exportCsvCard.setOnClickListener {
            dialog.dismiss()
            openFolderPicker()
        }

        dialog.show()
    }

    private fun backupDataLocally() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val date = dateFormat.format(Date())
        val fileName = "BeekeeperJournal_backup_$date$BACKUP_FILE_EXTENSION"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_FILE_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createBackupFileLauncher.launch(intent)
    }

    fun writeBackupDataToFile(uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    val hives = readHivesFromJson()
                    val notes = readAllNotesFromJson()
                    val backupData = BackupData(hives, notes)
                    gson.toJson(backupData, writer)
                }
            }
            Toast.makeText(context, "Резервну копію успішно створено!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при створенні резервної копії: ${e.message}", e)
            Toast.makeText(context, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreDataLocally() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_FILE_MIME_TYPE
        }
        openBackupFileLauncher.launch(intent)
    }

    // У файлі DataSynchronizer.kt

    fun readAndRestoreBackupDataFromFile(fileUri: Uri, loadHivesCallback: () -> Unit) {
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    val type = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData? = gson.fromJson(reader, type)

                    Log.d(TAG, "readAndRestoreBackupDataFromFile: Початок відновлення даних...")

                    if (backupData != null) {
                        Log.d(TAG, "readAndRestoreBackupDataFromFile: Зчитано ${backupData.hiveList.size} вуликів і ${backupData.notes.size} нотаток з резервної копії.")

                        // Повністю перезаписуємо дані про вулики
                        writeHivesToJson(backupData.hiveList)
                        Log.d(TAG, "readAndRestoreBackupDataFromFile: Викликано writeHivesToJson з ${backupData.hiveList.size} вуликами.")

                        // Повністю перезаписуємо дані про нотатки
                        writeAllNotesToJson(backupData.notes)
                        Log.d(TAG, "readAndRestoreBackupDataFromFile: Викликано writeAllNotesToJson з ${backupData.notes.size} нотатками.")

                        loadHivesCallback()
                        Toast.makeText(context, "Дані успішно відновлено!", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "readAndRestoreBackupDataFromFile: Відновлення завершено успішно.")
                    } else {
                        Log.e(TAG, "readAndRestoreBackupDataFromFile: Не вдалося розібрати дані резервної копії. Об'єкт backupData null.")
                        Toast.makeText(context, "Не вдалося розібрати дані резервної копії.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при відновленні даних: ${e.message}", e)
            Toast.makeText(context, "Помилка при відновленні: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // У файлі DataSynchronizer.kt

    fun readAllNotesFromJson(): MutableList<Note> {
        val file = File(context.filesDir, notesFileName)
        if (!file.exists() || file.length() == 0L) {
            Log.d(TAG, "readAllNotesFromJson: Файл нотаток не існує або порожній.")
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                val notesList: MutableList<Note> = gson.fromJson(reader, type) ?: mutableListOf()

                Log.d(TAG, "readAllNotesFromJson: Успішно зчитано ${notesList.size} нотаток.")

                val typesCount = notesList.groupingBy { it.type }.eachCount()
                Log.d(TAG, "readAllNotesFromJson: Кількість зчитаних записів за типом: $typesCount")

                notesList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання нотаток з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    // У файлі DataSynchronizer.kt

    // У файлі DataSynchronizer.kt
// ...
    fun writeAllNotesToJson(notes: List<Note>) {
        val file = File(context.filesDir, notesFileName)
        try {
            FileWriter(file).use { writer ->
                gson.toJson(notes, writer)
            }
            // Додаємо лог
            Log.d(TAG, "writeAllNotesToJson: Успішно записано ${notes.size} нотаток.")
            val typesCount = notes.groupingBy { it.type }.eachCount()
            Log.d(TAG, "writeAllNotesToJson: Кількість записів за типом: $typesCount")
        } catch (e: Exception) {
            Log.e(TAG, "Помилка запису нотаток до файлу: ${e.message}", e)
        }
    }



    private fun formatNoteToCsvRow(note: Note): String {
        // ✅ Тепер ми просто беремо рядок дати з об'єкта note.
        // SimpleDateFormat більше не потрібен!
        val dateString = note.date

        val escapedText = note.text.replace("\"", "\"\"").replace("\n", " ").trim()
        val hiveNumber = note.hiveNumber

        // ✅ Виправлений рядок CSV:
        // Порядок полів: Дата, Текст, Тип, Номер Вулика, Мітка Часу, ID
        return "\"$dateString\",\"$escapedText\",\"${note.type}\",\"$hiveNumber\",\"${note.timestamp}\",\"${note.id}\""
    }


    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        pickFolderLauncher.launch(intent)
    }

    fun exportNotesToCsvFiles(folderUri: Uri) {
        Log.d(TAG, "exportNotesToCsvFiles: Початок експорту даних у папку: $folderUri")
        val allNotes = readAllNotesFromJson()

        if (allNotes.isEmpty()) {
            Toast.makeText(context, "Немає записів для експорту.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "exportNotesToCsvFiles: Немає записів для експорту.")
            return
        }

        val baseFolder = DocumentFile.fromTreeUri(context, folderUri)
        if (baseFolder == null || !baseFolder.exists() || !baseFolder.isDirectory) {
            Toast.makeText(context, "Обрана папка недійсна або не існує.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "exportNotesToCsvFiles: Обрана папка недійсна або не існує: $folderUri")
            return
        }

        var totalExportedNotes = 0
        val csvHeader = "Дата,Текст,Тип,Номер Вулика,Мітка Часу,ID\n"
        val hiveList = readHivesFromJson()
        val hiveNumberToNameMap = hiveList.associate { it.number to it.name }
        val notesGroupedByTargetPath = mutableMapOf<Pair<String, String>, MutableList<Note>>()

        allNotes.forEach { note ->
            val subfolderName: String
            val fileName: String
            val hiveNameForFile = hiveNumberToNameMap[note.hiveNumber]
                ?.replace(Regex("[^a-zA-Z0-9А-Яа-я]"), "_")
                ?.trim('_')
                ?.take(50)
                ?: "hive_${note.hiveNumber}"

            when (note.type) {
                "general" -> {
                    subfolderName = "Загальні_записи"
                    fileName = "Загальні_записи.csv"
                }
                "hive" -> {
                    subfolderName = "Вулики"
                    fileName = "${hiveNameForFile}.csv"
                }
                "queen" -> {
                    subfolderName = "Матки"
                    fileName = "Матка_${hiveNameForFile}.csv"
                }
                "notes" -> {
                    subfolderName = "Примітки"
                    fileName = "Примітка_${hiveNameForFile}.csv"
                }
                else -> {
                    subfolderName = "Other"
                    fileName = "${note.type}_${hiveNameForFile}.csv"
                }
            }
            val key = Pair(subfolderName, fileName)
            notesGroupedByTargetPath.getOrPut(key) { mutableListOf() }.add(note)
        }

        notesGroupedByTargetPath.forEach { (key, notesList) ->
            val (subfolderName, fileName) = key
            try {
                var targetSubFolder = baseFolder.findFile(subfolderName)
                if (targetSubFolder == null || !targetSubFolder.exists() || !targetSubFolder.isDirectory) {
                    targetSubFolder = baseFolder.createDirectory(subfolderName)
                    Log.d(TAG, "exportNotesToCsvFiles: Створено підпапку: $subfolderName")
                }
                if (targetSubFolder == null) {
                    Log.e(TAG, "exportNotesToCsvFiles: Не вдалося створити або знайти підпапку: $subfolderName")
                    Toast.makeText(context, "Не вдалося створити підпапку: $subfolderName", Toast.LENGTH_SHORT).show()
                    return@forEach
                }

                val existingFile = targetSubFolder.findFile(fileName)
                if (existingFile != null && existingFile.exists()) {
                    existingFile.delete()
                    Log.d(TAG, "exportNotesToCsvFiles: Видалено старий файл: $fileName")
                }

                val csvFile = targetSubFolder.createFile("text/csv", fileName)
                if (csvFile == null) {
                    Log.e(TAG, "exportNotesToCsvFiles: Не вдалося створити файл: $fileName у $subfolderName")
                    Toast.makeText(context, "Не вдалося створити файл: $fileName", Toast.LENGTH_SHORT).show()
                    return@forEach
                }

                context.contentResolver.openOutputStream(csvFile.uri, "w")?.use { outputStream ->
                    OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                        writer.write(csvHeader)
                        notesList.sortedBy { it.timestamp }.forEach { note ->
                            writer.write(formatNoteToCsvRow(note))
                            writer.write("\n")
                        }
                    }
                }
                totalExportedNotes += notesList.size
                Log.d(TAG, "exportNotesToCsvFiles: Успішно експортовано файл $fileName. Записів: ${notesList.size}")
            } catch (e: Exception) {
                Log.e(TAG, "exportNotesToCsvFiles: Помилка при записі файлу у $subfolderName/$fileName: ${e.message}", e)
                Toast.makeText(context, "Помилка при записі у $subfolderName/$fileName: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        if (totalExportedNotes > 0) {
            Toast.makeText(context, "Експорт завершено. Експортовано $totalExportedNotes записів.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Немає записів для експорту.", Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "exportNotesToCsvFiles: Експорт завершено. Загальна кількість записів: $totalExportedNotes")
    }

    // Приватні функції для роботи з JSON-файлами
    private fun readHivesFromJson(): MutableList<HiveData> {
        val file = File(context.filesDir, hiveDataFileName)
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<HiveData>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання вуликів з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    private fun writeHivesToJson(hives: List<HiveData>) {
        val file = File(context.filesDir, hiveDataFileName)
        try {
            FileWriter(file).use { writer ->
                gson.toJson(hives, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка запису вуликів до файлу: ${e.message}", e)
        }
    }




}
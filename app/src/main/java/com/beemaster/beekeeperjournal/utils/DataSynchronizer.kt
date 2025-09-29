// DataSynchronizer.kt

package com.beemaster.beekeeperjournal.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.beemaster.beekeeperjournal.models.HiveData
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.R
import com.google.android.material.card.MaterialCardView
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
    private val pickFolderLauncher: ActivityResultLauncher<Uri?>,
    private val onDataSynchronized: () -> Unit
) {

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


    fun readAndRestoreBackupDataFromFile(fileUri: Uri, loadHivesCallback: () -> Unit) {
        try {

        } catch (e: Exception) {
            Log.e(TAG, "Помилка при відновленні даних: ${e.message}", e)
            Toast.makeText(context, "Помилка при відновленні: ${e.message}", Toast.LENGTH_LONG).show()
        }
        onDataSynchronized()
    }

    private fun formatNoteToCsvRow(note: Note): String {
        // ✅ Створюємо форматер дати для конвертації
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateString = dateFormat.format(Date(note.timestamp))

        val escapedText = note.text.replace("\"", "\"\"").replace("\n", " ").trim()
        val hiveNumber = note.hiveNumber

        // ✅ Виправлений рядок CSV:
        // Порядок полів: ID, Дата, Текст, Тип, Номер Вулика, Мітка Часу
        return "\"${note.id}\",\"$dateString\",\"$escapedText\",\"${note.type}\",\"$hiveNumber\",\"${note.timestamp}\""
    }

    private fun openFolderPicker() {
        pickFolderLauncher.launch(null)
    }


}
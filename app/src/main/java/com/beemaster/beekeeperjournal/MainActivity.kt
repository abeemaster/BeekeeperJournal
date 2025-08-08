package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
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
import androidx.documentfile.provider.DocumentFile

data class BackupData(
    val hiveList: List<HiveData>,
    val notes: List<Note>
)


class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var hiveListRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private val gson = Gson()
    private val NOTES_FILE_NAME = "notes.json"

    companion object {
        const val TAG = "MainActivity"
        const val EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE = "com.beemaster.beekeeperjournal.HIVE_NUMBER_FOR_COLOR_UPDATE"
        const val HIVE_DATA_FILE_NAME = "hives.json"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"
        private const val BACKUP_FILE_MIME_TYPE = "application/json"
        private const val BACKUP_FILE_EXTENSION = ".json"
        const val PREFS_NAME = "BeekeeperJournalPrefs"
        const val KEY_HIVE_LIST = "hive_list"
    }

    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                exportNotesToCsvFiles(uri)
            } else {
                Toast.makeText(this, "Не вдалося отримати URI папки.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val createBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                writeBackupDataToFile(uri)
            } else {
                Toast.makeText(this, "Не вдалося створити файл резервної копії.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Створення резервної копії скасовано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                readAndRestoreBackupDataFromFile(uri)
            } else {
                Toast.makeText(this, "Не вдалося отримати файл.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Відновлення даних скасовано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val colorPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedColor = result.data?.getIntExtra("selected_color", Color.TRANSPARENT)
            val hiveNumber = result.data?.getIntExtra("hive_number", -1)
            val colorType = result.data?.getStringExtra("color_type")

            if (hiveNumber != -1 && selectedColor != null && colorType != null) {
                val hives = readHivesFromJson()
                val hiveToUpdate = hives.find { it.number == hiveNumber }
                if (hiveToUpdate != null) {
                    when (colorType) {
                        "primary" -> {
                            hiveToUpdate.color = selectedColor
                        }
                        "secondary" -> {
                            hiveToUpdate.secondaryColor = selectedColor
                        }
                    }
                    writeHivesToJson(hives)
                    loadHives()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        hiveListRecyclerView = findViewById(R.id.hiveListRecyclerView)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
        hiveListRecyclerView.layoutManager = LinearLayoutManager(this)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_general_notes -> {
                    val intent = Intent(this, HiveInfoActivity::class.java).apply {
                        putExtra("TYPE", "general")
                        putExtra("TITLE", "Загальні записи")
                    }
                    startActivity(intent)
                }
                R.id.nav_search -> {
                    val intent = Intent(this, SearchActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_sync -> {
                    showSyncOptionsDialog()
                }
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_add_hive -> {
                    showAddHiveDialog()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        val generalNotesBtn: MaterialButton = findViewById(R.id.generalNotesBtn)

        loadHives()

        generalNotesBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "general")
                putExtra("TITLE", "Загальні записи")
                putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи")
            }
            startActivity(intent)
        }
    }

    private fun showAddHiveDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Додати новий вулик")

        val input = EditText(this)
        input.hint = "Назва вулика"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Додати") { dialog, _ ->
            val hiveName = input.text.toString().trim()
            if (hiveName.isNotEmpty()) {
                val hives = readHivesFromJson()
                val newHiveNumber = (hives.maxByOrNull { it.number }?.number ?: 0) + 1
                hives.add(HiveData(
                    number = newHiveNumber,
                    name = hiveName,
                    color = R.color.hive_button_color,
                    queenButtonColor = R.color.nav_button_color,
                    notesButtonColor = R.color.nav_button_color,
                    secondaryColor = android.R.color.transparent
                ))
                writeHivesToJson(hives)
                loadHives()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Назва вулика не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Скасувати") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun loadHives() {
        val hives = readHivesFromJson()

        if (hives.isEmpty()) {
            for (i in 1..30) {
                hives.add(HiveData(
                    number = i,
                    name = "Вулик №$i",
                    color = R.color.hive_button_color,
                    queenButtonColor = R.color.nav_button_color,
                    notesButtonColor = R.color.nav_button_color,
                    secondaryColor = android.R.color.transparent
                ))
            }
            writeHivesToJson(hives)
        }

        hiveCountTextView.text = getString(R.string.hive_count, hives.size)

        hiveAdapter = HiveAdapter(hives, this) { position ->
            showHiveOptionsDialog(hives[position])
        }
        hiveListRecyclerView.adapter = hiveAdapter
    }

    private fun readBackupDataFromFile(fileUri: Uri): String? {
        return try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання файлу: ${e.message}", e)
            null
        }
    }

    private fun readHivesFromJson(): MutableList<HiveData> {
        val file = getHivesFile()
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
        val file = getHivesFile()
        try {
            FileWriter(file).use { writer ->
                gson.toJson(hives, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка запису вуликів до файлу: ${e.message}", e)
        }
    }

    private fun getHivesFile(): File {
        return File(filesDir, HIVE_DATA_FILE_NAME)
    }

    private fun showSyncOptionsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sync_options, null)
        val dialogTitleTextView: TextView = dialogView.findViewById(R.id.dialogTitle)
        val backupCard: MaterialCardView = dialogView.findViewById(R.id.backupCard)
        val restoreCard: MaterialCardView = dialogView.findViewById(R.id.restoreCard)
        val exportCsvCard: MaterialCardView = dialogView.findViewById(R.id.exportCsvCard)

        dialogTitleTextView.text = "Опції синхронізації"

        val dialog = AlertDialog.Builder(this)
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

    private fun writeBackupDataToFile(uri: android.net.Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    val hives = readHivesFromJson()
                    val notes = readAllNotesFromJson()
                    val backupData = BackupData(hives, notes)
                    gson.toJson(backupData, writer)
                }
            }
            Toast.makeText(this, "Резервну копію успішно створено!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при створенні резервної копії: ${e.message}", e)
            Toast.makeText(this, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreDataLocally() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_FILE_MIME_TYPE
        }
        openBackupFileLauncher.launch(intent)
    }

    private fun readAndRestoreBackupDataFromFile(fileUri: android.net.Uri) {
        try {
            contentResolver.openInputStream(fileUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
                    val type = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData? = gson.fromJson(reader, type)

                    if (backupData != null) {
                        val hiveListFromFile = backupData.hiveList
                        val existingHiveList = readHivesFromJson()
                        val combinedHiveList = (existingHiveList + hiveListFromFile).distinctBy { it.number }.toMutableList()
                        writeHivesToJson(combinedHiveList)
                        loadHives()

                        val notesFromFile = backupData.notes
                        val existingNotes = readAllNotesFromJson()
                        val combinedNotes = (existingNotes + notesFromFile).distinctBy { it.id }.toMutableList()
                        writeAllNotesToJson(combinedNotes)

                        Toast.makeText(this, "Дані успішно відновлено!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Не вдалося розібрати дані резервної копії.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при відновленні даних: ${e.message}", e)
            Toast.makeText(this, "Помилка при відновленні: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun readAllNotesFromJson(): MutableList<Note> {
        val file = File(filesDir, NOTES_FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання нотаток з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    private fun writeAllNotesToJson(notes: List<Note>) {
        val file = File(filesDir, NOTES_FILE_NAME)
        try {
            FileWriter(file).use { writer ->
                gson.toJson(notes, writer)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка запису нотаток до файлу: ${e.message}", e)
        }
    }

    private fun formatNoteToCsvRow(note: Note): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date(note.timestamp))
        val escapedText = note.text.replace("\"", "\"\"").replace("\n", " ").trim()
        val hiveNumber = note.hiveNumber
        return "${note.id},\"$dateString\",\"$escapedText\",\"${note.type}\",\"$hiveNumber\",\"${note.timestamp}\""
    }

    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        pickFolderLauncher.launch(intent)
    }

    private fun exportNotesToCsvFiles(folderUri: android.net.Uri) {
        Log.d(TAG, "exportNotesToCsvFiles: Початок експорту даних у папку: $folderUri")
        val allNotes = readAllNotesFromJson()

        if (allNotes.isEmpty()) {
            Toast.makeText(this, "Немає записів для експорту.", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "exportNotesToCsvFiles: Немає записів для експорту.")
            return
        }

        val baseFolder = DocumentFile.fromTreeUri(this, folderUri)
        if (baseFolder == null || !baseFolder.exists() || !baseFolder.isDirectory) {
            Toast.makeText(this, "Обрана папка недійсна або не існує.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "exportNotesToCsvFiles: Обрана папка недійсна або не існує: $folderUri")
            return
        }

        var totalExportedNotes = 0
        val csvHeader = "ID,Дата,Текст,Тип,Номер Вулика,Мітка Часу\n"
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
                    Toast.makeText(this, "Не вдалося створити підпапку: $subfolderName", Toast.LENGTH_SHORT).show()
                    return@forEach
                }
                val csvFile = targetSubFolder.createFile("text/csv", fileName)
                if (csvFile == null) {
                    Log.e(TAG, "exportNotesToCsvFiles: Не вдалося створити файл: $fileName у $subfolderName")
                    Toast.makeText(this, "Не вдалося створити файл: $fileName", Toast.LENGTH_SHORT).show()
                    return@forEach
                }
                contentResolver.openOutputStream(csvFile.uri, "w")?.use { outputStream ->
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
                Toast.makeText(this, "Помилка при записі у $subfolderName/$fileName: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        if (totalExportedNotes > 0) {
            Toast.makeText(this, "Експорт завершено. Експортовано $totalExportedNotes записів.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Немає записів для експорту.", Toast.LENGTH_LONG).show()
        }
        Log.d(TAG, "exportNotesToCsvFiles: Експорт завершено. Загальна кількість записів: $totalExportedNotes")
    }

    private fun showHiveOptionsDialog(hive: HiveData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_hive_options, null)
        val dialogTitleTextView: TextView = dialogView.findViewById(R.id.dialogTitle)
        val editNameCard: MaterialCardView = dialogView.findViewById(R.id.editNameCard)
        val selectPrimaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectPrimaryColorCard)
        val selectSecondaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectSecondaryColorCard)
        val deleteHiveCard: MaterialCardView = dialogView.findViewById(R.id.deleteHiveCard)

        dialogTitleTextView.text = "Опції для ${hive.name}"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        editNameCard.setOnClickListener {
            dialog.dismiss()
            showEditHiveNameDialog(hive)
        }
        selectPrimaryColorCard.setOnClickListener {
            dialog.dismiss()
            openColorPicker(hive.number, "primary")
        }
        selectSecondaryColorCard.setOnClickListener {
            dialog.dismiss()
            openColorPicker(hive.number, "secondary")
        }
        deleteHiveCard.setOnClickListener {
            dialog.dismiss()
            showDeleteHiveDialog(hive)
        }

        dialog.show()
    }

    private fun showEditHiveNameDialog(hive: HiveData) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Редагувати назву вулика")

        val input = EditText(this)
        input.setText(hive.name)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("Зберегти") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty() && newName != hive.name) {
                val hives = readHivesFromJson()
                val hiveToUpdate = hives.find { it.number == hive.number }
                if (hiveToUpdate != null) {
                    hiveToUpdate.name = newName
                    writeHivesToJson(hives)
                    loadHives()
                }
            } else {
                Toast.makeText(this, "Назва не може бути порожньою", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Скасувати") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    private fun showDeleteHiveDialog(hive: HiveData) {
        AlertDialog.Builder(this)
            .setTitle("Видалити вулик")
            .setMessage("Ви впевнені, що хочете видалити вулик №${hive.number} (${hive.name})? Всі пов'язані з ним записи також будуть видалені.")
            .setPositiveButton("Видалити") { dialog, _ ->
                val hives = readHivesFromJson()
                val notes = readAllNotesFromJson()

                val updatedHives = hives.filter { it.number != hive.number }.toMutableList()
                writeHivesToJson(updatedHives)
                loadHives()

                val updatedNotes = notes.filter { it.hiveNumber != hive.number }.toMutableList()
                writeAllNotesToJson(updatedNotes)

                Toast.makeText(this, "Вулик ${hive.name} видалено.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun openColorPicker(hiveNumber: Int, colorType: String) {
        val intent = Intent(this, ColorPickerActivity::class.java).apply {
            putExtra("hive_number", hiveNumber)
            putExtra("color_type", colorType)
        }
        colorPickerLauncher.launch(intent)
    }
}
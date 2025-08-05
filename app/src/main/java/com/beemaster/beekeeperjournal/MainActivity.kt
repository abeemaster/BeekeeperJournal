package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import com.google.android.material.card.MaterialCardView
import android.widget.TextView
import android.widget.GridLayout
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.Color
import android.content.res.Resources
import android.view.Gravity // Import Gravity
import com.google.android.material.button.MaterialButton
import android.widget.ImageButton

// Клас для структури файлу резервної копії
data class BackupData(
    val hiveList: List<HiveData>,
    val notes: List<Note>
)

class MainActivity : AppCompatActivity() {

    private lateinit var hiveButtonsContainer: LinearLayout
    private var hiveList: MutableList<HiveData> = mutableListOf()
    private val TAG = "MainActivity"

    private val gson = Gson()
    private val NOTES_FILE_NAME = "notes.json"



    companion object {
        const val EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE = "com.beemaster.beekeeperjournal.HIVE_NUMBER_FOR_COLOR_UPDATE"
        const val HIVE_DATA_FILE_NAME = "hives.json" // Додано константу для імені файлу вуликів
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME" // Додано константу для передачі назви вулика
        private const val BACKUP_FILE_MIME_TYPE = "application/json"
        private const val BACKUP_FILE_EXTENSION = ".json"
        const val PREFS_NAME = "BeekeeperJournalPrefs"
        const val KEY_HIVE_LIST = "hive_list"
    }

    // Launcher для вибору папки для експорту CSV
    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "pickFolderLauncher: Результат отримано. Код: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d(TAG, "pickFolderLauncher: URI папки: $uri. Спроба обробки...")
                Toast.makeText(this, "Папку обрано: $uri. Спроба експорту...", Toast.LENGTH_LONG).show()

                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                    Log.d(TAG, "pickFolderLauncher: Дозвіл на URI папки отримано. Виклик exportNotesToCsvFiles.")
                    exportNotesToCsvFiles(uri)
                } catch (e: SecurityException) {
                    Log.e(TAG, "pickFolderLauncher: Помилка дозволу на папку: ${e.message}", e)
                    Toast.makeText(this, "Помилка дозволу на папку: ${e.message}. Переконайтеся, що ви надали доступ.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(TAG, "pickFolderLauncher: Невідома помилка при отриманні дозволу для папки: ${e.message}", e)
                    Toast.makeText(this, "Невідома помилка при отриманні дозволу для папки: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.w(TAG, "pickFolderLauncher: URI папки порожній після вибору.")
                Toast.makeText(this, "Папку обрано, але URI порожній. Спробуйте іншу папку.", Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.d(TAG, "pickFolderLauncher: Вибір папки скасовано користувачем.")
            Toast.makeText(this, "Вибір папки скасовано користувачем.", Toast.LENGTH_SHORT).show()
        } else {
            Log.e(TAG, "pickFolderLauncher: Вибір папки завершився з невідомою помилкою. Код: ${result.resultCode}")
            Toast.makeText(this, "Вибір папки завершився з невідомою помилкою (Код: ${result.resultCode}).", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher для створення файлу резервної копії
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

    // Launcher для відкриття файлу резервної копії
    private val openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                readBackupDataFromFile(uri)
            } else {
                Toast.makeText(this, "Не вдалося відкрити файл резервної копії.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Відновлення скасовано.", Toast.LENGTH_SHORT).show()
        }
    }

    private val hiveInfoActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val hiveNumber = data?.getIntExtra(EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE, -1)
            val queenColor = data?.getIntExtra(HiveInfoActivity.RESULT_QUEEN_BUTTON_COLOR, -1)
            val notesColor = data?.getIntExtra(HiveInfoActivity.RESULT_NOTES_BUTTON_COLOR, -1)
            val updatedHiveName = data?.getStringExtra(HiveInfoActivity.EXTRA_HIVE_NAME) // Отримуємо оновлену назву

            if (hiveNumber != -1) {
                val index = hiveList.indexOfFirst { it.number == hiveNumber }
                if (index != -1) {
                    var currentHiveData = hiveList[index] // Отримуємо поточні дані вулика

                    // Оновлюємо mutable властивості
                    if (queenColor != -1) {
                        currentHiveData.queenButtonColor = queenColor!!
                    }
                    if (notesColor != -1) {
                        currentHiveData.notesButtonColor = notesColor!!
                    }

                    // Якщо назва оновлена, створюємо нову копію об'єкта HiveData з новою назвою
                    if (updatedHiveName != null) {
                        currentHiveData = currentHiveData.copy(name = updatedHiveName)
                        // Логуємо, якщо HiveInfoActivity повернула оновлену назву
                        Log.d(TAG, "MainActivity: HiveInfoActivity returned updated name: ${currentHiveData.name} for hive number ${currentHiveData.number}")
                    } else {
                        Log.d(TAG, "MainActivity: HiveInfoActivity did NOT return an updated name for hive number ${currentHiveData.number}. Only colors might have changed.")
                    }

                    hiveList[index] = currentHiveData // Замінюємо старий об'єкт оновленим (або новою копією)
                    saveHiveList() // Зберігаємо зміни
                    createHiveButtons() // Оновлюємо UI
                    // Фінальний лог після оновлення та збереження
                    Log.d(TAG, "MainActivity: Hive data for ${currentHiveData.name} (number ${currentHiveData.number}) updated and saved from HiveInfoActivity result.")

                    // *** ВИДАЛІТЬ ЦІ РЯДКИ, ЯКЩО ВОНИ ДУБЛЮЮТЬСЯ У ВАШОМУ КОДІ ***
                    // saveHiveList() // Save the updated list to file
                    // createHiveButtons() // Refresh UI
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val generalNotesBtn: MaterialButton = findViewById(R.id.generalNotesBtn) // Змінено на MaterialButton
        val searchBtn: ImageButton = findViewById(R.id.searchBtn) // Нова кнопка пошуку
        hiveButtonsContainer = findViewById(R.id.hiveButtonsContainer)
        val addHiveBtn: MaterialButton = findViewById(R.id.addHiveBtn) // Змінено на MaterialButton
        val syncBtn: MaterialButton = findViewById(R.id.syncBtn) // Змінено на MaterialButton


        loadHiveList()
        if (hiveList.isEmpty()) {
            for (i in 1..30) {
                hiveList.add(HiveData(
                    number = i,
                    name = "Вулик №$i",
                    color = R.color.hive_button_color,
                    queenButtonColor = R.color.nav_button_color,
                    notesButtonColor = R.color.nav_button_color,
                    secondaryColor = android.R.color.transparent
                ))
            }
            saveHiveList()
        }

        createHiveButtons() // Створюємо кнопки після завантаження/ініціалізації списку

        generalNotesBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "general")
                putExtra("TITLE", "Загальні записи")
                putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, "Загальні записи") // Передаємо назву для загальних записів
            }
            startActivity(intent)
        }

        // Обробник натискання для кнопки пошуку
        searchBtn.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        addHiveBtn.setOnClickListener {
            addNewHive()
        }

        syncBtn.setOnClickListener {
            showSyncOptionsDialog()
        }
    }

    // Діалог для вибору опцій синхронізації (резервне копіювання / відновлення)
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

    // Метод для створення локальної резервної копії
    private fun backupDataLocally() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "beekeeper_backup_${timestamp}${BACKUP_FILE_EXTENSION}"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_FILE_MIME_TYPE
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createBackupFileLauncher.launch(intent)
    }

    // Метод для запису даних резервної копії у вибраний файл
    private fun writeBackupDataToFile(uri: android.net.Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
                    val allNotes = readAllNotesFromJson()
                    val backupData = BackupData(hiveList, allNotes)
                    gson.toJson(backupData, writer)
                    Toast.makeText(this, "Резервна копія успішно створена!", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "writeBackupDataToFile: Резервна копія успішно збережена за URI: $uri")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при записі резервної копії: ${e.message}", e)
            Toast.makeText(this, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Метод для ініціації відновлення з локальної резервної копії
    private fun restoreDataLocally() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = BACKUP_FILE_MIME_TYPE
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(BACKUP_FILE_MIME_TYPE))
        }
        openBackupFileLauncher.launch(intent)
    }

    // Метод для читання даних резервної копії з вибраного файлу та їх відновлення
    private fun readBackupDataFromFile(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<BackupData>() {}.type
                    val backupData: BackupData = gson.fromJson(reader, type)

                    hiveList.clear()
                    hiveList.addAll(backupData.hiveList)
                    Log.d(TAG, "MainActivity: Restored hive list from backup. Checking first few hives:")
                    backupData.hiveList.take(5).forEach { hive -> // Перевіримо перші 5 вуликів для прикладу
                        Log.d(TAG, "  Restored Hive: Number=${hive.number}, Name=${hive.name}")
                    }
                    saveHiveList()

                    writeAllNotesToJson(backupData.notes)

                    createHiveButtons() // Оновлюємо кнопки після відновлення
                    Toast.makeText(this, "Дані успішно відновлено з резервної копії!", Toast.LENGTH_LONG).show()
                    Log.d(TAG, "readBackupDataFromFile: Дані успішно відновлено з URI: $uri")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка при читанні/відновленні резервної копії: ${e.message}", e)
            Toast.makeText(this, "Помилка при відновленні даних: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadHiveList() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HIVE_LIST, null)
        if (json != null) {
            val type = object : TypeToken<MutableList<HiveData>>() {}.type
            hiveList = gson.fromJson(json, type) ?: mutableListOf()
            Log.d(TAG, "loadHiveList: Завантажено ${hiveList.size} вуликів.")

            var dataCorrected = false
            hiveList.forEach { hive ->
                if (hive.color == 0) {
                    hive.color = R.color.hive_button_color
                    Log.w(TAG, "loadHiveList: Колір вулика №${hive.number} був 0, виправлено на R.color.hive_button_color")
                    dataCorrected = true
                }
            }
            if (dataCorrected) {
                saveHiveList()
                Log.d(TAG, "loadHiveList: Виправлені дані вуликів збережено.")
            }

        } else {
            hiveList = mutableListOf()
            Log.d(TAG, "loadHiveList: Список вуликів порожній у SharedPreferences.")
        }
    }




    private fun saveHiveList() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(hiveList)
        editor.putString(KEY_HIVE_LIST, json)
        editor.apply()
        // Логуємо імена перших 5 вуликів, які були збережені
        Log.d(TAG, "saveHiveList: Збережено ${hiveList.size} вуликів. JSON for first 5 hives: ${hiveList.take(5).joinToString { it.name }}")
        Log.d(TAG, "saveHiveList: Збережено ${hiveList.size} вуликів.")
    }

    private fun addNewHive() {
        // Отримуємо наступний доступний номер, якщо список порожній, то 1, інакше max + 1
        val nextHiveNumber = if (hiveList.isEmpty()) 1 else hiveList.maxOf { it.number } + 1
        val newHive = HiveData(
            number = nextHiveNumber,
            name = "Вулик №$nextHiveNumber",
            color = R.color.hive_button_color,
            queenButtonColor = R.color.nav_button_color,
            notesButtonColor = R.color.nav_button_color,
            secondaryColor = android.R.color.transparent
        )
        hiveList.add(newHive)
        sortHiveListCustom() // Сортуємо список після додавання
        saveHiveList()
        createHiveButtons()
        Toast.makeText(this, getString(R.string.hive_added_message, nextHiveNumber), Toast.LENGTH_SHORT).show()

        // Прокрутка до нового вулика
        hiveButtonsContainer.post {
            val lastChild = hiveButtonsContainer.getChildAt(hiveButtonsContainer.childCount - 1)
            if (lastChild != null) {
                val scrollView = hiveButtonsContainer.parent as? View
                scrollView?.post {
                    scrollView.scrollTo(0, lastChild.bottom)
                }
            }
        }
    }

    private fun updateHiveData(updatedHive: HiveData) {
        // Логуємо дані, які надходять до функції оновлення
        Log.d(TAG, "MainActivity: updateHiveData called for Number=${updatedHive.number}, New Name=${updatedHive.name}")
        val index = hiveList.indexOfFirst { it.number == updatedHive.number }
        if (index != -1) {
            hiveList[index] = updatedHive // Оновлюємо елемент у списку
            sortHiveListCustom()
            saveHiveList() // Зберігаємо оновлений список
            // Логуємо стан після збереження
            Log.d(TAG, "MainActivity: hiveList updated and saveHiveList() called. Updated hive in list: ${hiveList[index].name}")
            createHiveButtons() // Оновлюємо кнопки
            Toast.makeText(this, "Вулик ${updatedHive.name} оновлено.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Помилка: Вулик №${updatedHive.number} не знайдено для оновлення.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteHive(hiveData: HiveData) {
        AlertDialog.Builder(this)
            .setTitle("Видалити ${hiveData.name}")
            .setMessage("Ви впевнені, що хочете видалити ${hiveData.name} та ВСІ пов'язані з ним записи (інформація, матка, примітки)? Цю дію неможливо скасувати.")
            .setPositiveButton("Видалити") { dialog, _ ->
                val removed = hiveList.remove(hiveData)
                if (removed) {
                    saveHiveList()

                    val allNotes = readAllNotesFromJson()
                    val notesToRemove = allNotes.filter { it.hiveNumber == hiveData.number && it.type != "general" }
                    val updatedNotes = allNotes.toMutableList()
                    updatedNotes.removeAll(notesToRemove)
                    writeAllNotesToJson(updatedNotes)

                    createHiveButtons() // Оновлюємо кнопки після видалення
                    Toast.makeText(this, "${hiveData.name} та його записи видалено.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Помилка: ${hiveData.name} не знайдено для видалення.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                Toast.makeText(this, "Видалення скасовано.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Сортує список вуликів за їх назвою (номером та літерою) у природному порядку.
     * Наприклад: "Вулик №1", "Вулик №2", ..., "Вулик №10", "Вулик №10а", "Вулик №11".
     */
    private fun sortHiveListCustom() {
        hiveList.sortWith(Comparator { hive1, hive2 ->
            val name1 = hive1.name
            val name2 = hive2.name

            // Витягуємо числову частину та літерний суфікс
            val regex = Regex("Вулик №(\\d+)([а-я]*)")
            val match1 = regex.find(name1)
            val match2 = regex.find(name2)

            if (match1 != null && match2 != null) {
                val num1 = match1.groupValues[1].toInt()
                val suffix1 = match1.groupValues[2]
                val num2 = match2.groupValues[1].toInt()
                val suffix2 = match2.groupValues[2]

                // Спочатку порівнюємо числові частини
                val numComparison = num1.compareTo(num2)
                if (numComparison != 0) {
                    return@Comparator numComparison
                }

                // Якщо числові частини однакові, порівнюємо літерні суфікси
                return@Comparator suffix2.compareTo(suffix1) // Змінено на suffix2.compareTo(suffix1) для правильного сортування "а", "б"
            }
            // Якщо формат назви не відповідає очікуваному, використовуємо стандартне порівняння рядків
            name1.compareTo(name2)
        })
    }


    private fun createHiveButtons() {
        hiveButtonsContainer.removeAllViews()
        sortHiveListCustom() // Завжди сортуємо перед відображенням

        for (hiveData in hiveList) {
            val button = Button(this).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    resources.getDimensionPixelSize(R.dimen.main_button_height)
                ).apply {
                    bottomMargin = resources.getDimensionPixelSize(R.dimen.button_margin_bottom)
                }
                text = hiveData.name
                textSize = resources.getDimension(R.dimen.hive_button_text_size) / resources.displayMetrics.density
                stateListAnimator = null
                elevation = resources.getDimension(R.dimen.button_elevation)
                tag = hiveData.number

                val textColor = if (hiveData.color == R.color.color_yellow || hiveData.color == R.color.white) {
                    ContextCompat.getColor(context, R.color.black)
                } else {
                    ContextCompat.getColor(context, android.R.color.white)
                }
                setTextColor(textColor)

                setPadding(0, resources.getDimensionPixelSize(R.dimen.button_padding_vertical),
                    0, resources.getDimensionPixelSize(R.dimen.button_padding_vertical))

                // Основний фон кнопки
                val mainShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.button_corner_radius)
                    var colorToSet: Int
                    if (hiveData.color == 0) {
                        colorToSet = Color.parseColor("#65A30D")
                    } else {
                        try {
                            colorToSet = ContextCompat.getColor(context, hiveData.color)
                        } catch (e: Resources.NotFoundException) {
                            colorToSet = Color.parseColor("#65A30D")
                        }
                    }
                    setColor(colorToSet)
                }

                // Додатковий кольоровий маркер (коло)
                val secondaryMarkerDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL // Встановлюємо форму кола
                    var secondaryColorToSet: Int
                    if (hiveData.secondaryColor == android.R.color.transparent) {
                        secondaryColorToSet = Color.TRANSPARENT
                    } else {
                        try {
                            secondaryColorToSet = ContextCompat.getColor(context, hiveData.secondaryColor)
                        } catch (e: Resources.NotFoundException) {
                            secondaryColorToSet = Color.TRANSPARENT
                        }
                    }
                    setColor(secondaryColorToSet)
                }

                // Рамка помаранчевого кольору
                val borderShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.button_corner_radius)
                    setColor(Color.TRANSPARENT)
                    setStroke(resources.getDimensionPixelSize(R.dimen.hive_button_border_width),
                        ContextCompat.getColor(context, R.color.color_orange))
                }

                // Об'єднуємо всі шари: рамка, основний фон, вторинний маркер
                // Порядок шарів важливий: рамка знизу, потім основний фон, потім маркер
                val layers = arrayOf(borderShapeDrawable, mainShapeDrawable, secondaryMarkerDrawable)
                background = LayerDrawable(layers)

                // Встановлення інсетів для mainShapeDrawable
                (background as LayerDrawable).setLayerInset(1,
                    resources.getDimensionPixelSize(R.dimen.hive_button_border_width), // left
                    resources.getDimensionPixelSize(R.dimen.hive_button_border_width), // top
                    resources.getDimensionPixelSize(R.dimen.hive_button_border_width), // right
                    resources.getDimensionPixelSize(R.dimen.hive_button_border_width)) // bottom

                // Встановлення розміру та гравітації для вторинного маркера
                val markerSize = resources.getDimensionPixelSize(R.dimen.secondary_marker_size)
                (background as LayerDrawable).setLayerSize(2, markerSize, markerSize) // Встановлюємо фіксований розмір маркера
                (background as LayerDrawable).setLayerGravity(2, Gravity.START or Gravity.CENTER_VERTICAL) // Вирівнюємо по лівому краю та вертикально по центру

                // Додаємо невеликий лівий інсет для маркера, щоб відсунути його від самого краю кнопки
                val horizontalInset = resources.getDimensionPixelSize(R.dimen.spacing_small)
                // Інсети: left, top, right, bottom. Для гравітації та розміру, нам потрібен лише лівий відступ.
                // Інші інсети (top, right, bottom) залишаємо 0, оскільки їх позиціонування вже визначено Gravity та Size.
                (background as LayerDrawable).setLayerInset(2, horizontalInset, 0, 0, 0)
            }

            button.setOnClickListener {
                val intent = Intent(this@MainActivity, HiveInfoActivity::class.java).apply {
                    putExtra("TYPE", "hive")
                    putExtra("HIVE_NUMBER", hiveData.number)
                    putExtra("TITLE", hiveData.name)
                    putExtra(HiveInfoActivity.EXTRA_HIVE_NAME, hiveData.name) // Передаємо актуальну назву вулика
                    putExtra(HiveInfoActivity.EXTRA_QUEEN_BUTTON_COLOR, hiveData.queenButtonColor)
                    putExtra(HiveInfoActivity.EXTRA_NOTES_BUTTON_COLOR, hiveData.notesButtonColor)
                }
                hiveInfoActivityResultLauncher.launch(intent)
            }

            button.setOnLongClickListener {
                showHiveOptionsDialog(hiveData)
                true
            }

            hiveButtonsContainer.addView(button)
        }
    }

    private fun showHiveOptionsDialog(hiveData: HiveData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_hive_options, null)
        val dialogTitleTextView: TextView = dialogView.findViewById(R.id.dialogTitle)
        val editNameCard: MaterialCardView = dialogView.findViewById(R.id.editNameCard)
        val selectColorCard: MaterialCardView = dialogView.findViewById(R.id.selectColorCard)
        val deleteHiveCard: MaterialCardView = dialogView.findViewById(R.id.deleteHiveCard)
        val selectSecondaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectSecondaryColorCard)

        val selectSecondaryColorTextView: TextView = dialogView.findViewById(R.id.selectSecondaryColorTextView)
        selectSecondaryColorTextView.text = "Вибрати додатковий колір"


        dialogTitleTextView.text = "Опції для ${hiveData.name}" // Використовуємо hiveData.name тут

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        editNameCard.setOnClickListener {
            dialog.dismiss()
            showEditHiveNameDialog(hiveData)
        }

        selectColorCard.setOnClickListener {
            dialog.dismiss()
            showColorPickerDialog(hiveData, "main")
        }

        selectSecondaryColorCard.setOnClickListener {
            dialog.dismiss()
            showColorPickerDialog(hiveData, "secondary")
        }

        deleteHiveCard.setOnClickListener {
            dialog.dismiss()
            deleteHive(hiveData)
        }

        dialog.show()
    }

    private fun showEditHiveNameDialog(hiveData: HiveData) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(hiveData.name)
            setHint("Введіть нову назву")
        }

        AlertDialog.Builder(this)
            .setTitle("Редагувати назву ${hiveData.name}") // Використовуємо hiveData.name тут
            .setView(input)
            .setPositiveButton("Зберегти") { dialog, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    // Додаємо лог тут, щоб побачити нову назву, що вводиться
                    Log.d(TAG, "MainActivity: Renaming hive ${hiveData.number} from '${hiveData.name}' to '$newName'")
                    val updatedHive = hiveData.copy(name = newName)
                    updateHiveData(updatedHive) // Виклик updateHiveData для збереження та оновлення
                    Toast.makeText(this, "Вулик №${hiveData.number} перейменовано на '$newName'", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Назва вулика не може бути порожньою.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Відображає діалогове вікно для вибору кольору з кольоровими кружечками.
     * @param hiveData Дані вулика, для якого змінюється колір.
     * @param colorType Тип кольору, який змінюється ("main", "secondary", "queen", "notes").
     */
    private fun showColorPickerDialog(hiveData: HiveData, colorType: String) {
        val colorsResIds = arrayOf(
            R.color.hive_button_color,
            R.color.nav_button_color,
            R.color.color_red,
            R.color.color_blue,
            R.color.color_yellow,
            R.color.color_purple,
            R.color.color_orange,
            R.color.black,
            R.color.white,
            android.R.color.transparent
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val colorPickerDialogTitle: TextView = dialogView.findViewById(R.id.colorPickerDialogTitle)
        val colorGrid: GridLayout = dialogView.findViewById(R.id.colorGrid)

        colorPickerDialogTitle.text = when (colorType) {
            "main" -> "Вибрати основний колір для ${hiveData.name}" // Використовуємо hiveData.name
            "secondary" -> "Вибрати додатковий колір для ${hiveData.name}" // Використовуємо hiveData.name
            "queen" -> "Вибрати колір для кнопки 'Матка'"
            "notes" -> "Вибрати колір для кнопки 'Примітки'"
            else -> "Вибрати колір"
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val dialog = dialogBuilder.create()

        for (colorResId in colorsResIds) {
            val colorSwatch = View(this).apply {
                val size = resources.getDimensionPixelSize(R.dimen.color_swatch_size)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(8, 8, 8, 8)
                }
                val circleShape = ShapeDrawable(OvalShape()).apply {
                    paint.color = ContextCompat.getColor(this@MainActivity, colorResId)
                }
                val borderShape = ShapeDrawable(OvalShape()).apply {
                    paint.color = ContextCompat.getColor(this@MainActivity, R.color.note_border)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = resources.getDimension(R.dimen.color_swatch_border_width)
                }
                val layers = arrayOf(circleShape, borderShape)
                background = LayerDrawable(layers)

                val currentColorForType = when (colorType) {
                    "main" -> hiveData.color
                    "secondary" -> hiveData.secondaryColor
                    "queen" -> hiveData.queenButtonColor
                    "notes" -> hiveData.notesButtonColor
                    else -> -1
                }

                if (colorResId == currentColorForType) {
                    val selectedBorder = ShapeDrawable(OvalShape()).apply {
                        paint.color = ContextCompat.getColor(this@MainActivity, R.color.note_border_selected)
                        paint.style = android.graphics.Paint.Style.STROKE
                        paint.strokeWidth = resources.getDimension(R.dimen.color_swatch_selected_border_width)
                    }
                    val currentLayers = (background as LayerDrawable).numberOfLayers
                    val newLayers = arrayOfNulls<android.graphics.drawable.Drawable>(currentLayers + 1)
                    for (i in 0 until currentLayers) {
                        newLayers[i] = (background as LayerDrawable).getDrawable(i)
                    }
                    newLayers[currentLayers] = selectedBorder
                    background = LayerDrawable(newLayers)
                }


                setOnClickListener {
                    val updatedHive = when (colorType) {
                        "main" -> hiveData.copy(color = colorResId)
                        "secondary" -> hiveData.copy(secondaryColor = colorResId)
                        "queen" -> hiveData.copy(queenButtonColor = colorResId)
                        "notes" -> hiveData.copy(notesButtonColor = colorResId)
                        else -> hiveData
                    }
                    updateHiveData(updatedHive)
                    dialog.dismiss()
                }
            }
            colorGrid.addView(colorSwatch)
        }
        dialog.show()
    }


    private fun openFolderPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        }
        pickFolderLauncher.launch(intent)
    }

    private fun readAllNotesFromJson(): MutableList<Note> {
        val file = File(filesDir, NOTES_FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            Log.d(TAG, "readAllNotesFromJson: JSON файл не знайдено або він порожній.")
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання записів з JSON файлу: ${e.message}", e)
            Toast.makeText(this, "Помилка читання записів з файлу: ${e.message}", Toast.LENGTH_LONG).show()
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
            e.printStackTrace()
            Toast.makeText(this, "Помилка запису записів до файлу: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun formatNoteToCsvRow(note: Note): String {
        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(note.timestamp))

        val escapedText = note.text.replace("\"", "\"\"")
        val textForCsv = if (escapedText.contains(",") || escapedText.contains("\n") || escapedText.contains("\"")) {
            "\"$escapedText\""
        } else {
            escapedText
        }

        return "${note.id},${formattedDate},$textForCsv,${note.type},${note.hiveNumber},${note.timestamp}"
    }

    // ... (початок функції)
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

            // Оновлена логіка для визначення папок
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

}

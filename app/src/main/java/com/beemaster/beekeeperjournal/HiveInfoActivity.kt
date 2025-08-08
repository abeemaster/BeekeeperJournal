package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.widget.GridLayout
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import androidx.activity.OnBackPressedCallback
import com.beemaster.beekeeperjournal.NewNoteActivity
import android.graphics.Color
import android.os.Build
import android.content.res.ColorStateList

class HiveInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HiveInfoActivity"
        private const val REQUEST_CODE_NEW_NOTE = 1001
        private const val REQUEST_CODE_EDIT_NOTE = 1002

        const val EXTRA_QUEEN_BUTTON_COLOR = "com.beemaster.beekeeperjournal.QUEEN_BUTTON_COLOR"
        const val EXTRA_NOTES_BUTTON_COLOR = "com.beemaster.beekeeperjournal.NOTES_BUTTON_COLOR"
        const val RESULT_QUEEN_BUTTON_COLOR = "com.beemaster.beekeeperjournal.RESULT_QUEEN_BUTTON_COLOR"
        const val RESULT_NOTES_BUTTON_COLOR = "com.beemaster.beekeeperjournal.RESULT_NOTES_BUTTON_COLOR"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME" // Константа для передачі назви вулика
    }

    private lateinit var infoTitle: TextView
    private lateinit var notesDisplayArea: LinearLayout
    private lateinit var microphoneBtn: ImageButton
    private lateinit var newNoteButton: com.google.android.material.button.MaterialButton
    private lateinit var queenBtn: Button
    private lateinit var notesBtn: Button

    private var currentHiveNumber: Int = 0
    private lateinit var currentHiveActualName: String // Зберігаємо актуальну назву вулика
    private var currentEntryType: String = ""

    private var currentQueenButtonColor: Int = 0
    private var currentNotesButtonColor: Int = 0

    private val gson = Gson()
    private val NOTES_FILE_NAME = "notes.json"

    private var currentlyVisibleActionsLayout: LinearLayout? = null
    private var currentlySelectedNoteItem: LinearLayout? = null

    private lateinit var newNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var editNoteActivityResultLauncher: ActivityResultLauncher<Intent>




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)

        // Ініціалізація змінних та отримання даних з Intent

        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        notesBtn = findViewById(R.id.notesBtn)

        currentEntryType = intent.getStringExtra("TYPE") ?: "hive"
        val titleFromIntent = intent.getStringExtra("TITLE")
        currentHiveNumber = intent.getIntExtra("EXTRA_HIVE_NUMBER", 0)
        currentQueenButtonColor = intent.getIntExtra(EXTRA_QUEEN_BUTTON_COLOR, R.color.nav_button_color)
        currentNotesButtonColor = intent.getIntExtra(EXTRA_NOTES_BUTTON_COLOR, R.color.nav_button_color)
        currentHiveActualName = intent.getStringExtra(EXTRA_HIVE_NAME) ?: "Вулик №$currentHiveNumber"

        // Встановлення заголовка екрану. Якщо titleFromIntent є, використовуємо його, інакше - актуальну назву.
        // infoTitle.text = titleFromIntent ?: currentHiveActualName
        if (titleFromIntent != null) {
            infoTitle.text = titleFromIntent
        } else {
            infoTitle.text = currentHiveActualName
        }

        if (currentEntryType == "queen" || currentEntryType == "notes") {
            queenBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
        } else {
            queenBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentQueenButtonColor))
            val queenShapeDrawable = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                setColor(ContextCompat.getColor(this@HiveInfoActivity, currentQueenButtonColor))
            }
            queenBtn.background = queenShapeDrawable

            notesBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentNotesButtonColor))
            val notesShapeDrawable = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                setColor(ContextCompat.getColor(this@HiveInfoActivity, currentNotesButtonColor))
            }
            notesBtn.background = notesShapeDrawable
            // Лог, щоб підтвердити, що кольори були встановлені
            Log.d("BeeDebug", "HiveInfoActivity: Кольори кнопок встановлено програмно.")
        }

        loadNotes()

        newNoteActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val noteText = data?.getStringExtra(NewNoteActivity.EXTRA_NOTE_TEXT)
                val entryType = data?.getStringExtra(NewNoteActivity.EXTRA_ENTRY_TYPE)
                val hiveNumber = data?.getIntExtra(NewNoteActivity.EXTRA_HIVE_NUMBER, 0)

                if (noteText != null && entryType != null && hiveNumber != null) {
                    addNoteFromNewNoteActivity(noteText, entryType, hiveNumber)
                }
            }
        }

        editNoteActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val noteId = data?.getStringExtra(EditNoteActivity.EXTRA_NOTE_ID)
                val updatedNoteText = data?.getStringExtra(EditNoteActivity.EXTRA_UPDATED_NOTE_TEXT)
                // Оновлюємо currentHiveActualName, якщо він змінився в EditNoteActivity
                val updatedHiveName = data?.getStringExtra(EditNoteActivity.EXTRA_HIVE_NAME)
                if (updatedHiveName != null) {
                    currentHiveActualName = updatedHiveName
                    // Оновлюємо заголовок, якщо ми на головному екрані вулика (не на вкладках "Матка" чи "Примітки")
                    if (currentEntryType == "hive") {
                        infoTitle.text = currentHiveActualName
                    }
                }

                if (noteId != null && updatedNoteText != null) {
                    updateNoteFromEditNoteActivity(noteId, updatedNoteText)
                }
            }
        }

        newNoteButton.setOnClickListener {
            val intent = Intent(this, NewNoteActivity::class.java).apply {
                putExtra(NewNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
                putExtra(NewNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
                putExtra(NewNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
                putExtra("TITLE", infoTitle.text.toString())
            }
            newNoteActivityResultLauncher.launch(intent)
        }

        microphoneBtn.setOnClickListener {
            val intent = Intent(this, NewNoteActivity::class.java).apply {
                putExtra(NewNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
                putExtra(NewNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
                putExtra(NewNoteActivity.EXTRA_START_VOICE_INPUT, true) // Передаємо прапорець для негайного запуску
                putExtra(NewNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
                putExtra("TITLE", infoTitle.text.toString())
            }
            newNoteActivityResultLauncher.launch(intent)
        }

        queenBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "queen")
                putExtra("HIVE_NUMBER", currentHiveNumber)
                putExtra(EXTRA_HIVE_NAME, "Матка " + currentHiveActualName) // Передаємо актуальну назву
                putExtra("TITLE", "Матка $currentHiveActualName") // Формуємо заголовок з актуальної назви
                putExtra(EXTRA_QUEEN_BUTTON_COLOR, currentQueenButtonColor)
                putExtra(EXTRA_NOTES_BUTTON_COLOR, currentNotesButtonColor)
            }
            startActivity(intent)
        }

        notesBtn.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "notes")
                putExtra("HIVE_NUMBER", currentHiveNumber)
                putExtra(EXTRA_HIVE_NAME, "Примітки " + currentHiveActualName) // Передаємо актуальну назву
                putExtra("TITLE", "Примітки $currentHiveActualName") // Формуємо заголовок з актуальної назви
                putExtra(EXTRA_QUEEN_BUTTON_COLOR, currentQueenButtonColor)
                putExtra(EXTRA_NOTES_BUTTON_COLOR, currentNotesButtonColor)
            }
            startActivity(intent)
        }

        queenBtn.setOnLongClickListener {
            showColorPickerDialogForNavButton("Матка", currentQueenButtonColor) { selectedColorResId ->
                currentQueenButtonColor = selectedColorResId
                queenBtn.backgroundTintList = ContextCompat.getColorStateList(this, currentQueenButtonColor)
                val queenShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                    setColor(ContextCompat.getColor(this@HiveInfoActivity, currentQueenButtonColor))
                }
                queenBtn.background = queenShapeDrawable
                setResultAndFinish()
            }
            true
        }

        notesBtn.setOnLongClickListener {
            showColorPickerDialogForNavButton("Примітки", currentNotesButtonColor) { selectedColorResId ->
                currentNotesButtonColor = selectedColorResId
                notesBtn.backgroundTintList = ContextCompat.getColorStateList(this, currentNotesButtonColor)
                val notesShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                    setColor(ContextCompat.getColor(this@HiveInfoActivity, currentNotesButtonColor))
                }
                notesBtn.background = notesShapeDrawable
                setResultAndFinish()
            }
            true
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.d("BeeDebug", "HiveInfoActivity: onNewIntent() був викликаний. Активність оновлюється.")
        loadNotes()
    }
    private fun setResultAndFinish() {
        val resultIntent = Intent().apply {
            putExtra(RESULT_QUEEN_BUTTON_COLOR, currentQueenButtonColor)
            putExtra(RESULT_NOTES_BUTTON_COLOR, currentNotesButtonColor)
            putExtra(MainActivity.EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE, currentHiveNumber)
            putExtra(EXTRA_HIVE_NAME, currentHiveActualName) // Передаємо актуальну назву назад до MainActivity
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    private fun addNoteFromNewNoteActivity(noteText: String, entryType: String, hiveNumber: Int) {
        if (noteText.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())
            val timestamp = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()

            val newNote = Note(
                id = newId,
                date = formattedDate,
                text = noteText,
                type = entryType,
                hiveNumber = hiveNumber,
                timestamp = timestamp
            )

            val allNotes = readAllNotesFromJson()
            allNotes.add(newNote)
            writeAllNotesToJson(allNotes)

            Toast.makeText(this, "Запис додано!", Toast.LENGTH_SHORT).show()
            loadNotes()
        } else {
            Toast.makeText(this, "Будь ласка, введіть текст для запису.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNoteFromEditNoteActivity(noteId: String, updatedNoteText: String) {
        val allNotes = readAllNotesFromJson()
        val noteIndex = allNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            val updatedNote = allNotes[noteIndex].copy(
                text = updatedNoteText,
                timestamp = System.currentTimeMillis()
            )
            allNotes[noteIndex] = updatedNote
            writeAllNotesToJson(allNotes)
            Toast.makeText(this, "Запис оновлено!", Toast.LENGTH_SHORT).show()
            loadNotes()
        } else {
            Toast.makeText(this, "Помилка: Запис для оновлення не знайдено.", Toast.LENGTH_SHORT).show()
        }
        hideActionsAndResetBackground()
    }

    private fun getNotesFile(): File {
        return File(filesDir, NOTES_FILE_NAME)
    }

    private fun readAllNotesFromJson(): MutableList<Note> {
        val file = getNotesFile()
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Помилка читання записів з файлу: ${e.message}", Toast.LENGTH_LONG).show()
            mutableListOf()
        }
    }

    private fun writeAllNotesToJson(notes: List<Note>) {
        val file = getNotesFile()
        try {
            FileWriter(file).use { writer ->
                gson.toJson(notes, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Помилка запису записів до файлу: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadNotes() {
        val allNotes = readAllNotesFromJson()
        val filteredNotes = allNotes.filter { note ->
            if (currentEntryType == "general") {
                note.type == currentEntryType
            } else {
                note.type == currentEntryType && note.hiveNumber == currentHiveNumber
            }
        }.sortedByDescending { it.timestamp }

        notesDisplayArea.removeAllViews()

        if (filteredNotes.isEmpty()) {
            val noNotesTextView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                text = "Записів поки що немає."
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.note_date_text))
                setPadding(resources.getDimensionPixelSize(R.dimen.content_padding),
                    resources.getDimensionPixelSize(R.dimen.content_padding),
                    resources.getDimensionPixelSize(R.dimen.content_padding),
                    resources.getDimensionPixelSize(R.dimen.content_padding))
            }
            notesDisplayArea.addView(noNotesTextView)
        } else {
            for (note in filteredNotes) {
                createNoteItem(note)
            }
        }
    }

    private fun createNoteItem(note: Note) {
        val noteItem = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.note_item_background)
            setPadding(resources.getDimensionPixelSize(R.dimen.content_padding),
                resources.getDimensionPixelSize(R.dimen.content_padding),
                resources.getDimensionPixelSize(R.dimen.content_padding),
                resources.getDimensionPixelSize(R.dimen.content_padding))
            elevation = resources.getDimension(R.dimen.button_elevation)
            tag = note.id
        }

        val dateTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            this.text = note.date
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.note_date_text))
        }

        val noteTextView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            this.text = note.text
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.note_text_color))
        }

        val actionsLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            visibility = View.GONE
        }

        val editButton = createActionButton("Редагувати", R.color.edit_button_color) {
            val intent = Intent(this, EditNoteActivity::class.java).apply {
                putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
                putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
                putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
                putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
                putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName) // Передаємо актуальну назву
            }
            editNoteActivityResultLauncher.launch(intent)
        }
        val deleteButton = createActionButton("Видалити", R.color.delete_button_color) {
            deleteNote(it as Button)
        }

        actionsLayout.addView(editButton)
        actionsLayout.addView(deleteButton)

        noteItem.addView(dateTextView)
        noteItem.addView(noteTextView)
        noteItem.addView(actionsLayout)

        noteItem.setOnLongClickListener {
            if (currentlyVisibleActionsLayout != null && currentlyVisibleActionsLayout != actionsLayout) {
                currentlyVisibleActionsLayout?.visibility = View.GONE
                currentlySelectedNoteItem?.background = ContextCompat.getDrawable(this, R.drawable.note_item_background)
            }

            if (actionsLayout.visibility == View.VISIBLE) {
                actionsLayout.visibility = View.GONE
                noteItem.background = ContextCompat.getDrawable(this, R.drawable.note_item_background)
                currentlyVisibleActionsLayout = null
                currentlySelectedNoteItem = null
            } else {
                actionsLayout.visibility = View.VISIBLE
                noteItem.background = ContextCompat.getDrawable(this, R.drawable.note_item_background_selected)
                currentlyVisibleActionsLayout = actionsLayout
                currentlySelectedNoteItem = noteItem
            }
            true
        }

        noteItem.setOnClickListener {
            if (currentlyVisibleActionsLayout == actionsLayout && actionsLayout.visibility == View.VISIBLE) {
                hideActionsAndResetBackground()
                Toast.makeText(this, "Дії приховано.", Toast.LENGTH_SHORT).show()
            }
        }

        notesDisplayArea.addView(noteItem)
    }

    private fun hideActionsAndResetBackground() {
        currentlyVisibleActionsLayout?.visibility = View.GONE
        currentlySelectedNoteItem?.background = ContextCompat.getDrawable(this, R.drawable.note_item_background)
        currentlyVisibleActionsLayout = null
        currentlySelectedNoteItem = null
    }



    fun editNote(button: Button) {
        val noteItem = button.parent.parent as LinearLayout
        val noteId = noteItem.tag as String
        val noteTextView = noteItem.getChildAt(1) as TextView
        val originalText = noteTextView.text.toString()

        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(EditNoteActivity.EXTRA_NOTE_ID, noteId)
            putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, originalText)
            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName) // Передаємо актуальну назву
        }
        editNoteActivityResultLauncher.launch(intent)
    }

    fun deleteNote(button: Button) {
        val noteItem = button.parent.parent as LinearLayout
        val noteId = noteItem.tag as String

        AlertDialog.Builder(this)
            .setTitle("Видалити запис")
            .setMessage("Ви впевнені, що хочете видалити цей запис?")
            .setPositiveButton("Видалити") { dialog, _ ->
                val allNotes = readAllNotesFromJson()
                val initialSize = allNotes.size
                allNotes.removeIf { it.id == noteId }
                if (allNotes.size < initialSize) {
                    writeAllNotesToJson(allNotes)
                    Toast.makeText(this, "Запис видалено!", Toast.LENGTH_SHORT).show()
                    loadNotes()
                } else {
                    Toast.makeText(this, "Помилка: Запис не знайдено.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                hideActionsAndResetBackground()
            }
            .setNegativeButton("Скасувати") { dialog, _ ->
                Toast.makeText(this, "Видалення скасовано.", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                hideActionsAndResetBackground()
            }
            .show()
    }

    private fun createActionButton(text: String, colorResId: Int, onClickListener: (View) -> Unit): Button {
        val params = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.action_button_min_width),
            resources.getDimensionPixelSize(R.dimen.action_button_min_height)
        ).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_small)
        }

        return Button(this).apply {
            this.text = text
            textSize = resources.getDimension(R.dimen.action_button_text_size) / resources.displayMetrics.density
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            backgroundTintList = ContextCompat.getColorStateList(context, colorResId)
            setPadding(resources.getDimensionPixelSize(R.dimen.action_button_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.action_button_padding_vertical),
                resources.getDimensionPixelSize(R.dimen.action_button_padding_horizontal),
                resources.getDimensionPixelSize(R.dimen.action_button_padding_vertical))
            val shapeDrawable = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.action_button_corner_radius)
                setColor(ContextCompat.getColor(context, colorResId))
            }
            background = shapeDrawable
            elevation = resources.getDimension(R.dimen.button_elevation)
            stateListAnimator = null
            this.layoutParams = params
            setOnClickListener(onClickListener)
        }
    }

    /**
     * Відображає діалогове вікно для вибору кольору для кнопок "Матка" та "Примітки" з кольоровими кружечками.
     * @param dialogTitle Назва діалогу.
     * @param currentColorResId Поточний ID ресурсу кольору кнопки.
     * @param onColorSelected Callback, який викликається при виборі нового кольору.
     */
    private fun showColorPickerDialogForNavButton(dialogTitle: String, currentColorResId: Int, onColorSelected: (Int) -> Unit) {
        val colorsResIds = arrayOf(
            R.color.nav_button_color, // Стандартний синій
            R.color.color_red,
            R.color.color_blue,
            R.color.color_yellow,
            R.color.color_purple,
            R.color.color_orange,
            R.color.black,
            R.color.white
        )

        val dialogView = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val colorPickerDialogTitle: TextView = dialogView.findViewById(R.id.colorPickerDialogTitle)
        val colorGrid: GridLayout = dialogView.findViewById(R.id.colorGrid)

        colorPickerDialogTitle.text = dialogTitle

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
                    paint.color = ContextCompat.getColor(this@HiveInfoActivity, colorResId)
                }
                val borderShape = ShapeDrawable(OvalShape()).apply {
                    paint.color = ContextCompat.getColor(this@HiveInfoActivity, R.color.note_border)
                    paint.style = android.graphics.Paint.Style.STROKE
                    paint.strokeWidth = resources.getDimension(R.dimen.color_swatch_border_width)
                }
                val layers = arrayOf(circleShape, borderShape)
                background = LayerDrawable(layers)

                if (colorResId == currentColorResId) {
                    val selectedBorder = ShapeDrawable(OvalShape()).apply {
                        paint.color = ContextCompat.getColor(this@HiveInfoActivity, R.color.note_border_selected)
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
                    onColorSelected(colorResId)
                    dialog.dismiss()
                }
            }
            colorGrid.addView(colorSwatch)
        }
        dialog.show()
    }

    private fun showInfo(entryType: String) {
        currentEntryType = entryType
        val title: String = when (currentEntryType) {
            "queen" -> "Записи про матку"
            "notes" -> "Загальні записи"
            else -> currentHiveActualName
        }
        infoTitle.text = title
        loadNotes()
    }

    // Обробник натискання для кнопки "Матка"
    fun openQueenInfo(view: View) {
        showInfo("queen")
    }

    // Обробник натискання для кнопки "Примітки"
    fun openNotesInfo(view: View) {
        showInfo("notes")
    }

}

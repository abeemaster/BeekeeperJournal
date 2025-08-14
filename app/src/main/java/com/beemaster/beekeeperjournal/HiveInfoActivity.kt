// HiveInfoActivity файл котрий спрацьовує при натисканні на кнопку "Вулик№"

package com.beemaster.beekeeperjournal

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.widget.GridLayout
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.content.res.ColorStateList
import androidx.core.view.isVisible

class HiveInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HiveInfoActivity"

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
    private lateinit var hiveInfoBtn: Button
    private lateinit var notesBtn: Button
    private lateinit var noteRepository: NoteRepository
    private lateinit var currentHiveActualName: String // Зберігаємо актуальну назву вулика
    private lateinit var newNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var editNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private var currentHiveNumber: Int = 0
    private var currentQueenButtonColor: Int = 0
    private var currentNotesButtonColor: Int = 0
    private var currentEntryType: String = ""
    private var currentlyVisibleActionsLayout: LinearLayout? = null
    private var currentlySelectedNoteItem: LinearLayout? = null




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)

        noteRepository = NoteRepository(this)

        // Ініціалізація змінних та отримання даних з Intent
        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn)
        notesBtn = findViewById(R.id.notesBtn)

        // Встановлення currentEntryType.
        // Якщо прийшло з Intent, то використовуємо його, інакше за замовчуванням "hive".
        val initialEntryType = intent.getStringExtra("TYPE") ?: "hive"
        val titleFromIntent = intent.getStringExtra("TITLE")
        currentHiveNumber = intent.getIntExtra("EXTRA_HIVE_NUMBER", 0)
        Log.d(TAG, "HiveInfoActivity: В onCreate, отриманий hiveNumber: $currentHiveNumber")
        currentQueenButtonColor = intent.getIntExtra(EXTRA_QUEEN_BUTTON_COLOR, R.color.nav_button_color)
        currentNotesButtonColor = intent.getIntExtra(EXTRA_NOTES_BUTTON_COLOR, R.color.nav_button_color)
        currentHiveActualName = intent.getStringExtra(EXTRA_HIVE_NAME) ?: "Вулик №$currentHiveNumber"


        if (titleFromIntent != null) {
            infoTitle.text = titleFromIntent
        } else {
            infoTitle.text = currentHiveActualName
        }

        // Викликаємо showInfo для ініціалізації
        showInfo(initialEntryType)

        // Встановлюємо кольори кнопок "Матка" та "Примітки"
        queenBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentQueenButtonColor))

        notesBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentNotesButtonColor))

        // Встановлюємо слухачів для кнопок "Матка" і "Примітки", щоб вони лише оновлювали поточну активність
        queenBtn.setOnClickListener {
            openQueenInfo()
        }
        hiveInfoBtn.setOnClickListener { // ⬅️ Додаємо обробник натискання
            showInfo("hive")
        }
        notesBtn.setOnClickListener {
            openNotesInfo()
        }

        val type = intent.getStringExtra("TYPE")

        if (type == "general") {
            // Якщо це екран для загальних записів, приховуємо кнопки "Матка" "Вулик" та "Примітки"
            queenBtn = findViewById(R.id.queenBtn)
            notesBtn = findViewById(R.id.notesBtn)
            hiveInfoBtn = findViewById(R.id.hiveInfoBtn)

            queenBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
        }

        // ... (залиште інший код onCreate без змін) ...
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

        queenBtn.setOnLongClickListener {
            showColorPickerDialogForNavButton("Матка", currentQueenButtonColor) { selectedColorResId ->
                currentQueenButtonColor = selectedColorResId
                queenBtn.backgroundTintList = ContextCompat.getColorStateList(this, currentQueenButtonColor)
                val newQueenShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                    setColor(ContextCompat.getColor(this@HiveInfoActivity, currentQueenButtonColor))
                }
                queenBtn.background = newQueenShapeDrawable
                setResultAndFinish()
            }
            true
        }

        notesBtn.setOnLongClickListener {
            showColorPickerDialogForNavButton("Примітки", currentNotesButtonColor) { selectedColorResId ->
                currentNotesButtonColor = selectedColorResId
                notesBtn.backgroundTintList = ContextCompat.getColorStateList(this, currentNotesButtonColor)
                val newnotesShapeDrawable = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.nav_button_corner_radius)
                    setColor(ContextCompat.getColor(this@HiveInfoActivity, currentNotesButtonColor))
                }
                notesBtn.background = newnotesShapeDrawable
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
        Log.d(TAG, "HiveInfoActivity: addNoteFromNewNoteActivity викликано з hiveNumber: $hiveNumber")
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

            val allNotes = noteRepository.readAllNotesFromJson()
            allNotes.add(newNote)
            noteRepository.writeAllNotesToJson(allNotes)

            Toast.makeText(this, "Запис додано!", Toast.LENGTH_SHORT).show()
            loadNotes()
        } else {
            Toast.makeText(this, "Будь ласка, введіть текст для запису.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateNoteFromEditNoteActivity(noteId: String, updatedNoteText: String) {
        val allNotes = noteRepository.readAllNotesFromJson()
        val noteIndex = allNotes.indexOfFirst { it.id == noteId }
        if (noteIndex != -1) {
            val updatedNote = allNotes[noteIndex].copy(
                text = updatedNoteText,
                timestamp = System.currentTimeMillis()
            )
            allNotes[noteIndex] = updatedNote
            noteRepository.writeAllNotesToJson(allNotes)
            Toast.makeText(this, "Запис оновлено!", Toast.LENGTH_SHORT).show()
            loadNotes()
        } else {
            Toast.makeText(this, "Помилка: Запис для оновлення не знайдено.", Toast.LENGTH_SHORT).show()
        }
        hideActionsAndResetBackground()
    }

    private fun loadNotes() {
        val allNotes = noteRepository.readAllNotesFromJson()

        val filteredNotes = allNotes.filter { note ->
            // Фільтруємо за типом запису та номером вулика
            note.type == currentEntryType && note.hiveNumber == currentHiveNumber

        }.sortedByDescending { it.timestamp }
        Log.d(TAG, "HiveInfoActivity: Знайдено ${filteredNotes.size} нотаток для hiveNumber: $currentHiveNumber і type: $currentEntryType")

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

            if (actionsLayout.isVisible) { // ⬅️ Оновлено
                actionsLayout.isVisible = false // ⬅️ Оновлено
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
            if (currentlyVisibleActionsLayout == actionsLayout && actionsLayout.isVisible) { // ⬅️ Оновлено
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

    private fun deleteNote(button: Button) {
        val noteItem = button.parent.parent as LinearLayout
        val noteId = noteItem.tag as String

        AlertDialog.Builder(this)
            .setTitle("Видалити запис")
            .setMessage("Ви впевнені, що хочете видалити цей запис?")
            .setPositiveButton("Видалити") { dialog, _ ->
                val allNotes = noteRepository.readAllNotesFromJson()
                val initialSize = allNotes.size
                allNotes.removeIf { it.id == noteId }
                if (allNotes.size < initialSize) {
                    noteRepository.writeAllNotesToJson(allNotes)
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
            "queen" -> "Матка $currentHiveActualName"
            "hive" -> "Вулик $currentHiveActualName"
            "notes" -> "Примітки $currentHiveActualName"
            else -> currentHiveActualName
        }
        infoTitle.text = title
        loadNotes()
    }

    // Обробник натискання для кнопки "Матка"
    private fun openQueenInfo() {
        showInfo("queen")
    }

    // Обробник натискання для кнопки "Примітки"
    private fun openNotesInfo() {
        showInfo("notes")
    }

}

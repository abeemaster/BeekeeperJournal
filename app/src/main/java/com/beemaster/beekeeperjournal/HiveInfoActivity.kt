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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.GridLayout
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.content.res.ColorStateList

class HiveInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HiveInfoActivity"

        const val EXTRA_QUEEN_BUTTON_COLOR = "com.beemaster.beekeeperjournal.QUEEN_BUTTON_COLOR"
        const val EXTRA_NOTES_BUTTON_COLOR = "com.beemaster.beekeeperjournal.NOTES_BUTTON_COLOR"
        const val RESULT_QUEEN_BUTTON_COLOR = "com.beemaster.beekeeperjournal.RESULT_QUEEN_BUTTON_COLOR"
        const val RESULT_NOTES_BUTTON_COLOR = "com.beemaster.beekeeperjournal.RESULT_NOTES_BUTTON_COLOR"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"
    }

    private lateinit var infoTitle: TextView
    private lateinit var notesDisplayArea: LinearLayout
    private lateinit var microphoneBtn: ImageButton
    private lateinit var newNoteButton: com.google.android.material.button.MaterialButton
    private lateinit var queenBtn: Button
    private lateinit var hiveInfoBtn: Button
    private lateinit var notesBtn: Button
    private lateinit var noteManager: NoteManager
    private lateinit var noteRepository: NoteRepository
    private lateinit var noteViewCreator: NoteViewCreator // ✅ Нова змінна для NoteViewCreator
    private lateinit var currentHiveActualName: String
    private lateinit var newNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var editNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private var currentHiveNumber: Int = 0
    private var currentQueenButtonColor: Int = 0
    private var currentNotesButtonColor: Int = 0
    private var currentEntryType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)

        noteRepository = NoteRepository(this)
        noteManager = NoteManager(this, noteRepository)

        // ✅ Ініціалізація NoteViewCreator з колбеками
        noteViewCreator = NoteViewCreator(
            this,
            onEditNote = { noteId ->
                openEditNoteActivity(noteId)
            },
            onDeleteNote = { noteId ->
                deleteNote(noteId)
            }
        )

        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        notesBtn = findViewById(R.id.notesBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn) // Ініціалізуємо hiveInfoBtn

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

        showInfo(initialEntryType)

        queenBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentQueenButtonColor))
        notesBtn.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, currentNotesButtonColor))

        queenBtn.setOnClickListener { openQueenInfo() }
        hiveInfoBtn.setOnClickListener { showInfo("hive") }
        notesBtn.setOnClickListener { openNotesInfo() }

        val type = intent.getStringExtra("TYPE")
        if (type == "general") {
            queenBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
        }

        newNoteActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadNotes()
            }
        }

        editNoteActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val noteId = data?.getStringExtra(EditNoteActivity.EXTRA_NOTE_ID)
                val updatedNoteText = data?.getStringExtra(EditNoteActivity.EXTRA_UPDATED_NOTE_TEXT)
                val updatedHiveName = data?.getStringExtra(EditNoteActivity.EXTRA_HIVE_NAME)

                if (updatedHiveName != null) {
                    currentHiveActualName = updatedHiveName
                    if (currentEntryType == "hive") {
                        infoTitle.text = currentHiveActualName
                    }
                }

                if (noteId != null && updatedNoteText != null) {
                    noteManager.updateNote(noteId, updatedNoteText) {
                        loadNotes()
                        noteViewCreator.hideActionsAndResetBackground() // Скидаємо виділення
                    }
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
                putExtra(NewNoteActivity.EXTRA_START_VOICE_INPUT, true)
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

    private fun setResultAndFinish() {
        val resultIntent = Intent().apply {
            putExtra(RESULT_QUEEN_BUTTON_COLOR, currentQueenButtonColor)
            putExtra(RESULT_NOTES_BUTTON_COLOR, currentNotesButtonColor)
            putExtra(MainActivity.EXTRA_HIVE_NUMBER_FOR_COLOR_UPDATE, currentHiveNumber)
            putExtra(EXTRA_HIVE_NAME, currentHiveActualName)
        }
        setResult(Activity.RESULT_OK, resultIntent)
    }

    private fun loadNotes() {
        val filteredNotes = noteManager.loadNotes(currentEntryType, currentHiveNumber)
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
                // ✅ Використовуємо новий клас для створення елементів
                val noteItemView = noteViewCreator.createNoteItem(note)
                notesDisplayArea.addView(noteItemView)
            }
        }
    }

    // ✅ Новий приватний метод для запуску EditNoteActivity
    private fun openEditNoteActivity(noteId: String) {
        val note = noteManager.loadNotes(currentEntryType, currentHiveNumber).firstOrNull { it.id == noteId }
        if (note != null) {
            val intent = Intent(this, EditNoteActivity::class.java).apply {
                putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
                putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
                putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
                putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
                putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
            }
            editNoteActivityResultLauncher.launch(intent)
        }
    }

    // ✅ Новий приватний метод для видалення нотатки, що використовує NoteManager
    private fun deleteNote(noteId: String) {
        // Ми більше не створюємо noteItem, тому що ця логіка тепер в NoteViewCreator.
        noteManager.deleteNote(noteId) {
            loadNotes()
            noteViewCreator.hideActionsAndResetBackground()
        }
    }

    private fun showColorPickerDialogForNavButton(dialogTitle: String, currentColorResId: Int, onColorSelected: (Int) -> Unit) {
        val colorsResIds = arrayOf(
            R.color.nav_button_color,
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

    private fun openQueenInfo() {
        showInfo("queen")
    }

    private fun openNotesInfo() {
        showInfo("notes")
    }
}
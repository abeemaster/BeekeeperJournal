// EditNoteActivity.kt - Файл для редагування/створення нотаток

package com.beemaster.beekeeperjournal.activities

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.beemaster.beekeeperjournal.BeekeeperApplication
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.utils.VoskRecognitionHelper
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditNoteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditNoteActivity"
        const val EXTRA_NOTE_ID = "com.beemaster.beekeeperjournal.NOTE_ID"
        const val EXTRA_ORIGINAL_NOTE_TEXT = "com.beemaster.beekeeperjournal.ORIGINAL_NOTE_TEXT"
        const val EXTRA_ENTRY_TYPE = "com.beemaster.beekeeperjournal.ENTRY_TYPE_EDIT"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER_EDIT"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"
        const val EXTRA_START_VOICE_INPUT = "com.beemaster.beekeeperjournal.START_VOICE_INPUT_EDIT"
    }

    private lateinit var voskHelper: VoskRecognitionHelper
    private lateinit var editNoteScreenTitle: TextView
    private lateinit var editNoteContentInput: EditText
    private lateinit var microphoneBtnEditNote: ImageButton
    private lateinit var saveEditedNoteButton: MaterialButton

    private var noteId: Int = 0
    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0
    private var currentHiveActualName: String = ""

    private val viewModel: EditNoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        bindViews()
        getIntentData()
        setupListeners()
        setupUI()
        voskHelper = VoskRecognitionHelper(this, editNoteContentInput, microphoneBtnEditNote)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing Vosk resources.")
        voskHelper.stopListening()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voskHelper.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun bindViews() {
        editNoteScreenTitle = findViewById(R.id.editNoteScreenTitle)
        editNoteContentInput = findViewById(R.id.editNoteContentInput)
        microphoneBtnEditNote = findViewById(R.id.microphoneBtnEditNote)
        saveEditedNoteButton = findViewById(R.id.saveEditedNoteButton)
    }

    private fun getIntentData() {
        noteId = intent.getIntExtra(EXTRA_NOTE_ID, 0)
        val originalNoteText = intent.getStringExtra(EXTRA_ORIGINAL_NOTE_TEXT)
        currentEntryType = intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: "hive"
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        currentHiveActualName = intent.getStringExtra(EXTRA_HIVE_NAME) ?: "Вулик №$currentHiveNumber"
        editNoteContentInput.setText(originalNoteText)
    }

    private fun setupListeners() {
        saveEditedNoteButton.setOnClickListener { saveEditedNote() }
        microphoneBtnEditNote.setOnClickListener { toggleListening() }
    }

    private fun setupUI() {
        // Ваш існуючий код для встановлення заголовка екрана та позиції курсора.
        editNoteContentInput.setSelection(editNoteContentInput.text.length)
        editNoteScreenTitle.text = when (currentEntryType) {
            "general" -> "Редагувати загальний запис"
            "hive" -> "Редагувати запис для $currentHiveActualName"
            "queen" -> "Редагувати запис для Матки $currentHiveActualName"
            "notes" -> "Редагувати запис для Приміток $currentHiveActualName"
            else -> "Редагувати запис"
        }

        // ✅ НОВА ЛОГІКА: Перевіряємо, чи потрібно одразу запускати голосовий ввід.
        val startVoiceInputImmediately = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (startVoiceInputImmediately) {
            // Якщо true, запускаємо голосовий ввід.
            toggleListening()
        } else {
            // Якщо false, фокусуємося на полі вводу і показуємо клавіатуру.
            editNoteContentInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editNoteContentInput, InputMethodManager.SHOW_IMPLICIT)
        }

    }

    private fun toggleListening() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val speechEngine = sharedPref.getString("speech_engine", "google")

        if (speechEngine == "vosk") {
            if (voskHelper.isVoskListening()) {
                voskHelper.stopListening()
            } else {
                // ✅ Ось тут ми використовуємо ваш слухач!
                BeekeeperApplication.addVoskModelReadyListener {
                    voskHelper.setupVoskAndStartListening()
                }
            }
        } else {
            Toast.makeText(this, "Google Speech Recognition буде запущено тут.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEditedNote() {
        val updatedNoteText = editNoteContentInput.text.toString().trim()
        if (updatedNoteText.isEmpty()) {
            Toast.makeText(this, "Запис не може бути порожнім", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveNote(
            noteId = noteId,
            hiveId = currentHiveNumber,
            type = currentEntryType,
            title = "Запис для вуликів",
            content = updatedNoteText,
            imagePath = null,
            createdAt = System.currentTimeMillis()
        )

        finish()
    }
}
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
import androidx.core.content.ContextCompat
import com.beemaster.beekeeperjournal.BeekeeperApplication
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.utils.VoskRecognitionHelper
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.content.Intent
import android.speech.RecognizerIntent

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
    private lateinit var speechRecognizer: SpeechRecognizer

    private var noteId: Int = 0
    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0
    private var currentHiveActualName: String = ""
    private var isGoogleListening = false
    private val viewModel: EditNoteViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        bindViews()
        getIntentData()
        setupListeners()
        setupUI()

        // Передаємо лише одну, правильну кнопку Vosk'у
        voskHelper = VoskRecognitionHelper(
            this,
            editNoteContentInput,
            microphoneBtnEditNote
        )

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                Toast.makeText(this@EditNoteActivity, "Слухаю...", Toast.LENGTH_SHORT).show()
                updateMicrophoneButtonState(true)
            }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                updateMicrophoneButtonState(false)
            }
            override fun onError(error: Int) {
                Log.e(TAG, "Google recognition error: $error")
                val errorMessage = when(error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Помилка аудіо"
                    SpeechRecognizer.ERROR_CLIENT -> "Помилка клієнта"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостатньо прав"
                    SpeechRecognizer.ERROR_NETWORK -> "Помилка мережі"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Час очікування мережі вичерпано"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Не розпізнано"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Розпізнавач зайнятий"
                    SpeechRecognizer.ERROR_SERVER -> "Помилка сервера"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Час очікування мовлення вичерпано"
                    else -> "Невідома помилка"
                }
                Toast.makeText(this@EditNoteActivity, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                updateMicrophoneButtonState(false)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    editNoteContentInput.append("$recognizedText ")
                    Log.d(TAG, "onResults: $recognizedText")
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing Vosk resources.")
        voskHelper.stopListening()
        speechRecognizer.destroy()
    }


    private fun toggleListening() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val speechEngine = sharedPref.getString("speech_engine", "google")

        if (speechEngine == "vosk") {
            if (voskHelper.isVoskListening()) {
                voskHelper.stopListening()
                updateMicrophoneButtonState(false)
            } else {
                voskHelper.setupVoskAndStartListening()
                updateMicrophoneButtonState(true)
            }
        } else {
            if (isGoogleListening) {
                speechRecognizer.stopListening()
                updateMicrophoneButtonState(false)
            } else {
                val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
                }
                speechRecognizer.startListening(speechIntent)
                updateMicrophoneButtonState(true)
            }
        }
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
        editNoteContentInput.setSelection(editNoteContentInput.text.length)
        editNoteScreenTitle.text = when (currentEntryType) {
            "general" -> "Редагувати загальний запис"
            "hive" -> "Редагувати запис для $currentHiveActualName"
            "queen" -> "Редагувати запис для Матки $currentHiveActualName"
            "notes" -> "Редагувати запис для Приміток $currentHiveActualName"
            else -> "Редагувати запис"
        }

        val startVoiceInputImmediately = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (startVoiceInputImmediately) {
            editNoteContentInput.post {
                toggleListening()
            }
        } else {
            editNoteContentInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editNoteContentInput, InputMethodManager.SHOW_IMPLICIT)
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

    private fun updateMicrophoneButtonState(isListening: Boolean) {
        if (isListening) {
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_active_color)
            isGoogleListening = true
        } else {
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)
            isGoogleListening = false
        }
    }
}
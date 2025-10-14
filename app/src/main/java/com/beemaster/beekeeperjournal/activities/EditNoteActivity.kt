// EditNoteActivity.kt - Файл для редагування/створення нотаток.

package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.utils.VoskRecognitionHelper
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditNoteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditNoteActivity"
    }

    @Inject
    lateinit var voskModelManager: VoskModelManager
    private lateinit var voskHelper: VoskRecognitionHelper
    private lateinit var editNoteScreenTitle: TextView
    private lateinit var editNoteContentInput: EditText
    private lateinit var microphoneBtnEditNote: ImageButton
    private lateinit var saveEditedNoteButton: MaterialButton
    private lateinit var speechRecognizer: SpeechRecognizer

    private var noteId: Int = 0
    private var currentEntryType: String = ""
    private var currentHiveId: Int = 0
    private lateinit var currentHiveDisplayTitle: String
    private var isGoogleListening = false
    private var originalCreatedAt: Long = System.currentTimeMillis()
    private val viewModel: EditNoteViewModel by viewModels()


    /**
     * Основний метод життєвого циклу Activity. Ініціалізує View, отримує дані з Intent,
     * налаштовує розпізнавання мовлення та завантажує заголовок.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        bindViews()
        getIntentData()
        setupListeners()
        loadDataAndSetupTitle()

        voskHelper = VoskRecognitionHelper(
            this,
            editNoteContentInput,
            microphoneBtnEditNote,
            voskModelManager
        )

        setupSpeechRecognizer()
    }

    /**
     * Налаштовує обробники подій для Google Speech Recognizer.
     */
    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
            }
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
                Toast.makeText(this@EditNoteActivity, getString(R.string.listening_message), Toast.LENGTH_SHORT).show()
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
                    SpeechRecognizer.ERROR_AUDIO -> getString(R.string.error_audio)
                    SpeechRecognizer.ERROR_CLIENT -> getString(R.string.error_client)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> getString(R.string.error_permissions)
                    SpeechRecognizer.ERROR_NETWORK -> getString(R.string.error_network)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> getString(R.string.error_network_timeout)
                    SpeechRecognizer.ERROR_NO_MATCH -> getString(R.string.error_no_match)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> getString(R.string.error_recognizer_busy)
                    SpeechRecognizer.ERROR_SERVER -> getString(R.string.error_server)
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> getString(R.string.error_speech_timeout)
                    else -> getString(R.string.error_unknown)
                }
                Toast.makeText(this@EditNoteActivity, getString(R.string.recognition_error, errorMessage), Toast.LENGTH_LONG).show()
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


    /**
     * Викликається при знищенні Activity.
     * Звільняє ресурси Vosk та SpeechRecognizer.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing Vosk resources.")
        voskHelper.stopListening()
        speechRecognizer.destroy()
    }

    /**
     * Перемикає стан розпізнавання мовлення (Vosk або Google)
     * залежно від налаштувань користувача.
     */
    private fun toggleListening() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
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

    /**
     * Обробляє результат запиту дозволів. Передає результат у VoskHelper.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voskHelper.onRequestPermissionsResult(requestCode, grantResults)
    }

    /**
     * Прив'язує змінні-члени класу до елементів View за їхніми ID.
     */
    private fun bindViews() {
        editNoteScreenTitle = findViewById(R.id.editNoteScreenTitle)
        editNoteContentInput = findViewById(R.id.editNoteContentInput)
        microphoneBtnEditNote = findViewById(R.id.microphoneBtnEditNote)
        saveEditedNoteButton = findViewById(R.id.saveEditedNoteButton)
    }

    /**
     * Отримує всі необхідні дані (ID нотатки, текст, ID вулика, тип запису) з Intent.
     */
    /**
     * Отримує всі необхідні дані (ID нотатки, текст, ID вулика, тип запису) з Intent.
     */
    private fun getIntentData() {
        noteId = intent.getIntExtra(Constants.EXTRA_NOTE_ID, 0)
        // val originalNoteText = intent.getStringExtra(Constants.EXTRA_ORIGINAL_NOTE_TEXT) // Текст тепер завантажується в loadDataAndSetupTitle
        currentEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"

        // ✅ ВИПРАВЛЕНО: Завжди отримуємо hiveId з Intent.
        // Це необхідно, оскільки нова модель Note більше не містить hiveId,
        // і ми не можемо отримати його звідти. Activity-попередник має його надати.
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)

        // editNoteContentInput.setText(originalNoteText) // Текст тепер завантажується в loadDataAndSetupTitle
    }

    /**
     * Налаштовує слухачі подій для кнопок "Зберегти" та "Мікрофон".
     */
    private fun setupListeners() {
        saveEditedNoteButton.setOnClickListener { saveEditedNote() }
        microphoneBtnEditNote.setOnClickListener { toggleListening() }
    }

    /**
     * Асинхронно завантажує номер вулика для відображення заголовка (якщо це не загальна нотатка)
     * і викликає [finishSetup] для завершення налаштування UI.
     */
    /**
     * Асинхронно завантажує дані нотатки (для редагування) та номер вулика для відображення заголовка
     * і викликає [finishSetup] для завершення налаштування UI.
     */
    private fun loadDataAndSetupTitle() {
        lifecycleScope.launch {

            if (noteId > 0) {

                val loadedNote = viewModel.getNoteById(noteId)

                if (loadedNote != null) {

                    currentEntryType = loadedNote.type
                    editNoteContentInput.setText(loadedNote.text)
                    originalCreatedAt = loadedNote.timestamp

                } else {
                    Toast.makeText(this@EditNoteActivity, getString(R.string.error_note_not_found), Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
            }

            // Використовуємо коректний currentHiveId для завантаження назви вулика
            val displayTitle = if (currentHiveId == 0) {
                getString(R.string.general_notes_title)
            } else {
                val hiveEntity = viewModel.getHiveById(currentHiveId)

                hiveEntity?.hiveNumber ?: getString(R.string.hive_number_not_found)
            }
            currentHiveDisplayTitle = displayTitle

            // Встановлення заголовка екрана на основі завантаженого номера вулика
            editNoteScreenTitle.text = when (currentEntryType) {
                "general" -> getString(R.string.general_record_title)
                "hive" -> getString(R.string.hive_record_title, currentHiveDisplayTitle)
                "queen" -> getString(R.string.queen_record_title, currentHiveDisplayTitle)
                "notes" -> getString(R.string.notes_record_title, currentHiveDisplayTitle)
                else -> getString(R.string.edit_record_title)
            }

            finishSetup()
        }
    }


    /**
     * Завершує налаштування UI: встановлює курсор, фокусує поле вводу
     * і, якщо потрібно, активує голосовий ввід.
     */
    private fun finishSetup() {
        editNoteContentInput.setSelection(editNoteContentInput.text.length)

        val startVoiceInputImmediately = intent.getBooleanExtra(Constants.EXTRA_START_VOICE_INPUT, false)
        if (startVoiceInputImmediately) {
            editNoteContentInput.post {
                toggleListening()
            }
        } else {
            editNoteContentInput.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editNoteContentInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * Зберігає нотатку (створює нову або оновлює існуючу) і закриває Activity.
     * Використовує завантажений [currentHiveDisplayTitle] для формування заголовка нотатки.
     */
    private fun saveEditedNote() {
        val updatedNoteText = editNoteContentInput.text.toString().trim()
        if (updatedNoteText.isEmpty()) {
            Toast.makeText(this, getString(R.string.note_cannot_be_empty), Toast.LENGTH_SHORT).show()
            return
        }

        // Формуємо заголовок на основі типу запису
        val noteTitle = when (currentEntryType) {
            "general" -> getString(R.string.general_record_title)
            "hive" -> getString(R.string.hive_record_title, currentHiveDisplayTitle)
            "queen" -> getString(R.string.queen_record_title, currentHiveDisplayTitle)
            "notes" -> getString(R.string.notes_record_title, currentHiveDisplayTitle)
            else -> getString(R.string.edit_record_title)
        }
        // ✅ ВИПРАВЛЕННЯ: Використовуємо originalCreatedAt, якщо редагуємо, або System.currentTimeMillis() для нової
        val saveTimestamp = if (noteId > 0) originalCreatedAt else System.currentTimeMillis()


        viewModel.saveNote(
            noteId = noteId,
            hiveId = currentHiveId,
            type = currentEntryType,
            title = noteTitle,
            content = updatedNoteText,
            createdAt = saveTimestamp
        )

        finish()
    }

    /**
     * Оновлює зовнішній вигляд кнопки мікрофона (колір) залежно від стану
     * голосового розпізнавання.
     * @param isListening True, якщо розпізнавання активне.
     */
    private fun updateMicrophoneButtonState(isListening: Boolean) {
        if (isListening) {
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.status_red)
            isGoogleListening = true
        } else {
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.color_primary)
            isGoogleListening = false
        }
    }
}
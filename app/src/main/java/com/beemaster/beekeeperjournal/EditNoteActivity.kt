package com.beemaster.beekeeperjournal

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager // Додано імпорт WindowManager
import com.google.android.material.button.MaterialButton // Додано імпорт MaterialButton

class EditNoteActivity : AppCompatActivity(), RecognitionListener {

    companion object {
        private const val TAG = "EditNoteActivity"
        const val EXTRA_NOTE_ID = "com.beemaster.beekeeperjournal.NOTE_ID"
        const val EXTRA_ORIGINAL_NOTE_TEXT = "com.beemaster.beekeeperjournal.ORIGINAL_NOTE_TEXT"
        const val EXTRA_UPDATED_NOTE_TEXT = "com.beemaster.beekeeperjournal.UPDATED_NOTE_TEXT"
        const val EXTRA_ENTRY_TYPE = "com.beemaster.beekeeperjournal.ENTRY_TYPE_EDIT"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER_EDIT"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"
        const val EXTRA_START_VOICE_INPUT = "com.beemaster.beekeeperjournal.START_VOICE_INPUT_EDIT"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    private lateinit var editNoteScreenTitle: TextView
    private lateinit var editNoteContentInput: EditText
    private lateinit var microphoneBtnEditNote: ImageButton

    private var noteId: String? = null
    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0
    private lateinit var currentHiveActualName: String

    private var speechService: SpeechService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_note)

        // Встановлюємо режим adjustResize програмно, щоб переконатися, що клавіатура коректно піднімає вміст
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        editNoteScreenTitle = findViewById(R.id.editNoteScreenTitle)
        editNoteContentInput = findViewById(R.id.editNoteContentInput)
        microphoneBtnEditNote = findViewById(R.id.microphoneBtnEditNote)
        val saveEditedNoteButton: MaterialButton = findViewById(R.id.saveEditedNoteButton)


        microphoneBtnEditNote.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_color))

        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnEditNote.isEnabled = true
        } else {
            microphoneBtnEditNote.isEnabled = false
            Toast.makeText(this, "Vosk модель завантажується, зачекайте...", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Vosk model not yet loaded. Disabling microphone button.")
        }

        noteId = intent.getStringExtra(EXTRA_NOTE_ID)
        val originalNoteText = intent.getStringExtra(EXTRA_ORIGINAL_NOTE_TEXT)
        currentEntryType = intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: "hive"
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        currentHiveActualName = intent.getStringExtra(EXTRA_HIVE_NAME) ?: "Вулик №$currentHiveNumber"

        editNoteContentInput.setText(originalNoteText)
        // Переміщуємо курсор в кінець тексту при відкритті
        editNoteContentInput.setSelection(editNoteContentInput.text.length)

        editNoteScreenTitle.text = when (currentEntryType) {
            "general" -> "Редагувати загальний запис"
            "hive" -> "Редагувати запис для $currentHiveActualName"
            "queen" -> "Редагувати запис для Матки $currentHiveActualName"
            "notes" -> "Редагувати запис для Приміток $currentHiveActualName"
            else -> "Редагувати запис"
        }

        editNoteContentInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editNoteContentInput, InputMethodManager.SHOW_IMPLICIT)

        val startVoiceInputImmediately = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (startVoiceInputImmediately) {
            if (BeekeeperApplication.voskModel != null && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startListening()
                Toast.makeText(this, "Слухаю...", Toast.LENGTH_SHORT).show()
                microphoneBtnEditNote.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_active_color))
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            } else {
                Toast.makeText(this, "Модель Vosk ще завантажується. Будь ласка, зачекайте і натисніть мікрофон.", Toast.LENGTH_LONG).show()
            }
        }

        saveEditedNoteButton.setOnClickListener {
            saveEditedNote()
        }

        microphoneBtnEditNote.setOnClickListener {
            if (speechService != null) {
                stopListening()
                // Toast.makeText(this, "Голосовий ввід зупинено.", Toast.LENGTH_SHORT).show()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Requesting RECORD_AUDIO permission.")
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
                } else {
                    Log.d(TAG, "RECORD_AUDIO permission granted, attempting to start recognition.")
                    startListening()
                    Toast.makeText(this, "Слухаю...", Toast.LENGTH_SHORT).show()
                    microphoneBtnEditNote.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_active_color))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing SpeechService resources.")
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "RECORD_AUDIO permission granted by user. Starting recognition.")
                startListening()
                Toast.makeText(this, "Слухаю...", Toast.LENGTH_SHORT).show()
                microphoneBtnEditNote.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_active_color))
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied by user.")
                Toast.makeText(this, "Дозвіл на запис аудіо відхилено. Голосовий ввід недоступний.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListening() {
        val currentVoskModel = BeekeeperApplication.voskModel
        if (currentVoskModel == null) {
            Log.w(TAG, "Attempting to start recognition, but Vosk model is not loaded yet (global).")
            Toast.makeText(this, "Модель Vosk ще не завантажена. Зачекайте або перезапустіть додаток.", Toast.LENGTH_SHORT).show()
            microphoneBtnEditNote.isEnabled = false
            return
        }
        try {
            Log.d(TAG, "Initializing Recognizer and SpeechService")
            val rec = Recognizer(currentVoskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
            Log.d(TAG, "Recognition started.")
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка запуску розпізнавання: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    private fun stopListening() {
        Log.d(TAG, "Stopping recognition.")
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
        microphoneBtnEditNote.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_color))
    }

    override fun onResult(hypothesis: String) {
        Log.d(TAG, "onResult: $hypothesis")
        try {
            val jsonResult = JSONObject(hypothesis)
            val text = jsonResult.optString("text", "")
            if (text.isNotEmpty()) {
                editNoteContentInput.append("$text ")
                // Переміщуємо курсор в кінець тексту після додавання нового розпізнаного тексту
                editNoteContentInput.setSelection(editNoteContentInput.text.length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    override fun onPartialResult(hypothesis: String) {
        Log.d(TAG, "onPartialResult: $hypothesis")
    }

    override fun onFinalResult(hypothesis: String) {
        Log.d(TAG, "onFinalResult: $hypothesis")
    }

    override fun onError(exception: Exception) {
        Log.e(TAG, "onError: ${exception.message}", exception)
        Toast.makeText(this, "Помилка голосового вводу: ${exception.message}", Toast.LENGTH_LONG).show()
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "onTimeout: Recognition timeout. Stopping recording.")
        Toast.makeText(this, "Тайм-аут голосового вводу. Запис зупинено.", Toast.LENGTH_SHORT).show()
        stopListening()
    }

    private fun saveEditedNote() {
        val updatedNoteText = editNoteContentInput.text.toString().trim()
        if (updatedNoteText.isNotEmpty()) {
            // Prepare data to send back to HiveInfoActivity
            val resultIntent = Intent().apply {
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_UPDATED_NOTE_TEXT, updatedNoteText)
                // Передаємо актуальну назву вулика назад, якщо вона була змінена
                putExtra(EXTRA_HIVE_NAME, currentHiveActualName)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Запис не може бути порожнім.", Toast.LENGTH_SHORT).show()
        }
    }
}

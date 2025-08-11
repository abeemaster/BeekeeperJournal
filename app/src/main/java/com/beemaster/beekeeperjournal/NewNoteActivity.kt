package com.beemaster.beekeeperjournal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.widget.Button
import android.os.Looper

class NewNoteActivity : AppCompatActivity(), RecognitionListener {

    companion object {
        private const val TAG = "NewNoteActivity"
        const val EXTRA_NOTE_TEXT = "com.beemaster.beekeeperjournal.NOTE_TEXT"
        const val EXTRA_ENTRY_TYPE = "com.beemaster.beekeeperjournal.ENTRY_TYPE"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
        const val EXTRA_START_VOICE_INPUT = "com.beemaster.beekeeperjournal.START_VOICE_INPUT"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    // Оголошення змінних
    private lateinit var newNoteRootLayout: LinearLayout
    private lateinit var newNoteScreenTitle: TextView
    private lateinit var noteContentInput: EditText
    private lateinit var microphoneBtnNewNote: ImageButton
    private lateinit var saveNoteButton: Button

    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0
    private var isVoiceInputStarted = false
    private var isVoskListening = false

    private var model: Model? = null
    private var speechService: SpeechService? = null

    // ActivityResultLauncher для голосового вводу
    private lateinit var voiceInputLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_note)


        // Ініціалізуємо ActivityResultLauncher
        voiceInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    noteContentInput.setText(results[0])
                    noteContentInput.setSelection(noteContentInput.text.length)
                }
            }
        }

        // Встановлюємо режим adjustResize програмно
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // Зв'язуємо змінні з елементами XML
        newNoteRootLayout = findViewById(R.id.new_note_root_layout)
        newNoteScreenTitle = findViewById(R.id.newNoteScreenTitle)
        noteContentInput = findViewById(R.id.noteContentInput)
        microphoneBtnNewNote = findViewById(R.id.microphoneBtnNewNote)
        saveNoteButton = findViewById(R.id.saveNoteButton)

        // Програмно встановлюємо білий фон для самого вікна Activity
        window.setBackgroundDrawableResource(android.R.color.white)


        // Отримуємо дані з Intent
        currentEntryType = intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: "hive"
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        val hiveNameFromIntent = intent.getStringExtra(EXTRA_HIVE_NAME)

        // Встановлюємо заголовок
        val newNoteTitle = when (currentEntryType) {
            "queen" -> "Матка $hiveNameFromIntent"
            "notes" -> "Примітки $hiveNameFromIntent"
            else -> hiveNameFromIntent
        }
        newNoteScreenTitle.text = newNoteTitle

        // Перевіряємо наявність моделі Vosk
        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnNewNote.isEnabled = true
        } else {
            microphoneBtnNewNote.isEnabled = false
            Toast.makeText(this, "Vosk модель завантажується, зачекайте...", Toast.LENGTH_LONG).show()
        }

        // Перевіряємо, чи був запит на голосовий ввід
        isVoiceInputStarted = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (isVoiceInputStarted) {
            // Якщо голосовий ввід потрібен, одразу запускаємо його
            setupVoskAndStartListening()
        } else {
            // Якщо ні, фокусуємося на полі введення і показуємо клавіатуру
            noteContentInput.requestFocus()
            showKeyboard(noteContentInput)
        }

        // Встановлюємо обробники натискань
        saveNoteButton.setOnClickListener {
            saveNote()
        }

        microphoneBtnNewNote.setOnClickListener {
            if (isVoskListening) {
                stopListening()
            } else {
                // Замість прямого виклику startListening()
                // Викликаємо функцію, яка перевіряє модель і дозволи
                setupVoskAndStartListening()
            }
        }
    }

    // Методи для роботи з Vosk
    // У вашому класі NewNoteActivity

    private fun setupVoskAndStartListening() {
        Log.d(TAG, "setupVoskAndStartListening() called")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Якщо дозвіл не надано, запитуємо його
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            return
        }

        // Якщо Vosk модель завантажено, запускаємо з затримкою
        if (BeekeeperApplication.voskModel != null) {
            // ⚠️ Новий код: додаємо невелику затримку в 300 мс
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                startListening()
            }, 300)
        } else {
            // Якщо модель ще не завантажена, чекаємо
            Toast.makeText(this, "Vosk модель завантажується, зачекайте...", Toast.LENGTH_LONG).show()
            BeekeeperApplication.addVoskModelReadyListener {
                if (!isFinishing) {
                    // ⚠️ Новий код: також додаємо затримку після завантаження моделі
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 300)
                }
            }
        }
    }

    // У вашому класі NewNoteActivity

    // У вашому класі NewNoteActivity

    private fun startListening() {
        // ⚠️ Новий рядок: Виходимо, якщо вже слухаємо
        if (isVoskListening) return

        Log.d(TAG, "startListening() викликано")
        if (BeekeeperApplication.voskModel == null) {
            Toast.makeText(this, "Vosk модель ще не завантажено. Спробуйте пізніше.", Toast.LENGTH_SHORT).show()
            return
        }

        if (speechService != null) {
            Log.d(TAG, "speechService не null, зупиняємо його.")
            speechService?.stop()
            speechService = null
        }

        try {
            // ⚠️ Новий рядок: Робимо кнопку неактивною, щоб запобігти подвійним натисканням
            microphoneBtnNewNote.isEnabled = false

            Log.d(TAG, "Створюємо Recognizer...")
            val rec = Recognizer(BeekeeperApplication.voskModel, 16000f)
            Log.d(TAG, "Створюємо SpeechService...")
            speechService = SpeechService(rec, 16000f)

            val timeoutInMs = 10000
            speechService?.startListening(this, timeoutInMs)
            Log.d(TAG, "SpeechService.startListening() викликано.")

            isVoskListening = true
            // ⚠️ Новий рядок: Робимо кнопку знову активною
            microphoneBtnNewNote.isEnabled = true
            microphoneBtnNewNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_active_color)
            Toast.makeText(this, "Початок голосового вводу...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // ⚠️ Новий рядок: В разі помилки, також повертаємо кнопку в активний стан
            microphoneBtnNewNote.isEnabled = true
            Toast.makeText(this, "Помилка Vosk: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Vosk start listening error", e)
        }
    }
    // У вашому класі NewNoteActivity

    private fun stopListening() {
        speechService?.stop()
        speechService = null

        isVoskListening = false
        microphoneBtnNewNote.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)
        Toast.makeText(this, "Голосовий ввід зупинено.", Toast.LENGTH_SHORT).show()

        // ⚠️ Новий рядок: Робимо кнопку знову активною
        microphoneBtnNewNote.isEnabled = true
    }

    // Методи інтерфейсу RecognitionListener

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Якщо дозвіл отримано, запускаємо Vosk з затримкою
                if (BeekeeperApplication.voskModel != null) {
                    // ⚠️ Новий код: додаємо затримку
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 300)
                }
            } else {
                Toast.makeText(this, "Дозвіл на запис аудіо відхилено. Голосовий ввід неможливий.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Методи для роботи з клавіатурою
    private fun showKeyboard(editText: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    // Методи для голосового вводу
    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говоріть...")
        }
        try {
            voiceInputLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка: голосове введення не підтримується на вашому пристрої.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopVoiceInput() {
        // Тут ви можете додати логіку для зупинки голосового вводу, якщо це необхідно.
        // Наприклад, закрити клавіатуру або змінити стан кнопки мікрофона.
    }

    // Методи для збереження запису
    private fun saveNote() {
        val noteText = noteContentInput.text.toString().trim()
        if (noteText.isNotEmpty()) {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_NOTE_TEXT, noteText)
                putExtra(EXTRA_ENTRY_TYPE, currentEntryType)
                putExtra(EXTRA_HIVE_NUMBER, currentHiveNumber)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } else {
            Toast.makeText(this, "Запис не може бути порожнім.", Toast.LENGTH_SHORT).show()
        }
    }

    // Методи RecognitionListener (Vosk)
    override fun onResult(hypothesis: String) {
        try {
            val voskResult = JSONObject(hypothesis).getString("text")
            noteContentInput.append(" $voskResult")
            noteContentInput.setSelection(noteContentInput.text.length)
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
}
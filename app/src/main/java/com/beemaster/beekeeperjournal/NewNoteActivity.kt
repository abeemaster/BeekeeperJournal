// NewNoteActivity.kt файл що відповідає за вікно "Новий запис". Функції Vosk перенесено.

package com.beemaster.beekeeperjournal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class NewNoteActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ENTRY_TYPE = "com.beemaster.beekeeperjournal.ENTRY_TYPE"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
        const val EXTRA_START_VOICE_INPUT = "com.beemaster.beekeeperjournal.START_VOICE_INPUT"
        const val EXTRA_HIVE_NAME = "com.beemaster.beekeeperjournal.HIVE_NAME"

    }

    // Зв'язуємо змінні з елементами XML
    private lateinit var newNoteRootLayout: LinearLayout
    private lateinit var newNoteScreenTitle: TextView
    private lateinit var noteContentInput: EditText
    private lateinit var microphoneBtnNewNote: ImageButton
    private lateinit var saveNoteButton: Button
    private lateinit var noteRepository: NoteRepository
    private lateinit var voskHelper: VoskRecognitionHelper // Екземпляр допоміжного класу
    private lateinit var voiceInputLauncher: ActivityResultLauncher<Intent> // ActivityResultLauncher для голосового вводу
    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_note)

        noteRepository = NoteRepository(this)
        // Ініціалізуємо ActivityResultLauncher
        voiceInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        }

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

        // Ініціалізація допоміжного класу Vosk
        voskHelper = VoskRecognitionHelper(
            this,
            noteContentInput,
            microphoneBtnNewNote
        )

        // Перевіряємо, чи був запит на голосовий ввід
        val isVoiceInputStarted = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (isVoiceInputStarted) {
            voskHelper.setupVoskAndStartListening()
            noteContentInput.requestFocus()
            showKeyboard(noteContentInput)
        } else {
            noteContentInput.requestFocus()
            showKeyboard(noteContentInput)
        }

        // Встановлюємо обробники натискань
        saveNoteButton.setOnClickListener {
            saveNote()
        }

        microphoneBtnNewNote.setOnClickListener {
            if (voskHelper.isVoskListening()) {
                voskHelper.stopListening()
            } else {
                voskHelper.setupVoskAndStartListening()
            }
        }
    }

    // Передаємо результат запиту дозволів у допоміжний клас
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voskHelper.onRequestPermissionsResult(requestCode, grantResults)
    }


    // Методи для роботи з клавіатурою
    private fun showKeyboard(editText: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    // Методи для збереження запису
    private fun saveNote() {
        val noteText = noteContentInput.text.toString().trim()
        if (noteText.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())
            val timestamp = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()

            val newNote = Note(
                id = newId,
                date = formattedDate,
                text = noteText,
                type = currentEntryType,
                hiveNumber = currentHiveNumber,
                timestamp = timestamp
            )

            val allNotes = noteRepository.readAllNotesFromJson()
            allNotes.add(newNote)
            noteRepository.writeAllNotesToJson(allNotes)

            Toast.makeText(this, "Запис додано!", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK) // Повертаємо успішний результат
            finish()
        } else {
            Toast.makeText(this, "Запис не може бути порожнім.", Toast.LENGTH_SHORT).show()
        }
    }
}
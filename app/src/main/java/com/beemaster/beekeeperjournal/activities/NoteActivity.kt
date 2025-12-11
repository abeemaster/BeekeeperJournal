// NoteActivity.kt - Файл для редагування/створення нотаток.

package com.beemaster.beekeeperjournal.activities

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.utils.VoiceManager
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NoteActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "NoteActivity"
    }

    @Inject
    lateinit var voskModelManager: VoskModelManager
    @Inject
    lateinit var voskHelper: VoiceManager
    private lateinit var editNoteScreenTitle: TextView
    private lateinit var editNoteContentInput: EditText
    private lateinit var microphoneBtnEditNote: ImageButton
    private lateinit var saveEditedNoteButton: MaterialButton
    private lateinit var currentHiveDisplayTitle: String
    private var noteId: Int = 0
    private var currentEntryType: String = ""
    private var currentHiveId: Int = 0
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

        // Викликаємо новий метод init у хелпері
        voskHelper.init(
            activity = this,
            inputField = editNoteContentInput,
            micButton = microphoneBtnEditNote
        )
    }

    /**
     * Викликається при знищенні Activity.
     * Звільняє ресурси Vosk та SpeechRecognizer.
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Releasing voice recognition resources.")
        voskHelper.destroy()

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
        microphoneBtnEditNote = findViewById(R.id.microphoneBtn)
        saveEditedNoteButton = findViewById(R.id.saveEditedNoteButton)
    }

    /**
     * Отримує всі необхідні дані (ID нотатки, текст, ID вулика, тип запису) з Intent.
     */
    private fun getIntentData() {
        noteId = intent.getIntExtra(Constants.EXTRA_NOTE_ID, 0)
        currentEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)
    }

    /**
     * Налаштовує слухачі подій для кнопок "Зберегти" та "Мікрофон".
     */
    private fun setupListeners() {
        saveEditedNoteButton.setOnClickListener { saveEditedNote() }
        microphoneBtnEditNote.setOnClickListener {

            voskHelper.checkPermissionAndStartListening()
        }
    }

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
                    Toast.makeText(this@NoteActivity, getString(R.string.error_note_not_found), Toast.LENGTH_LONG).show()
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

                voskHelper.checkPermissionAndStartListening()
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
        // Використовуємо originalCreatedAt, якщо редагуємо, або System.currentTimeMillis() для нової
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
}
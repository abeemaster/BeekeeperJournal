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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
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

    private lateinit var newNoteRootLayout: LinearLayout
    private lateinit var newNoteScreenTitle: TextView
    private lateinit var noteContentInput: EditText
    private lateinit var microphoneBtnNewNote: ImageButton
    private lateinit var saveNoteButton: Button
    private lateinit var noteRepository: NoteRepository
    private lateinit var voskHelper: VoskRecognitionHelper
    private lateinit var voiceInputLauncher: ActivityResultLauncher<Intent>

    // ✅ Додано змінні для бічної панелі та синхронізації
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var dataSynchronizer: DataSynchronizer

    private var currentEntryType: String = ""
    private var currentHiveNumber: Int = 0

    private val createBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.writeBackupDataToFile(uri)
            }
        }
    }

    private val openBackupFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.readAndRestoreBackupDataFromFile(uri)
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }
    }

    private val pickFolderLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                dataSynchronizer.exportNotesToCsvFiles(uri)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_note)

        // ✅ Ініціалізація бічної панелі та синхронізатора
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        dataSynchronizer = DataSynchronizer(
            this,
            createBackupFileLauncher,
            openBackupFileLauncher,
            pickFolderLauncher,
            null
        )

        // ✅ Додано: Знайдіть кнопку і встановіть обробник
        val drawerToggle = findViewById<ImageButton>(R.id.drawer_toggle_button)
        drawerToggle.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // ✅ Обробник кнопки "Назад" для закриття бічної панелі
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // ✅ Обробник для елементів меню
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
                R.id.nav_sync -> {
                    dataSynchronizer.showSyncOptionsDialog()
                }
                R.id.nav_add_hive -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        noteRepository = NoteRepository(this)
        voiceInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        }

        newNoteRootLayout = findViewById(R.id.new_note_root_layout)
        newNoteScreenTitle = findViewById(R.id.newNoteScreenTitle)
        noteContentInput = findViewById(R.id.noteContentInput)
        microphoneBtnNewNote = findViewById(R.id.microphoneBtnNewNote)
        saveNoteButton = findViewById(R.id.saveNoteButton)

        window.setBackgroundDrawableResource(android.R.color.white)

        currentEntryType = intent.getStringExtra(EXTRA_ENTRY_TYPE) ?: "hive"
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        val hiveNameFromIntent = intent.getStringExtra(EXTRA_HIVE_NAME)

        val newNoteTitle = when (currentEntryType) {
            "queen" -> "Матка $hiveNameFromIntent"
            "notes" -> "Примітки $hiveNameFromIntent"
            else -> hiveNameFromIntent
        }
        newNoteScreenTitle.text = newNoteTitle

        voskHelper = VoskRecognitionHelper(
            this,
            noteContentInput,
            microphoneBtnNewNote
        )

        val isVoiceInputStarted = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)
        if (isVoiceInputStarted) {
            voskHelper.setupVoskAndStartListening()
            noteContentInput.requestFocus()
            showKeyboard(noteContentInput)
        } else {
            noteContentInput.requestFocus()
            showKeyboard(noteContentInput)
        }

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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        voskHelper.onRequestPermissionsResult(requestCode, grantResults)
    }

    private fun showKeyboard(editText: EditText) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

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
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Запис не може бути порожнім.", Toast.LENGTH_SHORT).show()
        }
    }
}
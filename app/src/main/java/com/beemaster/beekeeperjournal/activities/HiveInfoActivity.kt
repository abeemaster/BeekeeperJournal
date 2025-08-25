// HiveInfoActivity файл котрий спрацьовує при натисканні на кнопку "Вулик№"
// оновлено

package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.viewmodel.HiveInfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.util.Log

@AndroidEntryPoint // ✅ Важливо для Hilt
class HiveInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HiveInfoActivity"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
    }

    private lateinit var infoTitle: TextView
    private lateinit var notesDisplayArea: LinearLayout
    private lateinit var newNoteButton: com.google.android.material.button.MaterialButton
    private lateinit var microphoneBtn: ImageButton
    private lateinit var queenBtn: Button
    private lateinit var hiveInfoBtn: Button
    private lateinit var notesBtn: Button
    private lateinit var currentHiveActualName: String

    private var currentHiveNumber: Int = 0
    private var currentEntryType: String = ""

    // ✅ Отримуємо ViewModel через by viewModels()
    private val viewModel: HiveInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)

        setupViews()
        setupListeners()
        loadInitialData()
        observeNotes()
    }

    private fun setupViews() {
        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn)
        notesBtn = findViewById(R.id.notesBtn)
    }

    private fun setupListeners() {
        newNoteButton.setOnClickListener { openNewNoteActivity() }
        microphoneBtn.setOnClickListener { openNewNoteActivity(startVoiceInput = true) }
        queenBtn.setOnClickListener { showInfo("queen") }
        hiveInfoBtn.setOnClickListener { showInfo("hive") }
        notesBtn.setOnClickListener { showInfo("notes") }
    }

    private fun loadInitialData() {
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        Log.d(TAG, "HiveInfoActivity: In onCreate, received hiveNumber: $currentHiveNumber")
        currentHiveActualName = "Вулик №$currentHiveNumber" // Або з іншого джерела
        showInfo("hive")
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesDisplayArea.removeAllViews()
                if (notes.isEmpty()) {
                    Log.d(TAG, "No notes found for hive $currentHiveNumber, type $currentEntryType")
                    // Додайте тут TextView "Записів немає"
                } else {
                    notes.forEach { note ->
                        // Тут потрібно створити та додати View для кожної нотатки
                        // Ця логіка буде впроваджена пізніше.
                    }
                }
            }
        }
    }

    private fun showInfo(entryType: String) {
        currentEntryType = entryType
        val title: String = when (currentEntryType) {
            "queen" -> "Матка $currentHiveActualName"
            "hive" -> currentHiveActualName
            "notes" -> "Примітки $currentHiveActualName"
            else -> currentHiveActualName
        }
        infoTitle.text = title
        viewModel.getNotesForHive(currentHiveNumber, currentEntryType)
    }

    private fun openNewNoteActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
            putExtra(EditNoteActivity.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivity(intent)
    }
}
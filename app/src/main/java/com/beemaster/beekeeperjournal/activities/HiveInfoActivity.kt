// HiveInfoActivity файл котрий спрацьовує при натисканні на кнопку "Вулик№"

package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.NotesAdapter
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveInfoViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HiveInfoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggleBtn: ImageButton
    private lateinit var navView: NavigationView
    private lateinit var infoTitle: TextView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var newNoteButton: com.google.android.material.button.MaterialButton
    private lateinit var microphoneBtn: ImageButton
    private lateinit var queenBtn: Button
    private lateinit var hiveInfoBtn: Button
    private lateinit var notesBtn: Button
    private lateinit var currentHiveNumber: String
    private var currentHiveId: Int = 0
    private var currentEntryType: String = ""
    private val viewModel: HiveInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)
        setupViews()
        setupListeners()
        setupNavigationView()
        setupRecyclerView()
        loadInitialData()
        observeNotes()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onLongClick = { note ->
                showNoteOptionsDialog(note)
            }
        )
        notesRecyclerView.adapter = notesAdapter
        // LayoutManager уже встановлено в XML, але можна додати тут, якщо потрібно:
        // notesRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)

                startActivityWithSlideAnimation(intent, finishCurrentActivity = true)
            }
            R.id.nav_general_notes -> {
                currentHiveId = 0
                updateUIAndLoadData("general")
            }
        }
        return true
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerToggleBtn = findViewById(R.id.drawer_toggle_button)
        navView = findViewById(R.id.nav_view)
        infoTitle = findViewById(R.id.infoTitle)
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn)
        notesBtn = findViewById(R.id.notesBtn)
    }

    private fun setupListeners() {
        newNoteButton.setOnClickListener { openNoteEditorActivity() }
        microphoneBtn.setOnClickListener { openNoteEditorActivity(startVoiceInput = true) }
        queenBtn.setOnClickListener { showInfo("queen") }
        hiveInfoBtn.setOnClickListener { showInfo("hive") }
        notesBtn.setOnClickListener { showInfo("notes") }
        drawerToggleBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    // У файлі HiveInfoActivity.kt

// ... (допоміжні змінні)
// ...
// ... (setupViews, setupListeners, setupNavigationView, setupRecyclerView)

    private fun loadInitialData() {
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)
        // ❌ ВИДАЛЕНО: Тут більше не намагаємося отримати номер з Intent
        // val hiveNameFromIntent = intent.getStringExtra(Constants.EXTRA_HIVE_NUMBER)
        // currentHiveNumber = hiveNameFromIntent ?: getString(R.string.general_notes_title)

        val initialEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"
        currentEntryType = if (currentHiveId == 0) "general" else initialEntryType

        // ✅ НОВИЙ КРОК: Запускаємо асинхронне завантаження номера вулика
        loadHiveData()
    }

    // ✅ НОВА ФУНКЦІЯ: Асинхронно завантажує номер вулика з бази даних
    private fun loadHiveData() {
        if (currentHiveId == 0) {
            // Для загальних записів
            currentHiveNumber = getString(R.string.general_notes_title)
            updateUIAndLoadData(currentEntryType)
            return
        }

        lifecycleScope.launch {
            val hive = viewModel.getHiveById(currentHiveId)

            if (hive != null) {
                // Отримуємо фактичний НОМЕР вулика (наприклад, "49")
                currentHiveNumber = hive.hiveNumber.toString()
            } else {
                // Резервний варіант, якщо вулик не знайдено
                currentHiveNumber = getString(R.string.error_hive_not_found_placeholder)
                Toast.makeText(this@HiveInfoActivity, getString(R.string.error_hive_loading), Toast.LENGTH_LONG).show()
            }

            // Після завантаження номера вулика, оновлюємо UI (включаючи заголовок)
            updateUIAndLoadData(currentEntryType)
        }
    }

    // Змінюємо updateUIAndLoadData - тепер вона використовує коректно встановлений currentHiveNumber
    private fun updateUIAndLoadData(entryType: String) {
        currentEntryType = entryType

        val titleResId: Int = when {
            currentHiveId == 0 -> R.string.general_notes_title
            entryType == "queen" -> R.string.queen_title
            entryType == "hive" -> R.string.hive_info_title
            entryType == "notes" -> R.string.hive_notes_title
            else -> R.string.hive_name
        }

        // ✅ ПЕРЕВІРКА: currentHiveNumber тепер містить "49" або "Загальні записи"
        infoTitle.text = if (currentHiveId == 0) {
            getString(titleResId)
        } else {
            getString(titleResId, currentHiveNumber)
        }

        // ... (логіка видимості кнопок залишається без змін)

        if (currentHiveId == 0) {
            queenBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
        } else {
            queenBtn.visibility = View.VISIBLE
            hiveInfoBtn.visibility = View.VISIBLE
            notesBtn.visibility = View.VISIBLE
        }
        viewModel.getNotesForHive(currentHiveId, currentEntryType)
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)
            // TODO: Додати логіку відображення заглушки, якщо список notes порожній.
            }
        }
    }



    private fun showInfo(entryType: String) {
        updateUIAndLoadData(entryType)
    }

    private fun openNoteEditorActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(Constants.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(Constants.EXTRA_HIVE_ID, currentHiveId)
            putExtra(Constants.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(Constants.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivity(intent)
    }

    private fun showNoteOptionsDialog(note: NoteEntity) {
        DialogUtils.showEditDeleteDialog(
            context = this,
            onEdit = {
                val intent = Intent(this, EditNoteActivity::class.java).apply {
                    putExtra(Constants.EXTRA_NOTE_ID, note.id)
                    putExtra(Constants.EXTRA_ORIGINAL_NOTE_TEXT, note.content)
                    putExtra(Constants.EXTRA_HIVE_ID, note.hiveId)
                    putExtra(Constants.EXTRA_ENTRY_TYPE, note.type)
                }
                startActivity(intent)
            },
            onDelete = {
                showDeleteConfirmationDialog(note)
            }
        )
    }

    private fun showDeleteConfirmationDialog(note: NoteEntity) {
        DialogUtils.showDeleteConfirmationDialog(
            context = this,
            titleResId = R.string.confirm_delete,
            messageResId = R.string.delete_confirm_message,
            onConfirm = {
                viewModel.deleteNote(note)
                Toast.makeText(this, getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

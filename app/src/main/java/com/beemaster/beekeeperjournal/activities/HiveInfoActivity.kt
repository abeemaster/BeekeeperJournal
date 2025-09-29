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

    // ✅ ЗМІНЕНО: Перейменовано для відображення номера/заголовка замість 'імені'
    private lateinit var currentHiveDisplayTitle: String
    private var currentHiveId: Int = 0
    private var currentEntryType: String = ""
    private val viewModel: HiveInfoViewModel by viewModels()

    /**
     * Основний метод життєвого циклу Activity.
     * Ініціалізує View, слухачів та розпочинає завантаження початкових даних.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)
        setupViews()
        setupListeners()
        setupNavigationView()
        setupRecyclerView()
        // ✅ loadInitialData тепер запускає процес асинхронного завантаження
        loadInitialData()
        // ✅ observeNotes викликається після завантаження заголовка в loadInitialData
    }

    /**
     * Налаштовує RecyclerView та його адаптер.
     * Встановлює обробник довгого натискання для відображення опцій нотатки.
     */
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

    /**
     * Встановлює слухача для навігаційного меню.
     */
    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener(this)
    }

    /**
     * Обробляє вибір елементів у навігаційному меню (Drawer Layout).
     * @param item Обраний елемент меню.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                // Використовуємо уніфіковану функцію анімації
                startActivityWithSlideAnimation(intent, finishCurrentActivity = true)
            }
            R.id.nav_general_notes -> {
                currentHiveId = 0
                updateUIAndLoadData("general")
            }
        }
        return true
    }

    /**
     * Ініціалізує всі елементи View, використовуючи findViewById.
     */
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

    /**
     * Налаштовує всі слухачі подій для кнопок.
     */
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

    /**
     * Завантажує початкові дані з Intent (ID вулика та тип запису)
     * і асинхронно отримує номер вулика для відображення заголовка.
     */
    private fun loadInitialData() {
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)
        val initialEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"
        currentEntryType = if (currentHiveId == 0) "general" else initialEntryType

        lifecycleScope.launch {
            if (currentHiveId != 0) {
                // ✅ Завантаження номера вулика для заголовка
                // ПРИМІТКА: Потрібно додати fun getHiveById(id: Int) до HiveInfoViewModel
                val hiveEntity = viewModel.getHiveById(currentHiveId)
                currentHiveDisplayTitle = hiveEntity?.hiveNumber ?: getString(R.string.hive_number_not_found)
            } else {
                // Загальні нотатки
                currentHiveDisplayTitle = getString(R.string.general_notes_title)
            }
            // Оновлення UI та запуск спостереження за даними після завантаження заголовка
            updateUIAndLoadData(currentEntryType)
            observeNotes()
        }
    }

    /**
     * Починає спостерігати за потоком нотаток із ViewModel
     * та оновлює адаптер RecyclerView.
     */
    private fun observeNotes() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)
                // TODO: Додати логіку відображення заглушки, якщо список notes порожній.
            }
        }
    }

    /**
     * Оновлює заголовок Activity та видимість кнопок залежно від типу запису (entryType).
     * Здійснює запит до ViewModel для завантаження відповідних нотаток.
     * @param entryType Тип нотатки, який потрібно відобразити ("queen", "hive", "notes", "general").
     */
    private fun updateUIAndLoadData(entryType: String) {
        currentEntryType = entryType
        val titleResId: Int = when {
            currentHiveId == 0 -> R.string.general_notes_title
            entryType == "queen" -> R.string.queen_title
            entryType == "hive" -> R.string.hive_info_title
            entryType == "notes" -> R.string.hive_notes_title
            else -> R.string.hive_name
        }

        // Встановлення заголовка з використанням currentHiveDisplayTitle
        infoTitle.text = if (currentHiveId == 0) {
            getString(titleResId)
        } else {
            getString(titleResId, currentHiveDisplayTitle)
        }

        // Управління видимістю кнопок
        if (currentHiveId == 0) {
            queenBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
        } else {
            queenBtn.visibility = View.VISIBLE
            hiveInfoBtn.visibility = View.VISIBLE
            notesBtn.visibility = View.VISIBLE
        }

        // Завантаження нотаток для обраного типу
        viewModel.getNotesForHive(currentHiveId, currentEntryType)
    }

    /**
     * Змінює поточний тип відображення інформації та оновлює UI.
     * @param entryType Новий тип запису для відображення.
     */
    private fun showInfo(entryType: String) {
        updateUIAndLoadData(entryType)
    }

    /**
     * Відкриває Activity для створення або редагування нотатки.
     * Передає ID вулика та поточний тип запису.
     * @param startVoiceInput Якщо true, активує голосовий ввід у редакторі.
     */
    private fun openNoteEditorActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(Constants.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(Constants.EXTRA_HIVE_ID, currentHiveId)
            putExtra(Constants.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivity(intent)
    }

    /**
     * Відображає діалог з опціями "Редагувати" та "Видалити" для обраної нотатки.
     * @param note Об'єкт нотатки, для якого відображаються опції.
     */
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

    /**
     * Відображає діалог підтвердження видалення нотатки.
     * @param note Об'єкт нотатки, яку потрібно видалити.
     */
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
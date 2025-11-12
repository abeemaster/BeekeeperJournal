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
import com.beemaster.beekeeperjournal.models.NoteDisplayModel
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.utils.startActivityWithReverseSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveInfoViewModel
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.beemaster.beekeeperjournal.dialogs.NoteActionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.showDeleteConfirmationDialog

/**
 * Активиті для відображення детальної інформації та нотаток конкретного вулика
 * або загальних записів.
 *
 * Використовує [HiveInfoViewModel] для отримання даних.
 */
@AndroidEntryPoint
class HiveInfoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // ------------------------------------
    // ПОЛЯ ТА ІНІЦІАЛІЗАЦІЯ
    // ------------------------------------

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
    private lateinit var emptyNotesPlaceholder: View
    private lateinit var currentHiveNumber: String // Номер вулика (String) або "Загальні записи"
    private var currentHiveId: Int = 0 // ID вулика (0 для загальних записів)
    private var currentEntryType: String = "" // Тип нотатки, що відображається ("hive", "queen", "notes", "general")
    private val viewModel: HiveInfoViewModel by viewModels() // Ін'єкція ViewModel за допомогою Hilt
    private val hiveInfoViewModel: HiveInfoViewModel by viewModels()

    // ✅ 2. НОВИЙ: Інжектуємо ViewModel, що містить логіку видалення
    private val editNoteViewModel: EditNoteViewModel by viewModels()

    /**
     * Викликається при створенні активиті.
     * Ініціалізує UI, встановлює слухачів, завантажує початкові дані та запускає спостереження за нотатками.
     */
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

    /**
     * Налаштовує RecyclerView та ініціалізує адаптер [NotesAdapter].
     * Встановлює слухача для довгого натискання на нотатку, що відкриває діалог опцій.
     */
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            // Адаптер тепер приймає NoteDisplayModel
            onLongClick = { note ->
                showNoteActionsDialog(note) // ✅ ВИКЛИК НОВОГО ДІАЛОГУ ДІЙ
            }
        )
        notesRecyclerView.adapter = notesAdapter

        // ✅ Встановлюємо слухача для обробки результату з діалогу дій
        setupNoteActionsListener()
    }

    /**
     * Налаштовує NavigationView (бокове меню) та встановлює слухача для обробки натискань.
     */
    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener(this)
    }

    /**
     * Обробляє вибір елементів у боковому меню (NavigationView).
     * @param item Обраний елемент меню.
     * @return true, якщо елемент оброблено.
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {
                // Перехід на головний екран
                val intent = Intent(this, MainActivity::class.java)
                startActivityWithReverseSlideAnimation(intent, finishCurrentActivity = true)
            }
            R.id.nav_general_notes -> {
                // Перехід до загальних нотаток (hiveId = 0)
                currentHiveId = 0
                updateUIAndLoadData("general")
            }
        }
        return true
    }

    /**
     * Знаходить та ініціалізує всі елементи інтерфейсу користувача (UI views).
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
        emptyNotesPlaceholder = findViewById(R.id.emptyNotesPlaceholder)
    }

    /**
     * Встановлює слухачів натискань (OnClickListener) для кнопок.
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
     * Зчитує початкові дані (Hive ID та тип запису) з Intent.
     * Запускає асинхронне завантаження номера вулика.
     */
    private fun loadInitialData() {
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)

        val initialEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"
        currentEntryType = if (currentHiveId == 0) "general" else initialEntryType

        loadHiveData()
    }

    /**
     * Асинхронно завантажує номер вулика (`currentHiveNumber`) за його ID.
     * Якщо `currentHiveId` дорівнює 0, встановлює "Загальні записи".
     */
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
                currentHiveNumber = hive.hiveNumber
            } else {
                currentHiveNumber = getString(R.string.error_hive_not_found_placeholder)
                Toast.makeText(this@HiveInfoActivity, getString(R.string.error_hive_loading), Toast.LENGTH_LONG).show()
            }

            // Оновлюємо UI після завантаження номера вулика
            updateUIAndLoadData(currentEntryType)
        }
    }

    /**
     * Оновлює заголовок активиті та видимість кнопок залежно від `entryType`
     * та `currentHiveId`.
     * Запускає завантаження відповідних нотаток у ViewModel.
     * @param entryType Тип нотаток для відображення ("hive", "queen", "notes", "general").
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

        infoTitle.text = if (currentHiveId == 0) {
            getString(titleResId)
        } else {
            // Форматуємо заголовок, використовуючи номер вулика
            getString(titleResId, currentHiveNumber)
        }

        // Керування видимістю кнопок для загальних записів
        if (currentHiveId == 0) {
            queenBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
        } else {
            queenBtn.visibility = View.VISIBLE
            hiveInfoBtn.visibility = View.VISIBLE
            notesBtn.visibility = View.VISIBLE
        }

        // Запит нотаток до ViewModel
        hiveInfoViewModel.getNotesForHive(currentHiveId, currentEntryType)
    }

    /**
     * Спостерігає за потоком нотаток (`viewModel.notes`) і оновлює [NotesAdapter].
     * Керує відображенням заглушки (`emptyNotesPlaceholder`) та прокручує список до початку
     * при отриманні нових даних.
     */
    private fun observeNotes() {
        lifecycleScope.launch {
            // ✅ УВАГА: ViewModel.notes тепер має надавати List<NoteDisplayModel>
            hiveInfoViewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)

                // Логіка відображення заглушки
                if (notes.isEmpty()) {
                    emptyNotesPlaceholder.visibility = View.VISIBLE
                    notesRecyclerView.visibility = View.GONE
                } else {
                    emptyNotesPlaceholder.visibility = View.GONE
                    notesRecyclerView.visibility = View.VISIBLE
                    // Прокручуємо до нового елемента (позиція 0)
                    notesRecyclerView.smoothScrollToPosition(0)
                }
            }
        }
    }

    /**
     * Перемикає тип нотаток, що відображаються (Queen, Info, Notes),
     * викликаючи оновлення UI та завантаження даних.
     * @param entryType Тип запису.
     */
    private fun showInfo(entryType: String) {
        updateUIAndLoadData(entryType)
    }

    /**
     * Відкриває активиті редактора нотаток ([NoteActivity]) для створення нової нотатки.
     * Передає ID вулика, його номер та поточний тип запису.
     * @param startVoiceInput Якщо true, активує голосове введення в редакторі.
     */
    private fun openNoteEditorActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra(Constants.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(Constants.EXTRA_HIVE_ID, currentHiveId)
            // Використовуємо локально завантажений номер вулика (НЕ з моделі Note)
            putExtra(Constants.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(Constants.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivityWithSlideAnimation(intent)
    }

    /**
     * Відображає BottomSheetDialogFragment з опціями "Редагувати" та "Видалити".
     * @param note Модель відображення [NoteDisplayModel], яку обрано.
     */
    private fun showNoteActionsDialog(note: NoteDisplayModel) {
        NoteActionsDialogFragment.newInstance(
            noteId = note.id // Передаємо ID нотатки
        ).show(supportFragmentManager, NoteActionsDialogFragment.TAG)
    }

    /**
     * Встановлює слухача для обробки результату з NoteActionsDialogFragment.
     */
    private fun setupNoteActionsListener() {
        supportFragmentManager.setFragmentResultListener(
            NoteActionsDialogFragment.KEY_REQUEST,
            this
        ) { _, bundle ->
            val noteId = bundle.getInt(NoteActionsDialogFragment.KEY_NOTE_ID)
            val action = bundle.getString(NoteActionsDialogFragment.KEY_ACTION)

            // Обробка обраної дії
            when (action) {
                NoteActionsDialogFragment.ACTION_EDIT -> {
                    // 1. Знаходимо нотатку (потрібно, щоб отримати content і type)
                    lifecycleScope.launch {
                        val noteModel = viewModel.getNoteDisplayModelById(noteId)

                        if (noteModel != null) {
                            // 2. Запуск редактора з даними нотатки
                            val intent = Intent(this@HiveInfoActivity, NoteActivity::class.java).apply {
                                putExtra(Constants.EXTRA_NOTE_ID, noteModel.id)
                                putExtra(Constants.EXTRA_ORIGINAL_NOTE_TEXT, noteModel.text)
                                putExtra(Constants.EXTRA_HIVE_ID, noteModel.hiveId)
                                putExtra(Constants.EXTRA_HIVE_NUMBER, noteModel.hiveDisplayNumber)
                                putExtra(Constants.EXTRA_ENTRY_TYPE, noteModel.type)
                            }
                            startActivityWithSlideAnimation(intent)
                        }
                    }
                }
                NoteActionsDialogFragment.ACTION_DELETE -> {
                    // Викликаємо діалог підтвердження видалення
                    showDeleteConfirmationDialog(noteId)
                }
            }
        }
    }

    /**
     * Відображає діалог підтвердження перед видаленням нотатки.
     * @param noteId ID нотатки, яку потрібно видалити.
     */
    private fun showDeleteConfirmationDialog(noteId: Int) { // Приймаємо ID
        showDeleteConfirmationDialog(
            context = this,
            titleResId = R.string.confirm_delete,
            messageResId = R.string.delete_confirm_message,
            onConfirm = {
                // Викликаємо видалення у ViewModel.
                editNoteViewModel.deleteNote(noteId) //  Передаємо ID
                Toast.makeText(this, getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
            }
        )
    }
}
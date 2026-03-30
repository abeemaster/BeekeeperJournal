package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.NotesAdapter
import com.beemaster.beekeeperjournal.models.NoteDisplayModel
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveInfoViewModel
import com.beemaster.beekeeperjournal.viewmodel.EditNoteViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.beemaster.beekeeperjournal.dialogs.NoteActionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.showDeleteConfirmationDialog
import com.google.android.material.button.MaterialButton

/**
 * Активиті для відображення детальної інформації та нотаток конкретного вулика.
 * * Клас підтримує розділення нотаток за категоріями (матка, інформація, замітки)
 * та забезпечує навігацію через бічне меню, успадковуючи [BaseActivity].
 * * @property viewModel Основна ViewModel для роботи з даними вулика та нотатками.
 * @property editNoteViewModel Допоміжна ViewModel для операцій редагування та видалення.
 */
@AndroidEntryPoint
class HiveInfoActivity : BaseActivity() {

    /** Визначає макет інтерфейсу для поточної Activity. Використовується в [BaseActivity]. */
    override fun getLayoutResId(): Int = R.layout.activity_hive_info

    private lateinit var infoTitle: TextView
    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var newNoteButton: MaterialButton
    private lateinit var microphoneBtn: ImageButton
    private lateinit var queenBtn: MaterialButton
    private lateinit var hiveInfoBtn: MaterialButton
    private lateinit var notesBtn: MaterialButton
    private lateinit var emptyNotesPlaceholder: View

    private lateinit var currentHiveNumber: String
    private var currentHiveId: Int = 0
    private var currentEntryType: String = ""

    private val hiveInfoViewModel: HiveInfoViewModel by viewModels()
    private val editNoteViewModel: EditNoteViewModel by viewModels()

    /**
     * Ініціалізує Activity, налаштовує компоненти інтерфейсу та завантажує дані.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
        setupListeners()
        setupRecyclerView()
        loadInitialData()
        observeNotes()
    }

    /**
     * Знаходить та ініціалізує View-елементи макета.
     */
    private fun setupViews() {
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
     * Встановлює слухачів натискань для керуючих елементів.
     */
    private fun setupListeners() {
        newNoteButton.setOnClickListener { openNoteEditorActivity() }
        microphoneBtn.setOnClickListener { openNoteEditorActivity(startVoiceInput = true) }
        queenBtn.setOnClickListener { showInfo("queen") }
        hiveInfoBtn.setOnClickListener { showInfo("hive") }
        notesBtn.setOnClickListener { showInfo("notes") }
    }

    /**
     * Налаштовує список [RecyclerView] та його адаптер.
     * Включає обробку довгого натискання для виклику меню дій.
     */
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onLongClick = { note ->
                showNoteActionsDialog(note)
            }
        )
        notesRecyclerView.adapter = notesAdapter
        setupNoteActionsListener()
    }

    /**
     * Витягує ідентифікатор вулика та тип запису з [Intent].
     */
    private fun loadInitialData() {
        currentHiveId = intent.getIntExtra(Constants.EXTRA_HIVE_ID, 0)
        val initialEntryType = intent.getStringExtra(Constants.EXTRA_ENTRY_TYPE) ?: "hive"
        currentEntryType = if (currentHiveId == 0) "general" else initialEntryType
        loadHiveData()
    }

    /**
     * Завантажує дані про вулик (номер вулика) на основі поточного [currentHiveId].
     * Якщо ID дорівнює 0, встановлюється заголовок "Загальні записи".
     * Після отримання даних оновлює інтерфейс.
     */
    private fun loadHiveData() {
        // 1. Обробка випадку із загальними записами (без корутин)
        if (currentHiveId == 0) {
            currentHiveNumber = getString(R.string.general_notes_title)
            updateUIAndLoadData(currentEntryType)
            return
        }

        // 2. Асинхронне отримання номера конкретного вулика
        lifecycleScope.launch {
            val hive = hiveInfoViewModel.getHiveById(currentHiveId)

            // Використовуємо Elvis-оператор (?:) для компактного присвоєння.
            // Це саме те, що просив Android Studio ("lift assignment out of if").
            currentHiveNumber = hive?.hiveNumber ?: getString(R.string.error_hive_not_found_placeholder)

            updateUIAndLoadData(currentEntryType)
        }
    }

    /**
     * Оновлює заголовки та керує видимістю кнопок залежно від вибраного типу даних.
     * * @param entryType Тип даних для відображення ("queen", "hive", "notes" або "general").
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
            getString(titleResId, currentHiveNumber)
        }

        val visibility = if (currentHiveId == 0) View.GONE else View.VISIBLE
        queenBtn.visibility = visibility
        hiveInfoBtn.visibility = visibility
        notesBtn.visibility = visibility

        hiveInfoViewModel.getNotesForHive(currentHiveId, currentEntryType)
    }

    /**
     * Підписується на оновлення списку нотаток у ViewModel.
     * Керує відображенням заглушки при порожньому списку.
     */
    private fun observeNotes() {
        lifecycleScope.launch {
            hiveInfoViewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)
                if (notes.isEmpty()) {
                    emptyNotesPlaceholder.visibility = View.VISIBLE
                    notesRecyclerView.visibility = View.GONE
                } else {
                    emptyNotesPlaceholder.visibility = View.GONE
                    notesRecyclerView.visibility = View.VISIBLE
                    notesRecyclerView.scrollToPosition(0)
                }
            }
        }
    }

    /**
     * Перемикає відображення на вказаний тип інформації.
     */
    private fun showInfo(entryType: String) {
        updateUIAndLoadData(entryType)
    }

    /**
     * Відкриває екран створення/редагування нотатки.
     * * @param startVoiceInput Визначає, чи потрібно автоматично активувати голос відразу після відкриття.
     */
    private fun openNoteEditorActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, NoteActivity::class.java).apply {
            putExtra(Constants.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(Constants.EXTRA_HIVE_ID, currentHiveId)
            putExtra(Constants.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(Constants.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivityWithSlideAnimation(intent)
    }

    /**
     * Відображає діалог вибору дій (редагування/видалення) для нотатки.
     */
    private fun showNoteActionsDialog(note: NoteDisplayModel) {
        NoteActionsDialogFragment.newInstance(noteId = note.id)
            .show(supportFragmentManager, NoteActionsDialogFragment.TAG)
    }

    /**
     * Налаштовує слухача результатів з діалогу дій нотатки.
     */
    private fun setupNoteActionsListener() {
        supportFragmentManager.setFragmentResultListener(
            NoteActionsDialogFragment.KEY_REQUEST,
            this
        ) { _, bundle ->
            val noteId = bundle.getInt(NoteActionsDialogFragment.KEY_NOTE_ID)
            val action = bundle.getString(NoteActionsDialogFragment.KEY_ACTION)

            when (action) {
                NoteActionsDialogFragment.ACTION_EDIT -> handleEditAction(noteId)
                NoteActionsDialogFragment.ACTION_DELETE -> showDeleteConfirmationDialog(noteId)
            }
        }
    }

    /**
     * Обробляє запит на редагування нотатки: завантажує дані та відкриває редактор.
     */
    private fun handleEditAction(noteId: Int) {
        lifecycleScope.launch {
            val noteModel = hiveInfoViewModel.getNoteDisplayModelById(noteId)
            if (noteModel != null) {
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

    /**
     * Відображає системний діалог підтвердження видалення запису.
     */
    private fun showDeleteConfirmationDialog(noteId: Int) {
        showDeleteConfirmationDialog(
            context = this,
            titleResId = R.string.confirm_delete,
            messageResId = R.string.delete_confirm_message,
            onConfirm = {
                editNoteViewModel.deleteNote(noteId)
                Toast.makeText(this, getString(R.string.note_deleted), Toast.LENGTH_SHORT).show()
            }
        )
    }
}
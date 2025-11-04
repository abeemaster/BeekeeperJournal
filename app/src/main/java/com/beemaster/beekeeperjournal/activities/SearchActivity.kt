package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SearchResultsAdapter
import com.beemaster.beekeeperjournal.dialogs.SearchResultActionsDialogFragment
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.utils.VoiceManager
import com.beemaster.beekeeperjournal.utils.VoskSearchListener
import com.beemaster.beekeeperjournal.viewmodel.SearchScreenState
import com.beemaster.beekeeperjournal.viewmodel.SearchViewModel
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Коментар тимчасовий заради коміта.
/**
 * Activity для здійснення пошуку нотаток та голосового вводу (Vosk/Google).
 * Відображає результати пошуку та дозволяє переходити до відповідних екранів.
 */
@AndroidEntryPoint
// ✅ РЕАЛІЗАЦІЯ ІНТЕРФЕЙСУ
class SearchActivity : BaseActivity(), VoskSearchListener {

    companion object {
        private const val TAG = "SearchActivity"
    }

    @Inject
    lateinit var voskModelManager: VoskModelManager
    @Inject
    lateinit var voskHelper: VoiceManager
    private lateinit var searchInput: EditText
    private lateinit var microphoneBtn: ImageButton
    private lateinit var searchExecuteButton: MaterialButton
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var emptySearchPlaceholder: TextView

    /**
     * ViewModel для керування даними та логікою пошуку.
     * ✅ МОЖЕ БУТИ PRIVATE, оскільки доступ до нього йде через інтерфейс/метод.
     */
    private val viewModel: SearchViewModel by viewModels()


    // -----------------------------------------------------------------------------------
    //  ІМПЛЕМЕНТАЦІЯ АБСТРАКТНОГО МЕТОДУ BASEACTIVITY
    // -----------------------------------------------------------------------------------
    override fun getLayoutResId(): Int = R.layout.activity_search

    /**
     * Викликається при першому створенні Activity.
     * Ініціалізує View, встановлює слухачів, налаштовує RecyclerView та Vosk.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindViews()
        setupListeners()
        setupRecyclerView()
        observeViewModel()

        // 💡 Ініціалізуємо VoiceManager:
        voskHelper.init(
            activity = this,
            inputField = searchInput, // Передаємо поле, куди вставляти текст
            micButton = microphoneBtn // Передаємо кнопку для управління кольором
        )

        // Фокусуємо поле вводу
        searchInput.requestFocus()
    }

    // ... (методи onResume, onRequestPermissionsResult, onDestroy, bindViews) ...

    /**
     * Викликається при відновленні Activity.
     * Автоматично виконує пошук за наявним текстом.
     */
    override fun onResume() {
        super.onResume()
        // viewModel.performSearch(searchQueryInput.text.toString())
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // ✅ Передаємо результат запиту дозволу хелперу:
        voskHelper.onRequestPermissionsResult(requestCode, grantResults)
    }

    /**
     * Викликається при знищенні Activity.
     * Зупиняє роботу сервісу розпізнавання мови, щоб уникнути витоків пам'яті.
     */
    override fun onDestroy() {
        super.onDestroy()
        // ✅ Звільняємо ресурси Vosk/Google:
        voskHelper.destroy()
    }

    /**
     * Прив'язує змінні-члени класу до елементів View за їхніми ID.
     */
    private fun bindViews() {
        searchInput = findViewById(R.id.searchQueryInput)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptySearchPlaceholder = findViewById(R.id.emptySearchPlaceholder)
    }


    /**
     * Налаштовує слухачі подій для кнопок "Пошук", "Мікрофон".
     */
    private fun setupListeners() {
        // 1. Кнопка Пошук
        searchExecuteButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            hideKeyboard()
            viewModel.performSearch(query)
        }

        // 2. Кнопка Голосовий ввід
        microphoneBtn.setOnClickListener {
            voskHelper.checkPermissionAndStartListening()

            // Приховуємо клавіатуру, коли починаємо голосовий ввід
            hideKeyboard()
        }
        searchInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showKeyboard(view)
            }
        }
    }

    /**
     * Налаштовує RecyclerView та адаптер для відображення результатів пошуку.
     */
    private fun setupRecyclerView() {
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = SearchResultsAdapter(
            onItemLongClick = { note ->
                showNoteActionsDialog(note)
            }
        )
        searchResultsRecyclerView.adapter = searchResultsAdapter

        setupNoteActionsListener()
    }

    /**
     * Відображає BottomSheetDialogFragment з опціями для нотатки з результатів пошуку.
     * @param note Об'єкт Note, який було натиснуто.
     */
    private fun showNoteActionsDialog(note: Note) {
        SearchResultActionsDialogFragment.newInstance(
            noteId = note.id,
            hiveId = note.hiveId
        ).show(supportFragmentManager, SearchResultActionsDialogFragment.TAG)
    }

    /**
     * Встановлює слухача для обробки результату з SearchResultActionsDialogFragment.
     */
    private fun setupNoteActionsListener() {
        supportFragmentManager.setFragmentResultListener(
            SearchResultActionsDialogFragment.KEY_REQUEST,
            this
        ) { _, bundle ->
            val noteId = bundle.getInt(SearchResultActionsDialogFragment.KEY_NOTE_ID)
            val targetHiveId = bundle.getInt(SearchResultActionsDialogFragment.KEY_HIVE_ID)
            val action = bundle.getString(SearchResultActionsDialogFragment.KEY_ACTION)

            // Використовуємо getCurrentResults()
            // Це отримує список, навіть якщо ViewModel знаходиться у стані Results
            val searchResult = viewModel.getCurrentResults().find { it.note.id == noteId }

            val note = searchResult?.note
            val noteType = note?.type ?: Constants.TYPE_HIVE

            when (action) {
                SearchResultActionsDialogFragment.ACTION_GO_TO_HIVE -> {
                    // Перейти у вулик
                    val intent = Intent(this, HiveInfoActivity::class.java).apply {
                        putExtra(Constants.EXTRA_HIVE_ID, targetHiveId)
                        putExtra(Constants.EXTRA_ENTRY_TYPE, noteType)
                    }
                    startActivity(intent)
                }
                SearchResultActionsDialogFragment.ACTION_EDIT_RECORD -> {
                    // Редагувати запис
                    val intent = Intent(this, NoteActivity::class.java).apply {
                        putExtra(Constants.EXTRA_NOTE_ID, noteId)
                        putExtra(Constants.EXTRA_ORIGINAL_NOTE_TEXT, note?.text)
                        putExtra(Constants.EXTRA_HIVE_ID, targetHiveId)
                        putExtra(Constants.EXTRA_ENTRY_TYPE, noteType)
                    }
                    startActivity(intent)
                }
            }
        }
    }
    /**
     * Спостерігає за результатами пошуку у ViewModel та оновлює адаптер RecyclerView.
     *
     * Підписка на StateFlow з результатами пошуку у ViewModel.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect { state ->
                    when (state) {
                        is SearchScreenState.Initial -> {
                            // СТАН 1: Початковий стан (екран, готовий до вводу)
                            searchResultsAdapter.submitList(emptyList())
                            emptySearchPlaceholder.visibility = View.GONE
                            searchResultsRecyclerView.visibility = View.VISIBLE
                        }
                        is SearchScreenState.Loading -> {
                            // СТАН 2: Завантаження (опціонально: можна показати прогрес-бар)
                            emptySearchPlaceholder.visibility = View.GONE
                            // Якщо потрібно: progressSpinner.visibility = View.VISIBLE
                        }
                        is SearchScreenState.Results -> {
                            // СТАН 3: Результати пошуку
                            val results = state.list
                            searchResultsAdapter.submitList(results)

                            val isResultsEmpty = results.isEmpty()
                            val queryWasExecuted = state.queryWasExecuted

                            val showPlaceholder = queryWasExecuted && isResultsEmpty

                            if (showPlaceholder) {
                                emptySearchPlaceholder.visibility = View.VISIBLE
                                searchResultsRecyclerView.visibility = View.GONE

                                val isQueryEmpty = searchInput.text.isBlank()

                                if (isQueryEmpty) {
                                    // Пошук був, але запит порожній
                                    emptySearchPlaceholder.setText(R.string.search_not_found)
                                } else {
                                    // Запит був, але результатів немає
                                    emptySearchPlaceholder.setText(R.string.search_no_results)
                                }
                            } else {
                                emptySearchPlaceholder.visibility = View.GONE
                                searchResultsRecyclerView.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------------
    // ІМПЛЕМЕНТАЦІЯ VOSKSEARCHLISTENER
    // --------------------------------------------------------------------------
    /**
     * ✅ Викликається з VoiceManager після успішного голосового вводу.
     */
    override fun performSearchFromVosk(query: String) {
        // Ми завжди використовуємо ViewModel для виконання пошуку
        viewModel.performSearch(query)
    }

    // --------------------------------------------------------------------------
    // Допоміжні функції
    // --------------------------------------------------------------------------

    /**
     * Відображає програмну клавіатуру для вказаного View.
     */
    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    /**
     * Приховує програмну клавіатуру.
     */
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }
}
package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
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
import com.beemaster.beekeeperjournal.viewmodel.SearchScreenState
import com.beemaster.beekeeperjournal.viewmodel.SearchViewModel
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

// Коментар тимчасовий заради коміта.
/**
 * Activity для здійснення пошуку нотаток та голосового вводу (Vosk).
 * Відображає результати пошуку та дозволяє переходити до відповідних екранів.
 */
@AndroidEntryPoint
class SearchActivity : BaseActivity(), RecognitionListener {

    companion object {
        private const val TAG = "SearchActivity"
    }

    @Inject
    lateinit var voskModelManager: VoskModelManager
    private lateinit var searchQueryInput: EditText
    private lateinit var microphoneBtnSearch: ImageButton
    private lateinit var searchExecuteButton: MaterialButton
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private lateinit var emptySearchPlaceholder: TextView
    private var speechService: SpeechService? = null

    /**
     * ViewModel для керування даними та логікою пошуку.
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
        setupVosk()
        // viewModel.performSearch(searchQueryInput.text.toString())
    }

    /**
     * Викликається при відновленні Activity.
     * Автоматично виконує пошук за наявним текстом.
     */
    override fun onResume() {
        super.onResume()
        // viewModel.performSearch(searchQueryInput.text.toString())
    }

    /**
     * Викликається при знищенні Activity.
     * Зупиняє роботу сервісу розпізнавання мови, щоб уникнути витоків пам'яті.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    /**
     * Прив'язує змінні-члени класу до елементів View за їхніми ID.
     */
    private fun bindViews() {
        searchQueryInput = findViewById(R.id.searchQueryInput)
        microphoneBtnSearch = findViewById(R.id.microphoneBtn)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
        emptySearchPlaceholder = findViewById(R.id.emptySearchPlaceholder)
    }

    /**
     * Налаштовує слухачі подій для кнопок "Пошук", "Мікрофон".
     */
    private fun setupListeners() {
        // 1. Кнопка Пошук
        searchExecuteButton.setOnClickListener { // ⬅️ ВИКОРИСТОВУЄМО ВАШУ ЗМІННУ
            val query = searchQueryInput.text.toString().trim() // ⬅️ ВИКОРИСТОВУЄМО ВАШУ ЗМІННУ
            hideKeyboard()
            viewModel.performSearch(query)
        }

        // 2. Кнопка Голосовий ввід
        microphoneBtnSearch.setOnClickListener { // ⬅️ ВИКОРИСТОВУЄМО ВАШУ ЗМІННУ
            if (speechService == null) {
                startListening()
            } else {
                stopListening()
            }
        }

        searchQueryInput.setOnFocusChangeListener { view, hasFocus ->
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
                    val intent = Intent(this, EditNoteActivity::class.java).apply {
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

                                val isQueryEmpty = searchQueryInput.text.isBlank()

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
    /**
     * Налаштовує компоненти для голосового розпізнавання Vosk.
     */
    private fun setupVosk() {
        if (voskModelManager.isModelReady) {
            microphoneBtnSearch.isEnabled = true
        } else {
            microphoneBtnSearch.isEnabled = false
            Toast.makeText(this, getString(R.string.vosk_model_loading), Toast.LENGTH_LONG).show()

            voskModelManager.addModelReadyListener {
                microphoneBtnSearch.isEnabled = true
                Toast.makeText(this, getString(R.string.vosk_model_loaded), Toast.LENGTH_SHORT).show()
            }
        }
        searchQueryInput.requestFocus()
    }

    /**
     * Запускає процес прослуховування Vosk.
     */
    private fun startListening() {
        val currentVoskModel = voskModelManager.getModel()

        if (currentVoskModel == null) {
            Toast.makeText(this, getString(R.string.vosk_model_not_loaded), Toast.LENGTH_SHORT).show()
            microphoneBtnSearch.isEnabled = false
            return
        }
        try {
            val rec = Recognizer(currentVoskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: Exception) {
            val message = getString(R.string.error_recognition_start, e.message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    /**
     * Зупиняє процес прослуховування Vosk та звільняє ресурси.
     */
    private fun stopListening() {
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
        microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_microphone)
    }

    // --------------------------------------------------------------------------
    // Реалізація RecognitionListener
    // --------------------------------------------------------------------------

    /**
     * Отримує остаточний результат розпізнавання мови, додає його до поля вводу
     * та виконує пошук.
     */
    override fun onResult(hypothesis: String) {
        stopListening()
        try {
            val jsonResult = JSONObject(hypothesis)
            val text = jsonResult.optString("text", "")
            if (text.isNotEmpty()) {
                searchQueryInput.append("$text ")
                viewModel.performSearch(searchQueryInput.text.toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    /**
     * Обробка проміжного результату розпізнавання (ігнорується).
     */
    override fun onPartialResult(hypothesis: String) {
        // У цьому додатку ігноруємо проміжний результат
    }

    /**
     * Обробка кінцевого результату розпізнавання (логіка вже в onResult).
     */
    override fun onFinalResult(hypothesis: String) {
        // Обробка final result відбувається в onResult, тут нічого не робимо
    }

    /**
     * Обробка помилок Vosk.
     */
    override fun onError(exception: Exception) {
        Log.e(TAG, "onError: ${exception.message}", exception)
        val message = getString(R.string.error_voice_input, exception.message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        stopListening()
    }

    /**
     * Обробка тайм-ауту Vosk.
     */
    override fun onTimeout() {
        Log.d(TAG, "onTimeout: Recognition timeout. Stopping recording.")
        Toast.makeText(this, getString(R.string.voice_input_timeout), Toast.LENGTH_SHORT).show()
        stopListening()
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
        imm.hideSoftInputFromWindow(searchQueryInput.windowToken, 0)
    }
}
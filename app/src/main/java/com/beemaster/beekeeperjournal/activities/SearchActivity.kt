package com.beemaster.beekeeperjournal.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.BeekeeperApplication
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SearchResultsAdapter
import com.beemaster.beekeeperjournal.models.Note
import com.beemaster.beekeeperjournal.viewmodel.SearchViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService


/**
 * Activity для здійснення пошуку нотаток та голосового вводу (Vosk).
 * Відображає результати пошуку та дозволяє переходити до відповідних екранів.
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity(), RecognitionListener {

    companion object {
        private const val TAG = "SearchActivity"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    private lateinit var searchQueryInput: EditText
    private lateinit var microphoneBtnSearch: ImageButton
    private lateinit var searchExecuteButton: MaterialButton
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: SearchResultsAdapter
    private var speechService: SpeechService? = null

    /**
     * ViewModel для керування даними та логікою пошуку.
     */
    private val viewModel: SearchViewModel by viewModels()

    /**
     * Викликається при першому створенні Activity.
     * Ініціалізує View, встановлює слухачів, налаштовує RecyclerView та Vosk.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        bindViews()
        setupListeners()
        setupRecyclerView()
        observeViewModel()
        setupVosk()
    }

    /**
     * Викликається при відновленні Activity.
     * Автоматично виконує пошук за наявним текстом.
     */
    override fun onResume() {
        super.onResume()
        viewModel.performSearch(searchQueryInput.text.toString())
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
        microphoneBtnSearch = findViewById(R.id.microphoneBtnSearch)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)
    }

    /**
     * Налаштовує слухачі подій для кнопок "Пошук" та "Мікрофон".
     */
    private fun setupListeners() {
        searchExecuteButton.setOnClickListener {
            viewModel.performSearch(searchQueryInput.text.toString())
            hideKeyboard()
        }

        microphoneBtnSearch.setOnClickListener {
            toggleListening()
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
                showNoteOptionsDialog(note)
            }
        )
        searchResultsRecyclerView.adapter = searchResultsAdapter
    }

    /**
     * Відображає діалог з опціями для обраної нотатки (перехід до вулика або редагування).
     * @param note Об'єкт Note, який було натиснуто.
     */
    private fun showNoteOptionsDialog(note: Note) {
        val options = arrayOf(
            getString(R.string.option_go_to_hive),
            getString(R.string.option_edit_record)
        )
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_choose_action))
            .setItems(options) { dialog, which ->
                // ✅ Використовуємо коректний ID вулика: note.hiveNumber
                val targetHiveId = note.hiveNumber

                when (which) {
                    0 -> { // Перейти у вулик
                        val intent = Intent(this, HiveInfoActivity::class.java).apply {
                            // Передаємо ID вулика, до якого належить нотатка
                            putExtra(Constants.EXTRA_HIVE_ID, targetHiveId)
                            putExtra(Constants.EXTRA_ENTRY_TYPE, note.type)
                        }
                        startActivity(intent)
                    }
                    1 -> { // Редагувати запис
                        // ❌ ВИПРАВЛЕНО: Також використовуємо targetHiveId для редагування
                        val intent = Intent(this, EditNoteActivity::class.java).apply {
                            // Передача ID нотатки для завантаження всього вмісту
                            putExtra(Constants.EXTRA_NOTE_ID, note.id)
                            // Передача тексту нотатки
                            putExtra(Constants.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
                            // Передача ID вулика
                            putExtra(Constants.EXTRA_HIVE_ID, targetHiveId) // ✅ ВИПРАВЛЕНО
                            // Передача типу запису
                            putExtra(Constants.EXTRA_ENTRY_TYPE, note.type)
                        }
                        startActivity(intent)
                    }
                }
            }
            .show()
    }

    /**
     * Спостерігає за результатами пошуку у ViewModel та оновлює адаптер RecyclerView.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                searchResultsAdapter.submitList(results)
                if (results.isEmpty() && searchQueryInput.text.isNotBlank()) {
                    val message = getString(R.string.search_not_found, searchQueryInput.text)
                    Toast.makeText(this@SearchActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Налаштовує компоненти для голосового розпізнавання Vosk.
     */
    private fun setupVosk() {
        microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)
        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnSearch.isEnabled = true
        } else {
            microphoneBtnSearch.isEnabled = false
            Toast.makeText(this, getString(R.string.vosk_model_loading), Toast.LENGTH_LONG).show()
        }
        searchQueryInput.requestFocus()
    }

    /**
     * Запускає або зупиняє прослуховування мікрофона.
     */
    private fun toggleListening() {
        if (speechService != null) {
            stopListening()
            Toast.makeText(this, getString(R.string.voice_input_stopped), Toast.LENGTH_SHORT).show()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            } else {
                startListening()
                Toast.makeText(this, getString(R.string.voice_input_listening), Toast.LENGTH_SHORT).show()
                microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_active_color)
            }
        }
    }

    /**
     * Запускає процес прослуховування Vosk.
     */
    private fun startListening() {
        val currentVoskModel = BeekeeperApplication.voskModel
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
        microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)
    }

    // --------------------------------------------------------------------------
    // Реалізація RecognitionListener
    // --------------------------------------------------------------------------

    /**
     * Отримує остаточний результат розпізнавання мови, додає його до поля вводу
     * та виконує пошук.
     */
    override fun onResult(hypothesis: String) {
        stopListening() // Зупиняємо прослуховування після отримання результату
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
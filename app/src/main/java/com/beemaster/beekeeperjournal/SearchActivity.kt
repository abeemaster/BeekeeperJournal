// SearchActivity.kt файл що відповідає за пошук

package com.beemaster.beekeeperjournal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import androidx.activity.result.contract.ActivityResultContracts

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
    private lateinit var hiveRepository: HiveRepository // ✅ ДОДАНО: Репозиторій для вуликів

    private var speechService: SpeechService? = null

    // У файлі SearchActivity.kt
    private val editNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Логіка, яка раніше була в onActivityResult
            performSearch(searchQueryInput.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        // При поверненні на екран пошуку, оновлюємо результати.
        performSearch(searchQueryInput.text.toString())
        Log.d(TAG, "SearchActivity: onResume called. Re-performing search to ensure updated hive names in results.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)



        // ✅ Підключаємо бічну панель за допомогою єдиного методу
        DrawerManager.setupDrawer(this)

        hiveRepository = HiveRepository(this) // ✅ ДОДАНО: Ініціалізація репозиторію

        searchQueryInput = findViewById(R.id.searchQueryInput)
        microphoneBtnSearch = findViewById(R.id.microphoneBtnSearch)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        // ✅ ЗМІНЮЄМО ІНІЦІАЛІЗАЦІЮ АДАПТЕРА
        searchResultsAdapter = SearchResultsAdapter(
            mutableListOf(),
            onItemLongClick = { note ->
                showOptionsDialog(note)
            }
        )
        searchResultsRecyclerView.adapter = searchResultsAdapter
        microphoneBtnSearch.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_color))

        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnSearch.isEnabled = true
        } else {
            microphoneBtnSearch.isEnabled = false
            Toast.makeText(this, "Vosk модель завантажується, голосовий пошук недоступний.", Toast.LENGTH_LONG).show()
        }

        searchExecuteButton.setOnClickListener {
            performSearch(searchQueryInput.text.toString())
            hideKeyboard()
        }

        microphoneBtnSearch.setOnClickListener {
            if (speechService != null) {
                stopListening()
                Toast.makeText(this, "Голосовий ввід зупинено.", Toast.LENGTH_SHORT).show()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
                } else {
                    startListening()
                    Toast.makeText(this, "Слухаю...", Toast.LENGTH_SHORT).show()
                    microphoneBtnSearch.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_active_color))
                }
            }
        }

        // ✅ ДОДАЙТЕ ЦЕЙ НОВИЙ БЛОК ДЛЯ АВТОМАТИЧНОГО ПОКАЗУ КЛАВІАТУРИ
        searchQueryInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        // ✅ Якщо ви хочете, щоб клавіатура з'являлася при старті активності,
        // просто викличте requestFocus()
        searchQueryInput.requestFocus()
    }

    // ✅ ДОДАЄМО НОВІ ФУНКЦІЇ ДЛЯ ДІАЛОГОВИХ ВІКОН ТА ПЕРЕХОДУ

    private fun showOptionsDialog(note: Note) {
        val options = arrayOf("Редагувати запис", "Перейти у вулик")

        // Створення діалогового вікна
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Оберіть дію")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> { // Редагувати запис
                    val hiveName = hiveRepository.readHivesFromJson().find { it.number == note.hiveNumber }?.name ?: "Вулик №${note.hiveNumber}"
                    showEditNoteDialog(note, hiveName)
                }
                1 -> { // Перейти у вулик
                    navigateToHive(note)
                }
            }
        }
        builder.show()
    }

    private fun showEditNoteDialog(note: Note, hiveName: String) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
            putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, note.type)
            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, note.hiveNumber)
            putExtra(EditNoteActivity.EXTRA_HIVE_NAME, hiveName)
        }
        // ✅ Використовуємо новий лаунчер
        editNoteLauncher.launch(intent)
    }

    private fun navigateToHive(note: Note) {
        val intent = Intent(this, HiveInfoActivity::class.java).apply {
            putExtra(HiveInfoActivity.EXTRA_HIVE_NUMBER, note.hiveNumber)
        }
        startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
                Toast.makeText(this, "Слухаю...", Toast.LENGTH_SHORT).show()
                microphoneBtnSearch.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_active_color))
            } else {
                Toast.makeText(this, "Дозвіл на запис аудіо відхилено. Голосовий ввід недоступний.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListening() {
        val currentVoskModel = BeekeeperApplication.voskModel
        if (currentVoskModel == null) {
            Toast.makeText(this, "Модель Vosk ще не завантажена. Зачекайте або перезапустіть додаток.", Toast.LENGTH_SHORT).show()
            microphoneBtnSearch.isEnabled = false
            return
        }
        try {
            val rec = Recognizer(currentVoskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Помилка запуску розпізнавання: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    private fun stopListening() {
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
        microphoneBtnSearch.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_color))
    }

    override fun onResult(hypothesis: String) {
        try {
            val jsonResult = JSONObject(hypothesis)
            val text = jsonResult.optString("text", "")
            if (text.isNotEmpty()) {
                searchQueryInput.setText(text)
                performSearch(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    override fun onPartialResult(hypothesis: String) {
    }

    override fun onFinalResult(hypothesis: String) {
    }

    override fun onError(exception: Exception) {
        Log.e(TAG, "onError: ${exception.message}", exception)
        Toast.makeText(this, "Помилка голосового вводу: ${exception.message}", Toast.LENGTH_LONG).show()
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "onTimeout: Recognition timeout. Stopping recording.")
        Toast.makeText(this, "Тайм-аут голосового вводу. Запис зупинено.", Toast.LENGTH_SHORT).show()
        stopListening()
    }

    private fun performSearch(query: String) {

        val allNotes = hiveRepository.readAllNotesFromJson()
        val allHives = hiveRepository.readHivesFromJson() // ✅ ВИПРАВЛЕНО: Завантажуємо вулики з репозиторію

        val filteredNotes = if (query.isBlank()) {
            emptyList()
        } else {
            allNotes.filter { note ->
                note.text.contains(query, ignoreCase = true) ||
                        note.date.contains(query, ignoreCase = true) ||
                        note.type.contains(query, ignoreCase = true) ||
                        (note.hiveNumber.toString() == query && note.type != "general") ||
                        (note.type == "hive" && allHives.find { it.number == note.hiveNumber }?.name?.contains(query, ignoreCase = true) == true)
            }.sortedByDescending { it.timestamp }
        }

        val searchResults = filteredNotes.map { note ->
            val hiveName = if (note.type == "general") {
                "Загальні записи"
            } else {
                val foundHive = allHives.find { it.number == note.hiveNumber }
                foundHive?.name ?: "Вулик №${note.hiveNumber}"
            }
            Log.d(TAG, "Note ID: ${note.id}, Hive Name resolved: $hiveName, Original Hive Number: ${note.hiveNumber}")
            NoteSearchResult(note, hiveName)
        }

        searchResultsAdapter.updateData(searchResults)

        if (searchResults.isEmpty() && query.isNotBlank()) {
            Toast.makeText(this, "Записів за запитом \"$query\" не знайдено.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchQueryInput.windowToken, 0)
    }
}


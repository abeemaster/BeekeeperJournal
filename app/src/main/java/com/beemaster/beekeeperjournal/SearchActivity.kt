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
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import db.NoteEntity

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
    private lateinit var hiveRepository: HiveRepository
    private lateinit var noteRepository: NoteRepository
    private var speechService: SpeechService? = null
    private val editNoteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            performSearch(searchQueryInput.text.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        performSearch(searchQueryInput.text.toString())
        Log.d(TAG, "SearchActivity: onResume called. Re-performing search to ensure updated hive names in results.")
    }

    override fun onResult(hypothesis: String) {
        try {
            val jsonResult = org.json.JSONObject(hypothesis)
            val text = jsonResult.optString("text", "")
            if (text.isNotEmpty()) {
                searchQueryInput.append("$text ")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        DrawerManager.setupDrawer(this)

        val appDatabase = (application as BeekeeperApplication).database
        hiveRepository = HiveRepository(appDatabase.noteDao(), appDatabase.hiveDao())
        noteRepository = NoteRepository(appDatabase.noteDao())

        searchQueryInput = findViewById(R.id.searchQueryInput)
        microphoneBtnSearch = findViewById(R.id.microphoneBtnSearch)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)

        searchResultsAdapter = SearchResultsAdapter(
            mutableListOf(),
            onItemLongClick = { note ->
                showOptionsDialog(note)
            }
        )
        searchResultsRecyclerView.adapter = searchResultsAdapter
        microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)

        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnSearch.isEnabled = true
        } else {
            microphoneBtnSearch.isEnabled = false
            Toast.makeText(this, "Vosk модель завантажується, голосовий пошук недоступний.", Toast.LENGTH_LONG).show()
        }

        searchExecuteButton.setOnClickListener {
            performSearch(searchQueryInput.text.toString())
            hideKeyboard() // ✅ ВИКЛИКАЄМО МЕТОД, ЯКИЙ ВИЩЕ ВІДСУТНІЙ
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
                    microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_active_color)
                }
            }
        }

        searchQueryInput.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        searchQueryInput.requestFocus()
    }

    private fun showOptionsDialog(note: Note) {
        val options = arrayOf("Редагувати запис", "Перейти у вулик")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Оберіть дію")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    lifecycleScope.launch {
                        val hive = hiveRepository.getHiveByNumber(note.hiveNumber)
                        val hiveName = hive?.name ?: "Вулик №${note.hiveNumber}"
                        showEditNoteDialog(note, hiveName)
                    }
                }
                1 -> {
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
        editNoteLauncher.launch(intent)
    }

    private fun navigateToHive(note: Note) {
        if (note.type == "general") {
            Toast.makeText(this, "Цей запис є загальним і не належить до конкретного вулика.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, HiveInfoActivity::class.java).apply {
            putExtra(HiveInfoActivity.EXTRA_HIVE_NUMBER, note.hiveNumber)
            putExtra("TYPE", note.type)
            putExtra("TITLE", "Вулик №${note.hiveNumber}")
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
                microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_active_color)
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
        microphoneBtnSearch.backgroundTintList = ContextCompat.getColorStateList(this, R.color.microphone_button_color)
    }

    override fun onPartialResult(hypothesis: String) {}

    override fun onFinalResult(hypothesis: String) {}

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

    // ✅ ВИПРАВЛЕНО: тепер ця функція правильно викликає логіку пошуку
    private fun performSearch(query: String) {
        noteRepository.allNotes.observe(this, Observer { notes -> // notes є List<NoteEntity>
            lifecycleScope.launch {
                val allHives = hiveRepository.getAllHives()

                val filteredNotes = if (query.isBlank()) {
                    emptyList()
                } else {
                    notes.filter { noteEntity -> // ✅ Змінено назву, щоб не плутати з Note
                        noteEntity.text.contains(query, ignoreCase = true) ||
                                noteEntity.date.contains(query, ignoreCase = true) ||
                                noteEntity.type.contains(query, ignoreCase = true) ||
                                (noteEntity.hiveNumber.toString() == query && noteEntity.type != "general") ||
                                (noteEntity.type == "hive" && allHives.find { it.hiveNumber == noteEntity.hiveNumber }?.name?.contains(query, ignoreCase = true) == true)
                    }.sortedByDescending { it.timestamp }
                }

                // ✅ ВИПРАВЛЕНО: Перетворення NoteEntity на Note для адаптера
                val searchResults = filteredNotes.map { noteEntity ->
                    val hiveName = if (noteEntity.type == "general") {
                        "Загальні записи"
                    } else {
                        val foundHive = allHives.find { it.hiveNumber == noteEntity.hiveNumber }
                        foundHive?.name ?: "Вулик №${noteEntity.hiveNumber}"
                    }
                    NoteSearchResult(Note(
                        id = noteEntity.id,
                        text = noteEntity.text,
                        type = noteEntity.type,
                        hiveNumber = noteEntity.hiveNumber,
                        timestamp = noteEntity.timestamp,
                        date = noteEntity.date
                    ), hiveName)
                }

                searchResultsAdapter.updateData(searchResults)
                if (searchResults.isEmpty() && query.isNotBlank()) {
                    Toast.makeText(this@SearchActivity, "Записів за запитом \"$query\" не знайдено.", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // ✅ ДОДАНО: відсутній метод для приховування клавіатури
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchQueryInput.windowToken, 0)
    }
}

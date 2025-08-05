package com.beemaster.beekeeperjournal

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.View
import android.view.ViewGroup
import com.beemaster.beekeeperjournal.MainActivity

// Об'єднана та виправлена декларація класу SearchActivity
class SearchActivity : AppCompatActivity(), RecognitionListener {

    companion object {
        private const val TAG = "SearchActivity"
        private const val NOTES_FILE_NAME = "notes.json"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
        const val REQUEST_CODE_EDIT_NOTE_FROM_SEARCH = 1003
    }

    private lateinit var searchQueryInput: EditText
    private lateinit var microphoneBtnSearch: ImageButton
    private lateinit var searchExecuteButton: MaterialButton
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var searchResultsAdapter: SearchResultsAdapter

    private var speechService: SpeechService? = null
    private val gson = Gson()


    override fun onResume() {
        super.onResume()
        // Цей метод викликається щоразу, коли SearchActivity стає видимою.
        // Ми викликаємо performSearch(), щоб вона заново завантажила список вуликів
        // і оновила результати, використовуючи найактуальніші назви.
        performSearch(searchQueryInput.text.toString())
        Log.d(TAG, "SearchActivity: onResume called. Re-performing search to ensure updated hive names in results.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        // Ініціалізація View елементів
        searchQueryInput = findViewById(R.id.searchQueryInput)
        microphoneBtnSearch = findViewById(R.id.microphoneBtnSearch)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        // Налаштування RecyclerView
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = SearchResultsAdapter(mutableListOf()) { note, hiveName ->
            // Обробник натискання на елемент результату пошуку
            val intent = Intent(this, EditNoteActivity::class.java).apply {
                putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
                putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
                putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, note.type)
                putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, note.hiveNumber)
                putExtra(EditNoteActivity.EXTRA_HIVE_NAME, hiveName) // Передаємо назву вулика
            }
            startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE_FROM_SEARCH)
        }
        searchResultsRecyclerView.adapter = searchResultsAdapter

        // Налаштування кнопки мікрофона
        microphoneBtnSearch.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.microphone_button_color))

        if (BeekeeperApplication.voskModel != null) {
            microphoneBtnSearch.isEnabled = true
        } else {
            microphoneBtnSearch.isEnabled = false
            Toast.makeText(this, "Vosk модель завантажується, голосовий пошук недоступний.", Toast.LENGTH_LONG).show()
        }

        // Обробник натискання кнопки "Пошук"
        searchExecuteButton.setOnClickListener {
            performSearch(searchQueryInput.text.toString())
            hideKeyboard() // Приховуємо клавіатуру після пошуку
        }

        // Обробник натискання кнопки мікрофона
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

        // *** Логіка для автоматичного фокусу та показу клавіатури - ПЕРЕНЕСЕНО СЮДИ ***
        searchQueryInput.requestFocus()
        searchQueryInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchQueryInput, InputMethodManager.SHOW_IMPLICIT)
        }, 100) // Невелика затримка для надійності
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_NOTE_FROM_SEARCH && resultCode == Activity.RESULT_OK) {
            // Після редагування нотатки, оновлюємо результати пошуку
            performSearch(searchQueryInput.text.toString())
        }
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
                searchQueryInput.setText(text) // Встановлюємо розпізнаний текст у поле пошуку
                performSearch(text) // Виконуємо пошук
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    override fun onPartialResult(hypothesis: String) {
        // Можна оновлювати поле вводу частковими результатами, якщо потрібно
        // searchQueryInput.setText(hypothesis)
    }

    override fun onFinalResult(hypothesis: String) {
        // Остаточний результат вже обробляється в onResult
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
        val allNotes = readAllNotesFromJson()
        val allHives = loadHiveListForSearch() // Завантажуємо список вуликів для отримання імен

        val filteredNotes = if (query.isBlank()) {
            emptyList() // Якщо запит порожній, не показуємо нічого
        } else {
            allNotes.filter { note ->
                note.text.contains(query, ignoreCase = true) ||
                        note.date.contains(query, ignoreCase = true) ||
                        note.type.contains(query, ignoreCase = true) ||
                        (note.hiveNumber.toString() == query && note.type != "general") ||
                        (note.type == "hive" && allHives.find { it.number == note.hiveNumber }?.name?.contains(query, ignoreCase = true) == true)
            }.sortedByDescending { it.timestamp } // Сортуємо за спаданням дати/часу
        }

        // Перетворюємо відфільтровані нотатки на NoteSearchResult, додаючи назву вулика
        val searchResults = filteredNotes.map { note ->
            val hiveName = if (note.type == "general") {
                "Загальні записи"
            } else {
                allHives.find { it.number == note.hiveNumber }?.name ?: "Вулик №${note.hiveNumber}"
            }
            NoteSearchResult(note, hiveName)
        }

        searchResultsAdapter.updateData(searchResults)

        if (searchResults.isEmpty() && query.isNotBlank()) {
            Toast.makeText(this, "Записів за запитом \"$query\" не знайдено.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readAllNotesFromJson(): MutableList<Note> {
        val file = File(filesDir, NOTES_FILE_NAME)
        if (!file.exists() || file.length() == 0L) {
            return mutableListOf()
        }
        return try {
            FileReader(file).use { reader ->
                val type = object : TypeToken<MutableList<Note>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Помилка читання записів з файлу: ${e.message}", e)
            mutableListOf()
        }
    }

    private fun loadHiveListForSearch(): List<HiveData> {
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(MainActivity.KEY_HIVE_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<MutableList<HiveData>>() {}.type
            val loadedList = gson.fromJson<MutableList<HiveData>>(json, type) ?: mutableListOf()
            Log.d(TAG, "loadHiveListForSearch: Завантажено ${loadedList.size} вуликів з SharedPreferences.")
            loadedList
        } else {
            Log.d(TAG, "loadHiveListForSearch: Список вуликів порожній у SharedPreferences.")
            emptyList()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchQueryInput.windowToken, 0)
    }
}

// Клас для адаптера RecyclerView
class SearchResultsAdapter(
    private val searchResults: MutableList<NoteSearchResult>,
    private val onItemClick: (Note, String) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder>() {

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val noteDate: TextView = itemView.findViewById(R.id.noteDate)
        val noteText: TextView = itemView.findViewById(R.id.noteText)
        val noteTypeAndHive: TextView = itemView.findViewById(R.id.noteTypeAndHive)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.note_item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = searchResults[position]
        val note = result.note
        val hiveName = result.hiveName

        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(note.timestamp))

        holder.noteDate.text = formattedDate
        holder.noteText.text = note.text

        val typeText = when (note.type) {
            "general" -> "Загальні записи"
            "hive" -> "Інформація"
            "queen" -> "Матка"
            "notes" -> "Примітки"
            else -> note.type
        }
        holder.noteTypeAndHive.text = "$typeText. $hiveName"

        holder.itemView.setOnClickListener {
            onItemClick(note, hiveName)
        }
    }

    override fun getItemCount(): Int = searchResults.size

    fun updateData(newResults: List<NoteSearchResult>) {
        searchResults.clear()
        searchResults.addAll(newResults)
        notifyDataSetChanged()
    }
}

// Клас даних для результату пошуку, що включає назву вулика
data class NoteSearchResult(
    val note: Note,
    val hiveName: String
)
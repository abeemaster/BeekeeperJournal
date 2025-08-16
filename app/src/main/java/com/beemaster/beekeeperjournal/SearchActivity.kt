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
    private lateinit var hiveRepository: HiveRepository // ✅ ДОДАНО: Репозиторій для вуликів

    private var speechService: SpeechService? = null
    private val gson = Gson()

    override fun onResume() {
        super.onResume()
        // При поверненні на екран пошуку, оновлюємо результати.
        performSearch(searchQueryInput.text.toString())
        Log.d(TAG, "SearchActivity: onResume called. Re-performing search to ensure updated hive names in results.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        hiveRepository = HiveRepository(this) // ✅ ДОДАНО: Ініціалізація репозиторію

        searchQueryInput = findViewById(R.id.searchQueryInput)
        microphoneBtnSearch = findViewById(R.id.microphoneBtnSearch)
        searchExecuteButton = findViewById(R.id.searchExecuteButton)
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView)

        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        searchResultsAdapter = SearchResultsAdapter(mutableListOf()) { note, hiveName ->
            val intent = Intent(this, EditNoteActivity::class.java).apply {
                putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
                putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.text)
                putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, note.type)
                putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, note.hiveNumber)
                putExtra(EditNoteActivity.EXTRA_HIVE_NAME, hiveName)
            }
            startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE_FROM_SEARCH)
        }
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

        searchQueryInput.requestFocus()
        searchQueryInput.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchQueryInput, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_EDIT_NOTE_FROM_SEARCH && resultCode == Activity.RESULT_OK) {
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
        val allNotes = readAllNotesFromJson()
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

    // ✅ ВИДАЛЕНО: Цей метод більше не потрібен, оскільки ми використовуємо HiveRepository
    // private fun loadHiveListForSearch(): List<HiveData> {
    //     ...
    // }

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
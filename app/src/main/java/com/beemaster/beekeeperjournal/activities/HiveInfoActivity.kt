// HiveInfoActivity файл котрий спрацьовує при натисканні на кнопку "Вулик№"
// оновлено

package com.beemaster.beekeeperjournal.activities


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.viewmodel.HiveInfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import android.widget.LinearLayout.LayoutParams
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.beemaster.beekeeperjournal.activities.EditNoteActivity
import com.beemaster.beekeeperjournal.activities.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler
import android.os.Looper
import com.beemaster.beekeeperjournal.db.NoteEntity
import com.beemaster.beekeeperjournal.db.HiveEntity
import kotlin.text.Typography.dagger

@AndroidEntryPoint
class HiveInfoActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val TAG = "HiveInfoActivity"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggleBtn: ImageButton
    private lateinit var navView: NavigationView
    private lateinit var infoTitle: TextView
    private lateinit var notesDisplayArea: LinearLayout
    private lateinit var newNoteButton: com.google.android.material.button.MaterialButton
    private lateinit var microphoneBtn: ImageButton
    private lateinit var queenBtn: Button
    private lateinit var hiveInfoBtn: Button
    private lateinit var notesBtn: Button
    private lateinit var currentHiveActualName: String

    private var currentHiveNumber: Int = 0
    private var currentEntryType: String = ""

    private val viewModel: HiveInfoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hive_info)

        Log.d(TAG, "HiveInfoActivity: Активність onCreate() запущено.")

        setupViews()
        setupListeners()
        setupNavigationView()
        loadInitialData()
        observeNotes()
    }

    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finish()
            }
            R.id.nav_general_notes -> {
                Toast.makeText(this, "Ви вже у Загальних записах", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerToggleBtn = findViewById(R.id.drawer_toggle_button)
        navView = findViewById(R.id.nav_view)
        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn)
        notesBtn = findViewById(R.id.notesBtn)
    }

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

    private fun loadInitialData() {
        currentHiveNumber = intent.getIntExtra(EXTRA_HIVE_NUMBER, 0)
        Log.d(TAG, "HiveInfoActivity: Отримано номер вулика: $currentHiveNumber")
        Log.d(TAG, "HiveInfoActivity: In onCreate, received hiveNumber: $currentHiveNumber")

        currentHiveActualName = if (currentHiveNumber == 0) "Загальні записи" else "Вулик №$currentHiveNumber"

        // Встановлюємо початковий тип записів на основі номера вулика
        currentEntryType = if (currentHiveNumber == 0) "general" else "hive"
        Log.d(TAG, "Тип записів встановлено: $currentEntryType")

        // ✅ Оновлюємо UI та завантажуємо дані лише один раз при завантаженні
        updateUIAndLoadData(currentEntryType)

        // ✅ Спостерігаємо за нотатками після початкового завантаження
        observeNotes()
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesDisplayArea.removeAllViews()
                if (notes.isEmpty()) {
                    val noNotesTextView = TextView(this@HiveInfoActivity).apply {
                        text = "Записів немає"
                        gravity = android.view.Gravity.CENTER
                        setTextColor(Color.GRAY)
                        setPadding(16, 16, 16, 16)
                    }
                    notesDisplayArea.addView(noNotesTextView)
                } else {
                    notes.forEach { note ->
                        val noteItemContainer = LinearLayout(this@HiveInfoActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, 0, 0, 16)
                            }
                            setBackgroundResource(R.drawable.note_item_background)
                            setPadding(16, 16, 16, 16)

                            setOnLongClickListener {
                                showNoteOptionsDialog(note)
                                true
                            }
                        }

                        val dateTextView = TextView(this@HiveInfoActivity).apply {
                            val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())
                            val formattedDate = dateFormat.format(Date(note.createdAt))
                            text = formattedDate
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            setTextColor(ContextCompat.getColor(this@HiveInfoActivity, R.color.green_700))
                            gravity = android.view.Gravity.END
                        }

                        val contentTextView = TextView(this@HiveInfoActivity).apply {
                            text = note.content
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                            setTextColor(Color.BLACK)
                        }

                        noteItemContainer.addView(dateTextView)
                        noteItemContainer.addView(contentTextView)
                        notesDisplayArea.addView(noteItemContainer)
                    }
                }
            }
        }
    }

    // ✅ НОВА ФУНКЦІЯ: відповідає лише за оновлення UI та завантаження нотаток
    private fun updateUIAndLoadData(entryType: String) {
        currentEntryType = entryType

        val title: String = when {
            currentHiveNumber == 0 -> "Загальні записи"
            entryType == "queen" -> "Матка $currentHiveActualName"
            entryType == "hive" -> " $currentHiveActualName"
            entryType == "notes" -> "Примітки $currentHiveActualName"
            else -> currentHiveActualName
        }
        infoTitle.text = title

        // ✅ Визначаємо видимість кнопок тут
        if (currentHiveNumber == 0) {
            queenBtn.visibility = View.GONE
            hiveInfoBtn.visibility = View.GONE
            notesBtn.visibility = View.GONE
        } else {
            queenBtn.visibility = View.VISIBLE
            hiveInfoBtn.visibility = View.VISIBLE
            notesBtn.visibility = View.VISIBLE
        }
        Log.d(TAG, "HiveInfoActivity: Запитуємо нотатки для номера: $currentHiveNumber і типу: $currentEntryType")
        viewModel.getNotesForHive(currentHiveNumber, currentEntryType)
    }

    private fun showInfo(entryType: String) {
        // ✅ Ця функція тепер просто викликає нову функцію
        updateUIAndLoadData(entryType)
    }

    // У файлі HiveInfoActivity.kt

    private fun openNoteEditorActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
            putExtra(EditNoteActivity.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivity(intent)

        // ✅ НІЯКОГО finish() та затримок тут.
        // HiveInfoActivity просто запускає нову активність.
    }

    private fun showNoteOptionsDialog(note: NoteEntity) {
        val options = arrayOf("Редагувати", "Видалити")
        AlertDialog.Builder(this)
            .setTitle("Оберіть дію")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Редагувати
                        val intent = Intent(this, EditNoteActivity::class.java).apply {
                            putExtra(EditNoteActivity.EXTRA_NOTE_ID, note.id)
                            putExtra(EditNoteActivity.EXTRA_ORIGINAL_NOTE_TEXT, note.content)
                            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, note.hiveId)
                            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, note.type)
                        }
                        startActivity(intent)
                    }
                    1 -> { // Видалити
                        showDeleteConfirmationDialog(note)
                    }
                }
            }
            .show()
    }

    private fun showDeleteConfirmationDialog(note: NoteEntity) {
        AlertDialog.Builder(this)
            .setTitle("Видалити запис?")
            .setMessage("Ви впевнені, що хочете видалити цю нотатку? Цю дію неможливо буде скасувати.")
            .setPositiveButton("Видалити") { dialog, which ->
                viewModel.deleteNote(note)
                Toast.makeText(this, "Запис видалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Скасувати", null)
            .show()
    }
}
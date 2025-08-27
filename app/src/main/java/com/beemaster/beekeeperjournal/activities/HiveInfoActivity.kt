// HiveInfoActivity файл котрий спрацьовує при натисканні на кнопку "Вулик№"
// оновлено

// У файлі HiveInfoActivity.kt
package com.beemaster.beekeeperjournal.activities

// У файлі HiveInfoActivity.kt

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
import com.beemaster.beekeeperjournal.activities.MainActivity // ✅ Додаємо імпорт для MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView // ✅ Додаємо цей імпорт
import android.view.MenuItem // ✅ Додаємо цей імпорт

@AndroidEntryPoint
class HiveInfoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HiveInfoActivity"
        const val EXTRA_HIVE_NUMBER = "com.beemaster.beekeeperjournal.HIVE_NUMBER"
    }

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerToggleBtn: ImageButton
    private lateinit var navView: NavigationView // ✅ Додаємо змінну для NavigationView
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

        setupViews()
        setupListeners()
        setupNavigationView() // ✅ Викликаємо нову функцію
        loadInitialData()
        observeNotes()
    }

    // ✅ Нова функція для налаштування бічної панелі
    // У файлі HiveInfoActivity.kt

    private fun setupNavigationView() {
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    // ✅ Додано анімацію переходу для плавного ефекту
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                // Тут можна додати інші елементи меню
                else -> false
            }
            // Закриваємо бічну панель після натискання
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerToggleBtn = findViewById(R.id.drawer_toggle_button)
        navView = findViewById(R.id.nav_view) // ✅ Знаходимо NavigationView за його ID
        infoTitle = findViewById(R.id.infoTitle)
        notesDisplayArea = findViewById(R.id.notesDisplayArea)
        microphoneBtn = findViewById(R.id.microphoneBtn)
        newNoteButton = findViewById(R.id.newNoteButton)
        queenBtn = findViewById(R.id.queenBtn)
        hiveInfoBtn = findViewById(R.id.hiveInfoBtn)
        notesBtn = findViewById(R.id.notesBtn)
    }

    private fun setupListeners() {
        newNoteButton.setOnClickListener { openNewNoteActivity() }
        microphoneBtn.setOnClickListener { openNewNoteActivity(startVoiceInput = true) }
        queenBtn.setOnClickListener { showInfo("queen") }
        hiveInfoBtn.setOnClickListener { showInfo("hive") }
        notesBtn.setOnClickListener { showInfo("notes") }

        drawerToggleBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    // ... решта вашого коду залишається без змін
    private fun loadInitialData() {
        val receivedNumberString = intent.getStringExtra(EXTRA_HIVE_NUMBER)
        currentHiveNumber = receivedNumberString?.toIntOrNull() ?: 0
        Log.d(TAG, "HiveInfoActivity: In onCreate, received hiveNumber: $currentHiveNumber")
        currentHiveActualName = "Вулик №$currentHiveNumber"
        showInfo("hive")
    }

    private fun observeNotes() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesDisplayArea.removeAllViews()
                if (notes.isEmpty()) {
                    Log.d(TAG, "No notes found for hive $currentHiveNumber, type $currentEntryType")
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

    private fun showInfo(entryType: String) {
        currentEntryType = entryType
        val title: String = when (currentEntryType) {
            "queen" -> "Матка $currentHiveActualName"
            "hive" -> currentHiveActualName
            "notes" -> "Примітки $currentHiveActualName"
            else -> currentHiveActualName
        }
        infoTitle.text = title
        viewModel.getNotesForHive(currentHiveNumber, currentEntryType)
    }

    private fun openNewNoteActivity(startVoiceInput: Boolean = false) {
        val intent = Intent(this, EditNoteActivity::class.java).apply {
            putExtra(EditNoteActivity.EXTRA_ENTRY_TYPE, currentEntryType)
            putExtra(EditNoteActivity.EXTRA_HIVE_NUMBER, currentHiveNumber)
            putExtra(EditNoteActivity.EXTRA_HIVE_NAME, currentHiveActualName)
            putExtra(EditNoteActivity.EXTRA_START_VOICE_INPUT, startVoiceInput)
        }
        startActivity(intent)
    }
}
// MainActivity Файл головної сторінки додатка

// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.HiveAdapter
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.db.NoteEntity
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggleButton: ImageButton
    private lateinit var generalNotesButton: MaterialButton
    private lateinit var hiveRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView

    private val viewModel: MainActivityViewModel by viewModels()

    private val selectJsonFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            importNotesFromJson(it)
        }
    }

    private fun startImport() {
        selectJsonFile.launch("application/json")
    }

    private fun importNotesFromJson(uri: Uri) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream: InputStream? = contentResolver.openInputStream(uri)
                    val jsonString = inputStream?.bufferedReader().use { it?.readText() }

                    if (jsonString.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Файл порожній або не вдалося прочитати.", Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }

                    data class ImportedNote(
                        val hiveNumber: Int,
                        val text: String,
                        val timestamp: Long? = null,
                        val type: String,
                        val date: String,
                        val id: String? = null
                    )
                    data class DataBackup(
                        val hiveList: List<Any>,
                        val notes: List<ImportedNote>
                    )

                    val gson = Gson()
                    val type = object : TypeToken<DataBackup>() {}.type
                    val backupData: DataBackup = gson.fromJson(jsonString, type)

                    val dateFormat = SimpleDateFormat("dd-MM-yy", Locale.getDefault())

                    backupData.notes.forEach { importedNote ->
                        val dateString = importedNote.date
                        val date = dateFormat.parse(dateString)
                        val timestamp = date?.time ?: System.currentTimeMillis()

                        val newNote = NoteEntity(
                            hiveId = importedNote.hiveNumber,
                            type = importedNote.type,
                            title = "",
                            content = importedNote.text,
                            createdAt = timestamp,
                            imagePath = null
                        )
                        viewModel.addNote(newNote)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Імпорт даних успішно завершено!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Помилка імпорту: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()
        observeHives()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggleButton = findViewById(R.id.drawer_toggle_button)
        generalNotesButton = findViewById(R.id.nav_general_notes)
        hiveRecyclerView = findViewById(R.id.hiveListRecyclerView)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
    }

    private fun setupListeners() {
        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        navigationView.setNavigationItemSelectedListener(this)

        // ✅ ВИПРАВЛЕНО: Кнопка "Загальні записи" тепер правильно передає 0
        generalNotesButton.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra(HiveInfoActivity.EXTRA_HIVE_NUMBER, 0)
            }
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        hiveRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        hiveAdapter = HiveAdapter(
            onClick = { hive ->
                // ✅ ВИПРАВЛЕНО: Цей рядок відповідає за перехід на сторінку вулика
                Log.d("MainActivity", "Надсилаємо номер вулика: ${hive.hiveNumber}")

                val intent = Intent(this, HiveInfoActivity::class.java).apply {
                    putExtra(HiveInfoActivity.EXTRA_HIVE_NUMBER, hive.hiveNumber)
                }
                startActivity(intent)
            },
            onLongClick = { hive ->
                DialogUtils.showHiveOptionsDialog(
                    context = this,
                    hive = hive,
                    onEditName = {
                        DialogUtils.showEditNameDialog(
                            context = this,
                            hive = hive,
                            onSave = { newName ->
                                val updatedHive = hive.copy(name = newName)
                                viewModel.updateHive(updatedHive)
                                Toast.makeText(this, "Назву вулика оновлено!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onSelectPrimaryColor = {
                        DialogUtils.showColorPickerDialog(
                            context = this
                        ) { newColor ->
                            val updatedHive = hive.copy(color = newColor)
                            viewModel.updateHive(updatedHive)
                            Toast.makeText(this, "Основний колір оновлено!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSelectSecondaryColor = {
                        DialogUtils.showColorPickerDialog(
                            context = this
                        ) { newColor ->
                            val updatedHive = hive.copy(secondaryColor = newColor)
                            viewModel.updateHive(updatedHive)
                            Toast.makeText(this, "Додатковий колір оновлено!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteHive = {
                        DialogUtils.showDeleteHiveDialog(
                            context = this,
                            hive = hive,
                            onDeleteConfirmed = {
                                viewModel.deleteHive(hive)
                                Toast.makeText(this, "Вулик видалено.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        )
        hiveRecyclerView.adapter = hiveAdapter
    }

    private fun observeHives() {
        lifecycleScope.launch {
            viewModel.hives.collect { hives ->
                hiveAdapter.submitList(hives)
                hiveCountTextView.text = getString(R.string.hive_count, hives.size)
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        when (item.itemId) {
            R.id.nav_home -> {
                Toast.makeText(this, "Головна сторінка", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_search -> {
                Toast.makeText(this, "Пошук", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_add_hive -> {
                addHive()
            }
            R.id.nav_sync -> {
                startImport()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "Налаштування", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    private fun addHive() {
        val currentHives = viewModel.hives.value
        if (currentHives.size >= 100) {
            Toast.makeText(this, "Досягнуто максимальну кількість вуликів", Toast.LENGTH_SHORT).show()
        } else {
            DialogUtils.showAddHiveDialog(this,
                onHiveAdded = { hiveName, hiveNumber ->
                    val newHive = HiveEntity(
                        hiveNumber = hiveNumber,
                        name = hiveName,
                        color = this.getColor(R.color.color_white),
                        secondaryColor = 0
                    )
                    viewModel.addHive(newHive)
                }
            )
        }
    }
}
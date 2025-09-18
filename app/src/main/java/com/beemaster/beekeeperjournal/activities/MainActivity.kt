// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal.activities

import android.R.attr.type
import android.annotation.SuppressLint
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.HiveAdapter
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.beemaster.beekeeperjournal.models.BackupData
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
import java.io.InputStreamReader
import java.io.OutputStreamWriter

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

    private val gson = Gson()

    private val getExportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { exportData(it) }
        }
    }

    private val getImportFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { importData(it) }
        }
    }

    @SuppressLint("MissingInflatedId")
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
                val intent = Intent(this, HiveInfoActivity::class.java).apply {
                    putExtra(HiveInfoActivity.EXTRA_HIVE_NUMBER, hive.hiveNumber)
                }
                Log.d("MainActivity", "onClick: Передача номера вулика: ${hive.hiveNumber} тип: $type\"")
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
        // ✅ ВИПРАВЛЕНО: Використовуємо .collect для Flow
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
                openSearchActivity()
            }
            R.id.nav_add_hive -> {
                addHive()
            }
            R.id.nav_sync -> {
                showSyncOptionsDialog()
            }
            R.id.nav_profitability -> {
                openProfitabilityActivity()
            }
            R.id.nav_settings -> {
                openSettingsActivity()
            }
            R.id.nav_exit_button -> {
                finishAffinity()
            }
        }
        return true
    }
    private fun openProfitabilityActivity() {
        val intent = Intent(this, ProfitabilityActivity::class.java)
        startActivity(intent)
    }
    private fun openSearchActivity() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
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

    private fun showSyncOptionsDialog() {
        val options = arrayOf("Створити резервну копію", "Відновити з резервної копії")
        AlertDialog.Builder(this)
            .setTitle("Оберіть дію")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> getExportFile.launch("beekeeper_backup.json")
                    1 -> getImportFile.launch(arrayOf("application/json"))
                }
            }
            .show()
    }

    private suspend fun exportData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val hives = viewModel.getAllHivesSuspend()
                val notes = viewModel.getAllNotesSuspend()
                val backupData = BackupData(hives, notes)
                val json = gson.toJson(backupData)

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Резервна копія створена успішно!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Backup", "Помилка експорту даних", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Помилка при створенні резервної копії: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun importData(uri: Uri) {
        withContext(Dispatchers.IO) {
            try {
                val json = contentResolver.openInputStream(uri)?.use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        reader.readText()
                    }
                } ?: return@withContext

                val backupDataType = object : TypeToken<BackupData>() {}.type
                val backupData: BackupData = gson.fromJson(json, backupDataType)

                viewModel.importHives(backupData.hives)
                viewModel.importNotes(backupData.notes)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Дані відновлено успішно!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Backup", "Помилка імпорту даних", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Помилка при відновленні даних: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }
}
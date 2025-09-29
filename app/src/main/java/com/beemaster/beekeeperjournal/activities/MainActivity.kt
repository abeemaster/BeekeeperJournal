// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.HiveAdapter
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.utils.BackupManager
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggleButton: ImageButton
    private lateinit var generalNotesButton: MaterialButton
    private lateinit var hiveRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private lateinit var backupManager: BackupManager

    private val viewModel: MainActivityViewModel by viewModels()

    /**
     * Контракт Activity Result для ініціації експорту даних у JSON-файл.
     * Викликає BackupManager.exportData з отриманим URI.
     */
    private val getExportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { backupManager.exportData(it) }
        }
    }

    /**
     * Контракт Activity Result для ініціації імпорту даних з JSON-файлу.
     * Викликає BackupManager.importData з обраним URI.
     */
    private val getImportFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { backupManager.importData(it) }
        }
    }

    /**
     * Основний життєвий цикл Activity. Ініціалізує компоненти,
     * налаштовує слухачів та перевіряє наявність першого вулика.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        setupRecyclerView()
        observeHives()

        // Перевірка та створення Вулика №1, якщо він відсутній
        lifecycleScope.launch {
            val existingHive = viewModel.getHiveByNumber("1")
            if (existingHive == null) {
                val defaultHiveNumber = "1"
                val newHive = HiveEntity(
                    hiveNumber = defaultHiveNumber,
                    name = defaultHiveNumber, // name використовує hiveNumber для спрощення логіки
                    color = this@MainActivity.getColor(R.color.color_white),
                    secondaryColor = 0
                )
                viewModel.addHive(newHive)
            }
        }

        backupManager = BackupManager(this, viewModel)
    }

    /**
     * Ініціалізує всі елементи View, використовуючи findViewById.
     */
    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggleButton = findViewById(R.id.drawer_toggle_button)
        generalNotesButton = findViewById(R.id.nav_general_notes)
        hiveRecyclerView = findViewById(R.id.hiveListRecyclerView)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
    }

    /**
     * Налаштовує всі слухачі подій: кнопку навігаційного меню, елементи меню,
     * та кнопку "Загальні нотатки".
     */
    private fun setupListeners() {
        // Відкриття навігаційного меню
        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Обробка вибору елементів у навігаційному меню
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Користувач вже на головній сторінці, нічого не робимо
                }
                R.id.nav_search -> {
                    openSearchActivity()
                }
                R.id.nav_add_hive -> {
                    addHive()
                }
                R.id.nav_sync -> {
                    // Відображення діалогу синхронізації (експорт/імпорт)
                    DialogUtils.showSyncOptionsDialog(
                        context = this,
                        onExport = {
                            getExportFile.launch(getString(R.string.backup_filename))
                        },
                        onImport = {
                            getImportFile.launch(arrayOf("application/json"))
                        }
                    )
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
            true
        }

        // Перехід до "Загальних нотаток" (id вулика = 0)
        generalNotesButton.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                putExtra(Constants.EXTRA_HIVE_NUMBER, 0)
            }
            startActivityWithSlideAnimation(intent)
        }
    }

    /**
     * Налаштовує RecyclerView для відображення списку вуликів.
     * Включає обробники натискання (перехід до HiveInfoActivity) та довгого натискання (опції вулика).
     */
    private fun setupRecyclerView() {
        hiveRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        hiveAdapter = HiveAdapter(
            // Обробка звичайного натискання: перехід до деталей вулика
            onClick = { hive ->
                val intent = Intent(this, HiveInfoActivity::class.java).apply {
                    putExtra(Constants.EXTRA_HIVE_ID, hive.id)
                }
                startActivityWithSlideAnimation(intent)
            },
            // Обробка довгого натискання: відображення опцій редагування
            onLongClick = { hive ->
                DialogUtils.showHiveOptionsDialog(
                    context = this,
                    hive = hive,
                    // Редагування номера вулика
                    onEditNumber = {
                        DialogUtils.showEditHiveNumberDialog(
                            context = this,
                            currentNumber = hive.hiveNumber,
                            onSave = { newNumber ->
                                lifecycleScope.launch {
                                    val existingHive = viewModel.getHiveByNumber(newNumber)
                                    // Перевірка на унікальність номера
                                    if (existingHive != null && existingHive.id != hive.id) {
                                        Toast.makeText(this@MainActivity, R.string.hive_number_exists, Toast.LENGTH_LONG).show()
                                    } else {
                                        // Оновлення hiveNumber і name (оскільки name більше не використовується явно)
                                        val updatedHive = hive.copy(hiveNumber = newNumber, name = newNumber)
                                        viewModel.updateHive(updatedHive)
                                        Toast.makeText(this@MainActivity, R.string.hive_number_updated, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    },
                    // Вибір основного кольору
                    onSelectPrimaryColor = {
                        DialogUtils.showColorPickerDialog(
                            context = this
                        ) { newColor ->
                            val updatedHive = hive.copy(color = newColor)
                            viewModel.updateHive(updatedHive)
                            Toast.makeText(this, R.string.primary_color_updated, Toast.LENGTH_SHORT).show()
                        }
                    },
                    // Вибір додаткового кольору
                    onSelectSecondaryColor = {
                        DialogUtils.showColorPickerDialog(
                            context = this
                        ) { newColor ->
                            val updatedHive = hive.copy(secondaryColor = newColor)
                            viewModel.updateHive(updatedHive)
                            Toast.makeText(this, R.string.secondary_color_updated, Toast.LENGTH_SHORT).show()
                        }
                    },
                    // Видалення вулика
                    onDeleteHive = {
                        DialogUtils.showDeleteHiveDialog(
                            context = this,
                            hive = hive,
                            onDeleteConfirmed = {
                                viewModel.deleteHive(hive)
                                Toast.makeText(this, R.string.hive_deleted, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        )
        hiveRecyclerView.adapter = hiveAdapter
    }

    /**
     * Спостерігає за списком вуликів з ViewModel і оновлює
     * RecyclerView та лічильник вуликів при зміні даних.
     */
    private fun observeHives() {
        lifecycleScope.launch {
            viewModel.hives.collect { hives ->
                hiveAdapter.submitList(hives)
                hiveCountTextView.text = getString(R.string.hive_count, hives.size)
            }
        }
    }

    /**
     * Відкриває Activity для перегляду прибутковості.
     */
    private fun openProfitabilityActivity() {
        val intent = Intent(this, ProfitabilityActivity::class.java)
        startActivityWithSlideAnimation(intent)
    }

    /**
     * Відкриває Activity для пошуку.
     */
    private fun openSearchActivity() {
        val intent = Intent(this, SearchActivity::class.java)
        startActivityWithSlideAnimation(intent)
    }

    /**
     * Відображає діалог для додавання нового вулика.
     * Виконує перевірку на унікальність номера та максимальну кількість вуликів.
     */
    private fun addHive() {
        val currentHives = viewModel.hives.value
        if (currentHives.size >= 100) {
            Toast.makeText(this, R.string.max_hives_reached, Toast.LENGTH_SHORT).show()
        } else {
            DialogUtils.showAddHiveDialog(this,
                onHiveAdded = { hiveNumber ->
                    lifecycleScope.launch {
                        val existingHive = viewModel.getHiveByNumber(hiveNumber)
                        if (existingHive != null) {
                            Toast.makeText(this@MainActivity, R.string.hive_number_exists, Toast.LENGTH_LONG).show()
                        } else {
                            val newHive = HiveEntity(
                                hiveNumber = hiveNumber,
                                name = hiveNumber, // name використовує hiveNumber
                                color = this@MainActivity.getColor(R.color.color_white),
                                secondaryColor = 0
                            )
                            viewModel.addHive(newHive)
                        }
                    }
                }
            )
        }
    }

    /**
     * Відкриває Activity для налаштувань.
     */
    private fun openSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityWithSlideAnimation(intent)
    }
}
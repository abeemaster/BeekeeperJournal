// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.HiveAdapter
import com.beemaster.beekeeperjournal.data.HiveCreator
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.dialogs.AddHiveDialogFragment
import com.beemaster.beekeeperjournal.dialogs.HiveOptionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.SyncOptionsDialogFragment
import com.beemaster.beekeeperjournal.dialogs.IOnHiveAddedListener
import com.beemaster.beekeeperjournal.utils.BackupManager
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveAddResult
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SyncOptionsDialogFragment.SyncOptionsListener, IOnHiveAddedListener {
    @Inject
    lateinit var hiveCreator: HiveCreator
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggleButton: ImageButton
    private lateinit var generalNotesButton: MaterialButton
    private lateinit var hiveRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView
    private lateinit var backupManager: BackupManager

    private val viewModel: MainActivityViewModel by viewModels()

    // Activity Result Launcher для вибору місця збереження файлу експорту.
    private val getExportFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { backupManager.exportData(it) }
        }
    }

    // Activity Result Launcher для вибору файлу імпорту.
    private val getImportFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch { backupManager.importData(it) }
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
        collectHiveEvents() // Запуск спостереження за подіями ViewModel

        // Перевірка та створення "Вулика №1" за замовчуванням, якщо він відсутній.
        lifecycleScope.launch {
            val existingHive = viewModel.getHiveByNumber("1")
            if (existingHive == null) {
                val defaultHiveNumber = "1"

                val newHive = hiveCreator.createDefaultHiveEntity(
                    defaultHiveNumber
                )
                viewModel.addHive(newHive)
            }
        }

        backupManager = BackupManager(this, viewModel)
    }

    /**
     * Ініціалізує всі елементи інтерфейсу (View).
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
     * Налаштовує всі слухачі подій для елементів інтерфейсу.
     */
    private fun setupListeners() {
        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_home -> {
                }
                R.id.nav_search -> {
                    openSearchActivity()
                }
                R.id.nav_add_hive -> {
                    addHive()
                }
                R.id.nav_sync -> {
                    SyncOptionsDialogFragment().show(supportFragmentManager, "SyncOptions")
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

        generalNotesButton.setOnClickListener {
            val intent = Intent(this, HiveInfoActivity::class.java).apply {
                // Використання ID 0 для позначення загальних нотаток.
                putExtra(Constants.EXTRA_HIVE_ID, 0)
            }
            startActivityWithSlideAnimation(intent)
        }
    }

    /**
     * Налаштовує RecyclerView для відображення списку вуликів.
     * Містить логіку обробки натискань та довгих натискань.
     */
    private fun setupRecyclerView() {
        hiveRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        hiveAdapter = HiveAdapter(
            onClick = { hive ->
                val intent = Intent(this, HiveInfoActivity::class.java).apply {
                    putExtra(Constants.EXTRA_HIVE_ID, hive.id)
                    putExtra(Constants.EXTRA_HIVE_NUMBER, hive.hiveNumber)
                    putExtra(Constants.EXTRA_HIVE_COLOR, hive.color)
                    putExtra(Constants.EXTRA_HIVE_SECONDARY_COLOR, hive.secondaryColor)
                }
                startActivityWithSlideAnimation(intent)
            },
            onLongClick = { hive ->
                showHiveOptionsDialog(hive)
            }
        )
        hiveRecyclerView.adapter = hiveAdapter
    }

    /**
     * Запускає спостереження за списком вуликів з ViewModel.
     * Оновлює адаптер RecyclerView та лічильник вуликів.
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
     * Обробляє одноразові події додавання вуликів, надіслані з ViewModel.
     * Відображає відповідні повідомлення Toast для користувача (успіх/помилка/ліміт).
     */
    private fun collectHiveEvents() {
        lifecycleScope.launch {
            viewModel.hiveEvents.collect { result ->
                // Визначаємо ідентифікатор рядка, що відповідає результату
                val messageResId = when (result) {
                    // Логіка додавання
                    HiveAddResult.SUCCESS -> R.string.hive_added_success

                    // ОБРОБКА КОНФЛІКТУ НОМЕРА ПРИ ДОДАВАННІ/РЕДАГУВАННІ
                    HiveAddResult.EXISTS -> R.string.hive_number_exists

                    // Логіка ліміту
                    HiveAddResult.LIMIT_REACHED -> R.string.max_hives_reached
                }
                Toast.makeText(this@MainActivity, messageResId, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Відкриває Activity для перегляду рентабельності.
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
     * Запускає діалог додавання нового вулика.
     * Тепер просто викликає DialogFragment.
     */
    private fun addHive() {
        AddHiveDialogFragment.newInstance()
            .show(supportFragmentManager, AddHiveDialogFragment.TAG)
    }

    /**
     * Обробка результату, повернутого з AddHiveDialogFragment.
     * Тут виконується бізнес-логіка додавання вулика.
     */
    override fun onHiveAdded(hiveNumber: String) {
        lifecycleScope.launch {
            // Логіка, яку ви раніше мали у лямбді:
            val newHive = hiveCreator.createDefaultHiveEntity(hiveNumber)
            viewModel.addNewHive(newHive)
        }
    }

    // ✅ Реалізуємо методи інтерфейсу
    override fun onExportSelected() {
        // Тут виконуємо логіку onExport, яка була в DialogUtils
        getExportFile.launch("beekeeper_backup.json")
    }

    override fun onImportSelected() {
        // Тут виконуємо логіку onImport, яка була в DialogUtils
        getImportFile.launch(arrayOf("application/json"))
    }
    /**
     * Відкриває Activity для налаштувань.
     */
    private fun openSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityWithSlideAnimation(intent)
    }

    // ---------------------------------------------------------------------
    // НОВІ МЕТОДИ ДЛЯ ЧИСТОЇ АРХІТЕКТУРИ DIALOGFRAGMENT
    // ---------------------------------------------------------------------

    /**
     * Відображає діалогове вікно опцій вулика (через DialogFragment)
     * та обробляє обрану дію.
     */
    private fun showHiveOptionsDialog(hive: HiveEntity) {
        // Передаємо дані для того, щоб Fragment міг працювати з конкретним вуликом.
        val dialog = HiveOptionsDialogFragment.newInstance(
            hive.id.toLong(),
            hive.hiveNumber,
            hive.color,
            hive.secondaryColor
        )
        // Викликаємо діалог через FragmentManager
        dialog.show(supportFragmentManager, HiveOptionsDialogFragment.TAG)
    }
}
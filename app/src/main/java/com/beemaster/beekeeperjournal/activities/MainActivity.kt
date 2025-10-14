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
import com.beemaster.beekeeperjournal.db.entity.HiveEntity // ✅ ДОДАНО: Необхідний імпорт
import com.beemaster.beekeeperjournal.fragments.EditHiveNumberDialogFragment
import com.beemaster.beekeeperjournal.utils.BackupManager
import com.beemaster.beekeeperjournal.utils.DialogUtils
import com.beemaster.beekeeperjournal.fragments.HiveOptionsDialogFragment
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveAddResult
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
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
                    // Перехід не потрібен, бо ми вже на головній сторінці.
                }
                R.id.nav_search -> {
                    openSearchActivity()
                }
                R.id.nav_add_hive -> {
                    addHive()
                }
                R.id.nav_sync -> {
                    DialogUtils.showSyncOptionsDialog(
                        context = this,
                        onExport = {
                            getExportFile.launch("beekeeper_backup.json")
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
                // ✅ ВИПРАВЛЕННЯ: Замінюємо стару, громіздку логіку на чистий виклик нового DialogFragment
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
                val messageResId = when (result) {
                    HiveAddResult.SUCCESS -> R.string.hive_added_success
                    HiveAddResult.EXISTS -> R.string.hive_number_exists
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
     * Використовує ViewModel для обробки бізнес-логіки (перевірка ліміту та унікальності).
     */
    private fun addHive() {
        DialogUtils.showAddHiveDialog(this,
            onHiveAdded = { hiveNumber ->
                lifecycleScope.launch {
                    // Створення об'єкта HiveEntity зі стандартними налаштуваннями кольорів.
                    val newHive = hiveCreator.createDefaultHiveEntity(hiveNumber)
                    // Делегуємо бізнес-логіку (перевірки) ViewModel.
                    viewModel.addNewHive(newHive)
                }
            }
        )
    }


    /**
     * Відкриває Activity для налаштувань.
     */
    private fun openSettingsActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivityWithSlideAnimation(intent)
    }

    // ---------------------------------------------------------------------
    // ✅ НОВІ МЕТОДИ ДЛЯ ЧИСТОЇ АРХІТЕКТУРИ DIALOGFRAGMENT
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

        // Реалізуємо слухача для обробки натискань опцій
        dialog.setHiveOptionListener(object : HiveOptionsDialogFragment.HiveOptionListener {

            // ✅ ЦЕЙ МЕТОД ТЕПЕР ПРАВИЛЬНО НЕ ПРИЙМАЄ АРГУМЕНТІВ (ПОТРІБНО ВИПРАВИТИ ІНТЕРФЕЙС!)
            override fun onEditNumberClicked() {
                // Викликаємо функцію для редагування номера, передаючи об'єкт hive, який у нас вже є.
                showEditNumberDialog(hive)
            }

            override fun onSelectPrimaryColorClicked() {
                // Тимчасово залишаємо DialogUtils для ColorPicker
                DialogUtils.showColorPickerDialog(
                    context = this@MainActivity
                ) { newColor ->
                    val updatedHive = hive.copy(color = newColor)
                    viewModel.updateHive(updatedHive)
                    Toast.makeText(this@MainActivity, R.string.primary_color_updated, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onSelectSecondaryColorClicked() {
                // Тимчасово залишаємо DialogUtils для ColorPicker
                DialogUtils.showColorPickerDialog(
                    context = this@MainActivity
                ) { newColor ->
                    val updatedHive = hive.copy(secondaryColor = newColor)
                    viewModel.updateHive(updatedHive)
                    Toast.makeText(this@MainActivity, R.string.secondary_color_updated, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDeleteHiveClicked() {
                // Тимчасово залишаємо DialogUtils для підтвердження видалення
                DialogUtils.showDeleteHiveDialog(
                    context = this@MainActivity,
                    hive = hive,
                    onDeleteConfirmed = {
                        viewModel.deleteHive(hive)
                        Toast.makeText(this@MainActivity, R.string.hive_deleted, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        })

        // Викликаємо діалог через FragmentManager
        dialog.show(supportFragmentManager, HiveOptionsDialogFragment.TAG)
    }

    /**
     * Відображає діалогове вікно для редагування номера вулика.
     * Замінює стару логіку DialogUtils на EditHiveNumberDialogFragment.
     */
    private fun showEditNumberDialog(hive: HiveEntity) {
        val editDialog = EditHiveNumberDialogFragment.newInstance(hive.hiveNumber)

        editDialog.setEditNumberListener(object : EditHiveNumberDialogFragment.EditNumberListener {
            override fun onNumberSaved(newNumber: String) {
                // Бізнес-логіка, яка раніше була у DialogUtils, тепер тут
                lifecycleScope.launch {
                    val existingHive = viewModel.getHiveByNumber(newNumber)

                    if (existingHive != null && existingHive.id != hive.id) {
                        Toast.makeText(this@MainActivity, R.string.hive_number_exists, Toast.LENGTH_LONG).show()
                    } else {
                        val updatedHive = hive.copy(hiveNumber = newNumber, name = newNumber)
                        viewModel.updateHive(updatedHive)
                        Toast.makeText(this@MainActivity, R.string.hive_number_updated, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

        editDialog.show(supportFragmentManager, EditHiveNumberDialogFragment.TAG)
    }
}
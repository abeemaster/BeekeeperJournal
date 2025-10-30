// MainActivity Файл головної сторінки додатка

package com.beemaster.beekeeperjournal.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton // Залишаємо, якщо використовується не для drawerToggleButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveAddResult
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity(), SyncOptionsDialogFragment.SyncOptionsListener, IOnHiveAddedListener {

    @Inject
    lateinit var hiveCreator: HiveCreator
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

    // -----------------------------------------------------------------------------------
    // 1. ІМПЛЕМЕНТАЦІЯ АБСТРАКТНОГО МЕТОДУ BASEACTIVITY
    // -----------------------------------------------------------------------------------
    override fun getLayoutResId(): Int = R.layout.activity_main


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ВИДАЛЕНО: setContentView(R.layout.activity_main) - викликається у BaseActivity.onCreate

        initViews()
        setupListeners()
        setupRecyclerView()
        observeHives()
        collectHiveEvents()

        lifecycleScope.launch {
            val existingHive = viewModel.getHiveByNumber("1")
            if (existingHive == null) {
                val defaultHiveNumber = "1"
                val newHive = hiveCreator.createDefaultHiveEntity(defaultHiveNumber)
                viewModel.addHive(newHive)
            }
        }

        backupManager = BackupManager(this, viewModel)
    }

    /**
     * Ініціалізує всі елементи інтерфейсу (View).
     */
    private fun initViews() {
        // ВИДАЛЕНО: Ініціалізація drawerLayout, navigationView, drawerToggleButton
        generalNotesButton = findViewById(R.id.nav_general_notes)
        hiveRecyclerView = findViewById(R.id.hive_list_recycler_view)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
    }

    /**
     * Налаштовує всі слухачі подій для елементів інтерфейсу.
     */
    private fun setupListeners() {
        // ВИДАЛЕНО: drawerToggleButton.setOnClickListener
        // ВИДАЛЕНО: navigationView.setNavigationItemSelectedListener

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
     * Запускає діалог додавання нового вулика.
     * Цей метод викликався з нав. панелі, тепер він викликається через BaseActivity
     * та обробляється тут.
     */
    fun addHive() {
        AddHiveDialogFragment.newInstance()
            .show(supportFragmentManager, AddHiveDialogFragment.TAG)
    }

    /**
     * Обробка результату, повернутого з AddHiveDialogFragment.
     * Тут виконується бізнес-логіка додавання вулика.
     */
    override fun onHiveAdded(hiveNumber: String) {
        lifecycleScope.launch {
            val newHive = hiveCreator.createDefaultHiveEntity(hiveNumber)
            viewModel.addNewHive(newHive)
        }
    }

    // Реалізуємо методи інтерфейсу (викликаються з BaseActivity через діалог SyncOptionsDialogFragment)
    override fun onExportSelected() {
        getExportFile.launch("beekeeper_backup.json")
    }

    override fun onImportSelected() {
        getImportFile.launch(arrayOf("application/json"))
    }

    // ---------------------------------------------------------------------
    // НОВІ МЕТОДИ ДЛЯ ЧИСТОЇ АРХІТЕКТУРИ DIALOGFRAGMENT
    // ---------------------------------------------------------------------

    /**
     * Відображає діалогове вікно опцій вулика (через DialogFragment)
     * та обробляє обрану дію.
     */
    private fun showHiveOptionsDialog(hive: HiveEntity) {
        val dialog = HiveOptionsDialogFragment.newInstance(
            hive.id.toLong(),
            hive.hiveNumber,
            hive.color,
            hive.secondaryColor
        )
        dialog.show(supportFragmentManager, HiveOptionsDialogFragment.TAG)
    }
}
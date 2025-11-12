// MainActivity Файл головної сторінки додатка


package com.beemaster.beekeeperjournal.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
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
import com.beemaster.beekeeperjournal.dialogs.OnHiveAddedListener
import com.beemaster.beekeeperjournal.utils.BackupManager
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager
import com.beemaster.beekeeperjournal.utils.startActivityWithSlideAnimation
import com.beemaster.beekeeperjournal.viewmodel.HiveAddResult
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity(), OnHiveAddedListener {

    // --------------------------------------------------------------------
    // Інжекція Hilt та ViewModel
    // --------------------------------------------------------------------

    // 1. Інжекція ViewModel (використовуємо делегат)
    private val viewModel: MainActivityViewModel by viewModels()

    @Inject
    lateinit var hiveCreator: HiveCreator

    // 2. Інжекція Singleton BackupManager (для Setter Injection)
    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupPrefsManager: BackupPrefsManager

    // --------------------------------------------------------------------
    // View References
    // --------------------------------------------------------------------

    private lateinit var generalNotesButton: MaterialButton
    private lateinit var hiveRecyclerView: RecyclerView
    private lateinit var hiveAdapter: HiveAdapter
    private lateinit var hiveCountTextView: TextView

    // -----------------------------------------------------------------------------------
    // 1. ІМПЛЕМЕНТАЦІЯ АБСТРАКТНОГО МЕТОДУ BASE ACTIVITY
    // -----------------------------------------------------------------------------------

    override fun getLayoutResId(): Int = R.layout.activity_main

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ініціалізуємо Singleton BackupManager ViewModelled, щоб він отримав
        // залежність з меншим скоупом (ViewModelC), обходячи помилку SingletonC.
        backupManager.setDataSource(viewModel)

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
    }


    // --------------------------------------------------------------------
    // НОВИЙ МЕТОД: АВТОМАТИЧНИЙ БЕКАП В UNSTOP()
    // --------------------------------------------------------------------


    /**
     * Викликається, коли Activity більше не видно.
     * Запускає автоматичний бекап.
     */
    override fun onStop() {
        super.onStop()

        // Запускаємо автоматичний бекап у фоновому режимі, коли додаток йде у фон.
        lifecycleScope.launch {
            backupManager.createAutomaticBackup()
        }
    }

    /**
     * Ініціалізує всі елементи інтерфейсу (View).
     */
    private fun initViews() {
        generalNotesButton = findViewById(R.id.nav_general_notes)
        hiveRecyclerView = findViewById(R.id.hive_list_recycler_view)
        hiveCountTextView = findViewById(R.id.hiveCountTextView)
    }

    /**
     * Налаштовує всі слухачі подій для елементів інтерфейсу.
     */
    private fun setupListeners() {

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
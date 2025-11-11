package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SettingsMenuAdapter
import com.beemaster.beekeeperjournal.data.SettingItem
import com.beemaster.beekeeperjournal.dialogs.SyncOptionsDialogFragment
import com.beemaster.beekeeperjournal.utils.BackupManager
import com.beemaster.beekeeperjournal.utils.BackupPrefsManager
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Activity, що відображає головне меню налаштувань.
 * Реалізує [SyncOptionsDialogFragment.SyncOptionsListener] для обробки дій синхронізації.
 */
@AndroidEntryPoint
class SettingsActivity : BaseActivity(), SyncOptionsDialogFragment.SyncOptionsListener {

    // 💡 ВИПРАВЛЕННЯ: Визначаємо TAG тут, щоб уникнути Unresolved reference 'TAG'
    private companion object {
        const val SYNC_DIALOG_TAG = "SyncOptionsDialogFragment"
    }

    // Елемент списку, ініціалізується у onCreate
    private lateinit var settingsRecyclerView: RecyclerView

    // 1. Інжекція ViewModel (для Setter Injection)
    private val viewModel: MainActivityViewModel by viewModels()

    // 2. Інжекція Singleton BackupManager
    @Inject
    lateinit var backupManager: BackupManager

    // 3. Інжекція BackupPrefsManager для роботи з каталогом
    @Inject
    lateinit var backupPrefsManager: BackupPrefsManager

// -----------------------------------------------------------------------------------
// ActivityResultContracts для роботи з файловою системою
// -----------------------------------------------------------------------------------

    // 1. Для ручного експорту (Створення файлу)
    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                // ЗМІНА: exportData -> exportManualData
                backupManager.exportManualData(uri)
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_backup_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Для ручного імпорту (Вибір існуючого файлу)
    private val restoreBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                backupManager.importData(uri)
            }
        } else {
            Toast.makeText(this, getString(R.string.toast_restore_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

// 3. ВИДАЛЕНО: pickDirectoryLauncher (Логіка більше не потрібна)

// ... Решта вашого коду SettingsActivity ...

    override fun getLayoutResId(): Int {
        return R.layout.activity_settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ВИПРАВЛЕННЯ Dagger/Hilt: Setter Injection
        backupManager.setDataSource(viewModel)

        supportActionBar?.title = getString(R.string.title_settings)

        settingsRecyclerView = findViewById(R.id.settingsRecyclerView)

        setupSettingsList()
    }

    /**
     * Формує список елементів налаштувань та налаштовує RecyclerView.
     */
    private fun setupSettingsList() {

        val settingsList = listOf(
            SettingItem(
                title = getString(R.string.setting_title_voice_input),
                targetActivity = VoiceSettingsActivity::class.java
            ),
            SettingItem(
                title = getString(R.string.setting_title_synchronization),
                targetActivity = SettingsActivity::class.java
            ),
/*
            SettingItem(
                title = getString(R.string.setting_title_auto_backup_directory),
                targetActivity = SettingsActivity::class.java
            )
            */

        )

        settingsRecyclerView.layoutManager = LinearLayoutManager(this)
        settingsRecyclerView.adapter = SettingsMenuAdapter(settingsList) { item ->

            // Обробка кліків
            if (item.targetActivity == VoiceSettingsActivity::class.java) {
                // Прямий перехід (якщо це не заглушка)
                startActivity(Intent(this, item.targetActivity))
            } else if (item.targetActivity == SettingsActivity::class.java) {
                // Обробка спеціальних дій за допомогою порівняння заголовків
                when (item.title) {
                    getString(R.string.setting_title_synchronization) -> showSyncOptionsDialog()
                }
            } else {
                // Обробка інших Activity
                startActivity(Intent(this, item.targetActivity))
            }
        }
    }

    /**
     * Відображає BottomSheetDialogFragment з опціями синхронізації (Експорт/Імпорт).
     */
    private fun showSyncOptionsDialog() {
        SyncOptionsDialogFragment()
            .show(supportFragmentManager, SYNC_DIALOG_TAG) // ВИПРАВЛЕННЯ: Використовуємо локальний TAG
    }

    // -----------------------------------------------------------------------------------
    // ІМПЛЕМЕНТАЦІЯ SyncOptionsDialogFragment.SyncOptionsListener
    // -----------------------------------------------------------------------------------

    /**
     * Викликається, коли користувач вибирає "Створити резервну копію (Експорт)".
     */
    override fun onExportSelected() {
        Log.d("SettingsActivity", "Опція: Експорт даних")
        createBackupLauncher.launch("beekeeper_manual_backup.json")
    }

    /**
     * Викликається, коли користувач вибирає "Відновити резервну копію (Імпорт)".
     */
    override fun onImportSelected() {
        Log.d("SettingsActivity", "Опція: Імпорт даних")
        restoreBackupLauncher.launch(arrayOf("application/json"))
    }

}
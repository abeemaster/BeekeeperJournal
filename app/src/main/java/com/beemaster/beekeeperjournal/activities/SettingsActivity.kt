package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SettingsMenuAdapter
import com.beemaster.beekeeperjournal.data.SettingItem

/**
 * Activity, що відображає головне меню налаштувань.
 * Успадковує від [BaseActivity] для використання загальної логіки DrawerLayout та навігації.
 */
class SettingsActivity : BaseActivity() {

    // Елемент списку, ініціалізується у onCreate
    private lateinit var settingsRecyclerView: RecyclerView

    /**
     * Повертає ID макета для цієї Activity.
     * Примітка: Для коректної роботи DrawerLayout у цьому макеті мають бути присутні
     * елементи з ID R.id.drawer_layout, R.id.nav_view та R.id.drawer_toggle_button.
     */
    override fun getLayoutResId(): Int {
        // ID ресурсу макета для Activity налаштувань
        return R.layout.activity_settings
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate() встановлює макет, ініціалізує Base-елементи (DrawerLayout, NavigationView)
        // та налаштовує їхні слухачі.
        super.onCreate(savedInstanceState)

        // Встановлюємо заголовок
        supportActionBar?.title = getString(R.string.title_settings)

        // Ініціалізація RecyclerView, оскільки вона специфічна для цієї Activity
        settingsRecyclerView = findViewById(R.id.settingsRecyclerView)

        // Налаштування списку налаштувань
        setupSettingsList()

    }

    /**
     * Формує список елементів налаштувань та налаштовує RecyclerView.
     */
    private fun setupSettingsList() {
        // Створення пунктів меню налаштувань
        val settingsList = listOf(
            SettingItem(
                title = getString(R.string.setting_title_voice_input),
                targetActivity = VoiceSettingsActivity::class.java
            )
            // Додайте тут інші пункти, наприклад:
            /*
            SettingItem(
                title = getString(R.string.setting_title_theme),
                targetActivity = ThemeSettingsActivity::class.java
            )
            */
        )

        settingsRecyclerView.layoutManager = LinearLayoutManager(this)
        settingsRecyclerView.adapter = SettingsMenuAdapter(settingsList) { item ->
            // Обробка кліку: перехід до відповідної Activity
            startActivity(Intent(this, item.targetActivity))
        }
    }

}
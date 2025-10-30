package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.adapters.SettingsMenuAdapter
import com.beemaster.beekeeperjournal.data.SettingItem
import com.google.android.material.navigation.NavigationView

/**
 * Activity, що відображає головне меню налаштувань.
 * Використовує DrawerLayout та RecyclerView для відображення пунктів.
 */
class SettingsActivity : AppCompatActivity() {

    // Елементи, додані для нового дизайну
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var drawerToggleButton: ImageButton

    // Елемент списку
    private lateinit var settingsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Встановлюємо ActionBar, хоча його елементи можуть бути приховані власним ConstraintLayout
        supportActionBar?.title = getString(R.string.title_settings)

        initViews()
        setupListeners()
        setupSettingsList()
    }

    /**
     * Ініціалізує елементи інтерфейсу, використовуючи ID з activity_settings.xml.
     */
    private fun initViews() {
        // Ініціалізація елементів DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggleButton = findViewById(R.id.drawer_toggle_button)

        // Ініціалізація RecyclerView для списку налаштувань
        settingsRecyclerView = findViewById(R.id.settingsRecyclerView)
    }

    /**
     * Налаштовує слухачів подій, зокрема для кнопки бічного меню.
     */
    private fun setupListeners() {
        // Кнопка для відкриття бічного меню
        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Обробка кліків на пунктах навігації (якщо ви використовуєте nav_menu)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            // Примітка: Логіка навігації тут має бути ідентична тій, що у MainActivity,
            // оскільки бічне меню спільне.

            // Наразі просто закриваємо, але якщо потрібно,
            // додайте сюди логіку openSearchActivity(), openProfitabilityActivity() тощо.

            // Якщо ви повертаєтеся до MainActivity, можна зробити так:
            if (menuItem.itemId == R.id.nav_home) {
                finish() // Просто закриваємо SettingsActivity, щоб повернутися на головний екран
            }
            // ... інша логіка, якщо це потрібно для вашого бічного меню

            true
        }
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

    /**
     * Обробка натискання кнопки "назад" у ActionBar.
     * Оскільки ми використовуємо DrawerLayout, ця кнопка закриватиме бічне меню
     * або повертатиме користувача назад.
     */
    override fun onSupportNavigateUp(): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
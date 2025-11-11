package com.beemaster.beekeeperjournal.activities

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.beemaster.beekeeperjournal.R
import com.google.android.material.navigation.NavigationView

abstract class BaseActivity : AppCompatActivity() {

    // Ці змінні повинні бути visible у дочірніх класах
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    protected lateinit var drawerToggleButton: ImageButton

    // Абстрактна функція, яку дочірні класи повинні імплементувати
    // для встановлення макета (наприклад, R.layout.activity_main)
    protected abstract fun getLayoutResId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        // Ініціалізація та налаштування має відбутися після встановлення макета
        initBaseViews()
        setupBaseListeners()
    }

    private fun initBaseViews() {
        // Припускаємо, що ці ID завжди присутні у кореневому DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        drawerToggleButton = findViewById(R.id.drawer_toggle_button)
    }

    private fun setupBaseListeners() {
        drawerToggleButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            handleNavigationItem(menuItem)
            true
        }
    }

    // Виносна функція для обробки навігації
    private fun handleNavigationItem(menuItem: MenuItem) {
        // Закриваємо бічну панель незалежно від обраного пункту
        drawerLayout.closeDrawer(GravityCompat.START)

        when (menuItem.itemId) {
            R.id.nav_home -> {
                // Якщо ми не на головному екрані, повертаємося на нього.
                // Використовуємо Intent, щоб забезпечити, що MainActivity є кореневою.
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                // finish()
            }

            R.id.nav_search -> {
                if (this::class.java != SearchActivity::class.java) {
                    startActivity(Intent(this, SearchActivity::class.java))
                    // finish()
                }
            }

            R.id.nav_profitability -> {
                if (this::class.java != ProfitabilityActivity::class.java) {
                    startActivity(Intent(this, ProfitabilityActivity::class.java))
                    // finish()
                }
            }

            R.id.nav_settings -> {
                if (this::class.java != SettingsActivity::class.java) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    // finish()
                }
            }

            // =========================================================================
            // СПЕЦІАЛЬНА ЛОГІКА: Додати вулик (на Головну)
            // =========================================================================

            R.id.nav_add_hive -> {
                if (this is MainActivity) {
                    this.addHive()
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }

            // =========================================================================
            // СПЕЦІАЛЬНА ЛОГІКА: Синхронізація (на Головну)
            // =========================================================================
            /**
            R.id.nav_sync -> {
                if (this is MainActivity) {
                    SyncOptionsDialogFragment().show(supportFragmentManager, "SyncOptions")
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }
*/
            R.id.nav_exit_button -> {
                finishAffinity()
            }
        }
    }

}
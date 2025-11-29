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

    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    protected lateinit var drawerToggleButton: ImageButton
    protected abstract fun getLayoutResId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        initBaseViews()
        setupBaseListeners()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun initBaseViews() {
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

    private fun handleNavigationItem(menuItem: MenuItem) {
        drawerLayout.closeDrawer(GravityCompat.START)

        when (menuItem.itemId) {
            R.id.nav_home -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }

            R.id.nav_search -> {
                if (this::class.java != SearchActivity::class.java) {
                    startActivity(Intent(this, SearchActivity::class.java))
                }
            }

            R.id.nav_profitability -> {
                if (this::class.java != ProfitabilityActivity::class.java) {
                    startActivity(Intent(this, ProfitabilityActivity::class.java))
                }
            }

            R.id.nav_settings -> {
                if (this::class.java != SettingsActivity::class.java) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }

            R.id.nav_add_hive -> {
                // Якщо поточна Activity - MainActivity, викликаємо її метод
                if (this is MainActivity) {
                    this.addHive()
                } else {
                    // Інакше повертаємося на головний екран і відкриваємо додавання
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    // Можливо, тут потрібно додати Extra, щоб MainActivity знала, що потрібно відкрити діалог додавання?
                    // Наразі залишаю без змін, але це потенційне місце для покращення.
                    startActivity(intent)
                }
            }

            R.id.nav_exit_button -> {
                // Завершуємо всі Activity в поточній задачі
                finishAffinity()
            }
        }
    }
}
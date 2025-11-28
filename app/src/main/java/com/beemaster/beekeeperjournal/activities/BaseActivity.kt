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

// -----------------------------------------------------------------------------------
// VOSK ІНІЦІАЛІЗАЦІЯ ІНТЕРФЕЙС
// -----------------------------------------------------------------------------------

/**
 * Інтерфейс для отримання подій ініціалізації моделі Vosk.
 * Використовується для оновлення UI (діалогів прогресу) в Activity.
 */
interface VoskInitListener {
    fun onModelInitStarted()
    fun onModelInitProgress(progress: Int, message: String)
    fun onModelInitUnpacking()
    fun onModelInitSuccessful()
    fun onModelInitFailed(error: String)
}

abstract class BaseActivity : AppCompatActivity() {

    // Статична змінна, яка тримає посилання на поточну активну BaseActivity.
    // Це дозволяє синглтонам (наприклад, VoskModelManager) отримувати доступ до
    // методів сповіщення про UI.
    companion object {
        @JvmStatic
        var currentActivity: BaseActivity? = null
            private set
    }

    // Ці змінні повинні бути visible у дочірніх класах
    protected lateinit var drawerLayout: DrawerLayout
    protected lateinit var navigationView: NavigationView
    protected lateinit var drawerToggleButton: ImageButton

    // Множина слухачів VoskInitListener, які активні в поточному життєвому циклі
    private val voskInitListeners = mutableSetOf<VoskInitListener>()

    // Абстрактна функція, яку дочірні класи повинні імплементувати
    protected abstract fun getLayoutResId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        initBaseViews()
        setupBaseListeners()
    }

    override fun onStart() {
        super.onStart()
        // Встановлюємо себе як поточну активну Activity
        currentActivity = this
    }

    override fun onStop() {
        super.onStop()
        // Якщо поточна активність закривається, очищаємо посилання
        if (currentActivity == this) {
            currentActivity = null
        }
    }

    /**
     * Реєструє VoskInitListener. Дочірні Activity повинні викликати це у onCreate.
     */
    fun registerVoskInitListener(listener: VoskInitListener) {
        voskInitListeners.add(listener)
    }

    /**
     * Видаляє VoskInitListener. Дочірні Activity повинні викликати це у onDestroy.
     */
    fun unregisterVoskInitListener(listener: VoskInitListener) {
        voskInitListeners.remove(listener)
    }

    // -----------------------------------------------------------------------------------
    // МЕТОДИ ДЛЯ РОЗСИЛКИ ПОДІЙ VOSK УСІМ ЗАРЕЄСТРОВАНИМ СЛУХАЧАМ
    // -----------------------------------------------------------------------------------

    /**
     * Розсилає подію: Ініціалізація моделі розпочата.
     * Цей метод буде викликаний з VoskModelManager.
     */
    fun notifyModelInitStarted() = voskInitListeners.forEach { it.onModelInitStarted() }

    /**
     * Розсилає подію: Оновлення прогресу завантаження.
     * Цей метод буде викликаний з VoskModelManager.
     */
    fun notifyModelInitProgress(progress: Int, message: String) = voskInitListeners.forEach { it.onModelInitProgress(progress, message) }

    /**
     * Розсилає подію: Розпакування моделі.
     * Цей метод буде викликаний з VoskModelManager.
     */
    fun notifyModelInitUnpacking() = voskInitListeners.forEach { it.onModelInitUnpacking() }

    /**
     * Розсилає подію: Ініціалізація моделі успішна.
     * Цей метод буде викликаний з VoskModelManager.
     */
    fun notifyModelInitSuccessful() = voskInitListeners.forEach { it.onModelInitSuccessful() }

    /**
     * Розсилає подію: Ініціалізація моделі не вдалася.
     * Цей метод буде викликаний з VoskModelManager.
     */
    fun notifyModelInitFailed(error: String) = voskInitListeners.forEach { it.onModelInitFailed(error) }

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

    // Виносна функція для обробки навігації
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
                if (this is MainActivity) {
                    this.addHive()
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                }
            }

            R.id.nav_exit_button -> {
                finishAffinity()
            }
        }
    }
}
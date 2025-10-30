// SettingsActivity файл меню налаштувань

package com.beemaster.beekeeperjournal.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R

/**
 * Activity для налаштування параметрів додатку, зокрема, вибору рушія розпізнавання мови.
 */
class VoiceSettingsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageButton
    private lateinit var speechEngineRadioGroup: RadioGroup
    private lateinit var googleRadioButton: RadioButton
    private lateinit var voskRadioButton: RadioButton
    private lateinit var saveButton: Button
    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(Constants.SETTINGS_PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_settings)

        initViews()
        loadSettings()
        setupListeners()
        // Встановлення заголовка ActionBar
        supportActionBar?.title = getString(R.string.title_settings)
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        speechEngineRadioGroup = findViewById(R.id.speechEngineRadioGroup)
        googleRadioButton = findViewById(R.id.googleRadioButton)
        voskRadioButton = findViewById(R.id.voskRadioButton)
        saveButton = findViewById(R.id.saveButton)
    }

    /**
     * Завантажує поточні налаштування з SharedPreferences і встановлює відповідний RadioButton.
     */
    private fun loadSettings() {
        val savedEngine = sharedPreferences.getString(
            Constants.KEY_SPEECH_ENGINE,
            Constants.DEFAULT_SPEECH_ENGINE // "google"
        )

        when (savedEngine) {
            Constants.ENGINE_GOOGLE -> googleRadioButton.isChecked = true
            Constants.ENGINE_VOSK -> voskRadioButton.isChecked = true
        }
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            // Використовуємо системну функцію "назад"
            onBackPressedDispatcher.onBackPressed()
        }
        saveButton.setOnClickListener {
            val selectedEngine = getSelectedEngine()
            saveSettings(selectedEngine)
            Toast.makeText(this, getString(R.string.settings_saved_message), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    // Окрема функція для визначення вибраного рушія
    private fun getSelectedEngine(): String {
        return if (googleRadioButton.isChecked) {
            Constants.ENGINE_GOOGLE // "google"
        } else {
            Constants.ENGINE_VOSK // "vosk"
        }
    }

    /**
     * Зберігає вибраний рушій розпізнавання мови у SharedPreferences.
     * ✅ ВИКОРИСТАННЯ KTX: Використовує функцію-розширення SharedPreferences.edit { ... }.
     * @param engine Вибраний рушій ("google" або "vosk").
     */
    private fun saveSettings(engine: String) {
        sharedPreferences.edit {
            putString(Constants.KEY_SPEECH_ENGINE, engine)
        }
    }
}
// SettingsActivity файл меню налаштувань

package com.beemaster.beekeeperjournal.activities

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.beemaster.beekeeperjournal.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var speechEngineRadioGroup: RadioGroup
    private lateinit var googleRadioButton: RadioButton
    private lateinit var voskRadioButton: RadioButton
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()
        setupListeners()
    }

    private fun initViews() {
        speechEngineRadioGroup = findViewById(R.id.speechEngineRadioGroup)
        googleRadioButton = findViewById(R.id.googleRadioButton)
        voskRadioButton = findViewById(R.id.voskRadioButton)
        saveButton = findViewById(R.id.saveButton)
    }

    private fun loadSettings() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedEngine = sharedPref.getString("speech_engine", "google")

        if (savedEngine == "google") {
            googleRadioButton.isChecked = true
        } else {
            voskRadioButton.isChecked = true
        }
    }

    private fun setupListeners() {
        saveButton.setOnClickListener {
            val selectedEngine = if (googleRadioButton.isChecked) "google" else "vosk"
            saveSettings(selectedEngine)
            Toast.makeText(this, "Налаштування збережено", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveSettings(engine: String) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("speech_engine", engine)
            apply()
        }
    }
}
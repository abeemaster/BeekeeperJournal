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
import com.beemaster.beekeeperjournal.dialogs.VoskModelDownloadDialog
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity для налаштування параметрів додатку, зокрема, вибору рушія розпізнавання мови.
 */
@AndroidEntryPoint
class VoiceSettingsActivity : AppCompatActivity(), VoskModelDownloadDialog.DownloadDialogListener {

    private companion object {
        const val DOWNLOAD_DIALOG_TAG = "VoskDownloadDialog"
    }

    private lateinit var backButton: ImageButton
    private lateinit var speechEngineRadioGroup: RadioGroup
    private lateinit var googleRadioButton: RadioButton
    private lateinit var voskRadioButton: RadioButton
    private lateinit var saveButton: Button

    @Inject
    lateinit var voskModelManager: VoskModelManager

    // isVoskModelReady тепер оновлюється через виклик методу VoskModelManager
    private var isVoskModelReady: Boolean = false

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(Constants.SETTINGS_PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_settings)

        initViews()
        loadSettings()
        setupListeners()

        // Ініціалізуємо стан моделі Vosk та додаємо слухача на готовність
        checkVoskModelStatus()
        addModelReadyListener()

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
            // Якщо обрано Vosk, але модель не готова, попереджаємо користувача
            if (selectedEngine == Constants.ENGINE_VOSK && !isVoskModelReady) {
                // Якщо Vosk вибрано, але не готово, то залишаємо Google як активний
                Toast.makeText(this, getString(R.string.toast_vosk_not_ready), Toast.LENGTH_LONG).show()
                // Переконаємось, що радіо-кнопка Vosk не залишається вибраною в налаштуваннях
                googleRadioButton.isChecked = true
                saveSettings(Constants.ENGINE_GOOGLE)
            } else {
                saveSettings(selectedEngine)
                Toast.makeText(this, getString(R.string.settings_saved_message), Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Обробник натискання на Vosk RadioButton
        voskRadioButton.setOnClickListener {
            if (!isVoskModelReady) {
                // Якщо модель не готова, показуємо діалог завантаження
                showDownloadDialog()
                // Відновлюємо попередній стан, щоб уникнути помилкового вибору
                val savedEngine = sharedPreferences.getString(Constants.KEY_SPEECH_ENGINE, Constants.DEFAULT_SPEECH_ENGINE)
                if (savedEngine == Constants.ENGINE_GOOGLE) {
                    googleRadioButton.isChecked = true
                }
                // Якщо була обрана Vosk, повертаємось до Google
                else {
                    googleRadioButton.isChecked = true
                }
            }
        }
    }

    /**
     * Перевіряє наявність моделі Vosk і оновлює isVoskModelReady.
     * Якщо модель не готова, вимикає RadioButton.
     */
    private fun checkVoskModelStatus() {
        // ВИПРАВЛЕННЯ: Викликаємо isModelReady() як функцію (як оголошено у VoskModelManager)
        isVoskModelReady = voskModelManager.isModelReady
        voskRadioButton.isEnabled = isVoskModelReady

        if (isVoskModelReady) {
            voskRadioButton.text = getString(R.string.radio_vosk_ready)
        } else {
            // Оскільки ми не маємо індикатора прогресу від Vosk StorageService,
            // відображаємо лише, що потрібно завантаження.
            voskRadioButton.text = getString(R.string.radio_vosk_download_required)
            // Якщо модель не готова, і вона була обрана, скидаємо до Google
            if (sharedPreferences.getString(Constants.KEY_SPEECH_ENGINE, "") == Constants.ENGINE_VOSK) {
                googleRadioButton.isChecked = true
            }
        }
    }

    /**
     * Додає слухача, який оновиться після завершення розпакування Vosk.
     */
    private fun addModelReadyListener() {
        voskModelManager.addModelReadyListener {
            // Цей код виконається, коли розпакування завершиться (навіть при помилці)
            // Оскільки Vosk StorageService не має on-error callback, ми припускаємо success
            // і оновлюємо стан.
            runOnUiThread {
                checkVoskModelStatus()
                // Якщо модель стала готова, автоматично обираємо Vosk (за бажанням)
                if (isVoskModelReady) {
                    voskRadioButton.isChecked = true
                    Toast.makeText(this, getString(R.string.toast_download_success), Toast.LENGTH_LONG).show()
                } else {
                    // Якщо модель не готова (розпакування не вдалося)
                    googleRadioButton.isChecked = true
                    Toast.makeText(this, getString(R.string.toast_download_failed), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Відображає діалог із пропозицією завантажити модель Vosk.
     */
    private fun showDownloadDialog() {
        VoskModelDownloadDialog().show(supportFragmentManager, DOWNLOAD_DIALOG_TAG)
    }

    // Реалізація інтерфейсу VoskModelDownloadDialog.DownloadDialogListener
    override fun onDownloadConfirmed() {
        // ВИПРАВЛЕННЯ: Викликаємо startModelSetup()
        // Цей метод запустить initVoskModel(), який запустить StorageService.unpack()
        voskModelManager.startModelSetup()
        // Показуємо, що процес розпочато
        voskRadioButton.isEnabled = false
        voskRadioButton.text = getString(R.string.radio_vosk_downloading_simple) // Новий, спрощений рядок
        Toast.makeText(this, getString(R.string.toast_download_in_progress_simple), Toast.LENGTH_SHORT).show()
    }

    override fun onDownloadCancelled() {
        Toast.makeText(this, getString(R.string.toast_download_cancelled_by_user), Toast.LENGTH_SHORT).show()
        checkVoskModelStatus() // Перевіряємо статус ще раз, щоб оновити інтерфейс
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
     * ВИКОРИСТАННЯ KTX: Використовує функцію-розширення SharedPreferences.edit { ... }.
     * @param engine Вибраний рушій ("google" або "vosk").
     */
    private fun saveSettings(engine: String) {
        sharedPreferences.edit {
            putString(Constants.KEY_SPEECH_ENGINE, engine)
        }
    }
}
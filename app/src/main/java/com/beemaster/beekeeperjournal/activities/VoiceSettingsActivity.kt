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
import com.beemaster.beekeeperjournal.dialogs.VoskModelProgressDialog
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity для налаштування параметрів додатку, зокрема, вибору рушія розпізнавання мови.
 */
@AndroidEntryPoint
class VoiceSettingsActivity : AppCompatActivity() {

    private companion object {
        const val DOWNLOAD_CONFIRMATION_TAG = "VoskDownloadConfirmationDialog"
        const val PROGRESS_DIALOG_TAG = "VoskProgressDialog"
    }

    private lateinit var backButton: ImageButton
    private lateinit var speechEngineRadioGroup: RadioGroup
    private lateinit var googleRadioButton: RadioButton
    private lateinit var voskRadioButton: RadioButton
    private lateinit var saveButton: Button

    @Inject
    lateinit var voskModelManager: VoskModelManager

    private var isVoskModelReady: Boolean = false
    private var progressDialog: VoskModelProgressDialog? = null


    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(Constants.SETTINGS_PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_settings)

        // Перевіряємо, чи діалог прогресу вже був у стані відновлення (наприклад, після зміни орієнтації)
        // Це дозволяє відновити посилання на діалог, якщо він пережив конфігураційні зміни.
        progressDialog = supportFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG) as? VoskModelProgressDialog

        initViews()
        loadSettings()
        setupListeners()

        checkVoskModelStatus()
        // Додаємо слухача. Якщо Vosk завантажився до відкриття Activity,
        // він одразу викличе цей слухач і оновить стан.
        addModelReadyListener()

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
            onBackPressedDispatcher.onBackPressed()
        }
        saveButton.setOnClickListener {
            val selectedEngine = getSelectedEngine()
            // Перевіряємо готовність моделі
            if (selectedEngine == Constants.ENGINE_VOSK && !isVoskModelReady) {
                // Якщо Vosk вибрано, але не готово, то залишаємо Google як активний
                Toast.makeText(this, getString(R.string.toast_vosk_not_ready), Toast.LENGTH_LONG).show()
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
                // Якщо модель не готова, показуємо діалог підтвердження
                showConfirmationDialog()
                // Відновлюємо попередній стан, щоб уникнути помилкового вибору
                loadSettings()
            } else {
                // Модель готова, дозволяємо вибір
            }
        }
    }

    /**
     * Перевіряє наявність моделі Vosk і оновлює isVoskModelReady та текст кнопки.
     */
    private fun checkVoskModelStatus() {
        isVoskModelReady = voskModelManager.isModelReady
        voskRadioButton.isEnabled = true

        if (isVoskModelReady) {
            voskRadioButton.text = getString(R.string.radio_vosk_ready)
            // Приховуємо діалог прогресу, якщо він був
            progressDialog?.dismiss()
            progressDialog = null
        } else {
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
            // Цей код виконається, коли розпакування завершиться (успіх або помилка)
            // Завжди викликається в Main Thread завдяки VoskModelManager.
            progressDialog?.dismiss()
            progressDialog = null
            checkVoskModelStatus()

            if (isVoskModelReady) {
                // Якщо модель стала готова, автоматично обираємо Vosk
                voskRadioButton.isChecked = true
                Toast.makeText(this, getString(R.string.toast_download_success), Toast.LENGTH_LONG).show()
            } else {
                // Якщо модель не готова (розпакування не вдалося або помилка)
                googleRadioButton.isChecked = true
                Toast.makeText(this, getString(R.string.toast_download_failed), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Відображає діалог із пропозицією завантажити модель Vosk.
     */
    private fun showConfirmationDialog() {
        VoskModelDownloadDialog.newInstance(
            onConfirm = { onDownloadConfirmed() },
            onCancel = { onDownloadCancelled() }
        ).show(supportFragmentManager, DOWNLOAD_CONFIRMATION_TAG)
    }

    /**
     * Обробка підтвердження завантаження.
     * Запускає асинхронну операцію Vosk та відображає діалог прогресу.
     */
    private fun onDownloadConfirmed() {
        // 1. Створюємо та відображаємо діалог прогресу, якщо його ще немає
        if (progressDialog == null) {
            progressDialog = VoskModelProgressDialog.newInstance()
            progressDialog?.show(supportFragmentManager, PROGRESS_DIALOG_TAG)
        } else {
            // Якщо діалог вже є (після відновлення Activity), просто перепоказуємо його
            progressDialog?.dismiss()
            progressDialog?.show(supportFragmentManager, PROGRESS_DIALOG_TAG)
        }

        // 2. Встановлюємо слухача прогресу в менеджер
        // Це зв'язує менеджер із діалогом.
        voskModelManager.setProgressUpdateCallback { state, progress ->
            progressDialog?.updateProgress(state, progress)
        }

        // 3. Запускаємо асинхронний процес (якщо він вже не запущений)
        voskModelManager.startModelSetup()

        // 4. Оновлюємо UI Activity
        voskRadioButton.isEnabled = false
        voskRadioButton.text = getString(R.string.radio_vosk_downloading_simple)
        Toast.makeText(this, getString(R.string.toast_download_in_progress_simple), Toast.LENGTH_SHORT).show()
    }

    /**
     * Обробка скасування завантаження.
     */
    private fun onDownloadCancelled() {
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
     * @param engine Вибраний рушій ("google" або "vosk").
     */
    private fun saveSettings(engine: String) {
        sharedPreferences.edit {
            putString(Constants.KEY_SPEECH_ENGINE, engine)
        }
    }
}
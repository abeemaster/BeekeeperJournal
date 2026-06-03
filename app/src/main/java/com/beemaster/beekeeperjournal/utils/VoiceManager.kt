// VoiceManager.kt Цей файл є контролером або хелпером, який працює найближче до Activity/View.
// Тут основні налаштування голосового введення.

package com.beemaster.beekeeperjournal.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import org.json.JSONObject
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener as VoskRecognitionListener
import org.vosk.android.SpeechService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер голосового вводу.
 *
 * Цей клас керує логікою розпізнавання мовлення,
 * підтримуючи перемикання між локальним рушієм Vosk та системним Google Speech Recognizer.
 * Інкапсулює логіку дозволів, ініціалізації обох рушіїв, обробку результатів та
 * керування тайм-аутом для Vosk.
 *
 * @property voskModelManager Керує завантаженням моделі Vosk.
 * @property sharedPreferences Для отримання вибраного рушія розпізнавання.
 * @property appContext Контекст застосунку, використовується для інфраструктурних потреб.
 */

@Singleton
class VoiceManager @Inject constructor(
    private val voskModelManager: VoskModelManager,
    private val sharedPreferences: SharedPreferences,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
) : VoskRecognitionListener, RecognitionListener {

    private val TAG = "VoiceManager"

    // --- Поля, що залежать від Activity ---
    private lateinit var activity: AppCompatActivity
    private lateinit var targetInput: EditText
    private lateinit var currentMicrophoneButton: ImageButton
    private var searchListener: VoskSearchListener? = null

    // --- Vosk ---
    private var speechService: SpeechService? = null

    // --- Google ---
    private var googleSpeechRecognizer: SpeechRecognizer? = null

    // --- Загальне ---
    private var isListening = false
    private var isVoskEngineInUse = false

    // Тайм-аут тиші для Vosk
    private val SILENCE_TIMEOUT_MS: Long = 3000
    private val silenceTimerHandler = Handler(Looper.getMainLooper())

    private val silenceTimeoutRunnable = Runnable {
        if (isListening && isVoskEngineInUse) {
            Log.e(TAG, "Vosk timeout reached. Stopping recording.")
            stopListening()
            Toast.makeText(activity, activity.getString(R.string.voice_input_timeout), Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------------
    // ІНІЦІАЛІЗАЦІЯ / ЖИТТЄВИЙ ЦИКЛ
    // ---------------------------------------------------------------------

    /**
     * Ініціалізує менеджер і прив'язує його до елементів Activity.
     * Викликається в Activity.onCreate().
     *
     * @param activity Об'єкт Activity для контексту та запиту дозволів.
     * @param inputField Поле EditText для вставки розпізнаного тексту.
     * @param micButton Кнопка мікрофона для зміни стану.
     */

    fun init(activity: AppCompatActivity, inputField: EditText, micButton: ImageButton) {
        this.activity = activity
        this.targetInput = inputField
        this.currentMicrophoneButton = micButton
        this.searchListener = activity as? VoskSearchListener

        if (googleSpeechRecognizer == null) {
            googleSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
            googleSpeechRecognizer?.setRecognitionListener(this)
        }

        // Перевірка готовності Vosk моделі
        if (!voskModelManager.isModelReady) {
            currentMicrophoneButton.isEnabled = false
            voskModelManager.addModelReadyListener {
                currentMicrophoneButton.isEnabled = true
                Toast.makeText(activity, activity.getString(R.string.vosk_model_loaded), Toast.LENGTH_SHORT).show()
            }
        } else {
            currentMicrophoneButton.isEnabled = true
        }
    }


    /**
     * Очищає ресурси та зупиняє всі процеси розпізнавання.
     * Викликається в Activity.onDestroy().
     */

    fun destroy() {
        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
        googleSpeechRecognizer?.destroy()
        googleSpeechRecognizer = null
        isListening = false
        this.searchListener = null
    }

    // ---------------------------------------------------------------------
    // УПРАВЛІННЯ СТАНОМ ТА ЗАПУСКОМ
    // ---------------------------------------------------------------------

    /**
     * Перевіряє дозвіл на запис аудіо і запускає розпізнавання, або запитує дозвіл.
     * Якщо розпізнавання вже активне, зупиняє його.
     */
    fun checkPermissionAndStartListening() {
        if (isListening) {
            stopListening()
            return
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                Constants.REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            executeStartListening()
        }
    }

    /**
     * Обробляє результат запиту дозволів на запис аудіо.
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == Constants.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executeStartListening()
            } else {
                Toast.makeText(activity, activity.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Запускає розпізнавання мовлення, використовуючи вибраний рушій.
     */
    private fun executeStartListening() {
        if (isListening) return

        if (isEmulator()) {
            Log.i(TAG, "Emulator detected. Forcing Google Speech Engine.")
            startGoogleListening()
            isListening = true
            updateMicrophoneButtonState(true)
            return
        }

        val speechEngine = sharedPreferences.getString(
            Constants.KEY_SPEECH_ENGINE,
            Constants.DEFAULT_SPEECH_ENGINE
        ) ?: Constants.DEFAULT_SPEECH_ENGINE

        val isVoskSelected = speechEngine == Constants.ENGINE_VOSK
        val isVoskReady = voskModelManager.isModelReady


        if (isVoskSelected) {
            if (isVoskReady) {
                // 1. Vosk обрано і готово
                startVoskListening()
            } else {
                // 2. Vosk обрано, але не готово (фальбек)
                val message = activity.getString(R.string.vosk_not_ready_fallback, activity.getString(R.string.google_voice_engine))
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
                startGoogleListening()
            }
        } else {
            // 3. Google обрано (за замовчуванням)
            startGoogleListening()
        }

        isListening = true
        updateMicrophoneButtonState(true)
    }

    /**
     * Зупиняє активне розпізнавання мовлення (Vosk або Google) і очищає ресурси.
     */
    fun stopListening() {
        if (!isListening) return

        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)

        if (isVoskEngineInUse) {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
        } else {
            googleSpeechRecognizer?.stopListening()
        }

        isListening = false
        updateMicrophoneButtonState(false)
    }

    /**
     * Повертає поточний стан розпізнавання мовлення.
     * @return True, якщо розпізнавання активне.
     */
    // fun isListening(): Boolean = isListening // Наразі не використовується, але може бути потрібною у майбутньому

    // ---------------------------------------------------------------------
    // VOSK ЛОГІКА
    // ---------------------------------------------------------------------

    /**
     * Запускає розпізнавання мовлення за допомогою Vosk.
     */
    private fun startVoskListening() {
        val model = voskModelManager.getModel()
        if (model == null) {
            Log.e(TAG, "Model is NULL. Cannot start Vosk.")
            startGoogleListening()
            return
        }

        // 1. Створюємо новий Recognizer для поточної сесії
        val recognizer: Recognizer
        try {
            recognizer = Recognizer(model, 16000.0f)
        } catch (e: Exception) {
            Log.e(TAG, "FAILED creating Recognizer: ${e.message}", e)
            startGoogleListening()
            return
        }

        try {
            // 2. Перед запуском переконуємося, що старий сервіс повністю зупинено
            speechService?.stop()
            speechService?.shutdown()
            speechService = null

            // 3. Створюємо та запускаємо новий SpeechService
            speechService = SpeechService(recognizer, 16000.0f)

            // 4. Запускаємо прослуховування
            speechService?.startListening(this)

            isVoskEngineInUse = true
            silenceTimerHandler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS)

        } catch (e: Exception) {
            Log.e(TAG, "CRITICAL ERROR starting SpeechService: ${e.message}", e)

            Toast.makeText(activity, activity.getString(R.string.recognition_error, e.message), Toast.LENGTH_LONG).show()
            val fallbackMessage = activity.getString(R.string.vosk_error_fallback, activity.getString(R.string.google_voice_engine))
            Toast.makeText(activity, fallbackMessage, Toast.LENGTH_LONG).show()

            startGoogleListening()
        }
    }

    /**
     * Запускає розпізнавання мовлення за допомогою системного Google Speech Recognizer.
     */
    private fun startGoogleListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uk-UA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

        googleSpeechRecognizer?.startListening(intent)
        isVoskEngineInUse = false
    }

    // ---------------------------------------------------------------------
    // VOSK LISTENER (VoskRecognitionListener)
    // ---------------------------------------------------------------------

    /**
     * Обробляє фінальний результат розпізнавання Vosk.
     */
    override fun onResult(hypothesis: String) {
        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)

        try {
            val resultJson = JSONObject(hypothesis)
            val recognizedText = resultJson.optString("text", "")

            if (recognizedText.isNotEmpty()) {
                insertTextIntoTargetInput(recognizedText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }

        // Явно зупиняємо Vosk після отримання результату,
        // щоб кнопка перейшла в неактивний стан.
        stopListening()
    }

    /**
     * Обробляє проміжний результат розпізнавання Vosk і скидає таймер тиші.
     */
    override fun onPartialResult(hypothesis: String?) {
        // Спочатку скасовуємо будь-який поточний тайм-аут,
        // оскільки отримання часткового результату означає, що запис АКТИВНИЙ.
        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)

        // Потім запускаємо новий таймер тиші на 3 секунди.
        silenceTimerHandler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS)
    }

    /**
     * Обробляє завершення сесії Vosk (не використовується, оскільки результат обробляється в onResult).
     */
    override fun onFinalResult(hypothesis: String) {
        // Vosk сам викликає onResult з фінальним результатом.
    }

    /**
     * Обробляє помилки Vosk.
     */
    override fun onError(exception: Exception?) {
        Log.e(TAG, "Vosk recognition error: ${exception?.message}")
        if (exception != null) {
            Toast.makeText(activity, activity.getString(R.string.recognition_error, exception.message), Toast.LENGTH_LONG).show()
        }
        stopListening()
    }

    /**
     * Обробляє внутрішній тайм-аут Vosk (якщо не було з'єднання з мікрофоном).
     */
    override fun onTimeout() {
        Log.d(TAG, "Vosk internal onTimeout called.")
        stopListening()
    }

    // ---------------------------------------------------------------------
    // GOOGLE LISTENER (RecognitionListener)
    // ---------------------------------------------------------------------

    /**
     * Обробляє готовність Google Recognizer до прийому мовлення.
     */
    override fun onReadyForSpeech(params: Bundle?) {
        Toast.makeText(activity, activity.getString(R.string.listening_message), Toast.LENGTH_SHORT).show()
    }
    // Інші методи Google RecognitionListener (залишаємо порожніми, оскільки їхня логіка не потрібна)
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    /**
     * Обробляє помилки Google Recognizer.
     */
    override fun onError(error: Int) {
        Log.e(TAG, "Google recognition error code: $error")

        val errorMessage = when(error) {
            SpeechRecognizer.ERROR_AUDIO -> activity.getString(R.string.error_audio)
            SpeechRecognizer.ERROR_CLIENT -> activity.getString(R.string.error_client)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> activity.getString(R.string.error_permissions)
            SpeechRecognizer.ERROR_NETWORK -> activity.getString(R.string.error_network)
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> activity.getString(R.string.error_network_timeout)
            SpeechRecognizer.ERROR_NO_MATCH -> activity.getString(R.string.error_no_match)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> activity.getString(R.string.error_recognizer_busy)
            SpeechRecognizer.ERROR_SERVER -> activity.getString(R.string.error_server)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> activity.getString(R.string.error_speech_timeout)
            else -> activity.getString(R.string.error_unknown)
        }

        Toast.makeText(activity, activity.getString(R.string.recognition_error, errorMessage), Toast.LENGTH_LONG).show()
        stopListening()
    }

    /**
     * Обробляє фінальні результати розпізнавання Google.
     */
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val recognizedText = matches[0]
            insertTextIntoTargetInput(recognizedText)
        } else {
            Toast.makeText(activity, activity.getString(R.string.error_no_match), Toast.LENGTH_SHORT).show()
        }
        stopListening()
    }


    // ---------------------------------------------------------------------
    // ДОПОМІЖНІ МЕТОДИ
    // ---------------------------------------------------------------------

    /**
     * Вставляє розпізнаний текст у цільове поле вводу (EditText).
     */
    private fun insertTextIntoTargetInput(recognizedText: String) {
        val currentText = targetInput.text.toString()

        val newText = if (currentText.isEmpty() || currentText.endsWith(" ")) {
            currentText + recognizedText
        } else {
            "$currentText $recognizedText"
        }
        targetInput.setText(newText)
        targetInput.setSelection(newText.length)

        searchListener?.performSearchFromVosk(newText)
    }
    /**
     * Оновлює колір кнопки мікрофона для відображення стану прослуховування.
     */
    private fun updateMicrophoneButtonState(isListening: Boolean) {
        val colorResId = if (isListening) R.color.status_red else R.color.button_microphone
        currentMicrophoneButton.backgroundTintList = ContextCompat.getColorStateList(activity, colorResId)
    }

    /**
     * Визначає, чи запущено додаток на емуляторі.
     */
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
    }
}
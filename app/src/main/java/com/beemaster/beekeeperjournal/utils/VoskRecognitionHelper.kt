// VoskRecognitionHelper.kt Цей файл є контролером або хелпером, який працює найближче до Activity/View. Тут основні налаштування Воск.
package com.beemaster.beekeeperjournal.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * Допоміжний клас для керування розпізнаванням мовлення Vosk.
 * Інкапсулює логіку дозволів, ініціалізації Vosk та обробки результатів.
 *
 * @param activity Об'єкт [AppCompatActivity], необхідний для запиту дозволів та відображення Toast-повідомлень.
 * @param noteContentInput Поле [EditText], куди вставляється розпізнаний текст (для нотаток).
 * @param microphoneBtnEditNote Кнопка [ImageButton], чий колір змінюється для відображення статусу запису (для нотаток).
 * @param voskModelManager Менеджер моделі Vosk.
 * @param searchInput Поле [EditText] для пошуку (використовується в SearchActivity).
 */
class VoskRecognitionHelper(
    private val activity: AppCompatActivity,
    private val noteContentInput: EditText,
    private val microphoneBtnEditNote: ImageButton,
    private val voskModelManager: VoskModelManager,
    private val searchInput: EditText? = null
) : RecognitionListener {

    private val TAG = "VoskRecognitionHelper"

    private var speechService: SpeechService? = null
    private var isListening = false
    /**
     * Повертає поточний стан прослуховування.
     */
    fun isListening(): Boolean = isListening

    // Таймаут тиші для Vosk, щоб автоматично завершити розпізнавання
    private val SILENCE_TIMEOUT_MS: Long = 3000
    private val silenceTimerHandler = Handler(Looper.getMainLooper())

    // Визначає, яке поле введення зараз є цільовим.
    // Якщо searchInput не null, використовуємо його. Інакше - noteContentInput.
    private val targetInput: EditText
        get() = searchInput ?: noteContentInput

    // Визначає, яка кнопка мікрофона зараз є цільовою.
    // Оскільки NoteActivity має свою кнопку, а SearchActivity - свою,
    // ми зробимо так, щоб цей клас викликався з двох різних Activity,
    // кожна зі своєю кнопкою. Але в цьому класі ми контролюємо кнопку з нотаток,
    // тому додамо метод для встановлення кнопки пошуку.

    private var currentMicrophoneButton: ImageButton = microphoneBtnEditNote

    // Метод, щоб SearchActivity міг передати свою кнопку
    fun setMicrophoneButton(button: ImageButton) {
        this.currentMicrophoneButton = button
    }


    private val silenceTimeoutRunnable = Runnable {
        if (isListening) {
            Log.d(TAG, "Vosk Silence timeout reached. Stopping recording.")
            stopListening()
            Toast.makeText(activity, activity.getString(R.string.voice_input_timeout), Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------------------------------------------------------------
    // ІНІЦІАЛІЗАЦІЯ / ЖИТТЄВИЙ ЦИКЛ
    // ---------------------------------------------------------------------

    init {
        // Логіка Vosk була тут. Зараз вона вся у методах.
    }

    /**
     * Починає процес розпізнавання: перевіряє дозволи та, якщо все готово,
     * запускає Vosk.
     */
    fun checkPermissionAndStartListening() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Запит дозволу, якщо його немає
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                Constants.REQUEST_RECORD_AUDIO_PERMISSION
            )
        } else {
            // Дозвіл є, запускаємо прослуховування
            startListening()
        }
    }

    /**
     * Викликається після обробки результату запиту дозволу.
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == Constants.REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening()
            } else {
                Toast.makeText(activity, activity.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListening() {
        if (isListening) return

        val model = voskModelManager.getModel() // ✅ ВИКОРИСТОВУЄМО НОВИЙ ГЕТТЕР

        if (!voskModelManager.isModelReady) {
            Toast.makeText(activity, activity.getString(R.string.vosk_model_not_ready), Toast.LENGTH_LONG).show()
            return
        }

        try {
            val recognizer = Recognizer(model, 16000.0f)

            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)

            isListening = true
            updateMicrophoneButtonState(true)
            silenceTimerHandler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS)

            Log.d(TAG, "Vosk started listening.")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Vosk: ${e.message}", e)
            Toast.makeText(activity, activity.getString(R.string.vosk_error, e.message), Toast.LENGTH_LONG).show()
            stopListening()
        }
    }

    fun stopListening() {
        if (!isListening) return

        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)
        speechService?.stop()

        isListening = false
        updateMicrophoneButtonState(false)
        Log.d(TAG, "Stopped listening.")
    }

    /**
     * Очищення ресурсів. Викликається в Activity.onDestroy().
     */
    fun destroy() {
        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)
        speechService?.cancel()
        speechService = null
        Log.d(TAG, "Vosk helper resources destroyed.")
    }

    // ---------------------------------------------------------------------
    // VOSK RECOGNITION LISTENER
    // ---------------------------------------------------------------------

    override fun onResult(hypothesis: String) {
        try {
            val resultJson = JSONObject(hypothesis)
            val recognizedText = resultJson.optString("text", "")

            if (recognizedText.isNotEmpty()) {
                // ✅ ЗМІНА: ВСТАВЛЯЄМО ТЕКСТ У ВИБРАНЕ ПОЛЕ
                val currentText = targetInput.text.toString()

                if (searchInput != null) {
                    // Якщо це пошук, замінюємо весь текст і одразу запускаємо пошук (це треба робити в Activity)
                    targetInput.setText(recognizedText)
                    targetInput.setSelection(recognizedText.length)
                    // ПРИМІТКА: Пошук потрібно запустити в SearchActivity
                } else {
                    // Якщо це нотатка, додаємо текст
                    val newText = if (currentText.isEmpty() || currentText.endsWith(" ")) {
                        currentText + recognizedText
                    } else {
                        currentText + " " + recognizedText
                    }
                    targetInput.setText(newText)
                    targetInput.setSelection(newText.length)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }

        // Скидаємо таймер тиші
        silenceTimerHandler.removeCallbacks(silenceTimeoutRunnable)
        silenceTimerHandler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS)
    }

    override fun onPartialResult(hypothesis: String?) { /* Ігноруємо */ }
    override fun onFinalResult(hypothesis: String) { /* Ігноруємо */ }

    override fun onError(exception: Exception?) {
        if (exception != null) {
            Log.e(TAG, "Vosk recognition error: ${exception.message}", exception)
            Toast.makeText(activity, activity.getString(R.string.vosk_error, exception.message), Toast.LENGTH_LONG).show()
        }
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "Recognition timeout. Stopping recording.")
        Toast.makeText(activity, activity.getString(R.string.voice_input_timeout), Toast.LENGTH_SHORT).show()
        stopListening()
    }

    // ---------------------------------------------------------------------
    // УПРАВЛІННЯ UI
    // ---------------------------------------------------------------------

    /**
     * Оновлює зовнішній вигляд кнопки мікрофона (колір) залежно від стану
     * голосового розпізнавання.
     */
    private fun updateMicrophoneButtonState(isListening: Boolean) {
        val colorResId = if (isListening) R.color.status_red else R.color.status_green
        // Використовуємо поточну кнопку
        currentMicrophoneButton.backgroundTintList = ContextCompat.getColorStateList(activity, colorResId)
    }
}
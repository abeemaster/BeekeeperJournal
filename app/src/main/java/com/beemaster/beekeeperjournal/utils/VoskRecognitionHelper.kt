// VoskRecognitionHelper.kt
// VoskRecognitionHelper.kt (Виправлено)

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
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.voice.VoskModelManager // Залишаємо імпорт
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

/**
 * Допоміжний клас для керування розпізнаванням мовлення Vosk.
 * Інкапсулює логіку дозволів, ініціалізації Vosk та обробки результатів.
 *
 * @param activity Об'єкт [AppCompatActivity], необхідний для запиту дозволів та відображення Toast-повідомлень.
 * @param noteContentInput Поле [EditText], куди вставляється розпізнаний текст.
 * @param microphoneBtnEditNote Кнопка [ImageButton], чий колір змінюється для відображення статусу запису.
 * @param voskModelManager Ін'єктований менеджер моделі Vosk. // ✅ НОВИЙ ПАРАМЕТР
 */
class VoskRecognitionHelper(
    private val activity: AppCompatActivity,
    private val noteContentInput: EditText,
    private val microphoneBtnEditNote: ImageButton,
    // ✅ ДОДАНО: Приймаємо VoskModelManager, інжектований у Activity
    private val voskModelManager: VoskModelManager
) : RecognitionListener {

    companion object {
        private const val TAG = "VoskRecognitionHelper"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    private var speechService: SpeechService? = null

    /**
     * Геттер для моделі Vosk.
     */
    private val voskModel: Model?
        get() = voskModelManager.getModel() // ✅ ВИПРАВЛЕНО: Використовуємо ін'єктований менеджер

    /**
     * Перевіряє, чи активний наразі процес розпізнавання.
     *
     * @return true, якщо Vosk слухає; false, якщо ні.
     */
    fun isVoskListening(): Boolean {
        return speechService != null
    }

    /**
     * Налаштовує Vosk, перевіряє дозволи та починає прослуховування.
     * Якщо дозвіл відсутній, запитує його у користувача.
     */
    fun setupVoskAndStartListening() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Запит дозволу
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            return
        }

        // ✅ ВИПРАВЛЕНО: Перевіряємо готовність моделі через менеджер
        if (!voskModelManager.isModelReady) {
            Toast.makeText(activity, activity.getString(R.string.vosk_model_loading), Toast.LENGTH_SHORT).show()

            // Додаємо слухача. Якщо модель стане готова, викликаємо setupVoskAndStartListening ще раз.
            voskModelManager.addModelReadyListener {
                Handler(Looper.getMainLooper()).post {
                    if (isVoskListening()) return@post // Уникнути повторного запуску
                    setupVoskAndStartListening()
                    Toast.makeText(activity, activity.getString(R.string.vosk_model_loaded), Toast.LENGTH_SHORT).show()
                }
            }
            return
        }

        try {
            // voskModel тепер коректно викликає voskModelManager.getModel(), тому він не null
            val rec = Recognizer(voskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            // Починаємо прослуховування з тайм-аутом 10 секунд (10000 мс)
            speechService?.startListening(this, 10000)

            // Візуалізація активного статусу
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.microphone_button_active_color)
            Toast.makeText(activity, activity.getString(R.string.vosk_listening), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, activity.getString(R.string.vosk_start_error, e.message), Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    /**
     * Зупиняє прослуховування та очищає ресурси Vosk.
     */
    fun stopListening() {
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null

        // Візуалізація неактивного статусу
        microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(activity, R.color.microphone_button_color)
    }

    /**
     * Обробляє результат запиту дозволів. Викликається з [AppCompatActivity.onRequestPermissionsResult].
     */
    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ✅ ВИПРАВЛЕНО: Перевіряємо готовність моделі через менеджер
                if (voskModelManager.isModelReady) {
                    // Якщо дозвіл отримано, повторно викликаємо запуск розпізнавання з невеликою затримкою.
                    Handler(Looper.getMainLooper()).postDelayed({
                        setupVoskAndStartListening()
                    }, 300)
                }
            } else {
                Toast.makeText(activity, activity.getString(R.string.audio_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResult(hypothesis: String?) {
        if (hypothesis != null) {
            try {
                val jsonResult = JSONObject(hypothesis)
                val text = jsonResult.optString("text", "")
                if (text.isNotEmpty()) {
                    noteContentInput.append("$text ")
                    // Встановлюємо курсор у кінець тексту
                    noteContentInput.setSelection(noteContentInput.text.length)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        // Логіка відображення проміжних результатів відсутня.
    }

    override fun onFinalResult(hypothesis: String) {
        // Логіка обробки фінальних результатів відсутня.
    }

    override fun onError(exception: Exception?) {
        if (exception != null) {
            Log.e(TAG, "Vosk recognition error: ${exception.message}", exception)
            // ✅ ВИПРАВЛЕНО: Жорстко закодований рядок замінено на ресурс
            Toast.makeText(activity, activity.getString(R.string.vosk_error, exception.message), Toast.LENGTH_LONG).show()
        }
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "Recognition timeout. Stopping recording.")
        stopListening()
        // ✅ ВИПРАВЛЕНО: Жорстко закодований рядок замінено на ресурс
        Toast.makeText(activity, activity.getString(R.string.vosk_timeout), Toast.LENGTH_SHORT).show()
    }
}
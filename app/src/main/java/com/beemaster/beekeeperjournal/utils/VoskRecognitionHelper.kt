// VoskRecognitionHelper.kt Цей файл відповідає за всю логіку, пов'язану з розпізнаванням мовлення.

// VoskRecognitionHelper.kt - Виправлений файл
package com.beemaster.beekeeperjournal.utils

import android.Manifest
import android.content.Context
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
import com.beemaster.beekeeperjournal.BeekeeperApplication
import com.beemaster.beekeeperjournal.R
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService

class VoskRecognitionHelper(
    private val context: Context,
    private val noteContentInput: EditText,
    private val microphoneBtnEditNote: ImageButton
) : RecognitionListener {

    companion object {
        private const val TAG = "VoskRecognitionHelper"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    private var speechService: SpeechService? = null
    private val voskModel: Model?
        get() = BeekeeperApplication.voskModel

    fun isVoskListening(): Boolean {
        return speechService != null
    }

    fun setupVoskAndStartListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as AppCompatActivity, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            return
        }

        if (voskModel == null) {
            Toast.makeText(context, "Модель Vosk ще не завантажена. Зачекайте.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val rec = Recognizer(voskModel, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(this)
            microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(context, R.color.microphone_button_active_color)
            Toast.makeText(context, "Слухаю...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Помилка запуску розпізнавання: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Error starting recognition", e)
        }
    }

    fun stopListening() {
        speechService?.cancel()
        speechService?.shutdown()
        speechService = null
        microphoneBtnEditNote.backgroundTintList = ContextCompat.getColorStateList(context, R.color.microphone_button_color)
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (voskModel != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        setupVoskAndStartListening()
                    }, 300)
                }
            } else {
                Toast.makeText(context, "Дозвіл на запис аудіо відхилено. Голосовий ввід недоступний.", Toast.LENGTH_LONG).show()
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
                    noteContentInput.setSelection(noteContentInput.text.length)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {
        // Залишаємо порожнім, бо нам потрібен лише кінцевий результат
    }

    override fun onFinalResult(hypothesis: String) {
        // Залишаємо порожнім, бо onResult обробляє кінцевий результат
    }

    override fun onError(exception: Exception?) {
        if (exception != null) {
            Log.e(TAG, "Vosk recognition error: ${exception.message}", exception)
            Toast.makeText(context, "Помилка голосового вводу: ${exception.message}", Toast.LENGTH_LONG).show()
        }
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "Recognition timeout. Stopping recording.")
        stopListening()
    }
}
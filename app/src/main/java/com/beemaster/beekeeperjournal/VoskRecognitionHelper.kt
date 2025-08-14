// VoskRecognitionHelper.kt Цей файл відповідає за всю логіку, пов'язану з розпізнаванням мовлення.


package com.beemaster.beekeeperjournal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.Recognizer
import org.vosk.Model

class VoskRecognitionHelper(
    private val context: Context,
    private val noteContentInput: EditText,
    private val microphoneBtnNewNote: ImageButton
) : RecognitionListener {

    companion object {
        private const val TAG = "VoskRecognitionHelper"
        private const val RECORD_AUDIO_PERMISSION_CODE = 1
    }

    private var speechService: SpeechService? = null
    private var isVoskListening = false
    private val voskModel: Model?
        get() = BeekeeperApplication.voskModel

    // Метод-хелпер для перевірки стану
    fun isVoskListening(): Boolean = isVoskListening

    fun setupVoskAndStartListening() {
        Log.d(TAG, "setupVoskAndStartListening() called")

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Запитуємо дозвіл, якщо його немає
            ActivityCompat.requestPermissions(context as NewNoteActivity, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_PERMISSION_CODE)
            return
        }

        // Якщо Vosk модель завантажена, запускаємо з затримкою
        if (voskModel != null) {
            // Додаємо невелику затримку в 300 мс
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                startListening()
            }, 300)
        } else {
            // Якщо модель ще не завантажена, чекаємо
            Toast.makeText(context, "Vosk модель завантажується, зачекайте...", Toast.LENGTH_LONG).show()
            BeekeeperApplication.addVoskModelReadyListener {
                // Також додаємо затримку після завантаження моделі
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    startListening()
                }, 300)
            }
        }
    }

    private fun startListening() {
        if (isVoskListening) return

        Log.d(TAG, "startListening() викликано")
        if (voskModel == null) {
            Toast.makeText(context, "Vosk модель ще не завантажено. Спробуйте пізніше.", Toast.LENGTH_SHORT).show()
            return
        }

        speechService?.stop()
        speechService = null

        try {
            microphoneBtnNewNote.isEnabled = false
            val rec = Recognizer(voskModel, 16000f)
            speechService = SpeechService(rec, 16000f)
            speechService?.startListening(this, 10000)

            isVoskListening = true
            microphoneBtnNewNote.isEnabled = true
            microphoneBtnNewNote.backgroundTintList = ContextCompat.getColorStateList(context, R.color.microphone_button_active_color)
            Toast.makeText(context, "Початок голосового вводу...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            microphoneBtnNewNote.isEnabled = true
            Toast.makeText(context, "Помилка Vosk: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Vosk start listening error", e)
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService = null
        isVoskListening = false
        microphoneBtnNewNote.backgroundTintList = ContextCompat.getColorStateList(context, R.color.microphone_button_color)
        Toast.makeText(context, "Голосовий ввід зупинено.", Toast.LENGTH_SHORT).show()
        microphoneBtnNewNote.isEnabled = true
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (voskModel != null) {
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        startListening()
                    }, 300)
                }
            } else {
                Toast.makeText(context, "Дозвіл на запис аудіо відхилено. Голосовий ввід неможливий.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Методи RecognitionListener (Vosk)
    override fun onResult(hypothesis: String) {
        try {
            val voskResult = JSONObject(hypothesis).getString("text")
            noteContentInput.append(" $voskResult")
            noteContentInput.setSelection(noteContentInput.text.length)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Vosk JSON result: ${e.message}", e)
        }
    }

    override fun onPartialResult(hypothesis: String) {
        Log.d(TAG, "onPartialResult: $hypothesis")
    }

    override fun onFinalResult(hypothesis: String) {
        Log.d(TAG, "onFinalResult: $hypothesis")
    }

    override fun onError(exception: Exception) {
        Log.e(TAG, "onError: ${exception.message}", exception)
        Toast.makeText(context, "Помилка голосового вводу: ${exception.message}", Toast.LENGTH_LONG).show()
        stopListening()
    }

    override fun onTimeout() {
        Log.d(TAG, "onTimeout: Recognition timeout. Stopping recording.")
        Toast.makeText(context, "Тайм-аут голосового вводу. Запис зупинено.", Toast.LENGTH_SHORT).show()
        stopListening()
    }
}
package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R

/**
 * Діалог, який відображає процес завантаження та розпаковування моделі Vosk.
 * Призначений для отримання оновлень прогресу від VoskModelManager.
 */
class VoskModelProgressDialog : DialogFragment() {

    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    // Встановлюємо стиль для DialogFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Використовуємо STYLE_NO_TITLE, щоб мати повний контроль над виглядом
        setStyle(STYLE_NO_TITLE, R.style.Theme_BeekeeperJournal_AlertDialog)
        isCancelable = false // Забороняємо скасування під час процесу
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Припускаємо, що у вас є layout resource R.layout.dialog_vosk_progress
        // Тут потрібен layout з ProgressBar та TextView
        val view = inflater.inflate(R.layout.dialog_vosk_progress, container, false)
        // Припускаємо, що ідентифікатори progressTextView та progressBar існують у цьому layout
        progressText = view.findViewById(R.id.progressTextView)
        progressBar = view.findViewById(R.id.progressBar)
        return view
    }

    /**
     * Оновлює діалогове вікно, відображаючи прогрес розпаковування.
     * Викликається з VoskModelManager у головному потоці.
     * @param state Поточний стан (наприклад, "Розпаковування: 50%").
     * @param progress Значення прогресу (0-100), або -1, якщо невідоме/невизначене.
     */
    fun updateProgress(state: String, progress: Int) {
        if (isAdded && !isStateSaved) {
            progressText.text = state
            if (progress >= 0) {
                progressBar.isIndeterminate = false
                progressBar.progress = progress
            } else {
                // Якщо прогрес невідомий (наприклад, під час ініціалізації)
                progressBar.isIndeterminate = true
            }
        }
    }

    companion object {
        const val TAG = "VoskModelProgressDialog"
        fun newInstance(): VoskModelProgressDialog = VoskModelProgressDialog()
    }
}
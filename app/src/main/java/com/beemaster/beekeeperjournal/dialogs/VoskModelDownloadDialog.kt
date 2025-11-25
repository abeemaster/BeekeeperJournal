package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R

/**
 * Діалог, який пропонує користувачу завантажити модель Vosk для офлайн-розпізнавання мови.
 */
class VoskModelDownloadDialog : DialogFragment() {

    // Інтерфейс для передачі результатів натискання в Activity
    interface DownloadDialogListener {
        fun onDownloadConfirmed()
        fun onDownloadCancelled()
    }

    private lateinit var listener: DownloadDialogListener

    // Викликається для створення діалогу
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.vosk_download_dialog_title)
            .setMessage(R.string.vosk_download_dialog_message)
            .setPositiveButton(R.string.vosk_download_dialog_confirm) { _, _ ->
                // Кнопка "Завантажити"
                listener.onDownloadConfirmed()
            }
            .setNegativeButton(R.string.vosk_download_dialog_cancel) { _, _ ->
                // Кнопка "Скасувати"
                listener.onDownloadCancelled()
            }
            .create()
    }

    // Викликається при приєднанні фрагмента до Activity
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Перевіряємо, чи Activity реалізує необхідний інтерфейс
        try {
            listener = context as DownloadDialogListener
        } catch (_: ClassCastException) {
            throw ClassCastException(
                "$context must implement VoskModelDownloadDialog.DownloadDialogListener"
            )
        }
    }
}
package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R

/**
 * Діалог, який запитує підтвердження на завантаження моделі Vosk.
 * Використовує лямбди для обробки подій Confirm/Cancel.
 */
class VoskModelDownloadDialog private constructor() : DialogFragment() {

    // Лямбди для обробки подій
    private var onConfirm: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.vosk_download_dialog_title)) // Наприклад: "Потрібне завантаження"
            .setMessage(getString(R.string.vosk_download_dialog_message)) // Наприклад: "Модель Vosk ще не встановлена. Завантажити її зараз?"
            .setPositiveButton(getString(R.string.vosk_download_dialog_confirm)) { _, _ ->
                onConfirm?.invoke()
            }
            .setNegativeButton(getString(R.string.vosk_download_dialog_cancel)) { _, _ ->
                onCancel?.invoke()
            }
            .create()
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onCancel?.invoke()
    }

    companion object {
        const val TAG = "VoskDownloadConfirmationDialog"

        fun newInstance(onConfirm: () -> Unit, onCancel: () -> Unit): VoskModelDownloadDialog {
            return VoskModelDownloadDialog().apply {
                this.onConfirm = onConfirm
                this.onCancel = onCancel
            }
        }
    }
}
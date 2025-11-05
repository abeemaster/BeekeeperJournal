package com.beemaster.beekeeperjournal.dialogs

import android.app.AlertDialog
import android.content.Context
import com.beemaster.beekeeperjournal.R

/**
 * Функція для відображення стандартного діалогу підтвердження видалення.
 * Цей механізм є простим і не вимагає складного життєвого циклу Fragment.
 */
fun showDeleteConfirmationDialog(
    context: Context,
    titleResId: Int,
    messageResId: Int,
    onConfirm: () -> Unit
) {
    AlertDialog.Builder(context)
        .setTitle(context.getString(titleResId))
        .setMessage(context.getString(messageResId))
        .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
            onConfirm.invoke()
        }
        .setNegativeButton(context.getString(R.string.cancel), null)
        .show()
}

// Спеціальне вікно для запитання про кількість вуликів.

package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class InitialSetupDialogFragment(private val onConfirm: (Int) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext()).apply {
            hint = "Наприклад: 10"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Ласкаво просимо!")
            .setMessage("Скільки у Вас вуликів? Ми автоматично створимо їх для Вас.")
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("Створити") { _, _ ->
                val count = editText.text.toString().toIntOrNull() ?: 1
                onConfirm(if (count > 0) count else 1)
            }
            .create()
    }

    companion object {
        const val TAG = "InitialSetupDialog"
    }
}
package com.beemaster.beekeeperjournal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

/**
 * DialogFragment для редагування номера вулика.
 * Використовує власний макет для повного контролю над дизайном.
 */
class EditHiveNumberDialogFragment : DialogFragment() {

    // ---------------------------------------------------------------------
    // ІНТЕРФЕЙС ТА COMPANION OBJECT
    // ---------------------------------------------------------------------

    interface EditNumberListener {
        fun onNumberSaved(newNumber: String)
    }

    private var listener: EditNumberListener? = null

    companion object {
        const val TAG = "EditNumberDialog"
        private const val ARG_CURRENT_NUMBER = "current_number"

        // Використовуємо newInstance для передачі початкових даних
        fun newInstance(currentNumber: String) =
            EditHiveNumberDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_NUMBER, currentNumber)
                }
            }
    }

    fun setEditNumberListener(listener: EditNumberListener) {
        this.listener = listener
    }
    override fun getTheme(): Int {
        // ✅ ВИПРАВЛЕННЯ: Повертаємо тему Material 3, щоб коректно відображати TextInputLayout.
        // Це виправляє помилку InflateException.
        return com.google.android.material.R.style.Theme_Material3_DayNight_Dialog
    }

    // ---------------------------------------------------------------------
    // ЖИТТЄВИЙ ЦИКЛ FRAGMENT
    // ---------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_edit_hive_number, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentNumber = arguments?.getString(ARG_CURRENT_NUMBER) ?: ""

        val numberEditText: TextInputEditText = view.findViewById(R.id.numberEditText)
        val saveButton: MaterialButton = view.findViewById(R.id.saveButton)
        val cancelButton: MaterialButton = view.findViewById(R.id.cancelButton)

        numberEditText.setText(currentNumber)

        saveButton.setOnClickListener {
            val newNumber = numberEditText.text.toString().trim()

            // Проста валідація
            if (newNumber.isEmpty()) {
                Toast.makeText(context, getString(R.string.hive_number_validation_error), Toast.LENGTH_SHORT).show()
            } else if (newNumber == currentNumber) {
                // Якщо номер не змінився, просто закриваємо
                dismiss()
            } else {
                listener?.onNumberSaved(newNumber)
                dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        // Встановлюємо ширину діалогу на MATCH_PARENT, щоб він виглядав добре
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
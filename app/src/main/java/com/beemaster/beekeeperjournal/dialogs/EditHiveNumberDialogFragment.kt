// Файл: EditHiveNumberDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

/**
 * Діалогове вікно для редагування номера вулика.
 * Повертає новий номер вулика через Fragment Result API.
 */
class EditHiveNumberDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EditNumberDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "editNumberRequest"
        const val KEY_NEW_NUMBER = "newHiveNumber"
        const val KEY_HIVE_ID = "hiveId"
        private const val ARG_CURRENT_NUMBER = "currentNumber"


        fun newInstance(hiveId: Long, currentNumber: String): EditHiveNumberDialogFragment {
            return EditHiveNumberDialogFragment().apply {
                arguments = bundleOf(
                    KEY_HIVE_ID to hiveId,
                    ARG_CURRENT_NUMBER to currentNumber
                )
            }
        }
    }

    // Отримуємо ID вулика для повернення його у результаті
    private val hiveId: Long
        get() = arguments?.getLong(KEY_HIVE_ID) ?: 0L

    // Отримуємо поточний номер для відображення
    private val currentNumber: String
        get() = arguments?.getString(ARG_CURRENT_NUMBER) ?: ""


    // Файл: EditHiveNumberDialogFragment.kt (у onCreateDialog)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentNumber)
            setHint("Введіть новий номер вулика")
            // Додаємо невеликий відступ для кращого вигляду
            setPadding(50, 50, 50, 50)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редагувати номер вулика")
            .setView(editText)

            // Кнопка "Зберегти"
            .setPositiveButton("Зберегти") { _, _ ->
                val newNumber = editText.text.toString()

                // ВСТАНОВЛЮЄМО РЕЗУЛЬТАТ
                setFragmentResult(KEY_REQUEST, bundleOf(
                    KEY_NEW_NUMBER to newNumber,
                    KEY_HIVE_ID to hiveId
                ))
            }

            // Кнопка "Скасувати"
            .setNegativeButton("Скасувати") { _, _ ->
                // ✅ ВИПРАВЛЕННЯ: Надсилаємо порожній результат, щоб HiveOptionsDialogFragment закрився
                setFragmentResult(KEY_REQUEST, bundleOf(
                    KEY_NEW_NUMBER to null, // Надсилаємо null, бо скасували
                    KEY_HIVE_ID to hiveId
                ))
                // Не потрібно викликати dialog.cancel() чи dialog.dismiss(), бо AlertDialog це зробить сам
            }
            .create()

        // ✅ ВИПРАВЛЕННЯ: ЗАБОРОНЯЄМО ЗАКРИТТЯ ПРИ НАТИСКАННІ ЗОВНІ
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }
}
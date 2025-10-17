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

        // Ключі для аргументів
        private const val ARG_HIVE_ID = "hiveId"
        private const val ARG_CURRENT_NUMBER = "currentNumber"
        const val KEY_HIVE_ID_RESULT = "hiveIdResult"

        fun newInstance(hiveId: Long, currentNumber: String): EditHiveNumberDialogFragment {
            return EditHiveNumberDialogFragment().apply {
                arguments = bundleOf(
                    ARG_HIVE_ID to hiveId,
                    ARG_CURRENT_NUMBER to currentNumber
                )
            }
        }
    }

    // Отримуємо ID вулика для повернення його у результаті
    private val hiveId: Long
        get() = arguments?.getLong(ARG_HIVE_ID) ?: 0L

    // Отримуємо поточний номер для відображення
    private val currentNumber: String
        get() = arguments?.getString(ARG_CURRENT_NUMBER) ?: ""


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(currentNumber)
            setHint("Введіть новий номер вулика")
            // Додаємо невеликий відступ для кращого вигляду
            setPadding(50, 50, 50, 50)
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Редагувати номер вулика")
            .setView(editText)

            // Кнопка "Зберегти"
            .setPositiveButton("Зберегти") { _, _ ->
                val newNumber = editText.text.toString()

                // ✅ ВСТАНОВЛЮЄМО РЕЗУЛЬТАТ З ВИКОРИСТАННЯМ НОВОГО ПУБЛІЧНОГО КЛЮЧА
                setFragmentResult(KEY_REQUEST, bundleOf(
                    KEY_NEW_NUMBER to newNumber,
                    KEY_HIVE_ID_RESULT to hiveId // ✅ Використовуйте цей ключ!
                ))
                // Встановлюємо результат, повертаючи ID вулика та новий номер
                //setFragmentResult(KEY_REQUEST, bundleOf(
                //    KEY_NEW_NUMBER to newNumber,
                //    ARG_HIVE_ID to hiveId
                //))
            }

            // Кнопка "Скасувати"
            .setNegativeButton("Скасувати") { _, _ ->
                // Нічого не робимо, просто закриваємо діалог
            }
            .create()
    }
}
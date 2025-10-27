// Файл: EditHiveNumberDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.beemaster.beekeeperjournal.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

/**
 * Діалогове вікно для редагування номера вулика.
 * Повертає новий номер вулика через Fragment Result API.
 */
class EditHiveNumberDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "EditNumberDialog"
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

    private val hiveId: Long
        get() = arguments?.getLong(KEY_HIVE_ID) ?: 0L

    private val currentNumber: String
        get() = arguments?.getString(ARG_CURRENT_NUMBER) ?: ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = layoutInflater.inflate(R.layout.dialog_edit_hive_number, null)
        val editText = view.findViewById<TextInputEditText>(R.id.numberEditText)

        editText.setText(currentNumber)

        // ВИКОРИСТОВУЄМО ТЕМУ ДЛЯ КРАСИВИХ КНОПОК
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_BeekeeperJournal_AlertDialog)
            // .setTitle("Редагувати номер вулика") // <-- ВИДАЛЕНО, щоб уникнути подвійного заголовка
            .setView(view)

            // Створюємо кнопки з текстом, але без стандартного слухача (використовуємо 'null')
            // Це дозволяє AlertDialog стилізувати кнопки Material Design.
            .setPositiveButton("Зберегти", null)
            .setNegativeButton("Скасувати", null)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.let { window ->
            // Примусове встановлення заокругленого фону для вікна
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_custom_corners)

            // Встановлюємо ширину 87% від ширини екрана

            val width = (resources.displayMetrics.widthPixels * 0.87).toInt()
            // Встановлюємо висоту по вмісту
            val height = WindowManager.LayoutParams.WRAP_CONTENT
            // Застосовуємо нові розміри до вікна діалогу
            window.setLayout(width, height)

            // ✅ ДОДАНО: Запобігає панорамуванню, змушуючи вікно змінювати розмір.
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Отримуємо AlertDialog для доступу до його елементів
        val alertDialog = dialog as? AlertDialog ?: return

        // 1. НАЛАШТУВАННЯ КНОПКИ "ЗБЕРЕГТИ" (позитивна)
        val positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            // Отримуємо посилання на поле вводу
            val editText = alertDialog.findViewById<TextInputEditText>(R.id.numberEditText)
            val newNumber = editText?.text?.toString()?.trim()

            // Тут може бути логіка валідації...

            // ПЕРЕДАЄМО РЕЗУЛЬТАТ
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NEW_NUMBER to newNumber,
                KEY_HIVE_ID to hiveId
            ))
            dismiss()
        }

        // 2. НАЛАШТУВАННЯ КНОПКИ "СКАСУВАТИ" (негативна)
        val negativeButton = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
        negativeButton.setOnClickListener {
            // Передаємо результат скасування (null)
            setFragmentResult(KEY_REQUEST, bundleOf(
                KEY_NEW_NUMBER to null,
                KEY_HIVE_ID to hiveId
            ))
            dismiss()
        }
    }
}
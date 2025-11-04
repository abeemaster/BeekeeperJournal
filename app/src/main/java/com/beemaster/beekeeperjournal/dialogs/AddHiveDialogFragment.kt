package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder


/**
 * Діалогове вікно для додавання нового вулика.
 * Використовує onCreateDialog() для коректної інтеграції в життєвий цикл Fragment.
 */
class AddHiveDialogFragment : DialogFragment() {

    private lateinit var listener: OnHiveAddedListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as OnHiveAddedListener
        } catch (_: ClassCastException) {
            throw ClassCastException("$context must implement OnHiveAddedListener")
        }
    }



    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 1. Inflate макет
        val dialogView = View.inflate(context, R.layout.dialog_add_hive, null)
        val numberEditText: EditText = dialogView.findViewById(R.id.numberEditText)

        // 2. Створюємо MaterialAlertDialogBuilder
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_BeekeeperJournal_AlertDialog)
            .setView(dialogView)

            // 3. Обробка кнопки "Зберегти"
            .setPositiveButton(requireContext().getString(R.string.button_save)) { _, _ ->
                val hiveNumber = numberEditText.text.toString().trim()

                if (hiveNumber.isNotBlank()) {
                    listener.onHiveAdded(hiveNumber) // Викликаємо метод інтерфейсу
                } else {
                    Toast.makeText(
                        context,
                        requireContext().getString(R.string.hive_number_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // 4. Обробка кнопки "Скасувати"
            .setNegativeButton(requireContext().getString(R.string.cancel), null)
            .create()

        // 5. Налаштування вікна (Фон, Ширина та  Клавіатура)
        dialog.window?.let { window ->
            // Примусове встановлення заокругленого фону для вікна
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_custom_corners)

            // Встановлюємо ширину 87% від ширини екрана
            val width = (resources.displayMetrics.widthPixels * 0.87).toInt()
            // Встановлюємо висоту по вмісту
            val height = WindowManager.LayoutParams.WRAP_CONTENT
            // Застосовуємо нові розміри до вікна діалогу
            window.setLayout(width, height)

            // Запобігає панорамуванню, змушуючи вікно змінювати розмір.
            // window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE) - цей рядок треба видалити якщо не виникатиме проблем
        }

        return dialog
    }

    companion object {
        const val TAG = "AddHiveDialogFragment"

        fun newInstance(): AddHiveDialogFragment {
            return AddHiveDialogFragment()
        }
    }
}
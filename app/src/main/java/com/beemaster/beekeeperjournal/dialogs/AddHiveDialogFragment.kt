package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.beemaster.beekeeperjournal.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.beemaster.beekeeperjournal.dialogs.IOnHiveAddedListener // ✅ ІМПОРТ ПРАЦЮВАТИМЕ

/**
 * Діалогове вікно для додавання нового вулика.
 * Використовує onCreateDialog() для коректної інтеграції в життєвий цикл Fragment.
 */
class AddHiveDialogFragment : DialogFragment() {

    private lateinit var listener: IOnHiveAddedListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Прив'язуємо слухача
        try {
            listener = context as IOnHiveAddedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement IOnHiveAddedListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // 1. Inflate макет
        val dialogView = View.inflate(context, R.layout.dialog_add_hive, null)
        val numberEditText: EditText = dialogView.findViewById(R.id.numberEditText)

        // 2. Створюємо MaterialAlertDialogBuilder
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)

            // 3. Обробка кнопки "Зберегти"
            .setPositiveButton(requireContext().getString(R.string.save)) { _, _ ->
                val hiveNumber = numberEditText.text.toString().trim()

                if (hiveNumber.isNotBlank()) {
                    listener.onHiveAdded(hiveNumber) // ✅ Викликаємо метод інтерфейсу
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
    }

    companion object {
        const val TAG = "AddHiveDialogFragment"

        fun newInstance(): AddHiveDialogFragment {
            return AddHiveDialogFragment()
        }
    }
}
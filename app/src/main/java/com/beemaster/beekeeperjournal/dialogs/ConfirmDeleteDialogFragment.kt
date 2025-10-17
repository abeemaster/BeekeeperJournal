// Файл: ConfirmDeleteDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult

/**
 * Діалогове вікно для підтвердження видалення елемента.
 * Повертає результат через Fragment Result API.
 */
class ConfirmDeleteDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "ConfirmDeleteDialog"

        // Ключі для Fragment Result API
        const val KEY_REQUEST = "deleteConfirmationRequest"
        const val KEY_CONFIRMED = "isConfirmed"

        // Ключі для аргументів
        private const val ARG_ITEM_ID = "itemId"
        private const val ARG_ITEM_TYPE = "itemType"

        /**
         * Створює новий екземпляр діалогу підтвердження.
         *
         * @param itemId ID елемента для видалення (повертається у bundle, якщо потрібно).
         * @param itemType Тип елемента (наприклад, "вулик", "запис") для відображення у тексті.
         */
        fun newInstance(itemId: Long, itemType: String): ConfirmDeleteDialogFragment {
            return ConfirmDeleteDialogFragment().apply {
                arguments = bundleOf(
                    ARG_ITEM_ID to itemId,
                    ARG_ITEM_TYPE to itemType
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val itemId = arguments?.getLong(ARG_ITEM_ID) ?: 0L
        val itemType = arguments?.getString(ARG_ITEM_TYPE) ?: "елемент"

        return AlertDialog.Builder(requireContext())
            .setTitle("Підтвердження видалення")
            .setMessage("Ви впевнені, що хочете видалити цей $itemType? Цю дію не можна скасувати.")

            // Кнопка "Так" (Підтвердити)
            .setPositiveButton("Видалити") { _, _ ->
                // Встановлюємо результат: видалення підтверджено (true)
                setFragmentResult(KEY_REQUEST, bundleOf(KEY_CONFIRMED to true, ARG_ITEM_ID to itemId))
            }

            // Кнопка "Ні" (Скасувати)
            .setNegativeButton("Скасувати") { _, _ ->
                // Встановлюємо результат: видалення скасовано (false)
                setFragmentResult(KEY_REQUEST, bundleOf(KEY_CONFIRMED to false, ARG_ITEM_ID to itemId))
            }
            .create()
    }
}
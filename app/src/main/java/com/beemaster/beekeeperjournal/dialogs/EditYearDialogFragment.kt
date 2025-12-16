// dialogs/EditYearDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.viewmodel.BeekeepingYearViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * DialogFragment для редагування назви існуючого пасічного року.
 * Використовує MaterialAlertDialogBuilder для коректної роботи з клавіатурою.
 */
@AndroidEntryPoint
class EditYearDialogFragment : DialogFragment() {

    private val viewModel: BeekeepingYearViewModel by activityViewModels()

    // Ініціалізація поля введення як властивість класу
    private var yearNameEditText: TextInputEditText? = null

    // ID року
    private val yearId: Long
        get() = arguments?.getLong(ARG_YEAR_ID)
            ?: throw IllegalStateException("EditYearDialogFragment requires year ID.")

    // Отримуємо поточний об'єкт року (завжди актуальний зі StateFlow)
    private fun getCurrentYear() = viewModel.yearListState.value.years.find { it.yearId == yearId }

    companion object {
        const val TAG = "EditYearDialog"
        private const val ARG_YEAR_ID = "year_id"

        fun newInstance(yearId: Long): EditYearDialogFragment {
            return EditYearDialogFragment().apply {
                arguments = bundleOf(ARG_YEAR_ID to yearId)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val yearData = getCurrentYear()

        if (yearData == null) {
            Toast.makeText(requireContext(), R.string.error_year_not_found, Toast.LENGTH_SHORT).show()
            dismiss()
            return super.onCreateDialog(savedInstanceState)
        }

        // 1. Створюємо view для вмісту діалогу
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_year, null)

        // 2. Ініціалізуємо поле введення як властивість класу, щоб до нього можна було звертатися пізніше
        yearNameEditText = view.findViewById(R.id.yearNameEditText)
        yearNameEditText?.setText(yearData.name)

        // 3. Створюємо AlertDialog з Material Design стилем
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.Theme_BeekeeperJournal_AlertDialog)
            .setView(view)
            .setPositiveButton(R.string.action_save, null) // Кнопка з null-слухачем
            .setNegativeButton(R.string.action_cancel, null) // Кнопка з null-слухачем
            .create()

        // 4. Налаштування вікна
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_custom_corners)

            val width = (resources.displayMetrics.widthPixels * 0.87).toInt()
            val height = WindowManager.LayoutParams.WRAP_CONTENT
            window.setLayout(width, height)

            // Фокус на полі введення і відкриття клавіатури
            yearNameEditText?.requestFocus()
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // 5. Отримуємо AlertDialog для доступу до кнопок (після створення діалогу)
        val alertDialog = dialog as? AlertDialog ?: return

        // НАЛАШТУВАННЯ КНОПКИ "ЗБЕРЕГТИ"
        val positiveButton = alertDialog.getButton(Dialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            // Викликаємо логіку збереження
            saveYearChanges()
        }

        // НАЛАШТУВАННЯ КНОПКИ "СКАСУВАТИ"
        val negativeButton = alertDialog.getButton(Dialog.BUTTON_NEGATIVE)
        negativeButton.setOnClickListener {
            dismiss()
        }
    }


    /**
     * Обробка збереження: валідація та виклик функції оновлення у ViewModel.
     */
    private fun saveYearChanges() {
        val newName = yearNameEditText?.text?.toString()?.trim()

        if (newName.isNullOrEmpty()) {
            yearNameEditText?.error = getString(R.string.error_year_name_required)
            return
        }

        val yearToUpdate = getCurrentYear()

        if (yearToUpdate == null) {
            Toast.makeText(requireContext(), R.string.error_year_not_found, Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // ВИПРАВЛЕННЯ: Змінюємо viewLifecycleOwner на lifecycleScope для надійності в onCreateDialog
        lifecycleScope.launch {
            val success = viewModel.updateYear(
                yearId = yearToUpdate.yearId,
                newName = newName,
                startDate = yearToUpdate.startDate
            )

            if (success) {
                Toast.makeText(requireContext(), getString(R.string.year_updated_successfully, newName), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.error_update_failed, Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
    }
}
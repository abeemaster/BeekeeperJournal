// dialogs/EditYearDialogFragment.kt

package com.beemaster.beekeeperjournal.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.viewmodel.BeekeepingYearViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * DialogFragment для редагування назви існуючого пасічного року.
 * Викликається після натискання на "Редагувати" у YearActionsDialogFragment.
 */
@AndroidEntryPoint
class EditYearDialogFragment : BottomSheetDialogFragment() {

    // Інжектуємо ViewModel, оскільки він має бути спільним з BeekeeperYearDialogFragment
    private val viewModel: BeekeepingYearViewModel by activityViewModels()

    private lateinit var yearNameEditText: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    // ID року, який ми редагуємо
    private val yearId: Long
        get() = arguments?.getLong(ARG_YEAR_ID)
            ?: throw IllegalStateException("EditYearDialogFragment requires year ID.")

    companion object {
        const val TAG = "EditYearDialog"
        private const val ARG_YEAR_ID = "year_id"

        fun newInstance(yearId: Long): EditYearDialogFragment {
            return EditYearDialogFragment().apply {
                arguments = bundleOf(ARG_YEAR_ID to yearId)
            }
        }
    }

    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Використовуємо наш новий макет
        return inflater.inflate(R.layout.dialog_edit_year, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadYearData()
        setupListeners()
    }

    private fun initViews(view: View) {
        yearNameEditText = view.findViewById(R.id.yearNameEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    /**
     * Завантажує поточні дані року (Назву) для відображення у полі редагування.
     */
    private fun loadYearData() {
        // Ми використовуємо stateFlow з ViewModel, щоб знайти потрібний рік
        val currentYear = viewModel.yearListState.value.years.find { it.yearId == yearId }

        if (currentYear != null) {
            // Відображаємо поточну назву року
            yearNameEditText.setText(currentYear.name)
        } else {
            // Якщо рік не знайдено, ми не можемо редагувати. Закриваємо діалог.
            Toast.makeText(requireContext(), R.string.error_year_not_found, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun setupListeners() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        saveButton.setOnClickListener {
            saveYearChanges()
        }
    }

    /**
     * Обробка збереження: валідація та виклик функції оновлення у ViewModel.
     */
    private fun saveYearChanges() {
        val newName = yearNameEditText.text.toString().trim()

        if (newName.isEmpty()) {
            yearNameEditText.error = getString(R.string.error_year_name_required)
            return
        }

        // 1. Отримуємо старий об'єкт року, щоб зберегти стару дату початку
        val currentYear = viewModel.yearListState.value.years.find { it.yearId == yearId }

        if (currentYear == null) {
            Toast.makeText(requireContext(), R.string.error_year_not_found, Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // 2. Викликаємо оновлення у ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            // NOTE: Ми викликаємо функцію updateYear. Припускаємо, що така функція вже існує
            // або буде реалізована у BeekeepingYearViewModel.
            val success = viewModel.updateYear(
                yearId = currentYear.yearId,
                newName = newName,
                startDate = currentYear.startDate // Передаємо стару дату, щоб не втратити її
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
package com.beemaster.beekeeperjournal.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Діалогове вікно для додавання або редагування витрат.
 * Використовує Hilt для доступу до ProfitabilityViewModel.
 */
@AndroidEntryPoint
class AddExpenseDialogFragment : DialogFragment() {

    private val viewModel: ProfitabilityViewModel by viewModels()
    private var expenseToEdit: Expense? = null
    private var hiveId: Int = 0

    // ✅ Ініціалізація та форматування
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    /** Отримує аргументи (hiveId та expenseToEdit). */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Перевіряємо ID вулика
            hiveId = it.getInt(ARG_HIVE_ID, 0)

            // ✅ ВИПРАВЛЕННЯ 1: Використання сучасного, безпечного getSerializable
            expenseToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_EXPENSE_TO_EDIT, Expense::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_EXPENSE_TO_EDIT) as? Expense
            }
        }
    }

    /** Створює об'єкт діалогу, налаштовує поля, кнопки та логіку збереження. */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val view = layoutInflater.inflate(R.layout.expense_dialog, null)

        // 1. Пошук елементів UI
        val nameEditText: EditText = view.findViewById(R.id.expense_name_edit_text)
        val quantityEditText: EditText = view.findViewById(R.id.expense_quantity)
        val quantityUnitsEditText: EditText = view.findViewById(R.id.expense_quantity_units)
        val amountEditText: EditText = view.findViewById(R.id.expense_amount_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.expense_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_expense_button)

        // 2. Налаштування полів на основі режиму (редагування чи додавання)
        setupFields(nameEditText, quantityEditText, quantityUnitsEditText, amountEditText, dateEditText)

        // 3. Логіка вибору дати
        setupDatePicking(context, dateEditText)

        // 4. Логіка кнопки збереження
        setupSaveButton(context, saveButton, nameEditText, quantityEditText, quantityUnitsEditText, amountEditText)

        // 5. Створення діалогу Material Design (без стандартних кнопок)
        val dialog = MaterialAlertDialogBuilder(context, R.style.Theme_BeekeeperJournal_AlertDialog)
            .setView(view)
            .create()

        // 6. Налаштування вікна (розмір та фон)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.drawable.bg_dialog_custom_corners)
            val width = (resources.displayMetrics.widthPixels * 0.87).toInt()
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        return dialog
    }

    /** Заповнює поля даними, якщо це режим редагування. */
    private fun setupFields(
        nameEditText: EditText,
        quantityEditText: EditText,
        quantityUnitsEditText: EditText,
        amountEditText: EditText,
        dateEditText: EditText
    ) {
        if (expenseToEdit != null) {
            expenseToEdit?.let { expense ->
                nameEditText.setText(expense.name)
                quantityEditText.setText(expense.quantityUnits.toString())
                quantityUnitsEditText.setText(expense.nameQuantity)
                amountEditText.setText(expense.amount.toString())
                calendar.time = Date(expense.date)
            }
        }
        // Встановлюємо дату в EditText (поточна для нового, або з об'єкта для редагування)
        dateEditText.setText(dateFormat.format(calendar.time))
    }

    /** Налаштовує виклик DatePickerDialog. */
    private fun setupDatePicking(context: Context, dateEditText: EditText) {
        dateEditText.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }

    /** Обробляє натискання кнопки "Зберегти" та виконує валідацію/збереження. */
    private fun setupSaveButton(
        context: Context,
        saveButton: Button,
        nameEditText: EditText,
        quantityEditText: EditText,
        quantityUnitsEditText: EditText,
        amountEditText: EditText
    ) {
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val quantityUnits = quantityEditText.text.toString().toDoubleOrNull() ?: 0.0
            val nameQuantity = quantityUnitsEditText.text.toString().trim()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val dateAsLong = calendar.time.time

            // Валідація
            if (name.isNotEmpty() && amount > 0) {
                if (expenseToEdit == null) {
                    // Режим додавання
                    val newExpense = Expense(
                        name = name,
                        amount = amount,
                        date = dateAsLong,
                        quantityUnits = quantityUnits,
                        nameQuantity = nameQuantity,
                        hiveId = hiveId
                    )
                    viewModel.insertExpense(newExpense)
                } else {
                    // Режим редагування
                    val updatedExpense = expenseToEdit!!.copy(
                        name = name,
                        amount = amount,
                        date = dateAsLong,
                        quantityUnits = quantityUnits,
                        nameQuantity = nameQuantity
                    )
                    viewModel.updateExpense(updatedExpense)
                }
                dismiss() // Закриваємо діалог
            } else {
                Toast.makeText(context, context.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val ARG_HIVE_ID = "hive_id"
        private const val ARG_EXPENSE_TO_EDIT = "expense_to_edit"
        const val TAG = "AddExpenseDialogFragment"

        /**
         * Створює новий екземпляр діалогу з передачею необхідних аргументів.
         * @param hiveId ID вулика, до якого прив'язана витрата.
         * @param expenseToEdit Об'єкт Expense для режиму редагування, або null для режиму додавання.
         */
        fun newInstance(hiveId: Int, expenseToEdit: Expense? = null): AddExpenseDialogFragment {
            return AddExpenseDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HIVE_ID, hiveId)
                    putSerializable(ARG_EXPENSE_TO_EDIT, expenseToEdit)
                }
            }
        }
    }
}
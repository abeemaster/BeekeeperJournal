package com.beemaster.beekeeperjournal.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.os.Build // ✅ ДОДАНО
import android.os.Bundle
// ❌ ВИДАЛЕНО: import android.view.LayoutInflater // Більше не потрібен
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Діалогове вікно для додавання або редагування прибутку.
 * Використовує Hilt для доступу до ProfitabilityViewModel.
 */
@AndroidEntryPoint
class AddIncomeDialogFragment : DialogFragment() {

    private val viewModel: ProfitabilityViewModel by viewModels()
    private var incomeToEdit: Income? = null
    private var hiveId: Int = 0

    // ✅ Ініціалізація та форматування
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    /**
     * Отримує аргументи (hiveId та incomeToEdit) з Bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Перевіряємо ID вулика
            hiveId = it.getInt(ARG_HIVE_ID, 0)

            // ✅ ВИПРАВЛЕННЯ 1: Використання сучасного, безпечного методу getSerializable
            incomeToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_INCOME_TO_EDIT, Income::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_INCOME_TO_EDIT) as? Income
            }
        }
    }

    /**
     * Створює об'єкт діалогу, налаштовує поля, кнопки та логіку збереження.
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        // ✅ ВИПРАВЛЕННЯ 2: Заміна LayoutInflater.from(context) на layoutInflater
        val view = layoutInflater.inflate(R.layout.income_dialog, null)

        // 1. Пошук елементів UI
        val descriptionEditText: EditText = view.findViewById(R.id.income_description_edit_text)
        val amountEditText: EditText = view.findViewById(R.id.income_amount_edit_text)
        val unitEditText: EditText = view.findViewById(R.id.income_unit_edit_text)
        val pricePerUnitEditText: EditText = view.findViewById(R.id.income_price_per_unit_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.income_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_income_button)

        // 2. Налаштування полів на основі режиму (редагування чи додавання)
        // ✅ ВИПРАВЛЕННЯ 3: Видалено saveButton з аргументів
        setupFields(descriptionEditText, amountEditText, unitEditText, pricePerUnitEditText, dateEditText)

        // 3. Логіка вибору дати
        setupDatePicking(context, dateEditText)

        // 4. Логіка кнопки збереження
        setupSaveButton(context, saveButton, descriptionEditText, amountEditText, unitEditText, pricePerUnitEditText)

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
        descriptionEditText: EditText,
        amountEditText: EditText,
        unitEditText: EditText,
        pricePerUnitEditText: EditText,
        dateEditText: EditText
        // saveButton: Button був видалений тут
    ) {
        if (incomeToEdit != null) {
            incomeToEdit?.let { income ->
                descriptionEditText.setText(income.productName)
                amountEditText.setText(income.quantity.toString())
                unitEditText.setText(income.unitName)
                pricePerUnitEditText.setText(income.price.toString())
                calendar.time = Date(income.date)
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
        descriptionEditText: EditText,
        amountEditText: EditText,
        unitEditText: EditText,
        pricePerUnitEditText: EditText
    ) {
        saveButton.setOnClickListener {
            val productName = descriptionEditText.text.toString().trim()
            val unitName = unitEditText.text.toString().trim()
            val quantity = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val price = pricePerUnitEditText.text.toString().toDoubleOrNull() ?: 0.0
            val dateAsLong = calendar.time.time

            // Валідація
            if (productName.isNotEmpty() && quantity > 0 && price > 0) {
                if (incomeToEdit == null) {
                    // Режим додавання
                    val newIncome = Income(
                        productName = productName,
                        quantity = quantity,
                        price = price,
                        unitName = unitName,
                        totalAmount = quantity * price,
                        date = dateAsLong,
                        hiveId = hiveId
                    )
                    viewModel.insertIncome(newIncome)
                } else {
                    // Режим редагування
                    val updatedIncome = incomeToEdit!!.copy(
                        productName = productName,
                        quantity = quantity,
                        price = price,
                        unitName = unitName,
                        totalAmount = quantity * price,
                        date = dateAsLong
                    )
                    viewModel.updateIncome(updatedIncome)
                }
                dismiss() // Закриваємо діалог
            } else {
                Toast.makeText(context, context.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }


    companion object {
        private const val ARG_HIVE_ID = "hive_id"
        // ВИПРАВЛЕНО: Income тепер має бути Serializable.
        private const val ARG_INCOME_TO_EDIT = "income_to_edit"
        const val TAG = "AddIncomeDialogFragment"

        /**
         * Створює новий екземпляр діалогу з передачею необхідних аргументів.
         * @param hiveId ID вулика, до якого прив'язаний прибуток.
         * @param incomeToEdit Об'єкт Income для режиму редагування, або null для режиму додавання.
         */
        fun newInstance(hiveId: Int, incomeToEdit: Income? = null): AddIncomeDialogFragment {
            return AddIncomeDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HIVE_ID, hiveId)
                    putSerializable(ARG_INCOME_TO_EDIT, incomeToEdit)
                }
            }
        }
    }
}
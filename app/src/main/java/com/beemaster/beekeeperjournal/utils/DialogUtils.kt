// DialogUtils.kt

package com.beemaster.beekeeperjournal.utils

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.models.Expense
import com.beemaster.beekeeperjournal.models.Income
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Об'єкт-утиліта, що містить статичні методи для відображення різних діалогових вікон
 * у додатку (додавання/редагування даних, підтвердження, опції вуликів).
 */
object DialogUtils {

    /**
     * Відображає діалог підтвердження перед виконанням деструктивної дії (видалення).
     *
     * @param context Контекст для створення діалогу.
     * @param titleResId Ресурс ID для заголовка діалогу.
     * @param messageResId Ресурс ID для тексту повідомлення.
     * @param onConfirm Лямбда, що викликається при підтвердженні дії.
     */
    fun showDeleteConfirmationDialog(
        context: Context,
        titleResId: Int,
        messageResId: Int,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(titleResId))
            .setMessage(context.getString(messageResId))
            .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                onConfirm.invoke()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    /**
     * Відображає діалог для додавання або редагування прибутку.
     *
     * @param context Контекст для створення діалогу.
     * @param viewModel ViewModel для взаємодії з даними (insert/update Income).
     * @param hiveId ID вулика, до якого прив'язаний прибуток.
     * @param incomeToEdit Опціональний об'єкт Income. Якщо не null, діалог працює в режимі редагування.
     */
    fun showAddIncomeDialog(
        context: Context,
        viewModel: ProfitabilityViewModel,
        hiveId: Int,
        incomeToEdit: Income? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.income_dialog, null)
        val descriptionEditText: EditText = view.findViewById(R.id.income_description_edit_text)
        val amountEditText: EditText = view.findViewById(R.id.income_amount_edit_text)
        val unitEditText: EditText = view.findViewById(R.id.income_unit_edit_text)
        val pricePerUnitEditText: EditText = view.findViewById(R.id.income_price_per_unit_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.income_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_income_button)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        if (incomeToEdit != null) {
            descriptionEditText.setText(incomeToEdit.productName)
            amountEditText.setText(incomeToEdit.quantity.toString())
            unitEditText.setText(incomeToEdit.unitName)
            pricePerUnitEditText.setText(incomeToEdit.price.toString())
            calendar.time = Date(incomeToEdit.date)
            saveButton.text = context.getString(R.string.button_save)
        } else {
            dateEditText.setText(dateFormat.format(calendar.time))
        }

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

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .show()

        saveButton.setOnClickListener {
            val productName = descriptionEditText.text.toString().trim()
            val unitName = unitEditText.text.toString().trim()
            val quantity = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val price = pricePerUnitEditText.text.toString().toDoubleOrNull() ?: 0.0
            val dateAsLong = calendar.time.time

            if (productName.isNotEmpty() && quantity > 0 && price > 0) {
                if (incomeToEdit == null) {
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
                    val updatedIncome = incomeToEdit.copy(
                        productName = productName,
                        quantity = quantity,
                        price = price,
                        unitName = unitName,
                        totalAmount = quantity * price,
                        date = dateAsLong
                    )
                    viewModel.updateIncome(updatedIncome)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Відображає діалог для додавання або редагування витрат.
     *
     * @param context Контекст для створення діалогу.
     * @param viewModel ViewModel для взаємодії з даними (insert/update Expense).
     * @param hiveId ID вулика, до якого прив'язана витрата.
     * @param expenseToEdit Опціональний об'єкт Expense. Якщо не null, діалог працює в режимі редагування.
     */
    fun showAddExpenseDialog(
        context: Context,
        viewModel: ProfitabilityViewModel,
        hiveId: Int,
        expenseToEdit: Expense? = null
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.expense_dialog, null)
        val nameEditText: EditText = view.findViewById(R.id.expense_name_edit_text)
        val quantityEditText: EditText = view.findViewById(R.id.expense_quantity)
        val quantityUnitsEditText: EditText = view.findViewById(R.id.expense_quantity_units)
        val amountEditText: EditText = view.findViewById(R.id.expense_amount_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.expense_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_expense_button)
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        if (expenseToEdit != null) {
            nameEditText.setText(expenseToEdit.name)
            quantityEditText.setText(expenseToEdit.quantityUnits.toString())
            quantityUnitsEditText.setText(expenseToEdit.nameQuantity)
            amountEditText.setText(expenseToEdit.amount.toString())
            calendar.time = Date(expenseToEdit.date)
            saveButton.text = context.getString(R.string.button_save)
        } else {
            dateEditText.setText(dateFormat.format(calendar.time))
        }

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

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .show()

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val quantityUnits = quantityEditText.text.toString().toDoubleOrNull() ?: 0.0
            val nameQuantity = quantityUnitsEditText.text.toString().trim()
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val dateAsLong = calendar.time.time

            if (name.isNotEmpty() && amount > 0) {
                if (expenseToEdit == null) {
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
                    val updatedExpense = expenseToEdit.copy(
                        name = name,
                        amount = amount,
                        date = dateAsLong,
                        quantityUnits = quantityUnits,
                        nameQuantity = nameQuantity
                    )
                    viewModel.updateExpense(updatedExpense)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(context, context.getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show()
            }
        }
    }

}
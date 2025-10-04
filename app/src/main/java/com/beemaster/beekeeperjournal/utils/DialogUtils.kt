package com.beemaster.beekeeperjournal.utils

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Toast
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.viewmodel.ProfitabilityViewModel
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DialogUtils {

    /** Загальний діалог для редагування або видалення.
     * @param onEdit Функція, що виконується при виборі "Редагувати".
     * @param onDelete Функція, що виконується при виборі "Видалити".
     */
    fun showEditDeleteDialog(
        context: Context,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val options = arrayOf("Редагувати", "Видалити")
        AlertDialog.Builder(context)
            .setTitle(R.string.choose_an_action)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onEdit()
                    1 -> onDelete()
                }
            }
            .show()
    }
    // ✅ Нова функція для відображення діалогу підтвердження видалення
    fun showDeleteConfirmationDialog(
        context: Context,
        // Приймаємо ресурси для гнучкості
        titleResId: Int,
        messageResId: Int,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(titleResId)) // Використовуємо переданий ресурс
            .setMessage(context.getString(messageResId)) // Використовуємо переданий ресурс
            .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                onConfirm.invoke()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }
    /**
     * Тепер ця функція може працювати і для редагування існуючого прибутку.
     * @param incomeToEdit Опціональний об'єкт IncomeEntity. Якщо він не null,
     * діалог працює в режимі редагування і заповнює поля даними.
     * @param hiveId Ідентифікатор вулика. Необхідний лише для додавання нового запису.
     */
    fun showAddIncomeDialog(
        context: Context,
        viewModel: ProfitabilityViewModel,
        hiveId: Int,
        incomeToEdit: IncomeEntity? = null
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
            saveButton.text = "Зберегти"
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
            val date = calendar.time
            val dateAsLong = date.time

            if (productName.isNotEmpty() && quantity > 0 && price > 0) {
                if (incomeToEdit == null) {
                    // ✅ ЛОГІКА ДОДАВАННЯ: створюємо новий об'єкт
                    val newIncome = IncomeEntity(
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
                Toast.makeText(context, "Будь ласка, заповніть усі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ ОНОВЛЕНО: Тепер ця функція може працювати і для редагування існуючої витрати.
     *
     * @param expenseToEdit Опціональний об'єкт ExpenseEntity. Якщо він не null,
     * діалог працює в режимі редагування і заповнює поля даними.
     * @param hiveId Ідентифікатор вулика. Необхідний лише для додавання нового запису.
     */
    fun showAddExpenseDialog(
        context: Context,
        viewModel: ProfitabilityViewModel,
        hiveId: Int,
        expenseToEdit: ExpenseEntity? = null
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
            saveButton.text = "Зберегти"
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
            val date = calendar.time
            val dateAsLong = date.time

            if (name.isNotEmpty() && amount > 0) {
                if (expenseToEdit == null) {
                    val newExpense = ExpenseEntity(
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
                Toast.makeText(context, "Будь ласка, заповніть усі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showHiveOptionsDialog(
        context: Context,
        hive: HiveEntity,
        onEditNumber: () -> Unit,
        onSelectPrimaryColor: () -> Unit,
        onSelectSecondaryColor: () -> Unit,
        onDeleteHive: () -> Unit
    ) {
        val dialogView = View.inflate(context, R.layout.dialog_hive_options, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val editNumberCard: MaterialCardView = dialogView.findViewById(R.id.editNumberCard)
        editNumberCard.setOnClickListener {
            onEditNumber()
            dialog.dismiss()
        }

        val selectPrimaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectPrimaryColorCard)
        selectPrimaryColorCard.setOnClickListener {
            onSelectPrimaryColor()
            dialog.dismiss()
        }

        val selectSecondaryColorCard: MaterialCardView = dialogView.findViewById(R.id.selectSecondaryColorCard)
        selectSecondaryColorCard.setOnClickListener {
            onSelectSecondaryColor()
            dialog.dismiss()
        }

        val deleteHiveCard: MaterialCardView = dialogView.findViewById(R.id.deleteHiveCard)
        deleteHiveCard.setOnClickListener {
            onDeleteHive()
            dialog.dismiss()
        }

        dialog.show()
    }


    fun showEditHiveNumberDialog(context: Context, currentNumber: String, onSave: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_hive_number, null)
        val newNumberEditText: EditText = dialogView.findViewById(R.id.newNumberEditText)
        newNumberEditText.setText(currentNumber)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_hive_number_title))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.save)) { _, _ ->
                val newNumber = newNumberEditText.text.toString().trim()
                if (newNumber.isNotEmpty() && newNumber != currentNumber) {
                    onSave(newNumber)
                } else {
                    Toast.makeText(context, "Номер вулика не може бути порожнім або незмінним", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    fun showColorPickerDialog(
        context: Context,
        onColorSelected: (Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_color_picker, null)
        val colorGrid: GridLayout = dialogView.findViewById(R.id.colorGrid)
        val colors = intArrayOf(
            context.getColor(R.color.color_yellow),
            context.getColor(R.color.color_blue),
            context.getColor(R.color.color_white),
            context.getColor(R.color.color_orange),
            context.getColor(R.color.color_purple),
            context.getColor(R.color.color_green),
            context.getColor(R.color.color_red),
            context.getColor(R.color.color_transparent)
        )
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        colors.forEach { color ->
            val colorView = LayoutInflater.from(context).inflate(R.layout.color_grid_item, colorGrid, false)
            val colorCircle: View = colorView.findViewById(R.id.colorView)
            colorCircle.setBackgroundColor(color)

            colorCircle.setOnClickListener {
                onColorSelected(color)
                dialog.dismiss()
            }
            colorGrid.addView(colorView)
        }

        dialog.show()
    }
    fun showDeleteHiveDialog(context: Context, hive: HiveEntity, onDeleteConfirmed: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.delete_hive_title))
            .setMessage(context.getString(R.string.delete_hive_message, hive.name))
            .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                onDeleteConfirmed()
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    fun showAddHiveDialog(
        context: Context,
        onHiveAdded: (hiveNumber: String) -> Unit
    ) {
        val dialogView = View.inflate(context, R.layout.dialog_add_hive, null)
        val numberEditText: EditText = dialogView.findViewById(R.id.numberEditText)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_hive_title))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.save)) { _, _ ->
                val hiveNumber = numberEditText.text.toString().trim()


                if (hiveNumber.isNotBlank()) {
                    onHiveAdded(hiveNumber) // ✅ Передаємо лише номер
                } else {
                    Toast.makeText(context, context.getString(R.string.hive_number_required), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .show()
    }

    fun showSyncOptionsDialog(
        context: Context,
        onExport: () -> Unit,
        onImport: () -> Unit
    ) {
        val options = arrayOf(
            context.getString(R.string.create_backup),
            context.getString(R.string.restore_backup)
        )
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.choose_an_action))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onExport()
                    1 -> onImport()
                }
            }
            .show()
    }
}

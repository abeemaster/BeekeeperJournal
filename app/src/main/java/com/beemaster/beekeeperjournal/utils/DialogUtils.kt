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
import java.util.Locale

object DialogUtils {

    fun showHiveOptionsDialog(
        context: Context,
        hive: HiveEntity,
        onEditName: () -> Unit,
        onSelectPrimaryColor: () -> Unit,
        onSelectSecondaryColor: () -> Unit,
        onDeleteHive: () -> Unit
    ) {
        val dialogView = View.inflate(context, R.layout.dialog_hive_options, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val editNameCard: MaterialCardView = dialogView.findViewById(R.id.editNameCard)
        editNameCard.setOnClickListener {
            onEditName()
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

    fun showEditNameDialog(context: Context, hive: HiveEntity, onSave: (String) -> Unit) {
        val dialogView = View.inflate(context, R.layout.dialog_edit_name, null)
        val newNameEditText: EditText = dialogView.findViewById(R.id.newNameEditText)
        newNameEditText.setText(hive.name)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.edit_name_title))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.save)) { _, _ ->
                val newName = newNameEditText.text.toString().trim()
                if (newName.isNotEmpty() && newName != hive.name) {
                    onSave(newName)
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

        // Створюємо AlertDialog, але поки не показуємо його
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        colors.forEach { color ->
            val colorView = LayoutInflater.from(context).inflate(R.layout.color_grid_item, colorGrid, false)
            val colorCircle: View = colorView.findViewById(R.id.colorView)
            colorCircle.setBackgroundColor(color)

            colorCircle.setOnClickListener {
                onColorSelected(color)
                dialog.dismiss() // ✅ Змінено: тепер ми закриваємо діалог
            }
            colorGrid.addView(colorView)
        }

        // Показуємо діалог після того, як всі View були додані
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
        onHiveAdded: (String, String) -> Unit
    ) {
        val dialogView = View.inflate(context, R.layout.dialog_add_hive, null)
        val nameEditText: EditText = dialogView.findViewById(R.id.nameEditText)
        val numberEditText: EditText = dialogView.findViewById(R.id.numberEditText)

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.add_hive_title))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.save)) { _, _ ->
                val hiveName = nameEditText.text.toString()
                val hiveNumber = numberEditText.text.toString()
                if (hiveName.isNotBlank() && hiveNumber.isNotBlank()) {
                    onHiveAdded(hiveName, hiveNumber)
                } else {
                    Toast.makeText(context, "Ім'я та номер вулика не можуть бути порожніми", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(context.getString(R.string.cancel), null)
            .create()
            .show()
    }

    fun showAddIncomeDialog(context: Context, viewModel: ProfitabilityViewModel, hiveId: Int?) {
        val view = LayoutInflater.from(context).inflate(R.layout.income_dialog, null)
        val descriptionEditText: EditText = view.findViewById(R.id.income_description_edit_text)
        val amountEditText: EditText = view.findViewById(R.id.income_amount_edit_text)
        val unitEditText: EditText = view.findViewById(R.id.income_unit_edit_text)
        val pricePerUnitEditText: EditText = view.findViewById(R.id.income_price_per_unit_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.income_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_income_button)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        dateEditText.setText(dateFormat.format(calendar.time))

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
            // ✅ Використовуємо правильні назви полів з UI
            val productName = descriptionEditText.text.toString().trim()
            val quantity = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val price = pricePerUnitEditText.text.toString().toDoubleOrNull() ?: 0.0
            val date = calendar.time // ✅ Використовуємо об'єкт Date, а не Long

            if (productName.isNotEmpty() && quantity > 0 && price > 0) {
                // ✅ Створення IncomeEntity з правильними параметрами
                val newIncome = IncomeEntity(
                    productName = productName,
                    quantity = quantity,
                    price = price,
                    totalAmount = quantity * price, // ✅ Обчислюємо totalAmount
                    date = date,
                    // hiveId = hiveId // Якщо це поле існує
                )
                viewModel.addIncome(newIncome)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Будь ласка, заповніть усі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun showAddExpenseDialog(context: Context, viewModel: ProfitabilityViewModel) {
        val view = LayoutInflater.from(context).inflate(R.layout.expense_dialog, null)
        // ✅ Використовуємо правильні назви змінних
        val nameEditText: EditText = view.findViewById(R.id.expense_name_edit_text)
        val quantityEditText: EditText = view.findViewById(R.id.expense_quantity)
        val amountEditText: EditText = view.findViewById(R.id.expense_amount_edit_text)
        val dateEditText: EditText = view.findViewById(R.id.expense_date_edit_text)
        val saveButton: Button = view.findViewById(R.id.save_expense_button)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        dateEditText.setText(dateFormat.format(calendar.time))

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
            val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
            val date = calendar.time // ✅ Використовуємо об'єкт Date

            if (name.isNotEmpty() && amount > 0) {
                // ✅ Створюємо ExpenseEntity з правильними параметрами
                val newExpense = ExpenseEntity(
                    name = name,
                    amount = amount,
                    date = date,
                    quantityUnits = quantityUnits
                )
                viewModel.addExpense(newExpense)
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Будь ласка, заповніть усі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
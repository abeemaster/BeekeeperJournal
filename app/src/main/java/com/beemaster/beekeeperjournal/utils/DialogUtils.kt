package com.beemaster.beekeeperjournal.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.HiveEntity
import com.google.android.material.card.MaterialCardView

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
}
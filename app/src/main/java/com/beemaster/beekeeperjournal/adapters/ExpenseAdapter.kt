// ExpenseAdapter.kt
// Адаптер для RecyclerView, який відображає список витрат.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Locale

// ✅ ОНОВЛЕНО: Тепер адаптер успадковується від ListAdapter.
class ExpenseAdapter(
    private val onClick: (ExpenseEntity) -> Unit,
    // ✅ НОВЕ: Додано обробник подій для довгого натискання.
    private val onLongClick: (ExpenseEntity) -> Unit
) : ListAdapter<ExpenseEntity, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expense_item, parent, false)
        // ✅ ОНОВЛЕНО: Передаємо обидва обробники до ViewHolder.
        return ExpenseViewHolder(view, onClick, onLongClick)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense)
    }

    class ExpenseViewHolder(
        view: View,
        private val onClick: (ExpenseEntity) -> Unit,
        private val onLongClick: (ExpenseEntity) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val date: TextView = view.findViewById(R.id.expense_date)
        private val description: TextView = view.findViewById(R.id.expense_description)
        private val totalPrice: TextView = view.findViewById(R.id.expense_total_price)
        private val quantity: TextView = view.findViewById(R.id.expense_quantity)
        private val quantityUnits: TextView = view.findViewById(R.id.expense_quantity_units)
        private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        fun bind(expense: ExpenseEntity) {
            date.text = dateFormat.format(expense.date)
            description.text = expense.name
            totalPrice.text = String.format(Locale.getDefault(), "-%.2f грн", expense.amount)
            quantity.text = String.format(Locale.getDefault(), "%.2f", expense.quantityUnits)
            quantityUnits.text = expense.nameQuantity

            // ✅ НОВЕ: Обробка звичайного натискання.
            itemView.setOnClickListener {
                onClick(expense)
            }

            // ✅ НОВЕ: Обробка довгого натискання.
            itemView.setOnLongClickListener {
                onLongClick(expense)
                true // Повертаємо true, щоб вказати, що подію оброблено.
            }
        }
    }

    // ✅ НОВЕ: DiffUtil.ItemCallback для оптимізації оновлень списку.
    private class ExpenseDiffCallback : DiffUtil.ItemCallback<ExpenseEntity>() {
        override fun areItemsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpenseEntity, newItem: ExpenseEntity): Boolean {
            return oldItem == newItem
        }
    }
}


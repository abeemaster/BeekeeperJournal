// ExpenseAdapter.kt
// Адаптер для RecyclerView, який відображає список витрат.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseAdapter(private var expenses: List<ExpenseEntity>) :
    RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    class ExpenseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.expense_date)
        val description: TextView = view.findViewById(R.id.expense_description)
        val totalPrice: TextView = view.findViewById(R.id.expense_total_price)
        val quantity: TextView = view.findViewById(R.id.expense_quantity)
        val quantityUnits: TextView = view.findViewById(R.id.expense_quantity_units)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expense_item, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        holder.date.text = dateFormat.format(expense.date)
        holder.description.text = expense.name
        holder.totalPrice.text = String.format(Locale.getDefault(), "-%.2f грн", expense.amount)
        holder.quantity.text = String.format(Locale.getDefault(), "%.2f", expense.quantityUnits)
        holder.quantityUnits.text = expense.nameQuantity
    }

    override fun getItemCount(): Int = expenses.size

    fun updateData(newExpenses: List<ExpenseEntity>) {
        expenses = newExpenses
        notifyDataSetChanged()
    }
}

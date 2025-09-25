// IncomeAdapter.kt
// Адаптер для RecyclerView, який відображає список прибутків.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.databinding.IncomeItemBinding
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import java.text.SimpleDateFormat
import java.util.Locale

class IncomeAdapter(
    private val onClick: (IncomeEntity) -> Unit,
    // ✅ ОНОВЛЕНО: Додано обробник подій для довгого натискання.
    private val onLongClick: (IncomeEntity) -> Unit
) : ListAdapter<IncomeEntity, IncomeAdapter.IncomeViewHolder>(IncomeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = IncomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // ✅ ОНОВЛЕНО: Тепер передаємо обидві лямбди до ViewHolder.
        return IncomeViewHolder(binding, onLongClick)
    }

    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = getItem(position)
        holder.bind(income, onClick)
    }

    class IncomeViewHolder(
        private val binding: IncomeItemBinding,
        // ✅ ОНОВЛЕНО: ViewHolder також отримує лямбду для довгого натискання.
        private val onLongClick: (IncomeEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        fun bind(income: IncomeEntity, onClick: (IncomeEntity) -> Unit) {
            binding.tvDescription.text = income.productName
            binding.tvPricePerUnit.text = String.format(Locale.getDefault(), "+%.2f грн", income.totalAmount)
            binding.tvQuantity.text = String.format(Locale.getDefault(), "%.2f %s по %.2f грн/%s",
                income.quantity, income.unitName, income.price, income.unitName)
            binding.tvDate.text = dateFormat.format(income.date)

            // ✅ ОНОВЛЕНО: Додано обробник подій для звичайного натискання.
            itemView.setOnClickListener {
                onClick(income)
            }

            // ✅ НОВЕ: Додано обробник подій для довгого натискання.
            itemView.setOnLongClickListener {
                onLongClick(income)
                true
            }
        }
    }

    private class IncomeDiffCallback : DiffUtil.ItemCallback<IncomeEntity>() {
        override fun areItemsTheSame(oldItem: IncomeEntity, newItem: IncomeEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncomeEntity, newItem: IncomeEntity): Boolean {
            return oldItem == newItem
        }
    }
}
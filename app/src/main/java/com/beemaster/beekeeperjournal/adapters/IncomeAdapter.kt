// IncomeAdapter.kt
// Адаптер для RecyclerView, який відображає список прибутків.

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.databinding.IncomeItemBinding
// import com.beemaster.beekeeperjournal.db.entity.IncomeEntity // ❌ ВИДАЛЯЄМО: більше не використовуємо Entity напряму
import com.beemaster.beekeeperjournal.models.Income // ✅ ДОДАЄМО: Чиста Domain Model Income
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [Income] у RecyclerView.
 *
 * @property onClick Лямбда-функція, що викликається при натисканні на елемент.
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент.
 */
class IncomeAdapter(
    private val onClick: (Income) -> Unit,
    private val onLongClick: (Income) -> Unit
) : ListAdapter<Income, IncomeAdapter.IncomeViewHolder>(IncomeDiffCallback()) {
    /**
     * Створює новий ViewHolder, використовуючи View Binding.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = IncomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IncomeViewHolder(binding, onClick, onLongClick)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = getItem(position)
        holder.bind(income)
    }

    /**
     * Внутрішній клас, що представляє елемент списку прибутків (ViewHolder).
     */
    class IncomeViewHolder(
        private val binding: IncomeItemBinding,
        private val onClick: (Income) -> Unit,
        private val onLongClick: (Income) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        private var currentIncome: Income? = null

        init {
            // Слухачі кліків налаштовуються ОДИН РАЗ тут
            itemView.setOnClickListener {
                currentIncome?.let(onClick)
            }

            itemView.setOnLongClickListener {
                currentIncome?.let(onLongClick)
                true
            }
        }

        /**
         * Прив'язує об'єкт [Income] до елементів інтерфейсу.
         *
         * @param income Об'єкт прибутку, який потрібно відобразити.
         */
        fun bind(income: Income) {
            currentIncome = income

            binding.tvDescription.text = income.productName
            binding.tvPricePerUnit.text = itemView.context.getString(R.string.income_amount_format, income.totalAmount)

            binding.tvQuantity.text = itemView.context.getString(
                R.string.income_quantity_details_format,
                income.quantity, income.unitName, income.price, income.unitName
            )

            binding.tvDate.text = dateFormat.format(income.date)
        }
    }

    /**
     * Внутрішній клас для обчислення різниці між старим і новим списком елементів.
     */
    private class IncomeDiffCallback : DiffUtil.ItemCallback<Income>() {
        /**
         * Перевіряє, чи представляють два об'єкти один і той самий елемент (за ID).
         */
        override fun areItemsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Перевіряє, чи мають два елементи однакові дані.
         */
        override fun areContentsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem == newItem
        }
    }
}
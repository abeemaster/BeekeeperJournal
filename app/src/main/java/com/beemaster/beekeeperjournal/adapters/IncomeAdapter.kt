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
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [IncomeEntity] у RecyclerView.
 * Використовує View Binding та [ListAdapter] для ефективності.
 *
 * @property onClick Лямбда-функція, що викликається при натисканні на елемент.
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент.
 */
class IncomeAdapter(
    private val onClick: (IncomeEntity) -> Unit,
    private val onLongClick: (IncomeEntity) -> Unit
) : ListAdapter<IncomeEntity, IncomeAdapter.IncomeViewHolder>(IncomeDiffCallback()) {

    /**
     * Створює новий ViewHolder, використовуючи View Binding.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeViewHolder {
        val binding = IncomeItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Передаємо onClick та onLongClick до конструктора ViewHolder
        return IncomeViewHolder(binding, onClick, onLongClick)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: IncomeViewHolder, position: Int) {
        val income = getItem(position)
        // Тепер метод bind приймає лише дані.
        holder.bind(income)
    }

    /**
     * Внутрішній клас, що представляє елемент списку прибутків (ViewHolder).
     */
    class IncomeViewHolder(
        private val binding: IncomeItemBinding,
        private val onClick: (IncomeEntity) -> Unit, // ✅ ВИПРАВЛЕНО
        private val onLongClick: (IncomeEntity) -> Unit // ✅ ВИПРАВЛЕНО
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
        private var currentIncome: IncomeEntity? = null

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
         * Прив'язує об'єкт [IncomeEntity] до елементів інтерфейсу.
         *
         * @param income Об'єкт прибутку, який потрібно відобразити.
         */
        fun bind(income: IncomeEntity) {
            currentIncome = income // Зберігаємо для використання у кліках

            binding.tvDescription.text = income.productName
            binding.tvPricePerUnit.text = itemView.context.getString(R.string.income_amount_format, income.totalAmount) // ✅ ВИПРАВЛЕННЯ 2

            // ✅ ВИПРАВЛЕННЯ 2: Використання ресурсу для детальної інформації про кількість
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
    private class IncomeDiffCallback : DiffUtil.ItemCallback<IncomeEntity>() {
        /**
         * Перевіряє, чи представляють два об'єкти один і той самий елемент (за ID).
         */
        override fun areItemsTheSame(oldItem: IncomeEntity, newItem: IncomeEntity): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Перевіряє, чи мають два елементи однакові дані.
         */
        override fun areContentsTheSame(oldItem: IncomeEntity, newItem: IncomeEntity): Boolean {
            return oldItem == newItem
        }
    }
}
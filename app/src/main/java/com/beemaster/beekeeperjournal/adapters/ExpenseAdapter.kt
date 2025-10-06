// ExpenseAdapter.kt
// Адаптер для RecyclerView, який відображає список витрат.
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
import com.beemaster.beekeeperjournal.models.Expense // ✅ ВИПРАВЛЕНО: Використовуємо чисту модель Expense
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Адаптер для відображення списку об'єктів [Expense] у RecyclerView.
 * Використовує [ListAdapter] та [DiffUtil] для ефективного оновлення списку.
 *
 * @property onClick Лямбда-функція, що викликається при натисканні на елемент.
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні на елемент.
 */
class ExpenseAdapter(
    private val onClick: (Expense) -> Unit, // ✅ Змінено тип аргументу
    private val onLongClick: (Expense) -> Unit // ✅ Змінено тип аргументу
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) { // ✅ Змінено тип ListAdapter

    /**
     * Створює новий ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.expense_item, parent, false)
        return ExpenseViewHolder(view, onClick, onLongClick)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = getItem(position)
        holder.bind(expense)
    }

    /**
     * Внутрішній клас, що представляє елемент списку витрат (ViewHolder).
     * Відповідає за прив'язку даних Expense до елементів View.
     */
    class ExpenseViewHolder(
        view: View,
        private val onClick: (Expense) -> Unit, // ✅ Змінено тип аргументу
        private val onLongClick: (Expense) -> Unit // ✅ Змінено тип аргументу
    ) : RecyclerView.ViewHolder(view) {
        private val date: TextView = view.findViewById(R.id.expense_date)
        private val description: TextView = view.findViewById(R.id.expense_description)
        private val totalPrice: TextView = view.findViewById(R.id.expense_total_price)
        private val quantity: TextView = view.findViewById(R.id.expense_quantity)
        private val quantityUnits: TextView = view.findViewById(R.id.expense_quantity_units)
        private val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())

        /**
         * Прив'язує об'єкт [Expense] до елементів інтерфейсу.
         *
         * @param expense Об'єкт витрати, який потрібно відобразити.
         */
        fun bind(expense: Expense) { // ✅ Змінено тип аргументу
            // Усі посилання на поля моделі Expense залишилися коректними,
            // оскільки ви зберегли ідентичні назви полів (date, name, amount, quantityUnits).
            date.text = dateFormat.format(expense.date)
            description.text = expense.name

            // Використання ресурсів для форматування ціни та кількості
            totalPrice.text = itemView.context.getString(R.string.expense_format_with_currency, expense.amount)
            quantity.text = itemView.context.getString(R.string.quantity_format, expense.quantityUnits)

            quantityUnits.text = expense.nameQuantity

            itemView.setOnClickListener {
                onClick(expense)
            }

            // Обробка довгого натискання
            itemView.setOnLongClickListener {
                onLongClick(expense)
                true // Повертаємо true, щоб вказати, що подія оброблена
            }
        }
    }

    /**
     * Внутрішній клас для обчислення різниці між старим і новим списком елементів.
     * Забезпечує плавну анімацію та ефективне оновлення [ListAdapter].
     */
    private class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() { // ✅ Змінено тип DiffUtil
        /**
         * Перевіряє, чи представляють два об'єкти один і той самий елемент (за ID).
         */
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Перевіряє, чи мають два елементи однакові дані (після перевірки areItemsTheSame).
         */
        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}
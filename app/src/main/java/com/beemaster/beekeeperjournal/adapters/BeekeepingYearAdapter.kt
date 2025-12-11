// BeekeepingYearAdapter.kt (ВИПРАВЛЕНО)
package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear

/**
 * Адаптер для відображення списку пасічних років.
 * @param activeYearId ID поточного активного року, використовується для маркування елемента.
 * @param onSwitchClicked Колбек для зміни активного року.
 * @param onLongClick Колбек, що викликається при довгому натисканні на назву року.
 */
class BeekeepingYearAdapter(
    private var activeYearId: Long,
    private val onSwitchClicked: (BeekeepingYear) -> Unit,
    // ДОДАНО: НОВИЙ ПАРАМЕТР для довгого натискання
    private val onLongClick: (BeekeepingYear, View) -> Unit
) : ListAdapter<BeekeepingYear, BeekeepingYearAdapter.YearViewHolder>(YearDiffCallback()) {

    fun setActiveYear(yearId: Long) {
        if (this.activeYearId != yearId) {
            this.activeYearId = yearId
            // Оскільки ми змінюємо лише вигляд, а не самі дані, використовуємо notifyDataSetChanged
            // або більш ефективно - оновлюємо два елементи, що змінюють стан.
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_beekeeper_year, parent, false)
        return YearViewHolder(view)
    }

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    /**
     * ViewHolder для відображення одного елемента року.
     */
    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Отримуємо посилання на View-елементи
        private val nameTextView: TextView = itemView.findViewById(R.id.yearTextView)
        private val activeLabel: TextView = itemView.findViewById(R.id.activeLabel)
        private val switchButton: Button = itemView.findViewById(R.id.switchButton)
        private val itemLayout: LinearLayout = itemView.findViewById(R.id.yearItemLayout)

        fun bind(year: BeekeepingYear) {
            nameTextView.text = year.name // Відображаємо назву року

            val isActive = year.yearId == activeYearId

            // Відображення мітки "АКТИВНИЙ"
            activeLabel.visibility = if (isActive) View.VISIBLE else View.GONE
            // Відображення кнопки "Перемкнути" лише для неактивних років
            switchButton.visibility = if (isActive) View.GONE else View.VISIBLE

            if (!isActive) {
                switchButton.setOnClickListener {
                    onSwitchClicked(year)
                }
            } else {
                switchButton.setOnClickListener(null)
            }

            // ДОДАНО: Обробник довгого натискання
            nameTextView.setOnLongClickListener {
                onLongClick(year, nameTextView) // Передаємо рік та View-якір
                true // Поглинаємо подію
            }

            // Додайте тут логіку для кращого візуального відображення вибраного/активного стану
            itemLayout.alpha = if (isActive) 1.0f else 0.8f
        }
    }

    private class YearDiffCallback : DiffUtil.ItemCallback<BeekeepingYear>() {
        override fun areItemsTheSame(oldItem: BeekeepingYear, newItem: BeekeepingYear): Boolean {
            return oldItem.yearId == newItem.yearId
        }

        override fun areContentsTheSame(oldItem: BeekeepingYear, newItem: BeekeepingYear): Boolean {
            // Порівнюємо всі поля, які можуть впливати на відображення
            return oldItem.name == newItem.name && oldItem.startDate == newItem.startDate
            // Активний стан не порівнюємо тут, оскільки він керується через setActiveYear
        }
    }
}
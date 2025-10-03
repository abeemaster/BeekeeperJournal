// HiveAdapter

package com.beemaster.beekeeperjournal.adapters

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.HiveEntity


/**
 * Адаптер для відображення списку вуликів.
 * Делегує обробку кліків та довгих кліків зовнішньому коду (Activity/Fragment).
 *
 * @property onClick Лямбда-функція, що викликається при натисканні на елемент (для переходу).
 * @property onLongClick Лямбда-функція, що викликається при довгому натисканні (для опцій).
 */
class HiveAdapter(
    private val onClick: (HiveEntity) -> Unit,
    private val onLongClick: (HiveEntity) -> Unit
) : ListAdapter<HiveEntity, HiveAdapter.HiveViewHolder>(HiveDiffCallback) {

    /**
     * ViewHolder для відображення окремого вулика.
     */
    class HiveViewHolder(
        itemView: View,
        private val onClick: (HiveEntity) -> Unit,
        private val onLongClick: (HiveEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val hivePrimaryColorView: View = itemView.findViewById(R.id.item_hive_primary_color)
        private val hiveNameTextView: TextView = itemView.findViewById(R.id.item_hive_name)
        private val secondaryColorView: View = itemView.findViewById(R.id.secondaryColorView)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)


        private var currentHive: HiveEntity? = null

        init {
            itemView.setOnClickListener {
                currentHive?.let {
                    // Використовуємо колбек onClick, делегуючи логіку переходу Activity
                    onClick(it)
                }
            }
            itemView.setOnLongClickListener {
                currentHive?.let {
                    onLongClick(it)
                }
                true
            }

            optionsButton.visibility = View.GONE
        }

        /**
         * Прив'язує об'єкт [HiveEntity] до елементів інтерфейсу.
         */
        @SuppressLint("SetTextI18n")
        fun bind(hive: HiveEntity) {
            currentHive = hive

            hiveNameTextView.text = itemView.context.getString(R.string.hive_name_format, hive.hiveNumber)

            hivePrimaryColorView.setBackgroundColor(hive.color)

            if (hive.secondaryColor != 0) {
                // ВАЖЛИВО: .mutate() дозволяє модифікувати Drawable без впливу на інші елементи.
                val drawable = secondaryColorView.background.mutate() as GradientDrawable
                drawable.setColor(hive.secondaryColor)
                secondaryColorView.visibility = View.VISIBLE
            } else {
                secondaryColorView.visibility = View.INVISIBLE
            }

        }
    }

    /**
     * Створює новий ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view, onClick, onLongClick)
    }

    /**
     * Прив'язує дані до ViewHolder.
     */
    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val hive = getItem(position)
        holder.bind(hive)
    }

    /**
     * Об'єкт для обчислення різниці між старим і новим списком елементів.
     */
    object HiveDiffCallback : DiffUtil.ItemCallback<HiveEntity>() {
        override fun areItemsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            // Перевіряємо лише ті поля, які можуть змінити відображення елемента
            return oldItem.name == newItem.name &&
                    oldItem.color == newItem.color &&
                    oldItem.secondaryColor == newItem.secondaryColor
        }
    }
}
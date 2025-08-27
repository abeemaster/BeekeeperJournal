// HiveAdapter

package com.beemaster.beekeeperjournal.adapters

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
import com.beemaster.beekeeperjournal.db.HiveEntity


class HiveAdapter(
    private val onClick: (HiveEntity) -> Unit,
    private val onOptionsClick: (HiveEntity, View) -> Unit
) : ListAdapter<HiveEntity, HiveAdapter.HiveViewHolder>(HiveDiffCallback) {


    class HiveViewHolder(
        itemView: View,
        val onClick: (HiveEntity) -> Unit,
        val onOptionsClick: (HiveEntity, View) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // ✅ Змінено: Тепер посилаємося на ConstraintLayout за його ID
        private val hivePrimaryColorView: View = itemView.findViewById(R.id.item_hive_primary_color)
        private val hiveNameTextView: TextView = itemView.findViewById(R.id.item_hive_name)
        private val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)

        // ✅ Посилання на елемент для додаткового кольору
        private val secondaryColorView: View = itemView.findViewById(R.id.secondaryColorView)

        private var currentHive: HiveEntity? = null

        init {
            itemView.setOnClickListener {
                currentHive?.let {
                    onClick(it)
                }
            }
            optionsButton.setOnClickListener {
                currentHive?.let { hive ->
                    onOptionsClick(hive, it)
                }
            }
        }

        fun bind(hive: HiveEntity) {
            currentHive = hive
            hiveNameTextView.text = hive.name

            // ✅ Змінено: Встановлюємо основний колір лише для ConstraintLayout ("кнопки")
            hivePrimaryColorView.setBackgroundColor(hive.color)

            // ✅ Змінено: Логіка відображення додаткового кольору для кружечка
            if (hive.secondaryColor != 0) {
                val drawable = secondaryColorView.background.mutate() as GradientDrawable
                drawable.setColor(hive.secondaryColor)
                secondaryColorView.visibility = View.VISIBLE
            } else {
                secondaryColorView.visibility = View.INVISIBLE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view, onClick, onOptionsClick)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val hive = getItem(position)
        holder.bind(hive)
    }

    object HiveDiffCallback : DiffUtil.ItemCallback<HiveEntity>() {
        override fun areItemsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            // ✅ Змінено: Перевірка вмісту тепер включає кольори
            return oldItem.name == newItem.name &&
                    oldItem.color == newItem.color &&
                    oldItem.secondaryColor == newItem.secondaryColor
        }
    }
}
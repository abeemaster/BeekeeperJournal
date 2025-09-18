// HiveAdapter
// Оновлено

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
    private val onLongClick: (HiveEntity) -> Unit
) : ListAdapter<HiveEntity, HiveAdapter.HiveViewHolder>(HiveDiffCallback) {

    class HiveViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        private val hivePrimaryColorView: View = itemView.findViewById(R.id.item_hive_primary_color)
        private val hiveNameTextView: TextView = itemView.findViewById(R.id.item_hive_name)
        private val secondaryColorView: View = itemView.findViewById(R.id.secondaryColorView)

        fun bind(hive: HiveEntity, onClick: (HiveEntity) -> Unit, onLongClick: (HiveEntity) -> Unit) {
            hiveNameTextView.text = hive.name
            hivePrimaryColorView.setBackgroundColor(hive.color)

            if (hive.secondaryColor != 0) {
                val drawable = secondaryColorView.background.mutate() as GradientDrawable
                drawable.setColor(hive.secondaryColor)
                secondaryColorView.visibility = View.VISIBLE
            } else {
                secondaryColorView.visibility = View.INVISIBLE
            }

            val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)
            optionsButton.visibility = View.GONE

            // ✅ ЗМІНА: Клік-слухач додано в bind(), щоб гарантувати, що він працює з правильним об'єктом.
            itemView.setOnClickListener {
                onClick(hive)
            }
            itemView.setOnLongClickListener {
                onLongClick(hive)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val hive = getItem(position)
        holder.bind(hive, onClick, onLongClick)
    }

    object HiveDiffCallback : DiffUtil.ItemCallback<HiveEntity>() {
        override fun areItemsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            return oldItem.name == newItem.name &&
                    oldItem.color == newItem.color &&
                    oldItem.secondaryColor == newItem.secondaryColor
        }
    }
}

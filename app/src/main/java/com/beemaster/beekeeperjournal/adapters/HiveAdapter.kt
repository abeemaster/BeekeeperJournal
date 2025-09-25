// HiveAdapter
// Оновлено
// HiveAdapter

package com.beemaster.beekeeperjournal.adapters

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.Constants
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.activities.HiveInfoActivity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity


class HiveAdapter(
    private val onClick: (HiveEntity) -> Unit,
    private val onLongClick: (HiveEntity) -> Unit
) : ListAdapter<HiveEntity, HiveAdapter.HiveViewHolder>(HiveDiffCallback) {


    class HiveViewHolder(
        itemView: View,
        val onClick: (HiveEntity) -> Unit,
        val onLongClick: (HiveEntity) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val hivePrimaryColorView: View = itemView.findViewById(R.id.item_hive_primary_color)
        private val hiveNameTextView: TextView = itemView.findViewById(R.id.item_hive_name)
        private val secondaryColorView: View = itemView.findViewById(R.id.secondaryColorView)

        private var currentHive: HiveEntity? = null

        init {
            itemView.setOnClickListener {
                currentHive?.let {
                    // Виправлено: передаємо повне ім'я вулика, а не лише номер
                    val intent = Intent(itemView.context, HiveInfoActivity::class.java).apply {
                        putExtra(Constants.EXTRA_HIVE_ID, it.id)
                        putExtra(Constants.EXTRA_HIVE_NUMBER, it.hiveNumber.toString())
                        putExtra(Constants.EXTRA_HIVE_NAME, it.name) // Використовуємо name замість hiveNumber
                        putExtra(Constants.EXTRA_HIVE_COLOR, it.color)
                        putExtra(Constants.EXTRA_HIVE_SECONDARY_COLOR, it.secondaryColor)
                    }
                    itemView.context.startActivity(intent)
                }
            }
            itemView.setOnLongClickListener {
                currentHive?.let {
                    onLongClick(it)
                }
                true
            }
        }

        fun bind(hive: HiveEntity) {
            currentHive = hive
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
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view, onClick, onLongClick)
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
            return oldItem.name == newItem.name &&
                    oldItem.color == newItem.color &&
                    oldItem.secondaryColor == newItem.secondaryColor
        }
    }
}

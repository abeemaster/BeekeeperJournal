// HiveAdapter

package com.beemaster.beekeeperjournal.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.HiveEntity

class HiveAdapter(private val onClick: (HiveEntity) -> Unit) :
    ListAdapter<HiveEntity, HiveAdapter.HiveViewHolder>(HiveDiffCallback) {

    class HiveViewHolder(itemView: View, val onClick: (HiveEntity) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val hiveNameTextView: TextView = itemView.findViewById(R.id.item_hive_name)
        private var currentHive: HiveEntity? = null

        init {
            itemView.setOnClickListener {
                currentHive?.let {
                    onClick(it)
                }
            }
        }

        fun bind(hive: HiveEntity) {
            currentHive = hive
            hiveNameTextView.text = hive.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val hive = getItem(position)
        holder.bind(hive)
    }

    object HiveDiffCallback : DiffUtil.ItemCallback<HiveEntity>() {
        override fun areItemsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            // Перевіряємо за унікальним ідентифікатором, а не за номером
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HiveEntity, newItem: HiveEntity): Boolean {
            return oldItem == newItem
        }
    }
}
// HiveAdapter

package com.beemaster.beekeeperjournal

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HiveAdapter(
    private var hives: List<HiveData>,
    private val onItemClick: (Int) -> Unit,
    private val onItemLongClick: (Int) -> Unit
) : RecyclerView.Adapter<HiveAdapter.HiveViewHolder>() {

    class HiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hiveCardView: MaterialCardView = view.findViewById(R.id.hiveCardView)
        val hiveNameTextView: TextView = view.findViewById(R.id.hiveName)
        val optionsButton: ImageButton = view.findViewById(R.id.optionsButton)
        val secondaryColorView: View = view.findViewById(R.id.secondaryColorView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val hive = hives[position]

        holder.hiveNameTextView.text = hive.name
        holder.hiveCardView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, hive.color))
        holder.secondaryColorView.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.context, hive.secondaryColor))

        // Обробник короткого натискання на всю картку
        holder.hiveCardView.setOnClickListener {
            onItemClick(position)
        }

        // Обробник довгого натискання на всю картку
        holder.hiveCardView.setOnLongClickListener {
            onItemLongClick(position)
            true
        }

        // Обробник натискання на кнопку "Опції"
        holder.optionsButton.setOnClickListener {
            onItemLongClick(position)
        }
    }

    override fun getItemCount(): Int = hives.size

    // Метод для оновлення списку вуликів
    fun updateHives(newHives: List<HiveData>) {
        hives = newHives
        notifyDataSetChanged()
    }
}
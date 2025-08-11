// HiveAdapter

package com.beemaster.beekeeperjournal

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HiveAdapter(
    private val hiveList: MutableList<HiveData>,
    private val context: Context,
    private val onHiveOptionsClick: (Int) -> Unit
) : RecyclerView.Adapter<HiveAdapter.HiveViewHolder>() {

    class HiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val hiveName: TextView = itemView.findViewById(R.id.hiveName)
        val hiveCardView: MaterialCardView = itemView.findViewById(R.id.hiveCardView)
        val optionsButton: ImageButton = itemView.findViewById(R.id.optionsButton)
        val secondaryColorView: View = itemView.findViewById(R.id.secondaryColorView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HiveViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_hive, parent, false)
        return HiveViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HiveViewHolder, position: Int) {
        val currentHive = hiveList[position]
        holder.hiveName.text = currentHive.name

        val resolvedColor = if (currentHive.color != 0) {
            ContextCompat.getColor(context, currentHive.color)
        } else {
            ContextCompat.getColor(context, R.color.hive_button_color)
        }

        (ContextCompat.getDrawable(context, R.drawable.rounded_background) as? GradientDrawable)?.let {
            it.setColor(resolvedColor)
            holder.hiveCardView.background = it
        }

        // Встановлення додаткового кольору
        val secondaryResolvedColor = if (currentHive.secondaryColor != 0) {
            ContextCompat.getColor(context, currentHive.secondaryColor)
        } else {
            ContextCompat.getColor(context, R.color.hive_button_color)
        }

        val secondaryColorDrawable = holder.secondaryColorView.background
        if (secondaryColorDrawable is GradientDrawable) {
            secondaryColorDrawable.setColor(secondaryResolvedColor)

            // Встановлюємо товщину та колір обводки залежно від кольору заливки
            if (currentHive.secondaryColor == R.color.transparent_color) {
                secondaryColorDrawable.setStroke(0, Color.TRANSPARENT)
            } else {
                secondaryColorDrawable.setStroke(2, ContextCompat.getColor(context, R.color.black))
            }
        }

        // Логіка видимості
        if (currentHive.secondaryColor != R.color.transparent_color) {
            holder.secondaryColorView.visibility = View.VISIBLE
        } else {
            holder.secondaryColorView.visibility = View.INVISIBLE
        }

        holder.optionsButton.setOnClickListener {
            onHiveOptionsClick(position)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "hive")
                putExtra("EXTRA_HIVE_NUMBER", currentHive.number)
                putExtra(NewNoteActivity.EXTRA_HIVE_NAME, currentHive.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = hiveList.size

    fun updateData(newHives: List<HiveData>) {
        hiveList.clear()
        hiveList.addAll(newHives)
        notifyDataSetChanged()
    }
}
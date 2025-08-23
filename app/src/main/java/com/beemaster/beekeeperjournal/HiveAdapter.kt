// HiveAdapter

package com.beemaster.beekeeperjournal

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import db.HiveEntity

class HiveAdapter(
    private val hives: List<HiveEntity>,
    private val context: Context,
    private val onHiveClick: (Int) -> Unit
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
        val currentHive = hives[position]
        holder.hiveName.text = currentHive.name

        val resolvedColor = ContextCompat.getColor(context, R.color.hive_button_color)
        (ContextCompat.getDrawable(context, R.drawable.rounded_background) as? GradientDrawable)?.let {
            it.setColor(resolvedColor)
            holder.hiveCardView.background = it
        }

        val secondaryResolvedColor = ContextCompat.getColor(context, R.color.hive_button_color)
        val secondaryColorDrawable = holder.secondaryColorView.background
        if (secondaryColorDrawable is GradientDrawable) {
            secondaryColorDrawable.setColor(secondaryResolvedColor)
            secondaryColorDrawable.setStroke(2, ContextCompat.getColor(context, R.color.black))
        }

        if (currentHive.secondaryColor != 0) {
            holder.secondaryColorView.visibility = View.VISIBLE
        } else {
            holder.secondaryColorView.visibility = View.INVISIBLE
        }


        holder.optionsButton.setOnClickListener {
            onHiveClick(position) // ✅ ВИПРАВЛЕНО: ВИКОРИСТОВУЄМО onHiveClick
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, HiveInfoActivity::class.java).apply {
                putExtra("TYPE", "hive")
                putExtra("EXTRA_HIVE_NUMBER", currentHive.hiveNumber)
                putExtra(NewNoteActivity.EXTRA_HIVE_NAME, currentHive.name)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = hives.size

}
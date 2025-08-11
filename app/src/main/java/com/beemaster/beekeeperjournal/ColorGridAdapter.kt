package com.beemaster.beekeeperjournal

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat

class ColorGridAdapter(context: Context, private val colors: List<Int>) :
    ArrayAdapter<Int>(context, 0, colors) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.color_grid_item, parent, false)

        val colorView: View = view.findViewById(R.id.colorView)
        val colorResId = colors[position]

        // *** ЗМІНЕНО: Отримуємо drawable і встановлюємо колір безпосередньо в ньому ***
        val backgroundDrawable = colorView.background
        if (backgroundDrawable is GradientDrawable) {
            backgroundDrawable.setColor(ContextCompat.getColor(context, colorResId))
        }

        return view
    }
}
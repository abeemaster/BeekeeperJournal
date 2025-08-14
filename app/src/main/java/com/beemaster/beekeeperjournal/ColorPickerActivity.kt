package com.beemaster.beekeeperjournal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class ColorPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        val hiveNumber = intent.getIntExtra("hive_number", -1)
        val colorType = intent.getStringExtra("color_type")

        if (hiveNumber == -1 || colorType == null) {
            Toast.makeText(this, "Помилка: не вдалося отримати дані вулика.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val colors = listOf(
            R.color.hive_button_white,
            R.color.hive_color_2,
            R.color.hive_color_3,
            R.color.hive_color_4,
            R.color.hive_color_5,
            R.color.hive_color_6,
            R.color.hive_color_7,
            R.color.hive_color_8,
            R.color.hive_color_9,
            R.color.hive_color_10,
            R.color.transparent_color
        )

        // val colorNames = colors.map { getString(it) } не використовується
        val colorGrid: GridView = findViewById(R.id.colorGrid)

        // Використовуємо адаптер для GridView
        colorGrid.adapter = ColorGridAdapter(this, colors)



        colorGrid.setOnItemClickListener { _, _, position, _ ->
            val selectedColor = colors[position]
            val resultIntent = Intent().apply {
                putExtra("selected_color", selectedColor)
                putExtra("hive_number", hiveNumber)
                putExtra("color_type", colorType)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        val cancelButton: Button = findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
}

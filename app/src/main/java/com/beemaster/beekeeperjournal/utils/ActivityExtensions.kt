// ActivityExtensions.kt (Новий файл)

package com.beemaster.beekeeperjournal.utils

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityOptionsCompat

/**
 * Запускає нову Activity із заданою кастомною анімацією переходу.
 * Викликає finish() для поточної Activity, якщо це необхідно (наприклад, для навігації "Вгору").
 *
 * @param intent Intent для запуску нової Activity.
 * @param finishCurrentActivity Чи потрібно закрити поточну Activity.
 */
fun Activity.startActivityWithSlideAnimation(
    intent: Intent,
    finishCurrentActivity: Boolean = false
) {
    // Використовуємо ActivityOptionsCompat для створення кастомної анімації
    val options = ActivityOptionsCompat.makeCustomAnimation(
        this,
        com.beemaster.beekeeperjournal.R.anim.slide_in_right,
        com.beemaster.beekeeperjournal.R.anim.slide_out_left
    )
    startActivity(intent, options.toBundle())

    if (finishCurrentActivity) {
        finish()
    }
}
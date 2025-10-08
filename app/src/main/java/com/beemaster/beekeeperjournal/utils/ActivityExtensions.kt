// ActivityExtensions.kt

package com.beemaster.beekeeperjournal.utils

import android.app.Activity
import android.content.Intent
import androidx.core.app.ActivityOptionsCompat

/**
 * Запускає нову Activity із заданою кастомною анімацією переходу (Вперед).
 * ... (Ваш оригінальний код)
 */
fun Activity.startActivityWithSlideAnimation(
    intent: Intent,
    finishCurrentActivity: Boolean = false
) {
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

/**
 * ✅ ДОДАНО: Запускає нову Activity із ЗВОРОТНОЮ анімацією (для навігації "Вгору" або "Назад").
 * Це забезпечує коректний UX, коли новий екран виглядає так, ніби він "в'їжджає" зліва,
 * а попередній "виїжджає" вправо.
 */
fun Activity.startActivityWithReverseSlideAnimation(
    intent: Intent,
    finishCurrentActivity: Boolean = false
) {
    val options = ActivityOptionsCompat.makeCustomAnimation(
        this,
        com.beemaster.beekeeperjournal.R.anim.slide_in_left, // Анімація входу зліва
        com.beemaster.beekeeperjournal.R.anim.slide_out_right // Анімація виходу вправо
    )
    startActivity(intent, options.toBundle())

    if (finishCurrentActivity) {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(
            com.beemaster.beekeeperjournal.R.anim.slide_in_left,  // Анімація входу для попередньої Activity
            com.beemaster.beekeeperjournal.R.anim.slide_out_right // Анімація виходу для поточної Activity
        )
    }
}
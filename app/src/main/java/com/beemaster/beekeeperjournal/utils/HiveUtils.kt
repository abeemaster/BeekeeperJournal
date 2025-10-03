// HiveUtils.kt

package com.beemaster.beekeeperjournal.utils

import android.content.Context
import com.beemaster.beekeeperjournal.R
import com.beemaster.beekeeperjournal.db.entity.HiveEntity

/**
 * Створює новий об'єкт HiveEntity зі стандартними значеннями.
 * Винесено з Data Layer, оскільки залежить від Context (R.color).
 */
fun createDefaultHiveEntity(context: Context, number: String) = HiveEntity(
    hiveNumber = number,
    name = number,
    color = context.getColor(R.color.color_white),
    secondaryColor = 0
)
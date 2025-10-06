package com.beemaster.beekeeperjournal.models

import com.beemaster.beekeeperjournal.R

/**
 * Клас даних, що представляє інформацію про окремий вулик для відображення у UI.
 *
 * @param number Унікальний ідентифікатор/номер вулика.
 * @param color Основний колір кнопки вулика (ідентифікатор ресурсу кольору).
 * @param queenButtonColor Колір кнопки "Матка" (ідентифікатор ресурсу кольору).
 * @param notesButtonColor Колір кнопки "Примітки" (ідентифікатор ресурсу кольору).
 * @param secondaryColor Додатковий колірний маркер на кнопці вулика (ідентифікатор ресурсу кольору).
 */
data class HiveData(
    val number: Int,
    val color: Int = R.color.hive_button_color,
    val queenButtonColor: Int = R.color.nav_button_color,
    val notesButtonColor: Int = R.color.nav_button_color,
    val secondaryColor: Int = android.R.color.transparent
)

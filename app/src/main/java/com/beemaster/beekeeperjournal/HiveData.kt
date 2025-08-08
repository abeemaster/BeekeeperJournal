package com.beemaster.beekeeperjournal

/**
 * Клас даних, що представляє інформацію про окремий вулик.
 *
 * @param number Унікальний номер вулика.
 * @param name Назва вулика (наприклад, "Вулик №1").
 * @param color Основний колір кнопки вулика (ідентифікатор ресурсу кольору).
 * @param queenButtonColor Колір кнопки "Матка" (ідентифікатор ресурсу кольору).
 * @param notesButtonColor Колір кнопки "Примітки" (ідентифікатор ресурсу кольору).
 * @param secondaryColor Додатковий колірний маркер на кнопці вулика (ідентифікатор ресурсу кольору).
 */
data class HiveData(
    val number: Int,
    var name: String, // <-- Змінили на 'var'
    var color: Int = R.color.hive_button_color,
    var queenButtonColor: Int = R.color.nav_button_color,
    var notesButtonColor: Int = R.color.nav_button_color,
    var secondaryColor: Int = android.R.color.transparent
)


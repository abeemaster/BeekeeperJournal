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
    val name: String,
    var color: Int,
    var queenButtonColor: Int,
    var notesButtonColor: Int,
    var secondaryColor: Int
)


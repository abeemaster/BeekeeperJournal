// HiveComparators.kt вирішує класичну проблему природного сортування (natural sorting), яка є критичною для будь-якого списку,
// що містить номери, за якими йдуть літери (наприклад, "1A", "2", "10A").

package com.beemaster.beekeeperjournal.utils

import java.util.Comparator

/**
 * Об'єкт-компаратор, який реалізує логіку природного сортування для рядкових номерів вуликів.
 *
 * Природне сортування гарантує, що багатозначні числа сортуються правильно
 * (наприклад, "2A", "10A" замість "10A", "2A").
 */
object NaturalHiveNumberComparator : Comparator<String> {

    // Регулярний вираз для розділення рядка на послідовності чисел (\d+) та нечислових символів (\D+).
    private val pattern = "(\\d+)|(\\D+)".toRegex()

    /**
     * Порівнює два номери вулика для визначення порядку сортування.
     *
     * @param num1 Перший номер вулика.
     * @param num2 Другий номер вулика.
     * @return Від'ємне число, нуль або додатне число, якщо num1 менший, дорівнює або більший за num2.
     */
    override fun compare(num1: String, num2: String): Int {
        // Отримуємо список токенів (числа або літери) для кожного номера.
        val tokens1 = pattern.findAll(num1).map { it.value }.toList()
        val tokens2 = pattern.findAll(num2).map { it.value }.toList()

        for (i in 0 until tokens1.size.coerceAtMost(tokens2.size)) {
            val token1 = tokens1[i]
            val token2 = tokens2[i]

            val numA = token1.toIntOrNull()
            val numB = token2.toIntOrNull()

            val result = if (numA != null && numB != null) {
                // Якщо обидва токени — числа, порівнюємо їх чисельно.
                numA.compareTo(numB)
            } else {
                // Інакше (один або обидва — рядки), порівнюємо як рядки.
                // Примітка: для кращої підтримки української мови можна використовувати
                // compareTo(token2, ignoreCase = true).
                token1.compareTo(token2)
            }

            if (result != 0) return result
        }

        // Якщо всі спільні токени рівні, довший рядок вважається більшим.
        return tokens1.size.compareTo(tokens2.size)
    }
}
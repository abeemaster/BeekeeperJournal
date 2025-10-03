// HiveComparators.kt

package com.beemaster.beekeeperjournal.utils

import java.util.Comparator

/**
 * Компаратор для природного сортування рядкових номерів вуликів.
 * (Наприклад, сортує "10А" після "2А", а не після "1А").
 */
object NaturalHiveNumberComparator : Comparator<String> {
    override fun compare(num1: String, num2: String): Int {
        val pattern = "(\\d+)|(\\D+)".toRegex()
        val tokens1 = pattern.findAll(num1).map { it.value }.toList()
        val tokens2 = pattern.findAll(num2).map { it.value }.toList()

        for (i in 0 until tokens1.size.coerceAtMost(tokens2.size)) {
            val token1 = tokens1[i]
            val token2 = tokens2[i]

            val numA = token1.toIntOrNull()
            val numB = token2.toIntOrNull()

            if (numA != null && numB != null) {
                val result = numA.compareTo(numB)
                if (result != 0) return result
            } else {
                val result = token1.compareTo(token2)
                if (result != 0) return result
            }
        }
        return tokens1.size.compareTo(tokens2.size)
    }
}
// HiveEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про кожен вулик.
// Цей клас описуватиме кожен вулик.

package com.beemaster.beekeeperjournal.db
import java.util.Comparator

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hiveNumber: String,
    val name: String,
    val color: Int,
    val secondaryColor: Int
)

object NaturalHiveNumberComparator : Comparator<String> {
    override fun compare(num1: String, num2: String): Int {
        val pattern = "(\\d+)|(\\D+)".toRegex()
        val tokens1 = pattern.findAll(num1).map { it.value }.toList()
        val tokens2 = pattern.findAll(num2).map { it.value }.toList()

        for (i in 0 until Math.min(tokens1.size, tokens2.size)) {
            val token1 = tokens1[i]
            val token2 = tokens2[i]

            val numA = token1.toIntOrNull()
            val numB = token2.toIntOrNull()

            if (numA != null && numB != null) {
                // Обидва токени - числа, сортуємо числово
                val result = numA.compareTo(numB)
                if (result != 0) return result
            } else {
                // Хоча б один токен - не число, сортуємо за алфавітом
                val result = token1.compareTo(token2)
                if (result != 0) return result
            }
        }
        return tokens1.size.compareTo(tokens2.size)
    }
}

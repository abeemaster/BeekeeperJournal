// HiveEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про кожен вулик.

package db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hives")
data class HiveEntity(
    @PrimaryKey(autoGenerate = false)
    val hiveNumber: Int,
    val name: String,
    val color: Int = 0,
    val queenButtonColor: Int = 0,
    val notesButtonColor: Int = 0,
    val secondaryColor: Int = 0
)

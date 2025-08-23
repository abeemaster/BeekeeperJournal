// NoteEntity сутність, яку Room буде зберігати в базі даних. Зберігатиме дані про вулики.

package db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var dateCreated: Long = System.currentTimeMillis(),
    var text: String,
    val type: String,
    @ColumnInfo(name = "hive_number")
    var hiveNumber: Int,
    val timestamp: Long = System.currentTimeMillis(), // ✅ ДОДАНО: timestamp
    val date: String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date()) // ✅ ДОДАНО: date

)
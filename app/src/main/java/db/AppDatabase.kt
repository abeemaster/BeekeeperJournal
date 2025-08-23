// AppDatabase.kt у вашому пакеті Цей клас буде точкою входу для всієї бази даних.

package db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [NoteEntity::class, HiveEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun hiveDao(): HiveDao
}
// AppDatabase.kt у вашому пакеті Цей клас буде точкою входу для всієї бази даних.

package com.beemaster.beekeeperjournal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [HiveEntity::class, NoteEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiveDao(): HiveDao
    abstract fun noteDao(): NoteDao
}
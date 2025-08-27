// AppDatabase.kt у вашому пакеті Цей клас буде точкою входу для всієї бази даних.

package com.beemaster.beekeeperjournal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HiveEntity::class, NoteEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiveDao(): HiveDao
    abstract fun noteDao(): NoteDao
}


// ✅ Додано: Об'єкт міграції
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL-запит для зміни стовпця hiveNumber з INT на TEXT
        database.execSQL("ALTER TABLE hives RENAME COLUMN hiveNumber TO hiveNumber_temp;")
        database.execSQL("ALTER TABLE hives ADD COLUMN hiveNumber TEXT;")
        database.execSQL("UPDATE hives SET hiveNumber = hiveNumber_temp;")
        database.execSQL("ALTER TABLE hives DROP COLUMN hiveNumber_temp;")
    }
}
// AppDatabase.kt у вашому пакеті Цей клас буде точкою входу для всієї бази даних.

package com.beemaster.beekeeperjournal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HiveEntity::class, NoteEntity::class, ExpenseEntity::class, IncomeEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hiveDao(): HiveDao
    abstract fun noteDao(): NoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun incomeDao(): IncomeDao
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
// SQL-запит для зміни стовпця hiveNumber з INT на TEXT
        database.execSQL("ALTER TABLE hives RENAME COLUMN hiveNumber TO hiveNumber_temp;")
        database.execSQL("ALTER TABLE hives ADD COLUMN hiveNumber TEXT;")
        database.execSQL("UPDATE hives SET hiveNumber = hiveNumber_temp;")
        database.execSQL("ALTER TABLE hives DROP COLUMN hiveNumber_temp;")
    }
}

// ✅ Оновлено: Об'єкт міграції. Створюємо нові таблиці.
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `incomes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` REAL NOT NULL, `price` REAL NOT NULL, `totalAmount` REAL NOT NULL)"
        )
    }
}
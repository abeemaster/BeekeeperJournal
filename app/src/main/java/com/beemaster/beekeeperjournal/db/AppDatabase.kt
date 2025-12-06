// AppDatabase.kt у вашому пакеті Цей клас буде точкою входу для всієї бази даних.

package com.beemaster.beekeeperjournal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beemaster.beekeeperjournal.db.dao.BeekeepingYearDao
import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.db.dao.HiveDao
import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.db.entity.BeekeepingYear
import com.beemaster.beekeeperjournal.db.entity.ExpenseEntity
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.IncomeEntity
import com.beemaster.beekeeperjournal.db.entity.NoteEntity

/**
 * Головний клас бази даних Room для програми "Beekeeper Journal".
 * Визначає всі сутності, версію бази даних та надає доступ до Data Access Objects (DAO).
 */
@Database(
    entities = [HiveEntity::class, NoteEntity::class, ExpenseEntity::class, IncomeEntity::class, BeekeepingYear::class],
    version = 9,
    exportSchema = false // Встановлено в 'false', оскільки схеми міграції винесені окремо.
)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Надає доступ до DAO для керування вуликами ([HiveEntity]).
     */
    abstract fun hiveDao(): HiveDao

    /**
     * Надає доступ до DAO для керування нотатками ([NoteEntity]).
     */
    abstract fun noteDao(): NoteDao

    /**
     * Надає доступ до DAO для керування витратами ([ExpenseEntity]).
     */
    abstract fun expenseDao(): ExpenseDao

    /**
     * Надає доступ до DAO для керування прибутками ([IncomeEntity]).
     */
    abstract fun incomeDao(): IncomeDao

    /**
     * Надає доступ до DAO для керування пасічними роками.
     */
    abstract fun beekeepingYearDao(): BeekeepingYearDao // <--- ДОДАЙТЕ ЦЕЙ РЯДОК
}
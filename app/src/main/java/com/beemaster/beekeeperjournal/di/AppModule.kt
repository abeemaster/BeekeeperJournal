// Цей об'єктний клас буде відповідати за надання (провайдінг) залежностей, таких як база даних та DAO.

package com.beemaster.beekeeperjournal.di

import android.content.Context
import androidx.room.Room
import com.beemaster.beekeeperjournal.db.AppDatabase
import com.beemaster.beekeeperjournal.db.ExpenseDao
import com.beemaster.beekeeperjournal.db.HiveDao
import com.beemaster.beekeeperjournal.db.IncomeDao
import com.beemaster.beekeeperjournal.db.MIGRATION_1_2
import com.beemaster.beekeeperjournal.db.MIGRATION_2_3
import com.beemaster.beekeeperjournal.db.NoteDao
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "beekeeper_journal_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }
    @Provides
    fun provideHiveDao(database: AppDatabase): HiveDao {
        return database.hiveDao()
    }

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }
    // ✅ Додаємо провайдери для нових DAO
    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao {
        return database.incomeDao()
    }
    @Provides
    fun provideHiveRepository(noteDao: NoteDao, hiveDao: HiveDao): HiveRepository {
        return HiveRepository(noteDao, hiveDao)
    }

    @Provides
    fun provideNoteRepository(noteDao: NoteDao, hiveDao: HiveDao): NoteRepository {
        return NoteRepository(noteDao, hiveDao)
    }
    @Provides
    fun provideIncomeRepository(incomeDao: IncomeDao): IncomeRepository {
        return IncomeRepository(incomeDao)
    }

    @Provides
    fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository {
        return ExpenseRepository(expenseDao)
    }
}


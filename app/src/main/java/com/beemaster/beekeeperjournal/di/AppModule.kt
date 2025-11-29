package com.beemaster.beekeeperjournal.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.beemaster.beekeeperjournal.db.ALL_MIGRATIONS
import com.beemaster.beekeeperjournal.db.AppDatabase
import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.db.dao.HiveDao
import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.repository.ExpenseRepository
import com.beemaster.beekeeperjournal.repository.HiveRepository
import com.beemaster.beekeeperjournal.repository.IncomeRepository
import com.beemaster.beekeeperjournal.repository.NoteRepository
import com.beemaster.beekeeperjournal.voice.VoskModelManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для надання залежностей на рівні життєвого циклу програми (Singleton).
 * Надає екземпляри бази даних, DAO та Репозиторіїв.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val DATABASE_NAME = "beekeeper_journal_database"

    /**
     * Надає єдиний екземпляр бази даних (Singleton).
     * Конфігурує Room та застосовує усі міграції.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    // НОВА ФУНКЦІЯ: Надання WorkManager
    /**
     * Надає singleton екземпляр WorkManager.
     * WorkManager завжди має бути Singleton у контексті додатку.
     */
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    /**
     * Надає singleton екземпляр VoskModelManager.
     * Використовує ApplicationContext та WorkManager, надані Hilt.
     */
    @Provides
    @Singleton
    fun provideVoskModelManager(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): VoskModelManager {
        return VoskModelManager(context, workManager)
    }

    /**
     * Надає Hive Data Access Object (DAO).
     */
    @Provides
    fun provideHiveDao(database: AppDatabase): HiveDao {
        return database.hiveDao()
    }

    /**
     * Надає Note Data Access Object (DAO).
     */
    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    /**
     * Надає Expense Data Access Object (DAO).
     */
    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    /**
     * Надає Income Data Access Object (DAO).
     */
    @Provides
    fun provideIncomeDao(database: AppDatabase): IncomeDao {
        return database.incomeDao()
    }

    // --------------------------------------------------------------------------
    // Repositories
    // --------------------------------------------------------------------------

    /**
     * Надає екземпляр Hive Repository.
     */
    @Provides
    fun provideHiveRepository(hiveDao: HiveDao, noteDao: NoteDao): HiveRepository {
        return HiveRepository(hiveDao, noteDao)
    }

    /**
     * Надає екземпляр Note Repository.
     * Тепер приймає NoteDao ТА HiveRepository.
     */
    @Provides
    fun provideNoteRepository(
        noteDao: NoteDao,
        hiveRepository: HiveRepository
    ): NoteRepository {
        return NoteRepository(noteDao, hiveRepository)
    }

    /**
     * Надає екземпляр Income Repository.
     */
    @Provides
    fun provideIncomeRepository(incomeDao: IncomeDao): IncomeRepository {
        return IncomeRepository(incomeDao)
    }

    /**
     * Надає екземпляр Expense Repository.
     */
    @Provides
    fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository {
        return ExpenseRepository(expenseDao)
    }

}
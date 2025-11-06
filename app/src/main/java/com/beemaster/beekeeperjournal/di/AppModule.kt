package com.beemaster.beekeeperjournal.di

import android.content.Context
import androidx.room.Room
import com.beemaster.beekeeperjournal.db.AppDatabase
import com.beemaster.beekeeperjournal.db.dao.ExpenseDao
import com.beemaster.beekeeperjournal.db.dao.HiveDao
import com.beemaster.beekeeperjournal.db.dao.IncomeDao
import com.beemaster.beekeeperjournal.db.ALL_MIGRATIONS
import com.beemaster.beekeeperjournal.db.dao.NoteDao
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
import dagger.Binds
import dagger.hilt.android.components.ViewModelComponent
import com.beemaster.beekeeperjournal.viewmodel.BackupDataSource
import com.beemaster.beekeeperjournal.viewmodel.MainActivityViewModel

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

    // --------------------------------------------------------------------------
    // Hilt Bindings для інтерфейсів
    // --------------------------------------------------------------------------

    /**
     * Абстрактний Dagger Hilt модуль для зв'язування інтерфейсів з їхніми реалізаціями.
     * Інстальовано у ViewModelComponent, оскільки він зв'язує ViewModel.
     */
    @Module
    @InstallIn(ViewModelComponent::class) // ✅ Встановлюємо у ViewModelComponent
    abstract class ViewModelBindsModule {

        /**
         * Зв'язує інтерфейс BackupDataSource з його реалізацією MainActivityViewModel.
         * Це дозволяє інжектувати BackupDataSource у BackupManager,
         * не порушуючи правила Hilt щодо HiltViewModel.
         */
        @Binds
        abstract fun bindBackupDataSource(
            mainActivityViewModel: MainActivityViewModel
        ): BackupDataSource
    }
}
// NoteDatabase — це абстрактний клас, який є головною точкою входу до бази даних Room. Він:
//Пов'язує всі сутності (наприклад, NoteEntity та HiveEntity).
//Вказує Room, яку версію бази даних використовувати.
//Надає доступ до DAO (Data Access Objects), через які ви взаємодієте з даними.

package com.beemaster.beekeeperjournal.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.beemaster.beekeeperjournal.db.dao.HiveDao
import com.beemaster.beekeeperjournal.db.dao.NoteDao
import com.beemaster.beekeeperjournal.db.entity.HiveEntity
import com.beemaster.beekeeperjournal.db.entity.NoteEntity

@Database(entities = [NoteEntity::class, HiveEntity::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun hiveDao(): HiveDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
// db.NoteDao DAO — це інтерфейс, який містить методи для виконання запитів до бази даних, наприклад, insert, update, delete та query.

package com.beemaster.beekeeperjournal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)

    @Query("SELECT * FROM notes WHERE hiveId = :hiveId AND type = :type ORDER BY createdAt DESC")
    fun getNotesByHiveAndType(hiveId: Int, type: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    // ✅ ДОДАНО: Метод для отримання всіх нотаток
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    // ✅ Додаємо метод для отримання всіх нотаток
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSuspend(): List<NoteEntity>

    // ✅ Додаємо метод для заміни всіх нотаток
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    // ✅ Вставляємо всі нотатки. OnConflictStrategy.REPLACE замінить існуючі записи з тими ж первинними ключами
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    // ✅ Об'єднуємо операції очищення та вставки в одну транзакцію
    suspend fun clearAndInsertNotes(notes: List<NoteEntity>) {
        deleteAllNotes()
        insertAllNotes(notes)
    }
}
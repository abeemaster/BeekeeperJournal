package db// db.NoteDao DAO — це інтерфейс, який містить методи для виконання запитів до бази даних, наприклад, insert, update, delete та query.

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {

    // ✅ Повертає LiveData. НЕ suspend. Використовувати для UI.
    @Query("SELECT * FROM notes ORDER BY dateCreated DESC")
    fun getAllNotes(): LiveData<List<NoteEntity>>

    // ✅ Одноразова операція в корутині. suspend.
    @Query("SELECT * FROM notes")
    suspend fun getAllNotes_suspend(): List<NoteEntity>

    // ✅ Одноразова операція в корутині. suspend.
    @Query("SELECT * FROM notes WHERE hive_number = :hiveNumber")
    suspend fun getNotesByHiveNumber(hiveNumber: Int): List<NoteEntity>

    // ✅ Повертає LiveData. НЕ suspend.
    @Query("SELECT * FROM notes WHERE id = :id")
    fun findNoteById(id: String): LiveData<NoteEntity?>

    // ✅ Новий метод для синхронного (блокуючого) отримання
    @Query("SELECT * FROM notes WHERE id = :id")
    fun findNoteById_blocking(id: String): NoteEntity?


    // ✅ Одноразова операція. suspend.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    // ✅ Одноразова операція. suspend.
    @Update
    suspend fun update(note: NoteEntity)

    // ✅ Одноразова операція. suspend.
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)

    @Query("SELECT * FROM notes WHERE type = :entryType AND hive_number = :hiveNumber ORDER BY dateCreated DESC")
    fun getNotesByTypeAndHive(entryType: String, hiveNumber: Int): List<NoteEntity>

}
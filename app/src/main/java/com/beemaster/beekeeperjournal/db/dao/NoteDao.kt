package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity // ✅ НОВИЙ ІМПОРТ
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

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /**
     * ✅ ВИПРАВЛЕНО: Змінено тип повернення на Flow<List<NoteSearchResultEntity>>.
     * Room тепер знає, як відобразити результат SQL JOIN.
     */
    @Query("""
        SELECT 
            N.*, 
            H.hiveNumber AS currentHiveDisplayNumber
        FROM notes AS N
        LEFT JOIN hives AS H ON N.hiveId = H.id
        WHERE N.content LIKE '%' || :query || '%' 
        OR N.title LIKE '%' || :query || '%' 
        OR N.type LIKE '%' || :query || '%' 
        OR H.name LIKE '%' || :query || '%' 
        OR H.hiveNumber LIKE '%' || :query || '%'
        ORDER BY N.createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteSearchResultEntity>> // ⬅️ ВИПРАВЛЕНО

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    @Transaction
    suspend fun clearAndInsertNotes(notes: List<NoteEntity>) {
        deleteAllNotes()
        insertAllNotes(notes)
    }
}
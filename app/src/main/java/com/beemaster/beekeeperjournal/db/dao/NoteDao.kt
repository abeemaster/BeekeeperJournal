package com.beemaster.beekeeperjournal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.beemaster.beekeeperjournal.db.entity.NoteEntity
import com.beemaster.beekeeperjournal.db.entity.NoteSearchResultEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) для роботи з сутностями [NoteEntity] (нотатки).
 */
@Dao
interface NoteDao {
    /**
     * Вставляє нову нотатку або замінює існуючу в разі конфлікту ID.
     * @param note Об'єкт [NoteEntity] для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    /**
     * Оновлює інформацію про існуючу нотатку.
     * @param note Об'єкт [NoteEntity] для оновлення.
     */
    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * Видаляє нотатку за її унікальним ID.
     * @param id ID нотатки для видалення.
     */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)


    @Query("DELETE FROM notes WHERE hiveId = :hiveId")
    suspend fun deleteNotesByHiveId(hiveId: Int)

    /**
     * Отримує нотатки, пов'язані з певним вуликом та типом запису,
     * відсортовані за датою створення.
     * @param hiveId ID вулика.
     * @param type Тип запису ("hive", "queen", "general" тощо).
     * @return [Flow], що містить список [NoteEntity].
     */
    @Query("SELECT * FROM notes WHERE hiveId = :hiveId AND type = :type ORDER BY createdAt DESC")
    fun getNotesByHiveAndType(hiveId: Int, type: String): Flow<List<NoteEntity>>

    /**
     * Отримує нотатку за її унікальним ID.
     * @param id ID нотатки.
     * @return [NoteEntity] або null.
     */
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    /**
     * Отримує всі нотатки з бази даних.
     * @return [Flow], що містить список усіх [NoteEntity], відсортованих за датою створення.
     */
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    /**
     * Отримує всі нотатки як статичний список. Використовується для операцій бекапу/експорту.
     * @return Список усіх [NoteEntity].
     */
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSuspend(): List<NoteEntity>

    /**
     * Виконує пошук нотаток, поєднуючи дані з таблиці notes та hives.
     * Пошук здійснюється за вмістом, заголовком нотатки, типом та номером/назвою вулика.
     * @param query Рядок пошуку.
     * @return [Flow], що містить список результатів [NoteSearchResultEntity].
     */
    /**
     * Шукає нотатки за текстом та номером вулика.
     * Фільтрує за активним роком.
     */
    /**
     * Шукає нотатки за текстом та номером вулика.
     * Фільтрує за активним роком.
     */
    @Query("""
        SELECT 
            N.id, 
            N.hiveId, 
            N.type, 
            N.title, 
            N.content, 
            N.createdAt,
            N.imagePath, 
            N.yearId, -- !!! ДОДАНО !!!
            H.hiveNumber AS currentHiveDisplayNumber
        FROM notes AS N 
        INNER JOIN hives AS H ON N.hiveId = H.id 
        WHERE N.yearId = :activeYearId AND (
            N.title LIKE '%' || :query || '%'
            OR N.content LIKE '%' || :query || '%'
            OR H.hiveNumber LIKE '%' || :query || '%'
        )
        ORDER BY N.createdAt DESC
    """)
    fun searchNotes(query: String, activeYearId: Int): Flow<List<NoteSearchResultEntity>>


    /**
     * Видаляє всі записи з таблиці нотаток.
     */
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    /**
     * Вставляє список нотаток у базу даних.
     * @param notes Список [NoteEntity] для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotes(notes: List<NoteEntity>)

    /**
     * Виконує очищення таблиці нотаток та подальшу вставку нового списку
     * в рамках однієї атомарної транзакції (для імпорту/відновлення).
     * @param notes Список [NoteEntity] для імпорту.
     */
    @Transaction
    suspend fun clearAndInsertNotes(notes: List<NoteEntity>) {
        deleteAllNotes()
        insertAllNotes(notes)
    }

    /**
     * Отримує нотатки за ID вулика та типом запису.
     * @param hiveId ID вулика (0 для загальних).
     * @param noteType Тип нотатки (наприклад, "hive", "queen", "general").
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow зі списком [NoteSearchResultEntity].
     */
    @Query("""
        SELECT 
            N.id, 
            N.hiveId, 
            N.type, 
            N.title, 
            N.content, 
            N.createdAt,
            N.imagePath, 
            N.yearId, 
            H.hiveNumber AS currentHiveDisplayNumber
        FROM notes AS N 
        INNER JOIN hives AS H ON N.hiveId = H.id 
        WHERE N.id = :noteId
    """)
    suspend fun getNoteSearchResultById(noteId: Int): NoteSearchResultEntity?

    @Query("UPDATE notes SET content = :newContent WHERE id = :noteId")
    suspend fun updateNoteContent(noteId: Int, newContent: String)

    /**
     * Отримує всі нотатки для відображення на головному екрані, відсортовані за датою.
     * @param activeYearId ID поточного активного пасічного року.
     */
    /**
     * Отримує всі нотатки для відображення на головному екрані.
     * Фільтрує за активним роком.
     */
    @Query("""
        SELECT 
            N.id, 
            N.hiveId, 
            N.type, 
            N.title, 
            N.content, 
            N.createdAt,
            N.imagePath, 
            N.yearId, -- !!! ДОДАНО !!!
            H.hiveNumber AS currentHiveDisplayNumber
        FROM notes AS N 
        INNER JOIN hives AS H ON N.hiveId = H.id 
        WHERE N.yearId = :activeYearId
        ORDER BY N.createdAt DESC
    """)
    fun getAllNotes(activeYearId: Int): Flow<List<NoteSearchResultEntity>>

    /**
     * Отримує всі нотатки, пов'язані з конкретним вуликом.
     * @param hiveId ID вулика.
     * @param activeYearId ID поточного активного пасічного року.
     */
    @Query("SELECT * FROM notes WHERE hiveId = :hiveId AND yearId = :activeYearId ORDER BY createdAt DESC")
    fun getNotesByHiveId(hiveId: Int, activeYearId: Int): Flow<List<NoteEntity>> // ДОДАНО ПАРАМЕТР

    /**
     * Отримує нотатки за ID вулика та типом запису.
     * @param hiveId ID вулика (0 для загальних).
     * @param noteType Тип нотатки (наприклад, "hive", "queen", "general").
     * @param activeYearId ID поточного активного пасічного року.
     * @return Flow зі списком [NoteSearchResultEntity].
     */
    @Query("""
        SELECT 
            N.id, 
            N.hiveId, 
            N.type, 
            N.title, 
            N.content, 
            N.createdAt,
            N.imagePath, 
            N.yearId, 
            H.hiveNumber AS currentHiveDisplayNumber
        FROM notes AS N 
        INNER JOIN hives AS H ON N.hiveId = H.id 
        WHERE N.hiveId = :hiveId 
          AND N.type = :noteType
          AND N.yearId = :activeYearId
        ORDER BY N.createdAt DESC
    """)
    fun getNotesForHiveAndType(hiveId: Int, noteType: String, activeYearId: Int): Flow<List<NoteSearchResultEntity>>

    /**
     * Отримує всі нотатки без фільтрації за роком. Використовується виключно для експорту (бекапу).
     * @return Список усіх [NoteEntity].
     */
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForExport(): List<NoteEntity> // НОВИЙ МЕТОД


}
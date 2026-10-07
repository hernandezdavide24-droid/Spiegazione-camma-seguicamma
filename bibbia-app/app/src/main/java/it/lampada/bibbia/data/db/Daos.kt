package it.lampada.bibbia.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM highlights WHERE book = :book AND chapter = :chapter")
    fun highlightsIn(book: Int, chapter: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdAt DESC")
    fun allHighlights(): Flow<List<HighlightEntity>>

    @Upsert
    suspend fun upsertHighlights(items: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE book = :book AND chapter = :chapter AND verse IN (:verses)")
    suspend fun deleteHighlights(book: Int, chapter: Int, verses: List<Int>)

    @Query("SELECT * FROM bookmarks WHERE book = :book AND chapter = :chapter")
    fun bookmarksIn(book: Int, chapter: Int): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun allBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT COUNT(*) FROM bookmarks WHERE book = :book AND chapter = :chapter AND verse = :verse")
    fun isBookmarked(book: Int, chapter: Int, verse: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmarks(items: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE book = :book AND chapter = :chapter AND verse IN (:verses)")
    suspend fun deleteBookmarks(book: Int, chapter: Int, verses: List<Int>)

    @Query("SELECT * FROM notes WHERE book = :book AND chapter = :chapter")
    fun notesIn(book: Int, chapter: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun allNotes(): Flow<List<NoteEntity>>

    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)
}

@Dao
interface BlockerDao {
    @Query("SELECT * FROM blocked_apps ORDER BY label COLLATE NOCASE")
    fun apps(): Flow<List<BlockedAppEntity>>

    @Upsert
    suspend fun upsertApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("SELECT * FROM block_schedules ORDER BY startMinute")
    fun schedules(): Flow<List<BlockScheduleEntity>>

    @Upsert
    suspend fun upsertSchedule(schedule: BlockScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: BlockScheduleEntity)
}

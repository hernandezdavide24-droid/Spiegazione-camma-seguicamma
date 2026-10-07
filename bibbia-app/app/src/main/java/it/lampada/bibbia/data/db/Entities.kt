package it.lampada.bibbia.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import it.lampada.bibbia.data.bible.VerseRef

enum class HighlightColor(val argb: Long, val label: String) {
    GIALLO(0xFFFFE58A, "Giallo"),
    VERDE(0xFFB9E4B0, "Verde"),
    AZZURRO(0xFFAED8F2, "Azzurro"),
    ROSA(0xFFF6C1D3, "Rosa"),
    ARANCIO(0xFFFFCC99, "Arancio"),
    VIOLA(0xFFD7C4F0, "Viola"),
}

@Entity(tableName = "highlights", primaryKeys = ["book", "chapter", "verse"])
data class HighlightEntity(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val color: HighlightColor,
    val createdAt: Long,
)

@Entity(tableName = "bookmarks", primaryKeys = ["book", "chapter", "verse"])
data class BookmarkEntity(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val createdAt: Long,
)

@Entity(tableName = "notes", primaryKeys = ["book", "chapter", "verse"])
data class NoteEntity(
    val book: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val updatedAt: Long,
)

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    /** 0 = nessun limite di tempo. */
    val dailyLimitMinutes: Int,
    val useSchedules: Boolean,
    val enabled: Boolean,
)

@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMinute: Int,
    val endMinute: Int,
    /** Bit 0 = lunedì ... bit 6 = domenica. */
    val daysMask: Int,
    val enabled: Boolean,
)

// Fuori dalle entità, così Room non prova a salvarli come colonne.
val HighlightEntity.ref get() = VerseRef(book, chapter, verse)
val BookmarkEntity.ref get() = VerseRef(book, chapter, verse)
val NoteEntity.ref get() = VerseRef(book, chapter, verse)

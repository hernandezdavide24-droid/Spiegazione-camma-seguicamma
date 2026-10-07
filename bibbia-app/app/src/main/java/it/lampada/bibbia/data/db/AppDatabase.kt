package it.lampada.bibbia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun colorToString(c: HighlightColor): String = c.name

    @TypeConverter
    fun stringToColor(s: String): HighlightColor =
        HighlightColor.entries.firstOrNull { it.name == s } ?: HighlightColor.GIALLO
}

@Database(
    entities = [
        HighlightEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        BlockedAppEntity::class,
        BlockScheduleEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyDao(): StudyDao
    abstract fun blockerDao(): BlockerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "lampada.db").build()
    }
}

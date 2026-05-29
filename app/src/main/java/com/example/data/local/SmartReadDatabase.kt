package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        BookEntity::class,
        BookPageEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        ReadingReportEntity::class,
        EmbeddingEntity::class,
        KnowledgeNodeEntity::class,
        KnowledgeEdgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SmartReadDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun bookPageDao(): BookPageDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
    abstract fun reportDao(): ReportDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        @Volatile
        private var INSTANCE: SmartReadDatabase? = null

        fun getDatabase(context: Context): SmartReadDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartReadDatabase::class.java,
                    "smart_read_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

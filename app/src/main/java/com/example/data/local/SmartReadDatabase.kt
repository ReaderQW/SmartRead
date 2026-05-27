package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Book::class,
        Highlight::class,
        Note::class,
        ChatMessage::class,
        KnowledgeNode::class,
        KnowledgeEdge::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartReadDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

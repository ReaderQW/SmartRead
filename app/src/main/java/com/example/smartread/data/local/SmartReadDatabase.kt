package com.example.smartread.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.smartread.data.local.dao.BookDao
import com.example.smartread.data.local.dao.ChatDao
import com.example.smartread.data.local.dao.EmbeddingDao
import com.example.smartread.data.local.dao.HighlightDao
import com.example.smartread.data.local.dao.KnowledgeGraphDao
import com.example.smartread.data.local.dao.NoteDao
import com.example.smartread.data.local.dao.ReportDao
import com.example.smartread.data.local.entities.BookEntity
import com.example.smartread.data.local.entities.BookPageEntity
import com.example.smartread.data.local.entities.ChatMessageEntity
import com.example.smartread.data.local.entities.ChatSessionEntity
import com.example.smartread.data.local.entities.EmbeddingEntity
import com.example.smartread.data.local.entities.HighlightEntity
import com.example.smartread.data.local.entities.KnowledgeEdgeEntity
import com.example.smartread.data.local.entities.KnowledgeNodeEntity
import com.example.smartread.data.local.entities.NoteEntity
import com.example.smartread.data.local.entities.ReadingReportEntity
import com.example.smartread.domain.model.AiStatus
import com.example.smartread.domain.model.BookType
import com.example.smartread.domain.model.ChatRole
import com.example.smartread.domain.model.KnowledgeNodeType
import com.example.smartread.domain.model.KnowledgeSourceType

@Database(
    entities = [
        BookEntity::class,
        BookPageEntity::class,
        HighlightEntity::class,
        NoteEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        EmbeddingEntity::class,
        KnowledgeNodeEntity::class,
        KnowledgeEdgeEntity::class,
        ReadingReportEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SmartReadTypeConverters::class)
abstract class SmartReadDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun highlightDao(): HighlightDao
    abstract fun noteDao(): NoteDao
    abstract fun chatDao(): ChatDao
    abstract fun embeddingDao(): EmbeddingDao
    abstract fun knowledgeGraphDao(): KnowledgeGraphDao
    abstract fun reportDao(): ReportDao
}

class SmartReadTypeConverters {
    @TypeConverter fun bookTypeToString(value: BookType): String = value.name
    @TypeConverter fun stringToBookType(value: String): BookType = BookType.valueOf(value)
    @TypeConverter fun aiStatusToString(value: AiStatus): String = value.name
    @TypeConverter fun stringToAiStatus(value: String): AiStatus = AiStatus.valueOf(value)
    @TypeConverter fun chatRoleToString(value: ChatRole): String = value.name
    @TypeConverter fun stringToChatRole(value: String): ChatRole = ChatRole.valueOf(value)
    @TypeConverter fun sourceTypeToString(value: KnowledgeSourceType): String = value.name
    @TypeConverter fun stringToSourceType(value: String): KnowledgeSourceType = KnowledgeSourceType.valueOf(value)
    @TypeConverter fun nodeTypeToString(value: KnowledgeNodeType): String = value.name
    @TypeConverter fun stringToNodeType(value: String): KnowledgeNodeType = KnowledgeNodeType.valueOf(value)
}


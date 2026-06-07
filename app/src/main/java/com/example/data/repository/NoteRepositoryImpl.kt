package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.repository.KnowledgeRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val aigc: AigcRemoteDataSource,
    private val knowledgeRepository: KnowledgeRepository
) : NoteRepository {
    override val allNotes: Flow<List<Note>> = noteDao.observeAllNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getNotesForBook(bookId: Int): Flow<List<Note>> =
        noteDao.observeNotesForBook(bookId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveNoteWithAiInsight(bookId: Int, originalText: String, userNote: String): Note = withContext(Dispatchers.IO) {
        val aiSummary = aigc.noteInsight(originalText, userNote)
        val tagsResponse = aigc.noteTags(originalText, userNote)
        val cleanTags = if (tagsResponse.contains("Error") || tagsResponse.contains("Failure")) {
            "阅读笔记,反思"
        } else {
            tagsResponse
        }

        val note = Note(
            bookId = bookId,
            originalText = originalText,
            userNote = userNote,
            aiSummary = aiSummary,
            tags = cleanTags
        )
        val id = noteDao.insertNote(note.toEntity()).toInt()
        val saved = note.copy(id = id)

        val noteNodeId = "note_$id"
        knowledgeRepository.addNode(KnowledgeNode(noteNodeId, bookId, "感悟#$id", "Note", 1.1f))
        knowledgeRepository.addEdge(KnowledgeEdge("edge_note_$id", bookId, "book_$bookId", noteNodeId, "撰写"))
        
        cleanTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
            val tagNodeId = "tag_$tag"
            knowledgeRepository.addNode(KnowledgeNode(tagNodeId, bookId, tag, "Mindset", 1.3f))
            knowledgeRepository.addEdge(KnowledgeEdge("edge_tag_${id}_$tag", bookId, noteNodeId, tagNodeId, "属于"))
        }

        try {
            val existingNoteEntities = noteDao.getNotesForBook(bookId)
            val recentNoteStrings = existingNoteEntities
                .filter { it.id != saved.id }
                .take(10)
                .map { "${it.originalText} -> ${it.userNote}" }
            
            if (recentNoteStrings.isNotEmpty()) {
                val relationships = aigc.inferRelationships("$originalText -> $userNote", recentNoteStrings)
                relationships.forEachIndexed { index, rel: JSONObject ->
                    val targetLabel = rel.optString("target")
                    val type = rel.optString("type")
                    val edgeId = "ai_edge_${saved.id}_$index"
                    knowledgeRepository.addEdge(
                        KnowledgeEdge(edgeId, bookId, noteNodeId, "book_$bookId", "$type: $targetLabel")
                    )
                }
            }
        } catch (e: Exception) {
            // AI 推理失败不影响主流程
        }

        knowledgeRepository.indexText(bookId, "NOTE", id.toString(), "$originalText\n$userNote\n$aiSummary")
        saved
    }

    override suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note.toEntity())
    }
}

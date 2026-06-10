package com.example.data.repository

import com.example.data.local.FileDataSource
import com.example.data.remote.AigcRemoteDataSource
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.repository.KnowledgeRepository
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class NoteRepositoryImpl(
    private val fileDataSource: FileDataSource,
    private val aigc: AigcRemoteDataSource,
    private val knowledgeRepository: KnowledgeRepository
) : NoteRepository {
    override val allNotes: Flow<List<Note>> = fileDataSource.observeAllNotes

    override fun getNotesForBook(bookId: Int): Flow<List<Note>> =
        fileDataSource.observeNotesForBook(bookId)

    override suspend fun saveNoteWithAiInsight(bookId: Int, originalText: String, userNote: String): Note = withContext(Dispatchers.IO) {
        // 1. 并行获取 AI 洞察、标签和中文主旨命名（减少串行等待时间）
        val aiSummaryDeferred = async { aigc.noteInsight(originalText, userNote) }
        val tagsResponseDeferred = async { aigc.noteTags(originalText, userNote) }
        val chineseTitleDeferred = async { aigc.extractChineseTitle(originalText, userNote) }

        val aiSummary = aiSummaryDeferred.await()
        val tagsResponse = tagsResponseDeferred.await()
        val chineseTitle = chineseTitleDeferred.await()

        val cleanTags = if (tagsResponse.isBlank() || tagsResponse.contains("Error") || tagsResponse.contains("Failure")) {
            "深度思考, 观点归档"
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
        val id = fileDataSource.addNote(note).toInt()
        val saved = note.copy(id = id)

        // 2. 同步到知识图谱，使用 AI 自动提取的中文主旨作为节点标签（禁用"感悟 #ID"格式）
        val noteNodeId = "note_$id"
        val nodeLabel = chineseTitle.ifBlank { "思想切片" }

        knowledgeRepository.addNode(
            KnowledgeNode(
                id = noteNodeId,
                bookId = bookId,
                label = nodeLabel,
                category = "Note",
                size = 1.2f,
                content = originalText,
                notes = userNote,
                aiAnalysis = aiSummary
            )
        )
        knowledgeRepository.addEdge(KnowledgeEdge("edge_note_$id", bookId, "book_$bookId", noteNodeId, "观点归档", 1.0f))

        // 3. 异步推理与其他笔记的关联（非阻塞，不等待结果）
        // 使用 IO 协程池启动后台任务，不阻塞当前保存流程
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingNotes = fileDataSource.getNotesForBook(bookId)
                val recentNotePairs = existingNotes
                    .filter { it.id != saved.id }
                    .take(5)
                    .map { note ->
                        Pair("note_${note.id}", "${note.originalText} -> ${note.userNote}")
                    }

                if (recentNotePairs.isNotEmpty()) {
                    val recentNoteStrings = recentNotePairs.map { it.second }
                    val relationships = aigc.inferRelationships("$originalText -> $userNote", recentNoteStrings)
                    relationships.forEachIndexed { index, rel: JSONObject ->
                        val targetLabel = rel.optString("target")
                        val type = rel.optString("type")
                        val edgeId = "ai_edge_${saved.id}_$index"

                        val targetNodeId = recentNotePairs.firstOrNull { pair ->
                            pair.second.contains(targetLabel.take(20))
                        }?.first

                        if (targetNodeId != null) {
                            knowledgeRepository.addEdge(
                                KnowledgeEdge(edgeId, bookId, noteNodeId, targetNodeId, type, 1.2f)
                            )
                        } else {
                            knowledgeRepository.addEdge(
                                KnowledgeEdge(edgeId, bookId, noteNodeId, "book_$bookId", "$type: $targetLabel", 0.8f)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NoteRepository", "Relationship inference failed", e)
            }
        }

        knowledgeRepository.indexText(bookId, "NOTE", id.toString(), "$originalText\n$userNote\n$aiSummary")
        saved
    }

    override suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        val nodeId = "note_${note.id}"
        // 删除笔记时，同步删除图谱中的结点和关联边
        knowledgeRepository.deleteNode(nodeId)
        knowledgeRepository.deleteEdgesForNode(nodeId)
        fileDataSource.deleteNote(note)
    }
}

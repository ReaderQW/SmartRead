package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.NoteDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.mapper.toEntity
import com.example.smartread.data.vector.VectorStore
import com.example.smartread.domain.model.KnowledgeSource
import com.example.smartread.domain.model.KnowledgeSourceType
import com.example.smartread.domain.model.Note
import com.example.smartread.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val vectorStore: VectorStore
) : NoteRepository {
    override fun observeNotes(bookId: String): Flow<List<Note>> =
        noteDao.observeNotes(bookId).map { notes -> notes.map { it.toDomain() } }

    override suspend fun getNote(noteId: String): Note? = noteDao.getNote(noteId)?.toDomain()

    override suspend fun saveNote(note: Note) {
        noteDao.upsertNote(note.toEntity())
        runCatching {
            vectorStore.indexText(
                source = KnowledgeSource(KnowledgeSourceType.NOTE, note.id, note.bookId),
                text = listOf(note.quoteText, note.userContent).filter { it.isNotBlank() }.joinToString("\n")
            )
        }
    }

    override suspend fun deleteNote(noteId: String) {
        noteDao.deleteNote(noteId)
    }
}

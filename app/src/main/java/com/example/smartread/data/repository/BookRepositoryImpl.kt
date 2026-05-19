package com.example.smartread.data.repository

import com.example.smartread.data.local.dao.BookDao
import com.example.smartread.data.mapper.toDomain
import com.example.smartread.data.mapper.toEntity
import com.example.smartread.domain.model.Book
import com.example.smartread.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {
    override fun observeBooks(): Flow<List<Book>> = bookDao.observeBooks().map { books ->
        books.map { it.toDomain() }
    }

    override suspend fun getBook(bookId: String): Book? = bookDao.getBook(bookId)?.toDomain()

    override suspend fun upsertBook(book: Book) {
        bookDao.upsertBook(book.toEntity())
    }

    override suspend fun deleteBook(bookId: String) {
        bookDao.deleteBook(bookId)
    }
}

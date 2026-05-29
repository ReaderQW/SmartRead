package com.example.data.repository

import com.example.data.local.BookDao
import com.example.data.local.KnowledgeDao
import com.example.data.mapper.toDomain
import com.example.data.mapper.toEntity
import com.example.domain.model.Book
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookRepositoryImpl(
    private val bookDao: BookDao,
    private val knowledgeDao: KnowledgeDao
) : BookRepository {
    override val allBooks: Flow<List<Book>> = bookDao.observeBooks().map { books ->
        books.map { it.toDomain() }
    }

    override suspend fun getBookById(id: Int): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)?.toDomain()
    }

    override suspend fun updateBook(book: Book) = withContext(Dispatchers.IO) {
        bookDao.updateBook(book.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    override suspend fun seedInitialBooks() = withContext(Dispatchers.IO) {
        if (bookDao.getBookById(1) != null) return@withContext

        val books = listOf(
            Book(
                id = 1,
                title = "苏格拉底的申辩",
                author = "柏拉图 (Plato)",
                coverResName = "ic_apology",
                summaryText = "古希腊哲学的经典文本，记录苏格拉底在法庭上的自我辩护。",
                category = "古典哲学",
                progress = 0.4f,
                totalPages = 4
            ),
            Book(
                id = 2,
                title = "新工具",
                author = "弗兰西斯·培根 (Francis Bacon)",
                coverResName = "ic_novum",
                summaryText = "经验主义方法论的代表，强调通过归纳与实验重建知识。",
                category = "科学认识论",
                progress = 0.1f,
                totalPages = 3
            ),
            Book(
                id = 3,
                title = "思想录",
                author = "帕斯卡尔 (Blaise Pascal)",
                coverResName = "ic_pensees",
                summaryText = "关于人的脆弱、尊严、理性与信仰的深刻沉思。",
                category = "存在反思录",
                progress = 0.0f,
                totalPages = 2
            )
        )
        books.forEach { bookDao.insertBook(it.toEntity()) }

        val nodes = listOf(
            KnowledgeNode("book_1", 1, "苏格拉底的申辩", "Book", 1.8f),
            KnowledgeNode("book_2", 2, "新工具", "Book", 1.8f),
            KnowledgeNode("book_3", 3, "思想录", "Book", 1.8f),
            KnowledgeNode("landmark_virtue", null, "美德即知识", "Concept", 1.4f),
            KnowledgeNode("landmark_idols", null, "四大假象", "Concept", 1.4f),
            KnowledgeNode("landmark_reed", null, "会思想的芦苇", "Concept", 1.4f),
            KnowledgeNode("landmark_epistemology", null, "认识论", "Mindset", 1.6f)
        )
        knowledgeDao.insertNodes(nodes.map { it.toEntity() })

        val edges = listOf(
            KnowledgeEdge("e1", 1, "book_1", "landmark_virtue", "核心主张"),
            KnowledgeEdge("e2", 2, "book_2", "landmark_idols", "批判对象"),
            KnowledgeEdge("e3", 3, "book_3", "landmark_reed", "存在隐喻"),
            KnowledgeEdge("e4", null, "landmark_virtue", "landmark_epistemology", "导向"),
            KnowledgeEdge("e5", null, "landmark_idols", "landmark_epistemology", "属于"),
            KnowledgeEdge("e6", null, "landmark_reed", "landmark_epistemology", "追问")
        )
        knowledgeDao.insertEdges(edges.map { it.toEntity() })
    }
}

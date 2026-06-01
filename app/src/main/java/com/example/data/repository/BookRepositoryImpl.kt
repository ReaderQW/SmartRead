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
        // 书籍仅在首次运行时插入一次
        if (bookDao.getBookById(1) == null) {
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
                ),
                Book(
                    id = 4,
                    title = "纯粹理性批判",
                    author = "康德 (Immanuel Kant)",
                    coverResName = "ic_kant",
                    summaryText = "批判哲学的开山之作，为理性划界，为信仰留出空间。",
                    category = "德国古典哲学",
                    progress = 0.0f,
                    totalPages = 4
                ),
                Book(
                    id = 5,
                    title = "查拉图斯特拉如是说",
                    author = "尼采 (Friedrich Nietzsche)",
                    coverResName = "ic_nietzsche",
                    summaryText = "宣告上帝已死，呼唤超人的诞生与价值重估。",
                    category = "存在哲学",
                    progress = 0.0f,
                    totalPages = 3
                ),
                Book(
                    id = 6,
                    title = "道德经",
                    author = "老子 (Laozi)",
                    coverResName = "ic_tao",
                    summaryText = "道家思想之源，道法自然，无为而治。",
                    category = "东方哲学",
                    progress = 0.0f,
                    totalPages = 3
                )
            )
            books.forEach { bookDao.insertBook(it.toEntity()) }
        }

        // 知识图谱始终 upsert（REPLACE 策略），确保种子数据始终为最新版本
        // 首次运行或数据变更时都会覆盖到完整 21 节点 + 21 边
        val seedNodes = listOf(
            // ---- 书籍节点 ----
            KnowledgeNode("book_1", 1, "苏格拉底的申辩", "Book", 1.8f),
            KnowledgeNode("book_2", 2, "新工具", "Book", 1.8f),
            KnowledgeNode("book_3", 3, "思想录", "Book", 1.8f),
            KnowledgeNode("book_4", 4, "纯粹理性批判", "Book", 1.8f),
            KnowledgeNode("book_5", 5, "查拉图斯特拉如是说", "Book", 1.8f),
            KnowledgeNode("book_6", 6, "道德经", "Book", 1.8f),
            // ---- 苏格拉底概念簇 ----
            KnowledgeNode("landmark_virtue", null, "美德即知识", "Concept", 1.4f),
            KnowledgeNode("concept_delphi", null, "德尔斐神谕", "Concept", 1.2f),
            KnowledgeNode("concept_gadfly", null, "牛虻精神", "Concept", 1.2f),
            KnowledgeNode("concept_soul", null, "灵魂不朽", "Concept", 1.1f),
            KnowledgeNode("concept_ethics", null, "伦理观", "Concept", 1.1f),
            // ---- 培根概念簇 ----
            KnowledgeNode("landmark_idols", null, "四大假象", "Concept", 1.4f),
            KnowledgeNode("concept_tribe", null, "种族假象", "Concept", 1.1f),
            KnowledgeNode("concept_cave", null, "洞穴假象", "Concept", 1.1f),
            KnowledgeNode("concept_market", null, "市场假象", "Concept", 1.1f),
            KnowledgeNode("concept_theater", null, "剧场假象", "Concept", 1.1f),
            KnowledgeNode("concept_induction", null, "经验归纳法", "Concept", 1.2f),
            // ---- 帕斯卡尔概念簇 ----
            KnowledgeNode("landmark_reed", null, "会思想的芦苇", "Concept", 1.4f),
            KnowledgeNode("concept_infinite", null, "无限与虚无", "Concept", 1.2f),
            KnowledgeNode("concept_geometry", null, "几何精神", "Concept", 1.1f),
            KnowledgeNode("concept_intuition", null, "敏感精神", "Concept", 1.1f),
            KnowledgeNode("concept_faith", null, "信仰之跃", "Concept", 1.1f),
            // ---- 康德概念簇 ----
            KnowledgeNode("concept_kant_apriori", null, "先天综合判断", "Concept", 1.3f),
            KnowledgeNode("concept_kant_phenomenon", null, "现象与物自体", "Concept", 1.2f),
            KnowledgeNode("concept_kant_moral", null, "道德律令", "Concept", 1.1f),
            // ---- 尼采概念簇 ----
            KnowledgeNode("concept_nietzsche_will", null, "权力意志", "Concept", 1.3f),
            KnowledgeNode("concept_nietzsche_ubermensch", null, "超人哲学", "Concept", 1.2f),
            KnowledgeNode("concept_nietzsche_eternal", null, "永恒轮回", "Concept", 1.1f),
            // ---- 老子概念簇 ----
            KnowledgeNode("concept_laozi_dao", null, "道法自然", "Concept", 1.3f),
            KnowledgeNode("concept_laozi_wuwei", null, "无为而治", "Concept", 1.2f),
            KnowledgeNode("concept_laozi_water", null, "上善若水", "Concept", 1.1f),
            // ---- 元认知节点 ----
            KnowledgeNode("landmark_epistemology", null, "认识论", "Mindset", 1.6f),
            KnowledgeNode("meta_wisdom", null, "智慧之爱", "Mindset", 1.2f),
            KnowledgeNode("meta_reason", null, "理性批判", "Mindset", 1.4f),
            KnowledgeNode("meta_nihilism", null, "虚无与超越", "Mindset", 1.3f),
            KnowledgeNode("meta_harmony", null, "自然之道", "Mindset", 1.4f)
        )
        knowledgeDao.insertNodes(seedNodes.map { it.toEntity() })

        val edges = listOf(
            // 书籍 → 核心概念
            KnowledgeEdge("e1", 1, "book_1", "landmark_virtue", "核心主张"),
            KnowledgeEdge("e2", 2, "book_2", "landmark_idols", "批判对象"),
            KnowledgeEdge("e3", 3, "book_3", "landmark_reed", "存在隐喻"),
            KnowledgeEdge("e22", 4, "book_4", "concept_kant_apriori", "核心问题"),
            KnowledgeEdge("e23", 5, "book_5", "concept_nietzsche_will", "核心主张"),
            KnowledgeEdge("e24", 6, "book_6", "concept_laozi_dao", "核心主张"),
            // 苏格拉底概念内部
            KnowledgeEdge("e4", null, "landmark_virtue", "concept_delphi", "神谕起源"),
            KnowledgeEdge("e5", null, "landmark_virtue", "concept_gadfly", "方法隐喻"),
            KnowledgeEdge("e6", null, "landmark_virtue", "concept_ethics", "延伸"),
            KnowledgeEdge("e7", null, "landmark_virtue", "concept_soul", "归宿"),
            // 培根概念内部
            KnowledgeEdge("e8", null, "landmark_idols", "concept_tribe", "包含"),
            KnowledgeEdge("e9", null, "landmark_idols", "concept_cave", "包含"),
            KnowledgeEdge("e10", null, "landmark_idols", "concept_market", "包含"),
            KnowledgeEdge("e11", null, "landmark_idols", "concept_theater", "包含"),
            KnowledgeEdge("e12", null, "landmark_idols", "concept_induction", "解决方案"),
            // 帕斯卡尔概念内部
            KnowledgeEdge("e13", null, "landmark_reed", "concept_infinite", "对立面"),
            KnowledgeEdge("e14", null, "landmark_reed", "concept_faith", "超越"),
            KnowledgeEdge("e15", null, "concept_geometry", "landmark_reed", "理性路径"),
            KnowledgeEdge("e16", null, "concept_intuition", "landmark_reed", "感性路径"),
            // 康德概念内部
            KnowledgeEdge("e25", null, "concept_kant_apriori", "concept_kant_phenomenon", "推导"),
            KnowledgeEdge("e26", null, "concept_kant_phenomenon", "concept_kant_moral", "实践延伸"),
            // 尼采概念内部
            KnowledgeEdge("e27", null, "concept_nietzsche_will", "concept_nietzsche_ubermensch", "目标"),
            KnowledgeEdge("e28", null, "concept_nietzsche_will", "concept_nietzsche_eternal", "前提"),
            // 老子概念内部
            KnowledgeEdge("e29", null, "concept_laozi_dao", "concept_laozi_wuwei", "方法"),
            KnowledgeEdge("e30", null, "concept_laozi_dao", "concept_laozi_water", "比喻"),
            // 跨书连接 → 元认知
            KnowledgeEdge("e17", null, "landmark_virtue", "landmark_epistemology", "导向"),
            KnowledgeEdge("e18", null, "landmark_idols", "landmark_epistemology", "属于"),
            KnowledgeEdge("e19", null, "landmark_reed", "landmark_epistemology", "追问"),
            KnowledgeEdge("e20", null, "concept_faith", "meta_wisdom", "源头"),
            KnowledgeEdge("e21", null, "concept_induction", "landmark_epistemology", "方法论"),
            // 康德 → 元认知
            KnowledgeEdge("e31", null, "concept_kant_apriori", "meta_reason", "奠基"),
            KnowledgeEdge("e32", null, "concept_kant_moral", "meta_wisdom", "实践理性"),
            // 尼采 → 元认知
            KnowledgeEdge("e33", null, "concept_nietzsche_will", "meta_nihilism", "驱动力"),
            KnowledgeEdge("e34", null, "concept_nietzsche_ubermensch", "meta_nihilism", "出路"),
            // 老子 → 元认知
            KnowledgeEdge("e35", null, "concept_laozi_dao", "meta_harmony", "本源"),
            KnowledgeEdge("e36", null, "concept_laozi_wuwei", "meta_harmony", "实践"),
            // 元认知互联
            KnowledgeEdge("e37", null, "meta_reason", "landmark_epistemology", "分支"),
            KnowledgeEdge("e38", null, "meta_nihilism", "meta_wisdom", "追问"),
            KnowledgeEdge("e39", null, "meta_harmony", "landmark_epistemology", "东方回响")
        )
        knowledgeDao.insertEdges(edges.map { it.toEntity() })
    }
}

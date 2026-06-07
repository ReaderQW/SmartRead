package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SmartReadDatabase
import com.example.data.mapper.toEntity
import com.example.data.remote.AigcRemoteDataSource
import com.example.data.remote.VivoClient
import com.example.data.repository.BookRepositoryImpl
import com.example.data.repository.ChatRepositoryImpl
import com.example.data.repository.KnowledgeRepositoryImpl
import com.example.data.repository.NoteRepositoryImpl
import com.example.data.repository.ReaderRepositoryImpl
import com.example.data.repository.ReportRepositoryImpl
import com.example.data.repository.VivoImageRepositoryImpl
import com.example.data.vector.VectorStore
import com.example.domain.model.Book
import com.example.domain.model.ChatMessage
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.model.ReadingReport
import com.example.domain.usecase.GenerateReportUseCase
import com.example.domain.usecase.SaveHighlightUseCase
import com.example.domain.usecase.SaveNoteUseCase
import com.example.domain.usecase.SendSocraticMessageUseCase
import com.example.utils.BookDummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SmartReadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartReadDatabase.getDatabase(application)
    private val aigcRemoteDataSource = AigcRemoteDataSource()
    private val vectorStore = VectorStore(db.embeddingDao())
    private val knowledgeRepository = KnowledgeRepositoryImpl(db.knowledgeDao(), vectorStore)
    private val bookRepository = BookRepositoryImpl(db.bookDao(), db.knowledgeDao(), aigcRemoteDataSource)
    private val readerRepository = ReaderRepositoryImpl(db.highlightDao(), knowledgeRepository)
    private val noteRepository = NoteRepositoryImpl(db.noteDao(), aigcRemoteDataSource, knowledgeRepository)
    private val chatRepository = ChatRepositoryImpl(db.chatDao(), aigcRemoteDataSource, knowledgeRepository)
    private val reportRepository = ReportRepositoryImpl(
        db.reportDao(),
        db.noteDao(),
        db.highlightDao(),
        db.chatDao(),
        aigcRemoteDataSource
    )
    private val vivoImageRepository = VivoImageRepositoryImpl(VivoClient().api)

    private val saveHighlightUseCase = SaveHighlightUseCase(readerRepository)
    private val saveNoteUseCase = SaveNoteUseCase(noteRepository)
    private val sendSocraticMessageUseCase = SendSocraticMessageUseCase(chatRepository)
    private val generateReportUseCase = GenerateReportUseCase(reportRepository)
    private val lastProgressByBook = mutableMapOf<Int, Float>()

    private val _currentBookId = MutableStateFlow<Int?>(null)
    val currentBookId: StateFlow<Int?> = _currentBookId.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isFloatingAssistantOpen = MutableStateFlow(false)
    val isFloatingAssistantOpen: StateFlow<Boolean> = _isFloatingAssistantOpen.asStateFlow()

    private val _isOcrScanning = MutableStateFlow(false)
    val isOcrScanning: StateFlow<Boolean> = _isOcrScanning.asStateFlow()

    private val _scannedOcrText = MutableStateFlow<String?>(null)
    val scannedOcrText: StateFlow<String?> = _scannedOcrText.asStateFlow()

    private val _activeReport = MutableStateFlow<ReadingReport?>(null)
    val activeReport: StateFlow<ReadingReport?> = _activeReport.asStateFlow()

    private val _artImageState = MutableStateFlow<ArtImageState>(ArtImageState.Idle)
    val artImageState: StateFlow<ArtImageState> = _artImageState.asStateFlow()

    val allBooks: StateFlow<List<Book>> = bookRepository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeNodes: StateFlow<List<KnowledgeNode>> = knowledgeRepository.allNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeEdges: StateFlow<List<KnowledgeEdge>> = knowledgeRepository.allEdges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val highlightsForCurrentBook: StateFlow<List<Highlight>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) readerRepository.getHighlightsForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesForCurrentBook: StateFlow<List<Note>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) noteRepository.getNotesForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessagesForCurrentBook: StateFlow<List<ChatMessage>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) chatRepository.getChatMessagesForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            bookRepository.seedInitialBooks()
            seedDemoNotes()
        }
    }

    /**
     * 插入演示笔记数据，首次运行且笔记为空时使用。
     * 每条笔记同时写入知识图谱（节点 + 边 + 嵌入索引），
     * 与 [NoteRepositoryImpl.saveNoteWithAiInsight] 的行为保持一致。
     */
    private suspend fun seedDemoNotes() {
        val noteDao = db.noteDao()
        val existing = noteDao.observeAllNotes() // 检查是否已有数据
        // 为避免复杂 Flow 收集，直接 count — 若首条笔记存在则跳过
        if (noteDao.getNotesForBook(1).isNotEmpty()) return

        val now = System.currentTimeMillis()
        val demoNotes = listOf(
            Note(
                bookId = 1,
                pageIndex = 2,
                originalText = "未经省察的生活是不值得度过的",
                userNote = "每天睡前问自己三个问题：今天我做了什么？为什么这么做？还能怎么改进？苏格拉底这句话不该只是格言，应该成为日常的自我审视工具。",
                tags = "苏格拉底,自我省察,生活哲学",
                aiSummary = "这句话是苏格拉底哲学的核心宣言，强调反思性自觉对人生意义的决定性作用。在当代语境下，它呼唤人们在信息洪流中保持独立思考与价值审视的能力。",
                timestamp = now - 3600000L
            ),
            Note(
                bookId = 1,
                pageIndex = 3,
                originalText = "我宁可顺从神，也绝不听从你们",
                userNote = "这种为了真理不惜牺牲的勇气让我震撼。真正的知识分子精神不就是这种'虽千万人吾往矣'的决绝吗？反观今天，多少人为了合群而放弃独立思考。",
                tags = "苏格拉底,勇气,知识分子",
                aiSummary = "体现苏格拉底对内在良知（daimonion）的绝对忠诚，这种超越世俗权威的精神姿态成为后世知识分子的理想原型，但也引发了关于个人良知与社会契约之间张力的永恒讨论。",
                timestamp = now - 7200000L
            ),
            Note(
                bookId = 1,
                pageIndex = 4,
                originalText = "我去赴死，而你们将继续生活",
                userNote = "苏格拉底面对死亡的平静让人深思。不是因为他不惧怕死亡，而是因为他确信自己度过了有意义的一生。这种坦然来自于对自我价值的确认。",
                tags = "苏格拉底,死亡,意义",
                aiSummary = "临终的平静源于知行合一的生命实践。苏格拉底以自身死亡完成了哲学的最高表达——哲学不是教人如何活着，更是教人如何体面地面对终结。",
                timestamp = now - 10800000L
            ),
            Note(
                bookId = 2,
                pageIndex = 1,
                originalText = "如果基本概念被草率而主观地抽象出来，那么建立在之上的整个思想大厦都注定在虚幻的争辩中轰然倒塌",
                userNote = "培根这段话一针见血地指出了学术空谈的根本问题。现在很多理论争辩往往就是因为基本概念没有厘清。需要回到经验事实，重新检验我们的前提。",
                tags = "培根,方法论,经验主义",
                aiSummary = "培根批判经院哲学脱离实际的空洞思辨，主张建立在对自然观察基础上的归纳法。这一洞见奠定了现代科学方法论的基石，提醒所有理论建构必须扎根于可验证的经验事实。",
                timestamp = now - 14400000L
            ),
            Note(
                bookId = 2,
                pageIndex = 2,
                originalText = "我们习惯将自己的尺度视作宇宙的绝对尺度",
                userNote = "族类偶像的提法让我想到人类中心主义。我们总是倾向于以自我为中心去理解世界，而忘记了我们的认知本身就有局限。做研究时要时刻警惕这种偏见。",
                tags = "培根,四大假象,认知偏见",
                aiSummary = "'族类偶像'揭示人类认知中根深蒂固的拟人化倾向——我们总以自身为尺度丈量宇宙。这一概念超前地预示了现代认知科学中的'归因偏差'与'确认偏误'研究。",
                timestamp = now - 18000000L
            ),
            Note(
                bookId = 3,
                pageIndex = 1,
                originalText = "人是一支会思想的芦苇",
                userNote = "帕斯卡尔用最诗意的比喻道出了人的本质——脆弱却又高贵。我们的身体如此脆弱，但思想却能理解宇宙。这种张力构成了人类最独特的处境。",
                tags = "帕斯卡尔,人的尊严,存在主义",
                aiSummary = "这一隐喻凝聚了帕斯卡尔对人类处境的深刻洞察：宇宙可以轻易摧毁人的肉身，却无法消灭人的思想。思想的尊严使人超越物理的脆弱，成为宇宙中唯一能意识到自身渺小的存在。",
                timestamp = now - 21600000L
            ),
            Note(
                bookId = 3,
                pageIndex = 2,
                originalText = "几何精神依赖严谨明晰的逻辑……敏感精神的规则却隐藏在凡尘生活的每一处细节中",
                userNote = "这两种精神的划分让我想到自己在学习和工作中的两种状态：有时需要严密的逻辑推理（写代码时），有时需要直觉 and 敏感（审美好坏时）。真正的智慧是知道什么时候用哪种能力。",
                tags = "帕斯卡尔,理性与直觉,认知方式",
                aiSummary = "帕斯卡尔对'几何精神'与'敏感精神'的区分，预示了两千年后心理学中的'双系统理论'（System 1 / System 2）。真正的智慧在于两种认知模式的灵活切换与互补。",
                timestamp = now - 25200000L
            ),
            Note(
                bookId = 2,
                pageIndex = 3,
                originalText = "市场偶像……来源于人与人相互交往时的粗糙命名与词义混乱",
                userNote = "在社交媒体时代，市场偶像的效应被无限放大了。很多争论其实是定义之争——大家用同一个词表达不同的意思。培根在四百年前就看透了这一点。",
                tags = "培根,语言,沟通",
                aiSummary = "'市场偶像'直指语言本身对思想的束缚——词汇的含混性使哲学讨论沦为字面之争。在互联网时代，这一现象愈演愈烈：标签化、情绪化的语言取代了精准的表达与交流。",
                timestamp = now - 28800000L
            )
        )

        // 已存在 aiSummary，不走 AI 接口直接写入
        demoNotes.forEach { note ->
            val id = noteDao.insertNote(note.toEntity()).toInt()
            val savedId = if (id == 0) note.id else id

            // 写入知识图谱节点与边（与 saveNoteWithAiInsight 一致）
            val noteNodeId = "note_seed_$savedId"
            knowledgeRepository.addNode(KnowledgeNode(noteNodeId, note.bookId, "感悟#${note.bookId}-$savedId", "Note", 1.1f))
            knowledgeRepository.addEdge(KnowledgeEdge("edge_seed_note_$savedId", note.bookId, "book_${note.bookId}", noteNodeId, "撰写"))

            note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                val tagNodeId = "seed_tag_$tag"
                knowledgeRepository.addNode(KnowledgeNode(tagNodeId, note.bookId, tag, "Mindset", 1.3f))
                knowledgeRepository.addEdge(KnowledgeEdge("edge_seed_tag_${savedId}_$tag", note.bookId, noteNodeId, tagNodeId, "属于"))
            }

            knowledgeRepository.indexText(note.bookId, "NOTE", savedId.toString(), "${note.originalText}\n${note.userNote}")
        }
    }

    fun selectBook(bookId: Int?) {
        _currentBookId.value = bookId
        _currentPageIndex.value = 0
        _selectedText.value = ""
        _isFloatingAssistantOpen.value = false
        _scannedOcrText.value = null
        _activeReport.value = null
        _artImageState.value = ArtImageState.Idle
    }

    fun setPageIndex(page: Int) {
        _currentPageIndex.value = page
        _selectedText.value = ""
    }

    fun nextPage(maxPages: Int) {
        if (_currentPageIndex.value < maxPages - 1) {
            _currentPageIndex.value += 1
            _selectedText.value = ""
        }
    }

    fun prevPage() {
        if (_currentPageIndex.value > 0) {
            _currentPageIndex.value -= 1
            _selectedText.value = ""
        }
    }

    fun updateBookProgress(progress: Float) {
        val bookId = _currentBookId.value ?: return
        val normalizedProgress = progress.coerceIn(0f, 1f)
        val lastProgress = lastProgressByBook[bookId]
        if (lastProgress != null && kotlin.math.abs(lastProgress - normalizedProgress) < 0.001f) {
            return
        }
        lastProgressByBook[bookId] = normalizedProgress

        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            if (book != null && kotlin.math.abs(book.progress - normalizedProgress) >= 0.001f) {
                bookRepository.updateBook(book.copy(progress = normalizedProgress))
            }
        }
    }

    fun selectTextSelection(text: String) {
        _selectedText.value = text
    }

    fun clearTextSelection() {
        _selectedText.value = ""
    }

    fun saveHighlight(colorHex: String) {
        val bookId = _currentBookId.value ?: return
        val text = _selectedText.value
        if (text.isEmpty()) return

        viewModelScope.launch {
            saveHighlightUseCase(bookId, _currentPageIndex.value, text, colorHex)
            _selectedText.value = ""
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            readerRepository.deleteHighlight(highlight)
        }
    }

    fun setFloatingAssistantOpen(open: Boolean) {
        _isFloatingAssistantOpen.value = open
    }

    fun sendSocraticMessage(userMsg: String) {
        val bookId = _currentBookId.value ?: return
        if (userMsg.trim().isEmpty()) return

        var contextText = _selectedText.value
        if (contextText.isEmpty()) {
            val bookExcerpts = BookDummyData.excerpts[bookId] ?: emptyList()
            val textPage = bookExcerpts.getOrNull(_currentPageIndex.value) ?: ""
            contextText = if (textPage.length > 100) textPage.take(100) + "..." else textPage
        }

        _isAiLoading.value = true
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            val title = book?.title ?: "经典"
            sendSocraticMessageUseCase(bookId, title, contextText, userMsg)
            _isAiLoading.value = false
        }
    }

    fun startSimulatedOcrScan(passageExcerpt: String) {
        _isOcrScanning.value = true
        _scannedOcrText.value = null
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800)
            _scannedOcrText.value = passageExcerpt
            _isOcrScanning.value = false
        }
    }

    fun clearOcrResult() {
        _scannedOcrText.value = null
    }

    fun saveNote(originalText: String, userInsight: String) {
        val bookId = _currentBookId.value ?: return
        if (originalText.isEmpty() || userInsight.isEmpty()) return

        _isAiLoading.value = true
        viewModelScope.launch {
            saveNoteUseCase(bookId, originalText, userInsight)
            _isAiLoading.value = false
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
        }
    }

    fun generateReport() {
        val bookId = _currentBookId.value ?: return
        _isAiLoading.value = true
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            val title = book?.title ?: "经典"
            _activeReport.value = generateReportUseCase(bookId, title)
            _isAiLoading.value = false
        }
    }

    fun closeReport() {
        _activeReport.value = null
    }

    fun generateArtImage() {
        val report = _activeReport.value ?: return
        val bookId = _currentBookId.value ?: return

        _artImageState.value = ArtImageState.Loading
        viewModelScope.launch {
            val prompt = aigcRemoteDataSource.generateArtPrompt(
                report.bookTitle,
                report.cognitiveIncrement,
                report.motto
            )
            val result = vivoImageRepository.generateArtImage(prompt, "水墨意境")
            result.onSuccess { url ->
                _artImageState.value = ArtImageState.Success(url)
            }.onFailure { e ->
                _artImageState.value = ArtImageState.Error(e.message ?: "生成失败")
            }
        }
    }
}

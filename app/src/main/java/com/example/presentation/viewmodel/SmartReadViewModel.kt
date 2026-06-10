package com.example.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FileDataSource
import com.example.data.local.SmartReadDatabase
import com.example.data.remote.AigcRemoteDataSource
import com.example.data.remote.VivoClient
import com.example.data.repository.VivoImageRepositoryImpl
import com.example.data.repository.BookRepositoryImpl
import com.example.data.repository.ChatRepositoryImpl
import com.example.data.repository.KnowledgeRepositoryImpl
import com.example.data.repository.NoteRepositoryImpl
import com.example.data.repository.ReaderRepositoryImpl
import com.example.data.repository.ReportRepositoryImpl
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
import com.example.ui.theme.ColorTheme
import com.example.ui.theme.FontOption
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.UiConfig
import com.example.utils.BookDummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SmartRead 主 ViewModel
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SmartReadViewModel(application: Application) : AndroidViewModel(application) {

    private val fileDataSource = FileDataSource(application)
    private val db = SmartReadDatabase.getDatabase(application)
    private val aigcRemoteDataSource = AigcRemoteDataSource()
    private val vectorStore = VectorStore(db.embeddingDao())
    private val knowledgeRepository = KnowledgeRepositoryImpl(fileDataSource, vectorStore)
    private val bookRepository = BookRepositoryImpl(fileDataSource, knowledgeRepository, aigcRemoteDataSource)
    private val readerRepository = ReaderRepositoryImpl(fileDataSource, knowledgeRepository)
    private val noteRepository = NoteRepositoryImpl(fileDataSource, aigcRemoteDataSource, knowledgeRepository)
    private val chatRepository = ChatRepositoryImpl(fileDataSource, aigcRemoteDataSource, knowledgeRepository)
    private val reportRepository = ReportRepositoryImpl(fileDataSource, aigcRemoteDataSource)

    private val saveHighlightUseCase = SaveHighlightUseCase(readerRepository)
    private val saveNoteUseCase = SaveNoteUseCase(noteRepository)
    private val sendSocraticMessageUseCase = SendSocraticMessageUseCase(chatRepository)
    private val generateReportUseCase = GenerateReportUseCase(reportRepository)
    private val lastProgressByBook = mutableMapOf<Int, Float>()

    private val _currentBookId = MutableStateFlow<Int?>(null)
    val currentBookId: StateFlow<Int?> = _currentBookId.asStateFlow()

    private val _selectedBookIdInNotes = MutableStateFlow<Int?>(null)
    val selectedBookIdInNotes: StateFlow<Int?> = _selectedBookIdInNotes.asStateFlow()

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

    private val vivoImageRepository = VivoImageRepositoryImpl(VivoClient().api, aigcRemoteDataSource)
    private val _artImageState = MutableStateFlow<ArtImageState>(ArtImageState.Idle)
    val artImageState: StateFlow<ArtImageState> = _artImageState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

    private val _uiConfig = MutableStateFlow(UiConfig())
    val uiConfig: StateFlow<UiConfig> = _uiConfig.asStateFlow()

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
            fileDataSource.init()
            bookRepository.seedInitialBooks()
            seedDemoNotes()
        }
    }

    private suspend fun seedDemoNotes() {
        // 检查是否已经初始化过，避免重复插入导致冲突或卡顿
        if (fileDataSource.observeAllNotes.stateIn(viewModelScope).value.isNotEmpty()) return

        val demoNotes = listOf(
            // ── 书籍 1: 苏格拉底的申辩 (bookId=1) ──
            Note(bookId = 1, pageIndex = 0, originalText = "我至少比他们聪明一点——因为我深知自己一无所知。", userNote = "‘承认无知’是智慧的最高形态。只有排空先入为主的偏见，真理才能涌入。", tags = "理智的谦逊", aiSummary = "否定之美"),
            Note(bookId = 1, pageIndex = 2, originalText = "未经省察的生活是不值得度过的。", userNote = "省察是对‘存在合法性’的追问。不思考的人只是在被动地通过时间。", tags = "生命省察", aiSummary = "存在自觉"),
            Note(bookId = 1, pageIndex = 3, originalText = "我像一只牛虻，不断叮咬这匹伟大的骏马——雅典城邦，唤醒它、劝诫它、责备它。", userNote = "苏格拉底自比牛虻，他的使命不是传授知识，而是刺痛沉睡的灵魂，迫使人们审视自己的信念。这种\"唤醒\"精神是一切批判性思维的起点。", tags = "牛虻精神", aiSummary = "唤醒灵魂"),

            // ── 书籍 2: 新工具 (bookId=2) ──
            Note(bookId = 2, pageIndex = 0, originalText = "我们必须建立‘新工具’——这就是通过系统的、彻底的经验积累和有目的地归纳实验。", userNote = "培根提倡抛弃虚空建塔的字面论说，去建立注重归纳实验的\"新工具\"。这不就是帕斯卡提到的\"几何精神\"在科学上的投影吗？拒绝无意义的文字障，用系统化的方式去抓取和重构真实世界的规律，才能让人类的智理真正落地。", tags = "经验归纳", aiSummary = "实证精神的基石"),
            Note(bookId = 2, pageIndex = 1, originalText = "洞穴偶像：每个人都生活在自己特定的狭小洞穴中。", userNote = "培根预言了\"信息茧房\"。个体的背景、教育和喜好成了阻碍真理的滤镜。", tags = "信息茧房", aiSummary = "主观滤镜"),

            // ── 书籍 3: 思想录 (bookId=3) ──
            Note(bookId = 3, pageIndex = 0, originalText = "人是一支会思想的芦苇。", userNote = "帕斯卡尔精确捕捉到了人的两面性：生理上的极度脆弱与精神上的无限崇高。", tags = "人性尊严", aiSummary = "思考的力量"),
            Note(bookId = 3, pageIndex = 1, originalText = "几何精神依赖严谨明晰的逻辑、公理体系以及一条条推论。", userNote = "逻辑与直觉并不是对立的。真正的大师必须在严密的推理与瞬间的顿悟间游走。", tags = "几何精神", aiSummary = "理性的秩序"),
            Note(bookId = 3, pageIndex = 2, originalText = "敏感精神——凭直觉在一瞥之间洞察事物本质的精神。", userNote = "与几何精神的步步推理不同，敏感精神是一种整体性的直觉把握。帕斯卡认为，真正伟大的思想者必须同时具备这两种精神。", tags = "敏感精神", aiSummary = "直觉洞见")
        )

        demoNotes.forEach { note ->
            val id = fileDataSource.addNote(note).toInt()
            val savedId = if (id == 0) note.id else id
            val nodeId = "note_$savedId"
            
            // 1. 注册基础结点（使用中文标签作为节点名称，禁用"感悟 #ID"格式）
            val nodeLabel = when (savedId) {
                1 -> "自知无知"
                2 -> "生命省察"
                3 -> "牛虻唤醒"
                4 -> "经验归纳"
                5 -> "信息茧房"
                6 -> "人性尊严"
                7 -> "几何精神"
                8 -> "敏感精神"
                else -> note.tags
            }
            knowledgeRepository.addNode(KnowledgeNode(nodeId, note.bookId, nodeLabel, "Note", 1.2f, note.originalText, note.userNote, note.aiSummary))
            
            // 2. 建立【归档关联】：链接到原著
            knowledgeRepository.addEdge(KnowledgeEdge("edge_book_${note.bookId}_$savedId", note.bookId, "book_${note.bookId}", nodeId, "观点归档", 1.0f))

            // 3. 跨书同源串联：苏格拉底「自知无知」(note_1) → 培根「经验归纳」(note_4) 关于认知局限
            if (savedId == 4) { 
                knowledgeRepository.addEdge(KnowledgeEdge("cross_link_1", null, "note_1", nodeId, "思想演进：从无知到工具", 1.5f))
            }

            // 4. 【核心演示】：培根「经验归纳/新工具」(note_4) → 帕斯卡「几何精神」(note_7) 跨时空连线
            if (savedId == 7) {
                knowledgeRepository.addEdge(KnowledgeEdge("contrast_cross", null, "note_4", nodeId, "方法论投影：实证归纳 vs 几何演绎", 2.2f))
            }

            // 5. 苏格拉底「牛虻/唤醒灵魂」(note_3) ↔ 帕斯卡「敏感精神」(note_8) 跨时代连线
            if (savedId == 8) {
                knowledgeRepository.addEdge(KnowledgeEdge("cross_awaken_intuit", null, "note_3", nodeId, "唤醒与直觉：刺痛灵魂 vs 一瞥洞见", 2.0f))
            }

            knowledgeRepository.indexText(note.bookId, "NOTE", savedId.toString(), "${note.originalText}\n${note.userNote}")
        }

        // 6. 核心聚合：批判性思维范式（连接三个时代的智慧）
        val paradigmId = "paradigm_critical"
        knowledgeRepository.addNode(KnowledgeNode(paradigmId, null, "全域批判性思维", "Note", 1.8f, "苏格拉底的牛虻 + 培根的偶像批判 + 帕斯卡尔的理性边界", "这是一个整合了三个时代智慧的\"超级基因\"。", "智慧结晶"))
        knowledgeRepository.addEdge(KnowledgeEdge("pe1", null, "note_1", paradigmId, "精神内核", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("pe2", null, "note_4", paradigmId, "实操手册", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("pe3", null, "note_7", paradigmId, "认知边界", 2.0f))

        // 7. 注册概念(Concept)结点——抽象思想基因
        knowledgeRepository.addNode(KnowledgeNode("concept_critical_thinking", null, "批判性思维", "Concept", 1.7f,
            "对思维本身的系统性反思与评估，是贯穿西方哲学的核心方法论。",
            "苏格拉底的诘问法、培根的偶像批判、帕斯卡尔的几何精神与敏感精神的统一，都是批判性思维在不同维度的体现。",
            "批判性思维是思想基因图谱中最核心的枢纽概念，连接了三位思想家的方法论精髓。"))
        knowledgeRepository.addNode(KnowledgeNode("concept_epistemic_humility", null, "认知谦逊", "Concept", 1.5f,
            "承认自身认知的局限性与可错性，是求真与学习的起点。",
            "苏格拉底\"自知无知\"是认知谦逊的终极宣言；培根对\"四大假象\"的揭露是对认知偏见的系统祛魅。",
            "认知谦逊不是软弱，而是智慧的开端——它同时驱动了苏格拉底的省察生活和培根的经验归纳。" ))
        knowledgeRepository.addNode(KnowledgeNode("concept_empiricism", null, "经验主义", "Concept", 1.5f,
            "一切知识来源于感官经验，通过归纳法从特殊事实推导一般原理。",
            "培根的《新工具》系统阐述了经验归纳法，强调\"从事实到原理\"的认知路径。",
            "经验主义与理性主义构成了西方认识论的两大支柱，培根是经验主义的近代奠基人。"))
        knowledgeRepository.addNode(KnowledgeNode("concept_rationalism", null, "理性主义", "Concept", 1.5f,
            "通过逻辑推理和先天观念获得确定知识，强调理性的自足性。",
            "帕斯卡尔的\"几何精神\"体现了理性主义的精髓——从自明公理出发，通过严密推理构建知识体系。",
            "帕斯卡尔的独特之处在于他同时看到了理性主义的边界，为\"敏感精神\"留下了空间。"))
        knowledgeRepository.addNode(KnowledgeNode("concept_dialectic", null, "辩证法", "Concept", 1.4f,
            "通过对立观点的碰撞与对话，逼近真理的思维方法。",
            "苏格拉底的问答法（elenchus）是辩证法的原初形态；帕斯卡尔的\"敏感精神\"也是一种整体性的辩证把握。",
            "辩证法在思想基因图谱中表现为\"对话\"与\"张力\"——不同思想之间的碰撞产生新的洞见。"))
        knowledgeRepository.addNode(KnowledgeNode("concept_human_dignity", null, "人的尊严", "Concept", 1.4f,
            "人因其理性、自觉或存在价值而拥有不可剥夺的尊严。",
            "帕斯卡尔\"会思想的芦苇\"精确捕捉了人的双重性：生理脆弱与精神崇高。苏格拉底\"未经省察的生活不值得过\"同样是对人的尊严的捍卫。",
            "人的尊严是连接三位思想家的价值主线——从苏格拉底的道德自觉，到培根的认知自主，再到帕斯卡尔的存在觉醒。"))

        // 8. 注册范式(Mindset)结点——高阶思维模式
        knowledgeRepository.addNode(KnowledgeNode("mindset_western_philosophy", null, "西方哲学原典", "Mindset", 1.9f,
            "以古希腊哲学为源头的西方思想传统，强调理性思辨、逻辑论证与系统性追问。",
            "苏格拉底的申辩是西方哲学的精神奠基，帕斯卡尔的《思想录》则是哲学与信仰的深度对话。",
            "这一范式代表了\"追问传统\"——从\"什么是正义\"到\"人是什么\"，西方哲学始终在追问根本问题. "))
        knowledgeRepository.addNode(KnowledgeNode("mindset_scientific_revolution", null, "科学革命思维", "Mindset", 1.7f,
            "以实验、观察和数学化为特征的近代科学方法论，强调可验证性与系统性。",
            "培根的《新工具》是科学革命的方法论宣言，其归纳法为近代实验科学奠定了基础。",
            "科学革命思维与西方哲学原典范式之间存在深刻的张力与互补——前者追求精确控制，后者追问意义价值。"))
        knowledgeRepository.addNode(KnowledgeNode("mindset_existential_reflection", null, "存在之思", "Mindset", 1.6f,
            "对个体存在意义、有限性与超越性的深层反思，关注人在宇宙中的位置。",
            "帕斯卡尔\"会思想的芦苇\"是存在之思的经典表达；苏格拉底的\"生命省察\"同样是对存在合法性的追问。",
            "存在之思是连接古典哲学与现代存在主义的思想基因桥梁。"))

        // 9. 注册跨书连接边（补充种子JSON中已有的边，确保动态注册的节点也有连接）
        // 苏格拉底 ↔ 帕斯卡尔
        knowledgeRepository.addEdge(KnowledgeEdge("cross_socrates_pascal_1", null, "note_1", "note_6", "人的有限性：无知与芦苇的共鸣", 1.8f))
        knowledgeRepository.addEdge(KnowledgeEdge("cross_socrates_pascal_2", null, "note_2", "note_6", "省察即尊严：自觉的生命高于一切", 1.6f))
        // 培根 ↔ 帕斯卡尔
        knowledgeRepository.addEdge(KnowledgeEdge("cross_bacon_pascal_1", null, "note_5", "note_8", "破除幻象：偶像批判与敏感精神的直觉洞见", 1.7f))
        knowledgeRepository.addEdge(KnowledgeEdge("cross_bacon_pascal_2", null, "note_4", "note_6", "经验与脆弱：归纳法如何安放人的尊严", 1.3f))
        // 苏格拉底 ↔ 培根
        knowledgeRepository.addEdge(KnowledgeEdge("cross_socrates_bacon_1", null, "note_3", "note_5", "唤醒与祛魅：牛虻精神与偶像破除", 1.9f))
        knowledgeRepository.addEdge(KnowledgeEdge("cross_socrates_bacon_2", null, "note_2", "note_4", "省察即方法：生命追问与经验归纳的对话", 1.4f))

        // 10. 概念连接边：笔记 → 概念
        knowledgeRepository.addEdge(KnowledgeEdge("c1", null, "note_1", "concept_epistemic_humility", "奠基", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("c2", null, "note_3", "concept_critical_thinking", "精神源头", 2.2f))
        knowledgeRepository.addEdge(KnowledgeEdge("c3", null, "note_4", "concept_empiricism", "方法论基础", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("c4", null, "note_7", "concept_rationalism", "逻辑典范", 1.8f))
        knowledgeRepository.addEdge(KnowledgeEdge("c5", null, "note_8", "concept_dialectic", "直觉辩证", 1.5f))
        knowledgeRepository.addEdge(KnowledgeEdge("c6", null, "note_6", "concept_human_dignity", "哲学基石", 1.8f))
        knowledgeRepository.addEdge(KnowledgeEdge("c7", null, "note_5", "concept_critical_thinking", "批判对象", 1.6f))
        knowledgeRepository.addEdge(KnowledgeEdge("c8", null, "note_2", "concept_epistemic_humility", "实践路径", 1.5f))
        // 概念 → 概念
        knowledgeRepository.addEdge(KnowledgeEdge("c9", null, "concept_epistemic_humility", "concept_critical_thinking", "前提条件", 1.6f))
        knowledgeRepository.addEdge(KnowledgeEdge("c10", null, "concept_empiricism", "concept_rationalism", "认识论双翼", 1.7f))
        knowledgeRepository.addEdge(KnowledgeEdge("c11", null, "concept_dialectic", "concept_critical_thinking", "核心方法", 1.5f))
        knowledgeRepository.addEdge(KnowledgeEdge("c12", null, "concept_human_dignity", "concept_epistemic_humility", "价值根基", 1.4f))

        // 11. 范式连接边：书籍 → 范式
        knowledgeRepository.addEdge(KnowledgeEdge("m1", null, "book_1", "mindset_western_philosophy", "奠基之作", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("m2", null, "book_2", "mindset_scientific_revolution", "科学宣言", 2.0f))
        knowledgeRepository.addEdge(KnowledgeEdge("m3", null, "book_3", "mindset_existential_reflection", "存在先声", 2.0f))
        // 概念 → 范式
        knowledgeRepository.addEdge(KnowledgeEdge("m4", null, "concept_critical_thinking", "mindset_western_philosophy", "核心基因", 1.8f))
        knowledgeRepository.addEdge(KnowledgeEdge("m5", null, "concept_empiricism", "mindset_scientific_revolution", "认识论引擎", 1.7f))
        knowledgeRepository.addEdge(KnowledgeEdge("m6", null, "concept_human_dignity", "mindset_existential_reflection", "价值内核", 1.6f))
        // 范式 → 范式
        knowledgeRepository.addEdge(KnowledgeEdge("m7", null, "mindset_western_philosophy", "mindset_scientific_revolution", "思想演进", 1.5f))
        knowledgeRepository.addEdge(KnowledgeEdge("m8", null, "mindset_existential_reflection", "mindset_western_philosophy", "人文转向", 1.4f))
    }



    // ──────────────────────────────────────────────
    // UI 配置更新方法
    // ──────────────────────────────────────────────

    fun updateUiConfig(config: UiConfig) {
        _uiConfig.value = config
    }

    fun updateThemeMode(mode: ThemeMode) {
        _uiConfig.value = _uiConfig.value.copy(themeMode = mode)
    }

    fun updateUiFont(font: FontOption) {
        _uiConfig.value = _uiConfig.value.copy(uiFont = font)
    }

    fun updateReadingFont(font: FontOption) {
        _uiConfig.value = _uiConfig.value.copy(readingFont = font)
    }

    fun updateColorTheme(colorTheme: ColorTheme) {
        _uiConfig.value = _uiConfig.value.copy(colorTheme = colorTheme)
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

    fun selectBookInNotes(bookId: Int?) {
        _selectedBookIdInNotes.value = bookId
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
        if (lastProgress != null && kotlin.math.abs(lastProgress - normalizedProgress) < 0.001f) return
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
            val bookExcerpts = BookDummyData.excerpts[bookId] ?: emptyList<String>()
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
            try {
                // 使用 withTimeout 防止 AI 调用超时导致永久 loading
                kotlinx.coroutines.withTimeout(30_000L) {
                    saveNoteUseCase(bookId, originalText, userInsight)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.e("SmartReadViewModel", "saveNote timed out", e)
            } catch (e: Exception) {
                android.util.Log.e("SmartReadViewModel", "saveNote failed", e)
            } finally {
                _isAiLoading.value = false
            }
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
        _artImageState.value = ArtImageState.Idle
        viewModelScope.launch {
            val book = bookRepository.getBookById(bookId)
            val title = book?.title ?: "经典"
            val report = generateReportUseCase(bookId, title)
            _activeReport.value = report
            
            // 如果报告中已经有生成的图片，直接显示
            if (report.artImageUrl != null) {
                _artImageState.value = ArtImageState.Success(report.artImageUrl)
            }
            
            _isAiLoading.value = false
        }
    }

    fun closeReport() {
        _activeReport.value = null
        _artImageState.value = ArtImageState.Idle
    }

    fun searchBooks(query: String) {
        if (query.trim().isEmpty()) return
        _isAiLoading.value = true
        viewModelScope.launch {
            _searchResults.value = bookRepository.searchBooks(query)
            _isAiLoading.value = false
        }
    }

    fun addBookToShelf(book: Book) {
        viewModelScope.launch {
            bookRepository.addBook(book)
            _searchResults.value = emptyList()
        }
    }

    fun generateArtImage() {
        val report = _activeReport.value ?: return
        _artImageState.value = ArtImageState.Loading
        viewModelScope.launch {
            try {
                // 使用升级版方法：基于丰富的报告数据生成文艺手账风格长图
                val result = vivoImageRepository.generateArtImageFromReport(report)
                result.fold(
                    onSuccess = { url ->
                        _artImageState.value = ArtImageState.Success(url)
                        // 将生成的图片 URL 保存到报告中，防止重复生成或显示旧图
                        val updatedReport = report.copy(artImageUrl = url)
                        _activeReport.value = updatedReport
                        reportRepository.saveReport(updatedReport)
                    },
                    onFailure = { e ->
                        _artImageState.value = ArtImageState.Error(e.message ?: "生成失败")
                    }
                )
            } catch (e: Exception) {
                _artImageState.value = ArtImageState.Error(e.message ?: "生成失败")
            }
        }
    }

    fun updateBookCover(bookId: Int, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val coversDir = java.io.File(context.filesDir, "covers").apply { mkdirs() }
                val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "jpg"
                val destFile = java.io.File(coversDir, "cover_$bookId.$extension")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val book = bookRepository.getBookById(bookId)
                if (book != null) {
                    bookRepository.updateBook(book.copy(coverUri = destFile.absolutePath))
                }
            } catch (e: Exception) {
                android.util.Log.e("SmartReadViewModel", "Failed to update book cover", e)
            }
        }
    }
}

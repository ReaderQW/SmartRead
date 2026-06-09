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

    private val vivoImageRepository = VivoImageRepositoryImpl(VivoClient().api)
    private val _artImageState = MutableStateFlow<ArtImageState>(ArtImageState.Idle)
    val artImageState: StateFlow<ArtImageState> = _artImageState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Book>>(emptyList())
    val searchResults: StateFlow<List<Book>> = _searchResults.asStateFlow()

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
        val existingNotes = fileDataSource.getNotesForBook(1)
        if (existingNotes.isNotEmpty()) return

        val demoNotes = listOf(
            Note(bookId = 1, pageIndex = 2, originalText = "未经省察的生活是不值得度过的", userNote = "每天睡前问自己三个问题：今天我做了什么？为什么这么做？还能怎么改进？", tags = "苏格拉底,自我省察", aiSummary = "强调反思性自觉对人生意义的决定性作用。", timestamp = System.currentTimeMillis() - 3600000L)
        )

        demoNotes.forEach { note ->
            val id = fileDataSource.addNote(note).toInt()
            val savedId = if (id == 0) note.id else id
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
        val prompt = "书籍：《${report.bookTitle}》。认知增量：${report.cognitiveIncrement}。金句：${report.motto}"
        _artImageState.value = ArtImageState.Loading
        viewModelScope.launch {
            try {
                val result = vivoImageRepository.generateArtImage(prompt, "水墨意境")
                result.fold(
                    onSuccess = { url ->
                        _artImageState.value = ArtImageState.Success(url)
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
}

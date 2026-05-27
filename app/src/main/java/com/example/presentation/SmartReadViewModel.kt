package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.ReadingReport
import com.example.data.repository.SmartReadRepository
import com.example.utils.BookDummyData
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmartReadViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SmartReadDatabase.getDatabase(application)
    private val repository = SmartReadRepository(db)

    // --- Core states ---
    private val _currentBookId = MutableStateFlow<Int?>(null)
    val currentBookId: StateFlow<Int?> = _currentBookId.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _selectedText = MutableStateFlow("")
    val selectedText: StateFlow<String> = _selectedText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Socratic Chat open / closed
    private val _isFloatingAssistantOpen = MutableStateFlow(false)
    val isFloatingAssistantOpen: StateFlow<Boolean> = _isFloatingAssistantOpen.asStateFlow()

    // Live OCR State
    private val _isOcrScanning = MutableStateFlow(false)
    val isOcrScanning: StateFlow<Boolean> = _isOcrScanning.asStateFlow()

    private val _scannedOcrText = MutableStateFlow<String?>(null)
    val scannedOcrText: StateFlow<String?> = _scannedOcrText.asStateFlow()

    // Blind box report
    private val _activeReport = MutableStateFlow<ReadingReport?>(null)
    val activeReport: StateFlow<ReadingReport?> = _activeReport.asStateFlow()

    // Database Flows
    val allBooks: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeNodes: StateFlow<List<KnowledgeNode>> = repository.allNodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knowledgeEdges: StateFlow<List<KnowledgeEdge>> = repository.allEdges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Book-specific Flow switchers
    val highlightsForCurrentBook: StateFlow<List<Highlight>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) repository.getHighlightsForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesForCurrentBook: StateFlow<List<Note>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) repository.getNotesForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessagesForCurrentBook: StateFlow<List<ChatMessage>> = _currentBookId
        .flatMapLatest { id ->
            if (id != null) repository.getChatMessagesForBook(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Build initial shelf on startup
        viewModelScope.launch {
            repository.seedInitialBooks()
        }
    }

    // --- Book Navigation ---
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
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            if (book != null) {
                repository.updateBook(book.copy(progress = progress))
            }
        }
    }

    // --- Highlight selections ---
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
            // Emulate basic canvas coordinates in a simulated grid
            repository.addHighlight(
                bookId = bookId,
                pageIndex = _currentPageIndex.value,
                text = text,
                startX = (10..50).random().toFloat(),
                startY = (100..200).random().toFloat(),
                endX = (200..350).random().toFloat(),
                endY = (300..450).random().toFloat(),
                colorHex = colorHex
            )
            _selectedText.value = ""
        }
    }

    fun deleteHighlight(highlight: Highlight) {
        viewModelScope.launch {
            repository.deleteHighlight(highlight)
        }
    }

    // --- Custom Socratic Chat ---
    fun setFloatingAssistantOpen(open: Boolean) {
        _isFloatingAssistantOpen.value = open
    }

    fun sendSocraticMessage(userMsg: String) {
        val bookId = _currentBookId.value ?: return
        if (userMsg.trim().isEmpty()) return
        
        // Excerpt or highlight context fallback
        var contextText = _selectedText.value
        if (contextText.isEmpty()) {
            val bookExcerpts = BookDummyData.excerpts[bookId] ?: emptyList()
            val textPage = bookExcerpts.getOrNull(_currentPageIndex.value) ?: ""
            contextText = if (textPage.length > 100) textPage.take(100) + "..." else textPage
        }

        _isAiLoading.value = true
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            val title = book?.title ?: "经典"
            repository.sendSocraticMessage(bookId, title, contextText, userMsg)
            _isAiLoading.value = false
        }
    }

    // --- Notes and OCR card ---
    fun startSimulatedOcrScan(passageExcerpt: String) {
        _isOcrScanning.value = true
        _scannedOcrText.value = null
        viewModelScope.launch {
            kotlinx.coroutines.delay(1800) // Simulated digital sci-fi scanning beam time
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
            repository.saveNoteWithAiInsight(bookId, originalText, userInsight)
            _isAiLoading.value = false
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- Report generation ---
    fun generateReport() {
        val bookId = _currentBookId.value ?: return
        _isAiLoading.value = true
        viewModelScope.launch {
            val book = repository.getBookById(bookId)
            val title = book?.title ?: "经典"
            val report = repository.generateBlindBoxReport(bookId, title)
            _activeReport.value = report
            _isAiLoading.value = false
        }
    }

    fun closeReport() {
        _activeReport.value = null
    }
}

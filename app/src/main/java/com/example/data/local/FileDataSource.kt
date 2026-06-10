package com.example.data.local

import android.content.Context
import android.os.Environment
import com.example.BuildConfig
import com.example.domain.model.Book
import com.example.domain.model.ChatMessage
import com.example.domain.model.DialogueFrequency
import com.example.domain.model.Highlight
import com.example.domain.model.HighlightSemantics
import com.example.domain.model.InteractionMatrix
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.model.NoteDepth
import com.example.domain.model.ReadingReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 基于 JSON 文件的数据源，替代 Room SQLite 数据库。
 * 数据存储在内部存储的 data/ 目录下，首次启动时从 assets/data/ 复制种子数据。
 *
 * 所有数据改动直接写入 JSON 文件，可通过 ADB pull 或文件管理器导出。
 */
class FileDataSource(private val context: Context) {

    private val dataDir: File
        get() = File(context.filesDir, "data").also { it.mkdirs() }

    /** Debug 模式下用于导出数据的目录（可通过 adb pull 访问） */
    private val syncDir: File
        get() = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "SmartRead").also { it.mkdirs() }

    // ── 数据缓存（替代 Room Flow） ──
    private val _books = MutableStateFlow<List<Book>>(emptyList())
    private val _highlights = MutableStateFlow<List<Highlight>>(emptyList())
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val _knowledgeNodes = MutableStateFlow<List<KnowledgeNode>>(emptyList())
    private val _knowledgeEdges = MutableStateFlow<List<KnowledgeEdge>>(emptyList())
    private val _readingReports = MutableStateFlow<List<ReadingReport>>(emptyList())

    // ── 公开只读 Flow ──
    val observeBooks: Flow<List<Book>> = _books.asStateFlow()
    val observeAllNotes: Flow<List<Note>> = _notes.asStateFlow()
    val observeNodes: Flow<List<KnowledgeNode>> = _knowledgeNodes.asStateFlow()
    val observeEdges: Flow<List<KnowledgeEdge>> = _knowledgeEdges.asStateFlow()

    fun observeHighlightsForBook(bookId: Int): Flow<List<Highlight>> =
        _highlights.asStateFlow().map { it.filter { h -> h.bookId == bookId } }

    fun observeNotesForBook(bookId: Int): Flow<List<Note>> =
        _notes.asStateFlow().map { it.filter { n -> n.bookId == bookId } }

    fun observeMessagesForBook(bookId: Int): Flow<List<ChatMessage>> =
        _chatMessages.asStateFlow().map { it.filter { m -> m.bookId == bookId } }

    fun observeReport(bookId: Int): Flow<ReadingReport?> =
        _readingReports.asStateFlow().map { reports -> reports.firstOrNull { it.bookId == bookId } }

    // ── 初始化 ──
    suspend fun init() {
        loadAll()
        if (_books.value.isEmpty()) {
            copySeedFromAssets()
            loadAll()
        }
    }

    // ── Book CRUD ──
    fun getBookById(id: Int): Book? = _books.value.firstOrNull { it.id == id }

    fun addBook(book: Book): Int {
        val list = _books.value.toMutableList()
        val newId = (list.maxOfOrNull { it.id } ?: 0) + 1
        val newBook = book.copy(id = newId)
        list.add(newBook)
        _books.value = list
        saveBooks()
        return newId
    }

    fun updateBook(book: Book) {
        _books.value = _books.value.map { if (it.id == book.id) book else it }
        saveBooks()
    }

    fun deleteBook(book: Book) {
        _books.value = _books.value.filter { it.id != book.id }
        saveBooks()
    }

    // ── Highlight CRUD ──
    fun getHighlightsForBook(bookId: Int): List<Highlight> =
        _highlights.value.filter { it.bookId == bookId }

    fun getHighlightById(id: Int): Highlight? = _highlights.value.firstOrNull { it.id == id }

    fun addHighlight(highlight: Highlight): Long {
        val list = _highlights.value.toMutableList()
        val newHighlight = highlight.copy(id = genId(list))
        list.add(newHighlight)
        _highlights.value = list
        saveHighlights()
        return newHighlight.id.toLong()
    }

    fun deleteHighlight(highlight: Highlight) {
        _highlights.value = _highlights.value.filter { it.id != highlight.id }
        saveHighlights()
    }

    // ── Note CRUD ──
    fun getNotesForBook(bookId: Int): List<Note> =
        _notes.value.filter { it.bookId == bookId }

    fun addNote(note: Note): Long {
        val list = _notes.value.toMutableList()
        val newNote = note.copy(id = genId(list))
        list.add(newNote)
        _notes.value = list
        saveNotes()
        return newNote.id.toLong()
    }

    fun deleteNote(note: Note) {
        _notes.value = _notes.value.filter { it.id != note.id }
        saveNotes()
    }

    // ── Chat CRUD ──
    fun getMessagesForBook(bookId: Int): List<ChatMessage> =
        _chatMessages.value.filter { it.bookId == bookId }

    fun addMessage(message: ChatMessage): Long {
        val list = _chatMessages.value.toMutableList()
        val newMsg = message.copy(id = genId(list))
        list.add(newMsg)
        _chatMessages.value = list
        saveChatMessages()
        return newMsg.id.toLong()
    }

    // ── Knowledge CRUD ──
    fun addNode(node: KnowledgeNode) {
        _knowledgeNodes.value = _knowledgeNodes.value + node
        saveKnowledgeNodes()
    }

    fun addEdge(edge: KnowledgeEdge) {
        _knowledgeEdges.value = _knowledgeEdges.value + edge
        saveKnowledgeEdges()
    }

    fun clearGraph() {
        _knowledgeNodes.value = emptyList()
        _knowledgeEdges.value = emptyList()
        saveKnowledgeNodes()
        saveKnowledgeEdges()
    }

    // ── Report CRUD ──
    fun getReport(bookId: Int): ReadingReport? =
        _readingReports.value.firstOrNull { it.bookId == bookId }

    fun insertReport(report: ReadingReport): Long {
        val list = _readingReports.value.toMutableList()
        list.removeAll { it.bookId == report.bookId }
        list.add(report)
        _readingReports.value = list
        saveReadingReports()
        return 1
    }

    // ── 数据持久化 ──
    private fun genId(list: List<Any>): Int = (list.maxOfOrNull {
        when (it) {
            is Book -> it.id
            is Highlight -> it.id
            is Note -> it.id
            is ChatMessage -> it.id
            else -> 0
        }
    } ?: 0) + 1

    private fun loadAll() {
        _books.value = readJsonList("books.json") { it.toBook() }
        _highlights.value = readJsonList("highlights.json") { it.toHighlight() }
        _notes.value = readJsonList("notes.json") { it.toNote() }
        _chatMessages.value = readJsonList("chat_messages.json") { it.toChatMessage() }
        _knowledgeNodes.value = readJsonList("knowledge_nodes.json") { it.toNode() }
        _knowledgeEdges.value = readJsonList("knowledge_edges.json") { it.toEdge() }
        _readingReports.value = readJsonList("reading_reports.json") { it.toReport() }
    }

    private fun saveBooks() = writeJsonAndSync("books.json", _books.value)
    private fun saveHighlights() = writeJsonAndSync("highlights.json", _highlights.value)
    private fun saveNotes() = writeJsonAndSync("notes.json", _notes.value)
    private fun saveChatMessages() = writeJsonAndSync("chat_messages.json", _chatMessages.value)
    private fun saveKnowledgeNodes() = writeJsonAndSync("knowledge_nodes.json", _knowledgeNodes.value)
    private fun saveKnowledgeEdges() = writeJsonAndSync("knowledge_edges.json", _knowledgeEdges.value)
    private fun saveReadingReports() = writeJsonAndSync("reading_reports.json", _readingReports.value)

    private fun copySeedFromAssets() {
        listOf(
            "books", "highlights", "notes", "chat_messages",
            "knowledge_nodes", "knowledge_edges", "reading_reports", "embeddings"
        ).forEach { name ->
            val srcPath = "data/$name.json"
            val dstFile = File(dataDir, "$name.json")
            if (!dstFile.exists()) {
                try {
                    context.assets.open(srcPath).use { input ->
                        dstFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // ── JSON 读写工具 ──
    private inline fun <T> readJsonList(fileName: String, mapper: (JSONObject) -> T): List<T> {
        val file = File(dataDir, fileName)
        if (!file.exists()) return emptyList()
        return try {
            val text = file.readText()
            if (text.isBlank()) return emptyList()
            val array = JSONArray(text)
            (0 until array.length()).map { i -> mapper(array.getJSONObject(i)) }
        } catch (e: Exception) {
            android.util.Log.e("FileDataSource", "Error reading $fileName", e)
            emptyList()
        }
    }

    /** 写入内部存储 + Debug 模式下同步写入外部存储 */
    private fun writeJsonAndSync(fileName: String, data: List<*>) {
        writeJsonFile(fileName, data)
        if (BuildConfig.DEBUG) {
            exportToSyncDir(fileName, data)
        }
    }

    /** 将数据导出到外部存储的同步目录，供 adb pull 拉取到 assets/ */
    private fun exportToSyncDir(fileName: String, data: List<*>) {
        try {
            val file = File(syncDir, fileName)
            val array = JSONArray()
            data.forEach { item ->
                array.put(when (item) {
                    is Book -> item.toJson()
                    is Highlight -> item.toJson()
                    is Note -> item.toJson()
                    is ChatMessage -> item.toJson()
                    is KnowledgeNode -> item.toJson()
                    is KnowledgeEdge -> item.toJson()
                    is ReadingReport -> item.toJson()
                    else -> JSONObject()
                })
            }
            file.writeText(array.toString(2))
        } catch (e: Exception) {
            android.util.Log.e("FileDataSource", "Error exporting $fileName", e)
        }
    }

    private fun writeJsonFile(fileName: String, data: List<*>) {
        try {
            val file = File(dataDir, fileName)
            val array = JSONArray()
            data.forEach { item ->
                array.put(when (item) {
                    is Book -> item.toJson()
                    is Highlight -> item.toJson()
                    is Note -> item.toJson()
                    is ChatMessage -> item.toJson()
                    is KnowledgeNode -> item.toJson()
                    is KnowledgeEdge -> item.toJson()
                    is ReadingReport -> item.toJson()
                    else -> JSONObject()
                })
            }
            file.writeText(array.toString(2))
        } catch (e: Exception) {
            android.util.Log.e("FileDataSource", "Error writing $fileName", e)
        }
    }

    // ── Domain ↔ JSON 转换 ──
    private fun JSONObject.toBook() = Book(
        id = optInt("id"),
        title = optString("title"),
        author = optString("author"),
        coverResName = optString("coverResName"),
        summaryText = optString("summaryText"),
        category = optString("category"),
        progress = optDouble("progress", 0.0).toFloat(),
        type = optString("type", "DEMO_TEXT"),
        fileUri = optString("fileUri", null),
        coverUri = optString("coverUri", null),
        totalPages = optInt("totalPages"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )
    private fun Book.toJson() = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("author", author)
        put("coverResName", coverResName)
        put("summaryText", summaryText)
        put("category", category)
        put("progress", progress.toDouble())
        put("type", type)
        fileUri?.let { put("fileUri", it) }
        coverUri?.let { put("coverUri", it) }
        put("totalPages", totalPages)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toHighlight() = Highlight(
        id = optInt("id"),
        bookId = optInt("bookId"),
        pageIndex = optInt("pageIndex"),
        text = optString("text"),
        comment = optString("comment", null),
        colorHex = optString("colorHex", "#FFEB3B"),
        timestamp = optLong("timestamp", System.currentTimeMillis())
    )
    private fun Highlight.toJson() = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        put("pageIndex", pageIndex)
        put("text", text)
        comment?.let { put("comment", it) }
        put("colorHex", colorHex)
        put("timestamp", timestamp)
    }

    private fun JSONObject.toNote() = Note(
        id = optInt("id"),
        bookId = optInt("bookId"),
        pageIndex = if (has("pageIndex")) optInt("pageIndex") else null,
        originalText = optString("originalText"),
        userNote = optString("userNote"),
        aiSummary = optString("aiSummary", null),
        tags = optString("tags", ""),
        aiStatus = optString("aiStatus", "READY"),
        timestamp = optLong("timestamp", System.currentTimeMillis())
    )
    private fun Note.toJson() = JSONObject().apply {
        put("id", id)
        put("bookId", bookId)
        pageIndex?.let { put("pageIndex", it) }
        put("originalText", originalText)
        put("userNote", userNote)
        aiSummary?.let { put("aiSummary", it) }
        put("tags", tags)
        put("aiStatus", aiStatus)
        put("timestamp", timestamp)
    }

    private fun JSONObject.toChatMessage() = ChatMessage(
        id = optInt("id"),
        sessionId = if (has("sessionId")) optInt("sessionId") else null,
        bookId = optInt("bookId"),
        sender = optString("sender"),
        content = optString("content"),
        selectedText = optString("selectedText", null),
        timestamp = optLong("timestamp", System.currentTimeMillis())
    )
    private fun ChatMessage.toJson() = JSONObject().apply {
        put("id", id)
        sessionId?.let { put("sessionId", it) }
        put("bookId", bookId)
        put("sender", sender)
        put("content", content)
        selectedText?.let { put("selectedText", it) }
        put("timestamp", timestamp)
    }

    private fun JSONObject.toNode() = KnowledgeNode(
        id = optString("id"),
        bookId = if (has("bookId") && !isNull("bookId")) optInt("bookId") else null,
        label = optString("label"),
        category = optString("category"),
        size = optDouble("size", 1.0).toFloat()
    )
    private fun KnowledgeNode.toJson() = JSONObject().apply {
        put("id", id)
        bookId?.let { put("bookId", it) }
        put("label", label)
        put("category", category)
        put("size", size.toDouble())
    }

    private fun JSONObject.toEdge() = KnowledgeEdge(
        id = optString("id"),
        bookId = if (has("bookId") && !isNull("bookId")) optInt("bookId") else null,
        source = optString("source"),
        target = optString("target"),
        relation = optString("relation"),
        weight = optDouble("weight", 1.0).toFloat()
    )
    private fun KnowledgeEdge.toJson() = JSONObject().apply {
        put("id", id)
        bookId?.let { put("bookId", it) }
        put("source", source)
        put("target", target)
        put("relation", relation)
        put("weight", weight.toDouble())
    }

    // ── ReadingReport 升级版 JSON 序列化（含三维交互矩阵 + 丰富内容字段） ──
    private fun JSONObject.toReport() = ReadingReport(
        bookId = optInt("bookId"),
        bookTitle = optString("bookTitle"),
        bookAuthor = optString("bookAuthor", ""),
        bookSummary = optString("bookSummary", ""),
        logic = optInt("logic"),
        empathy = optInt("empathy"),
        critical = optInt("critical"),
        width = optInt("width"),
        innovation = optInt("innovation"),
        interactionMatrix = parseInteractionMatrix(optJSONObject("interactionMatrix")),
        cognitiveIncrement = optString("cognitiveIncrement"),
        motto = optString("motto"),
        highlightsList = parseStringArray(optJSONArray("highlightsList")),
        notesList = parseStringArray(optJSONArray("notesList")),
        chatExcerpts = parseStringArray(optJSONArray("chatExcerpts")),
        artImageUrl = if (has("artImageUrl")) optString("artImageUrl") else null,
        timestamp = optLong("timestamp", System.currentTimeMillis())
    )
    private fun ReadingReport.toJson() = JSONObject().apply {
        put("bookId", bookId)
        put("bookTitle", bookTitle)
        put("bookAuthor", bookAuthor)
        put("bookSummary", bookSummary)
        put("logic", logic)
        put("empathy", empathy)
        put("critical", critical)
        put("width", width)
        put("innovation", innovation)
        put("interactionMatrix", interactionMatrix.toJson())
        put("cognitiveIncrement", cognitiveIncrement)
        put("motto", motto)
        put("highlightsList", JSONArray(highlightsList))
        put("notesList", JSONArray(notesList))
        put("chatExcerpts", JSONArray(chatExcerpts))
        artImageUrl?.let { put("artImageUrl", it) }
        put("timestamp", timestamp)
    }

    private fun InteractionMatrix.toJson() = JSONObject().apply {
        put("highlightSemantics", highlightSemantics.toJson())
        put("noteDepth", noteDepth.toJson())
        put("dialogueFrequency", dialogueFrequency.toJson())
    }
    private fun HighlightSemantics.toJson() = JSONObject().apply {
        put("totalCount", totalCount)
        put("keyConcepts", JSONArray(keyConcepts))
        put("emotionalTone", emotionalTone)
        put("representativeExcerpts", JSONArray(representativeExcerpts))
    }
    private fun NoteDepth.toJson() = JSONObject().apply {
        put("totalCount", totalCount)
        put("depthScore", depthScore)
        put("insightThemes", JSONArray(insightThemes))
        put("representativeNotes", JSONArray(representativeNotes))
    }
    private fun DialogueFrequency.toJson() = JSONObject().apply {
        put("totalCount", totalCount)
        put("questionTypes", JSONArray(questionTypes))
        put("engagementLevel", engagementLevel)
        put("representativeDialogues", JSONArray(representativeDialogues))
    }

    private fun parseInteractionMatrix(obj: JSONObject?): InteractionMatrix {
        if (obj == null) return InteractionMatrix(HighlightSemantics(), NoteDepth(), DialogueFrequency())
        val hs = obj.optJSONObject("highlightSemantics")
        val nd = obj.optJSONObject("noteDepth")
        val df = obj.optJSONObject("dialogueFrequency")
        return InteractionMatrix(
            highlightSemantics = HighlightSemantics(
                totalCount = hs?.optInt("totalCount", 0) ?: 0,
                keyConcepts = parseStringArray(hs?.optJSONArray("keyConcepts")),
                emotionalTone = hs?.optString("emotionalTone", "") ?: "",
                representativeExcerpts = parseStringArray(hs?.optJSONArray("representativeExcerpts"))
            ),
            noteDepth = NoteDepth(
                totalCount = nd?.optInt("totalCount", 0) ?: 0,
                depthScore = nd?.optInt("depthScore", 0) ?: 0,
                insightThemes = parseStringArray(nd?.optJSONArray("insightThemes")),
                representativeNotes = parseStringArray(nd?.optJSONArray("representativeNotes"))
            ),
            dialogueFrequency = DialogueFrequency(
                totalCount = df?.optInt("totalCount", 0) ?: 0,
                questionTypes = parseStringArray(df?.optJSONArray("questionTypes")),
                engagementLevel = df?.optInt("engagementLevel", 0) ?: 0,
                representativeDialogues = parseStringArray(df?.optJSONArray("representativeDialogues"))
            )
        )
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.optString(it, "") }.filter { it.isNotEmpty() }
    }

    // JSONObject 工具方法
    private fun JSONObject.optString(key: String, default: String?): String? {
        return if (has(key) && !isNull(key)) optString(key, default!!) else default
    }
}

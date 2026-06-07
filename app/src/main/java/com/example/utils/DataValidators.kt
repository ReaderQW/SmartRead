package com.example.utils

/**
 * 数据验证器
 */
object DataValidators {

    /**
     * 验证书籍ID
     */
    fun validateBookId(bookId: Int) {
        if (bookId <= 0) {
            throw AppException.ValidationException("Book ID must be positive", "bookId")
        }
    }

    fun validateBookData(title: String, author: String) {
        if (title.isBlank()) {
            throw AppException.ValidationException("Book title cannot be empty", "title")
        }
        if (title.length > 500) {
            throw AppException.ValidationException("Book title is too long", "title")
        }
        if (author.isBlank()) {
            throw AppException.ValidationException("Author cannot be empty", "author")
        }
        if (author.length > 255) {
            throw AppException.ValidationException("Author name is too long", "author")
        }
    }

    /**
     * 验证笔记数据
     */
    fun validateNoteContent(originalText: String, userNote: String) {
        if (originalText.isBlank()) {
            throw AppException.ValidationException("Original text cannot be empty", "originalText")
        }
        if (originalText.length > 10000) {
            throw AppException.ValidationException("Original text is too long", "originalText")
        }
        if (userNote.isBlank()) {
            throw AppException.ValidationException("Note content cannot be empty", "userNote")
        }
        if (userNote.length > 10000) {
            throw AppException.ValidationException("Note content is too long", "userNote")
        }
    }

    /**
     * 验证聊天消息
     */
    fun validateChatMessage(content: String) {
        if (content.isBlank()) {
            throw AppException.ValidationException("Message cannot be empty", "content")
        }
        if (content.length > 5000) {
            throw AppException.ValidationException("Message is too long", "content")
        }
    }

    /**
     * 验证搜索关键词
     */
    fun validateSearchQuery(query: String) {
        if (query.length > 200) {
            throw AppException.ValidationException("Search query is too long", "query")
        }
    }

    /**
     * 验证分页参数
     */
    fun validatePageParams(pageIndex: Int) {
        if (pageIndex < 0) {
            throw AppException.ValidationException("Page index cannot be negative", "pageIndex")
        }
    }
}

/**
 * 数据约束定义
 */
object DataConstraints {
    // 字段长度约束
    const val MAX_BOOK_TITLE_LENGTH = 500
    const val MAX_AUTHOR_LENGTH = 255
    const val MAX_NOTE_CONTENT_LENGTH = 10000
    const val MAX_CHAT_MESSAGE_LENGTH = 5000
    const val MAX_HIGHLIGHT_TEXT_LENGTH = 5000
    const val MAX_SEARCH_QUERY_LENGTH = 200

    // 分页约束
    const val DEFAULT_PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 1000
    const val MIN_PAGE_SIZE = 1

    // 数据库约束
    const val MAX_EMBEDDINGS_PER_BOOK = 10000
    const val MAX_KNOWLEDGE_NODES_PER_BOOK = 5000
    const val MAX_CHAT_MESSAGES_PER_SESSION = 1000
}
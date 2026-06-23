package com.example.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Book
import com.example.presentation.creation.BookCreationScreen
import com.example.presentation.dashboard.BookListScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.reader.ReaderScreen
import com.example.presentation.viewmodel.SmartReadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReadApp(
    viewModel: SmartReadViewModel,
    modifier: Modifier = Modifier,
    pendingOcrText: String? = null,
    pendingScreenshotPath: String? = null,
    onOcrHandled: () -> Unit = {}
) {
    val currentBookId by viewModel.currentBookId.collectAsStateWithLifecycle()
    val isCreatingBook by viewModel.isCreatingBook.collectAsStateWithLifecycle()
    val showBookList by viewModel.showBookList.collectAsStateWithLifecycle()

    // 处理来自悬浮窗的 OCR 结果
    LaunchedEffect(pendingOcrText) {
        if (pendingOcrText != null) {
            val bookId = currentBookId
            if (bookId != null) {
                // 如果当前已有选中的书籍，则将 OCR 结果存为该书的思绪卡片
                viewModel.saveNote(pendingOcrText, "来自悬浮窗的截图识别")
            } else {
                // 如果没有选中书籍，则自动创建书籍
                val book = Book(
                    title = pendingOcrText.take(50).trim().lines().firstOrNull { it.length in 2..50 } ?: "OCR 识别",
                    author = "OCR 识别",
                    coverResName = "ic_ocr_book",
                    summaryText = pendingOcrText.take(200),
                    category = "截图识别",
                    type = "FLOATING",
                    totalPages = 0,
                    progress = 0f
                )
                viewModel.createBook(book) { newBookId ->
                    viewModel.selectBook(newBookId)
                }
            }
            onOcrHandled()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // 书籍列表页面
            showBookList -> {
                BookListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.setShowBookList(false) }
                )
            }
            // 书籍创建流程
            isCreatingBook -> {
                BookCreationScreen(viewModel = viewModel)
            }
            // 没有选中书籍 → 仪表盘
            currentBookId == null -> {
                DashboardScreen(viewModel = viewModel)
            }
            // 已选中书籍 → 阅读器
            else -> {
                ReaderScreen(viewModel = viewModel)
            }
        }
    }
}

package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Book
import com.example.presentation.creation.BookEditDialog
import com.example.presentation.viewmodel.SmartReadViewModel

/**
 * 书籍列表页面
 * 展示所有通过 OCR 截图创建的书籍，支持点击进入阅读
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: SmartReadViewModel,
    onNavigateBack: () -> Unit
) {
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    var showOcrDialog by remember { mutableStateOf(false) }
    var pendingOcrText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "我的书籍",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (books.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有书籍",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "在其他应用界面点击悬浮球截图识别文字\n即可自动创建书籍",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(books) { book ->
                    BookListItem(
                        book = book,
                        onClick = {
                            viewModel.selectBook(book.id)
                        },
                        onDelete = {
                            viewModel.deleteBook(book)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // OCR 创建书籍对话框
    if (showOcrDialog && pendingOcrText.isNotEmpty()) {
        BookEditDialog(
            ocrText = pendingOcrText,
            onConfirm = { title ->
                // 创建书籍
                val book = Book(
                    title = title,
                    author = "OCR 识别",
                    coverResName = "ic_ocr_book",
                    summaryText = pendingOcrText.take(200),
                    category = "截图识别",
                    type = "FLOATING",
                    totalPages = 0,
                    progress = 0f
                )
                viewModel.createBook(book) { bookId ->
                    viewModel.selectBook(bookId)
                }
                showOcrDialog = false
                pendingOcrText = ""
            },
            onDismiss = {
                showOcrDialog = false
                pendingOcrText = ""
            }
        )
    }
}

@Composable
private fun BookListItem(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面占位
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.title.take(1),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.author,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.summaryText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.summaryText.take(80) + if (book.summaryText.length > 80) "..." else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

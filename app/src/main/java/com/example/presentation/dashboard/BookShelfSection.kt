package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.Book
import com.example.ui.components.EmptyStateView
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookShelfView(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onAddBookClick: () -> Unit = {},
    onDeleteBook: (Book) -> Unit = {},
    selectionMode: Boolean = false,
    selectedBooks: Set<Int> = emptySet(),
    onToggleSelection: (Book) -> Unit = {},
    onEnterSelection: (Book) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
    ) {
        Text(
            text = "藏书阁 Exquisite Bookshelf",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Text(
            text = "点击经典，长按进入选择模式。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (books.isEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            EmptyStateView(
                icon = Icons.Default.Warning,
                title = "智能书架目前为空",
                subtitle = "点击下方按钮添加你的第一本书籍。"
            )
            Spacer(modifier = Modifier.height(24.dp))
            AddBookCard(onClick = onAddBookClick)
        } else {
            // 添加新书卡片（最顶部）
            if (!selectionMode) {
                AddBookCard(onClick = onAddBookClick)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 新书在旧书上面（倒序显示）
            books.reversed().forEach { book ->
                BookCard(
                    book = book,
                    onClick = {
                        if (selectionMode) onToggleSelection(book)
                        else onBookClick(book)
                    },
                    onLongClick = { onEnterSelection(book) },
                    isSelected = selectedBooks.contains(book.id),
                    selectionMode = selectionMode
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    selectionMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .testTag("book_card_${book.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (selectionMode) {
                Box(
                    modifier = Modifier.size(28.dp).padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (!isSelected) Modifier.let { Modifier.background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                ) } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Done, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            BookCover(book = book)
            Spacer(modifier = Modifier.width(16.dp))
            BookDetails(book = book, showClickHint = !selectionMode)
        }
    }
}

@Composable
private fun BookCover(book: Book) {
    val coverFile = book.coverUri?.let { File(it) }
    val hasCover = coverFile != null && coverFile.exists() && coverFile.length() > 0

    Box(
        modifier = Modifier
            .size(width = 85.dp, height = 115.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = when (book.id % 3) {
                        1 -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        2 -> listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                        else -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasCover) {
            AsyncImage(
                model = coverFile,
                contentDescription = "封面",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(modifier = Modifier.padding(6.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Book, "Book", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(book.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BookDetails(book: Book, showClickHint: Boolean = true) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text(book.category, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
            if (showClickHint) {
                Text("点击开启阅读", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(book.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(book.summaryText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { book.progress }, modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("${(book.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
        }
    }
}

@Composable
private fun AddBookCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(width = 85.dp, height = 115.dp).clip(RoundedCornerShape(6.dp)).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("添加新书", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                Spacer(modifier = Modifier.height(4.dp))
                Text("上传文档或创建悬浮阅读窗", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Text("+ 创建书籍", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun BookCardPreview() {
    MaterialTheme {
        BookCard(book = Book(id = 1, title = "苏格拉底的申辩", author = "柏拉图", category = "西方哲学", summaryText = "柏拉图的早期对话录。", progress = 0.65f, coverResName = "", type = "DEMO_TEXT"), onClick = {}, onLongClick = {}, isSelected = false, selectionMode = false)
    }
}
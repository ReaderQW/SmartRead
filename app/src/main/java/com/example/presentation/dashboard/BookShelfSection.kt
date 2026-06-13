package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.model.Book
import com.example.ui.components.EmptyStateView

// --- TAB 0: Bookcase Grid View ---

/**
 * 书架视图入口。根据图书列表是否为空，展示空状态或图书列表。
 *
 * @param books 图书列表，为空时展示空状态，非空时渲染 [BookCard] 列表
 * @param onBookClick 点击某本图书时的回调，参数为被点击 of [Book]
 * @param onAddClick 点击添加新书按钮时的回调
 */
@Composable
fun BookShelfView(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onAddClick: () -> Unit
) {
    if (books.isEmpty()) {
        BookShelfEmptyState(onAddClick = onAddClick)
    } else {
        BookShelfListContent(books = books, onBookClick = onBookClick, onDeleteBook = onDeleteBook, onAddClick = onAddClick)
    }
}

/**
 * 书架空状态。显示提示图标和引导文案，引导用户调整搜索条件。
 */
@Composable
private fun BookShelfEmptyState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateView(
            icon = Icons.Default.Warning,
            title = "智能书架目前为空",
            subtitle = "在搜索框调整关键字，或点击下方按钮添加新书。"
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.Button(onClick = onAddClick) {
            Icon(Icons.Default.Book, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("添加新书 / 导入文档")
        }
    }
}

/**
 * 书架非空时的内容列表。包含标题头和每本图书的卡片。
 *
 * @param books 非空的图书列表
 * @param onBookClick 点击某本图书时的回调
 * @param onAddClick 点击添加新书按钮时的回调
 */
@Composable
private fun BookShelfListContent(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ... (Header Row remains same)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "藏书阁 Exquisite Bookshelf",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "点击经典，即可进入智能高亮、OCR截图与伴阅空间。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            androidx.compose.material3.IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.Book,
                    contentDescription = "Add Book",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        books.forEach { book ->
            BookCard(book = book, onClick = { onBookClick(book) }, onDelete = { onDeleteBook(book) })
        }
    }
}

/**
 * 单本图书的卡片组件。左侧为 [BookCover] 封面，右侧为 [BookDetails] 详情信息。
 *
 * @param book 要展示的图书数据
 * @param onClick 点击卡片时的回调
 */
@Composable
private fun BookCard(book: Book, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
            .testTag("book_card_${book.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Styled Book Cover Mockup
            BookCover(book = book)

            Spacer(modifier = Modifier.width(16.dp))

            // Book Infos
            BookDetails(book = book, onDelete = onDelete)
        }
    }
}

/**
 * 图书封面模拟块。使用渐变色背景 + 书本图标 + 标题文字的装饰性封面。
 * 颜色方案根据 book.id 在预置色板中选取。
 *
 * @param book 用于获取标题和 id 以决定颜色方案的图书数据
 */
@Composable
private fun BookCover(book: Book) {
    Box(
        modifier = Modifier
            .size(width = 85.dp, height = 115.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    colors = when (book.id % 3) {
                        1 -> listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                        2 -> listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.primary
                        )
                        else -> listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                )
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Book,
                contentDescription = "Book icon",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 图书详情信息区域。包含分类标签、书名、作者、简介摘要和阅读进度条。
 *
 * @param book 用于获取分类、标题、作者、简介和进度的图书数据
 */
@Composable
private fun BookDetails(book: Book, onDelete: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    book.category,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            androidx.compose.material3.IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Book",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            book.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
        Text(
            book.author,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = book.summaryText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(10.dp))
        // Progress bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { book.progress },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${(book.progress * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * 图书卡片预览。展示一本示例图书的 [BookCard] 渲染效果。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun BookCardPreview() {
    MaterialTheme {
        BookCard(
            book = Book(
                id = 1,
                title = "苏格拉底的申辩",
                author = "柏拉图",
                category = "西方哲学",
                summaryText = "柏拉图的早期对话录，记录了苏格拉底在雅典法庭上的自我辩护。面对不公正的指控，他以非凡的智慧和勇气捍卫真理。",
                progress = 0.65f,
                coverResName = "",
                type = "DEMO_TEXT"
            ),
            onClick = {},
            onDelete = {}
        )
    }
}

/**
 * 书架空状态预览。展示 [BookShelfEmptyState] 的渲染效果。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun BookShelfEmptyStatePreview() {
    MaterialTheme {
        BookShelfEmptyState(onAddClick = {})
    }
}

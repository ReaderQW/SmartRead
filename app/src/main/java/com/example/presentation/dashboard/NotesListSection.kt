package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.example.domain.model.Book
import com.example.domain.model.Note
import com.example.ui.components.EmptyStateView

/**
 * 笔记列表主视图入口
 *
 * 根据 selectedBookId 是否为空，显示不同的界面：
 * - 为空：显示书籍分类列表
 * - 不为空：显示该书籍下的思绪卡片
 *
 * @param notes 笔记数据列表
 * @param books 书籍数据列表
 * @param selectedBookId 当前选中的书籍 ID（用于笔记分类显示）
 * @param onSelectBook 选中书籍的回调
 * @param onDelete 删除笔记的回调
 */
@Composable
fun NotesListView(
    notes: List<Note>,
    books: List<Book>,
    selectedBookId: Int?,
    onSelectBook: (Int?) -> Unit,
    onDelete: (Note) -> Unit
) {
    if (selectedBookId == null) {
        NotesBookClassification(notes, books, onSelectBook)
    } else {
        val book = books.find { it.id == selectedBookId }
        val bookNotes = notes.filter { it.bookId == selectedBookId }
        NotesListContent(
            bookTitle = book?.title ?: "未知书籍",
            notes = bookNotes,
            onBack = { onSelectBook(null) },
            onDelete = onDelete
        )
    }
}

/**
 * 思想档案首页：按书籍分类显示
 */
@Composable
private fun NotesBookClassification(
    notes: List<Note>,
    books: List<Book>,
    onSelectBook: (Int?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "思存档案 Thought Archives",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "按书籍归纳的思想基因元。点击书籍进入查看对应的思绪卡片。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }

        val booksWithNotes = books.filter { book -> notes.any { it.bookId == book.id } }

        if (booksWithNotes.isEmpty()) {
            item {
                NotesEmptyState()
            }
        } else {
            items(booksWithNotes) { book ->
                val count = notes.count { it.bookId == book.id }
                BookNoteFolderCard(book, count, onClick = { onSelectBook(book.id) })
            }
        }
    }
}

/**
 * 笔记列表空状态视图
 */
@Composable
private fun NotesEmptyState() {
    EmptyStateView(
        icon = Icons.Default.Warning,
        title = "尚未记录思辨卡片",
        subtitle = "在经典阅读器中长按选句、撰写感悟并保存，即可在此呈现AI极速剖析摘要。"
    )
}

/**
 * 书籍笔记文件夹卡片
 */
@Composable
private fun BookNoteFolderCard(book: Book, noteCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${book.author} · ${noteCount}条思绪卡片",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 笔记列表内容区域
 */
@Composable
private fun NotesListContent(
    bookTitle: String,
    notes: List<Note>,
    onBack: () -> Unit,
    onDelete: (Note) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = bookTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "思绪卡片 · 共 ${notes.size} 条",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        if (notes.isEmpty()) {
            NotesEmptyState()
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(notes) { note ->
                    NoteCard(note = note, primaryColor = primaryColor, onDelete = onDelete)
                }
            }
        }
    }
}

/**
 * 单个笔记卡片组件
 *
 * 展示一条完整的笔记信息，包括：
 * - 标签列表
 * - 删除按钮
 * - 原文引用（带左侧装饰线）
 * - 用户感悟
 * - AI 智能摘要（如果存在）
 *
 * @param note 要展示的笔记对象
 * @param primaryColor 主题主色，用于绘制左侧装饰线
 * @param onDelete 删除笔记的回调函数，参数为当前笔记对象
 */
@Composable
private fun NoteCard(note: Note, primaryColor: Color, onDelete: (Note) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("note_card_${note.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    note.tags.split(",").forEach { tag ->
                        val trimmed = tag.trim()
                        if (trimmed.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    trimmed,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                IconButton(
                    onClick = { onDelete(note) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Note",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .drawBehind {
                        drawLine(
                            color = primaryColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "“${note.originalText}”",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    style = TextStyle(lineHeight = 18.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "您的感悟：",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = note.userNote,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            note.aiSummary?.let { insight ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Insight icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI智慧深透剖解",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            insight,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            style = TextStyle(lineHeight = 16.sp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 笔记卡片预览（用于 Android Studio 预览）
 *
 * 提供一个示例笔记数据，用于在开发时实时预览 NoteCard 的视觉效果。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun NoteCardPreview() {
    MaterialTheme {
        NoteCard(
            note = Note(
                id = 1,
                bookId = 1,
                originalText = "未经省察的生活是不值得度过的",
                userNote = "这句话提醒我要时常反思自己的选择和信念。苏格拉底的这句话穿越千年仍然振聋发聩。",
                aiSummary = "这句话体现了苏格拉底的核心哲学思想——反思性人生。在快节奏的现代生活中，人们往往被惯性驱使，很少停下来审视自己的价值观和选择。",
                tags = "苏格拉底,哲学,反思",
                aiStatus = "READY"
            ),
            primaryColor = Color(0xFF6750A4),
            onDelete = {}
        )
    }
}

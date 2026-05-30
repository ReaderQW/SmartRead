package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
import com.example.domain.model.Note
import com.example.ui.components.EmptyStateView

/**
 * 笔记列表主视图入口
 *
 * 根据笔记列表是否为空，显示不同的界面状态：
 * - 空列表：显示空状态提示
 * - 非空列表：显示笔记卡片列表
 *
 * @param notes 笔记数据列表
 * @param onDelete 删除笔记的回调函数，参数为要删除的笔记对象
 */
@Composable
fun NotesListView(notes: List<Note>, onDelete: (Note) -> Unit) {
    if (notes.isEmpty()) {
        NotesEmptyState()
    } else {
        NotesListContent(notes = notes, onDelete = onDelete)
    }
}

/**
 * 笔记列表空状态视图
 *
 * 当用户尚未创建任何笔记时显示的提示界面，
 * 包含图标、标题和引导性副标题。
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
 * 笔记列表内容区域
 *
 * 使用 LazyColumn 展示可滚动的笔记卡片列表，
 * 包含列表头部标题和说明文字。
 *
 * @param notes 笔记数据列表（非空）
 * @param onDelete 删除笔记的回调函数，参数为要删除的笔记对象
 */
@Composable
private fun NotesListContent(notes: List<Note>, onDelete: (Note) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "思绪卡片 Notebook Archives",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "在此可以回溯您摘录的黄金概念句，与您当时所作思考。AI智慧洞察亦将实时陪伴。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(notes) { note ->
            NoteCard(note = note, primaryColor = primaryColor, onDelete = onDelete)
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

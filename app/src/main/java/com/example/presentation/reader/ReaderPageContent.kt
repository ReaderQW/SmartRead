package com.example.presentation.reader

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Highlight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.ui.components.TagChip

@Composable
fun ReaderPageContent(
    pageContent: String,
    pageIndex: Int,
    currentBookId: Int?,
    highlights: List<Highlight>,
    onPhraseSelected: (String) -> Unit,
    onDeleteHighlight: (Highlight) -> Unit,
    onEditHighlight: (Highlight) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Book Title Heading decoration inside content
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SmartRead Immersive Engine",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
            Text(
                text = "Page ${pageIndex + 1}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        // Interactive paragraph text body
        Text(
            text = pageContent,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            style = TextStyle(lineHeight = 31.sp, letterSpacing = 1.sp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(12.dp))

        // Section of list of Highlights on this page
        val pHighlights = highlights.filter { it.pageIndex == pageIndex }
        if (pHighlights.isNotEmpty()) {
            Text(
                "本页高亮句子及批注：",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            pHighlights.forEach { hl ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            Color(
                                when (hl.colorHex) {
                                    "#FFEB3B" -> 0x7FFFE082
                                    "#69F0AE" -> 0x7FE8DEF8
                                    "#40C4FF" -> 0x7FD0BCFF
                                    else -> 0x7FF3EDF7
                                }
                            ), RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "“${hl.text}”",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                        hl.comment?.let { comment ->
                            Text(
                                text = "我的思考：$comment",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = { onEditHighlight(hl) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Add comment",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteHighlight(hl) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove highlight",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = "💡 高亮选句提示",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "在下方可以直接选定代表性的‘探询点’或句子，开启高亮或AI对话。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Preset sentence selector tags
            val presetPhrases = remember(pageIndex, currentBookId) {
                when (currentBookId) {
                    1 -> when (pageIndex) {
                        0 -> listOf("我深知自己一无所知", "理智的谦逊")
                        1 -> listOf("骏马身上的牛虻", "探讨智慧 and 灵魂")
                        2 -> listOf("未经省察的生活是不值得度过的", "天天省察自己")
                        else -> listOf("我去赴死，而你们生活", "灵魂的迁徙")
                    }
                    2 -> when (pageIndex) {
                        0 -> listOf("新方法与自然的挑战", "通过系统的经验积累")
                        1 -> listOf("四大偶像", "族类偶像和洞穴偶像")
                        else -> listOf("市场偶像", "剧场偶像")
                    }
                    else -> when (pageIndex) {
                        0 -> listOf("人是一支会思想的芦苇", "灵魂思考的尊贵")
                        else -> listOf("几何精神与敏感精神", "直觉与理性的交融")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetPhrases.forEach { text ->
                    TagChip(
                        text = text,
                        onClick = { onPhraseSelected(text) },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

package com.example.presentation.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SmartReadViewModel

// --- OCR Note Creation Dialog ---
@Composable
fun OcrNoteDialog(
    scannedOcrText: String?,
    onDismiss: () -> Unit,
    viewModel: SmartReadViewModel
) {
    val ocrRawPassage = scannedOcrText ?: return
    var draftUserThought by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveNote(ocrRawPassage, draftUserThought)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("确认存入思绪卡片", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("丢弃", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "ocr icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "ML Kit 截图字符识别成功",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "系统已通过数字镜头识别当前页面文字块：",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState())
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        ocrRawPassage,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    "请记录您的当期理解或批注心得（AI将根据该反思构建思想路径）：",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = draftUserThought,
                    onValueChange = { draftUserThought = it },
                    placeholder = {
                        Text(
                            "例如：这个类比启示了我对于真理的认识...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// --- Highlight Comment / Note Composer Dialog ---
@Composable
fun ReaderCommentDialog(
    isVisible: Boolean,
    chosenHighlightId: Int,
    initialCommentText: String,
    selectedText: String,
    onDismiss: () -> Unit,
    viewModel: SmartReadViewModel,
    onDeleteHighlight: (() -> Unit)? = null,
    initialAiSummary: String = ""
) {
    if (!isVisible) return

    var localThought by remember(initialCommentText) { mutableStateOf(initialCommentText) }
    var selectedColorHex by remember { mutableStateOf("#FFEB3B") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (chosenHighlightId > 0) {
                        // 编辑已有高亮：如果用户修改了感悟则更新 Note
                        if (localThought.isNotBlank()) {
                            viewModel.updateNoteForHighlight(chosenHighlightId, localThought)
                        }
                    } else if (localThought.isNotBlank()) {
                        viewModel.saveHighlightWithNote(selectedColorHex, selectedText, localThought)
                        viewModel.clearTextSelection()
                    } else {
                        viewModel.saveHighlight(colorHex = selectedColorHex)
                        viewModel.clearTextSelection()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "保存并在思维树中织入点",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp
                )
            }
        },
        dismissButton = {
            Row {
                if (onDeleteHighlight != null) {
                    TextButton(
                        onClick = {
                            onDeleteHighlight()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete highlight",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除批注", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "notes icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "添加即刻思忆 / 批注",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                Text(
                    "标注原句 passage：",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "“${selectedText.ifEmpty { "文本标注片段" }}”",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "高亮色彩：",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colorOptions = listOf(
                        "#FFEB3B" to Color(0xFFFFEB3B),
                        "#69F0AE" to Color(0xFF69F0AE),
                        "#40C4FF" to Color(0xFF40C4FF),
                        "#FF4081" to Color(0xFFFF4081)
                    )
                    colorOptions.forEach { (hex, colorValue) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(colorValue, CircleShape)
                                .border(
                                    2.dp,
                                    if (selectedColorHex == hex)
                                        MaterialTheme.colorScheme.onSurface
                                    else
                                        Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                        )
                    }
                }
                Text(
                    "您的感悟(Insight Input)：",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp
                )
                OutlinedTextField(
                    value = localThought,
                    onValueChange = { localThought = it },
                    placeholder = {
                        Text(
                            "记录该哲理或段落带给您的敏锐感悟或反驳方向...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
                if (initialAiSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "AI 智慧深透剖解：",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = initialAiSummary,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(10.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

package com.example.presentation
// 阅读思维盲盒报告弹窗 - 文艺手账风格升级版

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.ReadingReport
import com.example.presentation.viewmodel.ArtImageState
import com.example.presentation.viewmodel.SmartReadViewModel
import kotlin.math.cos
import kotlin.math.sin


// --- COGNITIVE BLIND BOX REPORT & RADAR DETAIL DIALOG ---
@Composable
fun ReadingReportDialog(
    report: ReadingReport,
    viewModel: SmartReadViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val artImageState by viewModel.artImageState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    val shareStr = buildShareText(report)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "SmartRead 思想基因卡")
                        putExtra(Intent.EXTRA_TEXT, shareStr)
                    }
                    context.startActivity(Intent.createChooser(intent, "导出心智盲盒档案"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享文本报告", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("继续探索", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CardGiftcard, contentDescription = "report icon", tint = primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("思想基因 · 盲盒鉴定", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            val bgBrush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    Color(0xFFFFF9C4).copy(alpha = 0.2f), // 温暖的治愈黄
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f)
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgBrush)
                    .padding(4.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "通过深度阅读与AI共鸣，您的思想维度在此刻具象化呈现：", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- 书籍信息区 ---
                if (report.bookAuthor.isNotEmpty() || report.bookSummary.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("《${report.bookTitle}》", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (report.bookAuthor.isNotEmpty()) {
                                    Text(" — ${report.bookAuthor}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            if (report.bookSummary.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = report.bookSummary,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    style = TextStyle(lineHeight = 16.sp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- RADAR CHART SECTION ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, primaryColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = 65.dp.toPx()

                        val angles = floatArrayOf(0f, 72f, 144f, 216f, 288f)
                        val angleRad = angles.map { it * (Math.PI / 180f).toFloat() }

                        // Background mesh
                        for (ring in 1..5) {
                            val curR = maxR * (ring / 5f)
                            val ringPath = Path().apply {
                                moveTo(cx + curR * cos(angleRad[0]), cy + curR * sin(angleRad[0]))
                                for (i in 1 until angleRad.size) {
                                    lineTo(cx + curR * cos(angleRad[i]), cy + curR * sin(angleRad[i]))
                                }
                                close()
                            }
                            drawPath(
                                path = ringPath,
                                color = primaryColor.copy(alpha = 0.08f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Data polygon
                        val values = floatArrayOf(
                            report.logic.toFloat(),
                            report.empathy.toFloat(),
                            report.critical.toFloat(),
                            report.width.toFloat(),
                            report.innovation.toFloat()
                        )
                        val scorePath = Path().apply {
                            val r0 = maxR * (values[0] / 100f)
                            moveTo(cx + r0 * cos(angleRad[0]), cy + r0 * sin(angleRad[0]))
                            for (i in 1 until values.size) {
                                val ri = maxR * (values[i] / 100f)
                                lineTo(cx + ri * cos(angleRad[i]), cy + ri * sin(angleRad[i]))
                            }
                            close()
                        }

                        drawPath(path = scorePath, color = primaryColor.copy(alpha = 0.25f))
                        drawPath(path = scorePath, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
                    }

                    // Floating Labels
                    LabelText("逻辑", Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-10).dp))
                    LabelText("创见", Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-8).dp))
                    LabelText("广博", Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 15.dp, y = (-15).dp))
                    LabelText("共鸣", Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 8.dp))
                    LabelText("批判", Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 15.dp, y = 15.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- 三维交互矩阵可视化 ---
                val matrix = report.interactionMatrix
                Text(
                    "三维交互矩阵",
                    color = primaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 划线语义
                MatrixDimensionCard(
                    icon = Icons.Default.Edit,
                    title = "划线语义",
                    color = Color(0xFF5C6BC0), // 靛蓝
                    totalCount = matrix.highlightSemantics.totalCount,
                    tags = matrix.highlightSemantics.keyConcepts,
                    description = matrix.highlightSemantics.emotionalTone,
                    excerpts = matrix.highlightSemantics.representativeExcerpts
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 笔记深度
                MatrixDimensionCard(
                    icon = Icons.Default.Style,
                    title = "笔记深度",
                    color = Color(0xFF26A69A), // 青绿
                    totalCount = matrix.noteDepth.totalCount,
                    tags = matrix.noteDepth.insightThemes,
                    description = "深度评分 ${matrix.noteDepth.depthScore}/100",
                    excerpts = matrix.noteDepth.representativeNotes
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 对话频率
                MatrixDimensionCard(
                    icon = Icons.Default.Forum,
                    title = "对话频率",
                    color = Color(0xFFEC407A), // 粉红
                    totalCount = matrix.dialogueFrequency.totalCount,
                    tags = matrix.dialogueFrequency.questionTypes,
                    description = "参与度 ${matrix.dialogueFrequency.engagementLevel}/100",
                    excerpts = matrix.dialogueFrequency.representativeDialogues
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- 精选划线摘录 ---
                if (report.highlightsList.isNotEmpty()) {
                    Text("✧ 精选划线摘录", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    report.highlightsList.take(4).forEach { excerpt ->
                        ExcerptCard(text = excerpt, color = Color(0xFFFFF176))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // --- 精选读书笔记 ---
                if (report.notesList.isNotEmpty()) {
                    Text("✧ 读书笔记精选", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    report.notesList.take(3).forEach { note ->
                        ExcerptCard(text = note, color = Color(0xFFA5D6A7))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // --- 精选对话摘录 ---
                if (report.chatExcerpts.isNotEmpty()) {
                    Text("✧ AI对话摘录", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    report.chatExcerpts.take(2).forEach { chat ->
                        ExcerptCard(text = chat, color = Color(0xFFCE93D8))
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // --- COGNITIVE INCREMENT ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TipsAndUpdates, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("核心认知增量", color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = report.cognitiveIncrement,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            style = TextStyle(lineHeight = 20.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- MOTTO QUOTE ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = primaryColor.copy(alpha = 0.3f), modifier = Modifier.size(24.dp))
                        Text(
                            text = report.motto,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(lineHeight = 22.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("—— 思想刻印", color = primaryColor.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- VIVO AI ART IMAGE SECTION ---
                Divider(color = primaryColor.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("灵魂共鸣 · 文艺手账长图", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    "基于阅读脉络与三维交互矩阵，由 vivo AI 生成文艺手账风格长图",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = artImageState) {
                    is ArtImageState.Idle -> {
                        Button(
                            onClick = { viewModel.generateArtImage() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("生成文艺手账长图", fontSize = 12.sp)
                        }
                    }
                    is ArtImageState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("vivo AI 正在绘制您的阅读手账...", fontSize = 11.sp, color = primaryColor)
                        }
                    }
                    is ArtImageState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = state.imageUrl,
                                contentDescription = "Generated Art Image",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "这是我在 SmartRead 阅读《${report.bookTitle}》生成的文艺手账长图：${state.imageUrl}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "分享我的艺术长图"))
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("点击图片分享您的阅读手账", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.imageUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("下载高清原图", fontSize = 11.sp)
                            }
                        }
                    }
                    is ArtImageState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("生成失败: ${state.message}", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            TextButton(onClick = { viewModel.generateArtImage() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("重新生成", fontSize = 11.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun LabelText(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

/**
 * 三维矩阵维度卡片
 */
@Composable
private fun MatrixDimensionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    color: Color,
    totalCount: Int,
    tags: List<String>,
    description: String,
    excerpts: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                Text("${totalCount}条", color = color.copy(alpha = 0.7f), fontSize = 10.sp)
            }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tags.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic
                )
            }
            if (excerpts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                excerpts.take(2).forEach { excerpt ->
                    Text(
                        text = "· $excerpt",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(lineHeight = 14.sp)
                    )
                }
            }
        }
    }
}

/**
 * 摘录卡片 - 模拟手账便签风格
 */
@Composable
private fun ExcerptCard(text: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontSize = 10.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(lineHeight = 14.sp)
        )
    }
}

/**
 * 构建分享文本
 */
private fun buildShareText(report: ReadingReport): String {
    val matrix = report.interactionMatrix
    return """
        【SmartRead 阅读盲盒报告】
        原著：《${report.bookTitle}》${if (report.bookAuthor.isNotEmpty()) " — ${report.bookAuthor}" else ""}

        【五维思想基因】
        - 批判性思考：${report.critical}/100
        - 逻辑严谨度：${report.logic}/100
        - 认识创新性：${report.innovation}/100
        - 认知关联广度：${report.width}/100
        - 思想情感共鸣：${report.empathy}/100

        【三维交互矩阵】
        📝 划线语义：${matrix.highlightSemantics.totalCount}条
          关注概念：${matrix.highlightSemantics.keyConcepts.joinToString("、")}
        📖 笔记深度：${matrix.noteDepth.totalCount}条（深度${matrix.noteDepth.depthScore}分）
          洞察主题：${matrix.noteDepth.insightThemes.joinToString("、")}
        💬 对话频率：${matrix.dialogueFrequency.totalCount}次（参与度${matrix.dialogueFrequency.engagementLevel}分）
          问题类型：${matrix.dialogueFrequency.questionTypes.joinToString("、")}

        💡【认知增量】
        ${report.cognitiveIncrement}
        📜【精神烙印】
        ${report.motto}
        —— 来自 SmartRead 伴读脑图与AIGC仪式感鉴定模块。
    """.trimIndent()
}

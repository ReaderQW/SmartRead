package com.example.presentation
// 阅读思维盲盒报告弹窗 - 仪式感升级版

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ReadingReport
import kotlin.math.cos
import kotlin.math.sin


// --- COGNITIVE BLIND BOX REPORT & RADAR DETAIL DIALOG ---
@Composable
fun ReadingReportDialog(
    report: ReadingReport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    val shareStr = """
                        【SmartRead 学术思想基因鉴定】
                        原著：《${report.bookTitle}》

                        - 批判性思考得分：${report.critical}/100
                        - 逻辑严谨度得分：${report.logic}/100
                        - 认识创新性得分：${report.innovation}/100
                        - 认知关联广度：${report.width}/100
                        - 思想情感共鸣：${report.empathy}/100

                        💡【思辨认知增量】：
                        ${report.cognitiveIncrement}
                        📜【精神烙印名言】：
                        ${report.motto}
                        —— 来自 SmartRead 伴读脑图与AIGC终期仪式感鉴定模块。
                    """.trimIndent()
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
                Icon(Icons.Default.AutoAwesome, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("分享这枚思想果实", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "通过深度阅读与AI共鸣，您的思想维度在此刻具象化呈现：", 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                    LabelText("逻辑", Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp))
                    LabelText("创见", Modifier.align(Alignment.BottomCenter).offset(y = (-8).dp))
                    LabelText("广博", Modifier.align(Alignment.BottomStart).offset(x = 15.dp, y = (-15).dp))
                    LabelText("共鸣", Modifier.align(Alignment.TopCenter).offset(y = 8.dp))
                    LabelText("批判", Modifier.align(Alignment.TopStart).offset(x = 15.dp, y = 15.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

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

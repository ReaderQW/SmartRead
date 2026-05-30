package com.example.presentation
// 阅读思维盲盒报告弹窗

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
import androidx.compose.ui.geometry.Offset
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
                    // Export and Share long receipt
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出并分享这枚心智盲盒", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("返回书海", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "report icon", tint = primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SmartRead 阅读思维盲盒", color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontFamily = FontFamily.Serif)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("基于您在本期原著划线、笔记撰写、和与有批判性的苏格拉底式对话汇总分析，为您定制心智思想雷达：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(12.dp))

                // Custom Canvas Dynamic 5-axis Radar Chart!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = 60.dp.toPx()

                        // Calculated Angles: 5 axis pentagon
                        val angles = floatArrayOf(0f, 72f, 144f, 216f, 288f)
                        val angleRad = angles.map { it * (Math.PI / 180f).toFloat() }

                        // Outer concentric wireframes
                        for (ring in 1..4) {
                            val curR = maxR * (ring / 4f)
                            val ringPath = Path().apply {
                                val sX = cx + curR * cos(angleRad[0])
                                val sY = cy + curR * sin(angleRad[0])
                                moveTo(sX, sY)
                                for (i in 1..4) {
                                    val x = cx + curR * cos(angleRad[i])
                                    val y = cy + curR * sin(angleRad[i])
                                    lineTo(x, y)
                                }
                                close()
                            }
                            drawPath(
                                path = ringPath,
                                color = primaryColor.copy(alpha = 0.1f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Coordinates axis lines
                        angleRad.forEach { rad ->
                            drawLine(
                                color = primaryColor.copy(alpha = 0.15f),
                                start = Offset(cx, cy),
                                end = Offset(cx + maxR * cos(rad), cy + maxR * sin(rad)),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Plotting user indices path
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
                            for (i in 1..4) {
                                val ri = maxR * (values[i] / 100f)
                                lineTo(cx + ri * cos(angleRad[i]), cy + ri * sin(angleRad[i]))
                            }
                            close()
                        }

                        // Color fill score path
                        drawPath(
                            path = scorePath,
                            color = Color(0x336750A4)
                        )
                        drawPath(
                            path = scorePath,
                            color = primaryColor,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Labels on axis
                    Text("逻辑度(${report.logic})", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp))
                    Text("创见力(${report.innovation})", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-5).dp))
                    Text("广博度(${report.width})", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = 10.dp))
                    Text("同理心(${report.empathy})", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = 5.dp))
                    Text("批判度(${report.critical})", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cognitive growth descriptions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row {
                            Icon(Icons.Default.Star, contentDescription = "grain", tint = primaryColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("核心认知增量分析 Cognitive Growth：", color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report.cognitiveIncrement,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            style = TextStyle(lineHeight = 18.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Personalized philosophic quote matching
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("【书友刻印 · 思想灯塔古训】", color = Color(0xFFBF360C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${report.motto}”",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            style = TextStyle(lineHeight = 19.sp)
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

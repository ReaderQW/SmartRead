package com.example.presentation.dashboard
// Canvas 知识图谱

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import kotlin.math.cos
import kotlin.math.sin

// --- TAB 2: Dynamic Canvas Interactive Mind map Graph ---
@Composable
fun KnowledgeGraphView(
    nodes: List<KnowledgeNode>,
    edges: List<KnowledgeEdge>
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }
    var selectedNodeInfo by remember { mutableStateOf<KnowledgeNode?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary

    // Assign interactive physics-like center layouts for books
    val mockPositions = remember(nodes) {
        val widthSim = 1000f
        val heightSim = 750f
        val positionsMap = mutableMapOf<String, Offset>()

        // Circular disperse layouts
        nodes.forEachIndexed { i, node ->
            val angle = (i * 360f / nodes.size.coerceAtLeast(1)) * (Math.PI / 180f)
            val radius = when (node.category) {
                "Book" -> 80f
                "Concept" -> 190f
                "Note" -> 280f
                else -> 120f
            }
            val cx = (widthSim / 2f) + radius * cos(angle).toFloat()
            val cy = (heightSim / 2f) + radius * sin(angle).toFloat()
            positionsMap[node.id] = Offset(cx, cy)
        }
        positionsMap
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("mind_graph_canvas")
    ) {
        // Draw links & nodes via Jetpack Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerScreenX = size.width / 2f + offsetX
            val centerScreenY = size.height / 2f + offsetY

            // Draw line edges first
            edges.forEach { edge ->
                val startPos = mockPositions[edge.source] ?: Offset.Zero
                val endPos = mockPositions[edge.target] ?: Offset.Zero

                if (startPos != Offset.Zero && endPos != Offset.Zero) {
                    val sPix = Offset(
                        (startPos.x - 500f) * scale + centerScreenX,
                        (startPos.y - 375f) * scale + centerScreenY
                    )
                    val ePix = Offset(
                        (endPos.x - 500f) * scale + centerScreenX,
                        (endPos.y - 375f) * scale + centerScreenY
                    )

                    drawLine(
                        color = primaryColor.copy(alpha = 0.25f),
                        start = sPix,
                        end = ePix,
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                }
            }

            // Draw vector node structures
            nodes.forEach { node ->
                val pos = mockPositions[node.id] ?: Offset.Zero
                if (pos != Offset.Zero) {
                    val pix = Offset(
                        (pos.x - 500f) * scale + centerScreenX,
                        (pos.y - 375f) * scale + centerScreenY
                    )

                    val radius = when (node.category) {
                        "Book" -> 24.dp.toPx()
                        "Concept" -> 16.dp.toPx()
                        "Note" -> 14.dp.toPx()
                        else -> 12.dp.toPx()
                    }

                    val color = when (node.category) {
                        "Book" -> primaryColor
                        "Concept" -> Color(0xFFFFB300)
                        "Note" -> Color(0xFFE91E63)
                        else -> Color(0xFF625B71)
                    }

                    // Outer glowing shell
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = radius + 6.dp.toPx(),
                        center = pix
                    )

                    // Core solid
                    drawCircle(
                        color = color,
                        radius = radius,
                        center = pix
                    )

                    // Drawing beautiful borders for the circles
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = radius,
                        center = pix,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        // Overlay click intercept labels
        mockPositions.forEach { (nodeId, offset) ->
            val nodeObj = nodes.find { it.id == nodeId } ?: return@forEach

            Box(
                modifier = Modifier
                    .offset(
                        x = ((offset.x - 500f) * scale + offsetX + 100).dp,
                        y = ((offset.y - 375f) * scale + offsetY + 110).dp
                    )
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .clickable { selectedNodeInfo = nodeObj }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = nodeObj.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Left top layout description
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                "思想基因图谱",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "全景展示读书、感悟、批注等知识元关系",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("原著中心", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB300), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("反思概念", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE91E63), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("笔记档案", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp)
            }
            Text(
                "提示：双手指腹可平移图谱。点击卡词获取洞察。",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Details floating pop-up card
        selectedNodeInfo?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "思想基因元：${node.label}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif
                        )
                        IconButton(
                            onClick = { selectedNodeInfo = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "归属分类: ${node.category}  |  连接强度: ${node.size * 10}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在您阅读期间，AI辅助思想思想基因引擎提取该原子标记，并将其锚定在您的自动思想基因图谱中，用以追踪和整合关于本期经典的全部思辨关联。这是一个端到端完全连接的关系元宇宙。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

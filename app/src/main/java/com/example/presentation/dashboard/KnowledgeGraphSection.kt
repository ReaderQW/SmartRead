package com.example.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.ui.components.EmptyStateView
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ---- 物理引擎参数 (基于真实世界坐标系，而非归一化 0..1) ----
private const val REPULSION_FORCE = 80000f      // 节点排斥力（越大越分散）
private const val SPRING_FORCE = 0.003f         // 弹簧引力系数（越小拉力越弱，节点更分散）
private const val SPRING_LENGTH = 500f          // 边理想长度
private const val CLUSTER_FORCE = 0.001f        // 同类节点聚类力（减弱，避免同类抱团）
private const val GRAVITY = 0.003f              // 向心力（大幅减弱，让节点自由扩散）
private const val DAMPING = 0.75f               // 速度衰减（0..1，越小越快稳定）
private const val MIN_ENERGY = 0.1f             // 停止模拟的最小能量阈值

/**
 * 内部物理节点模型。用于力导向模拟的实体对象。
 *
 * 所有字段均为可变 [var]，直接读写而不经过 Compose State 快照，
 * 避免每帧更新触发整个重组流程（[mutableStateOf] 会导致严重卡顿）。
 * 位置更新后通过 [renderTrigger] 信号量通知 Canvas 重绘。
 *
 * @property node 对应的领域知识节点
 * @property x 世界坐标系 X 轴位置
 * @property y 世界坐标系 Y 轴位置
 * @property vx X 轴速度分量
 * @property vy Y 轴速度分量
 * @property radius 渲染半径（单位：像素 px）
 * @property color 节点渲染颜色
 */
private class PhysicsNode(
    val node: KnowledgeNode,
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val radius: Float = 10f,
    val color: Color = Color.Gray
)

/**
 * 知识图谱视图。以 Canvas 绘制力导向图，支持拖拽平移、双指缩放和点击查看节点详情。
 *
 * 本组件用于 Dashboard 的"思想基因图"Tab。布局分为两阶段：
 * 1. 圆形点阵初始化：节点根据度数大小和类别颜色分布在半径为 [startRadius] 的圆上
 * 2. 力导向物理模拟：每帧运算库仑斥力 + 弹簧引力 + 向心力，直至系统动能低于阈值
 *
 * 所有渲染使用原生 Canvas API（[drawCircle]、[drawText] 等），
 * 标签文字通过 [TextMeasurer] 直接绘制在 Canvas 上，避免 Box 悬浮层的性能开销。
 *
 * @param nodes 知识图谱中的节点列表。包含 id、label、category、size 等属性。
 *              支持类别: "Book"(蓝色)、"Concept"(琥珀色)、"Note"(玫红)、"Mindset"(绿色)。
 *              为空时显示引导空状态。
 * @param edges 节点间的关联边列表。每条边包含 source、target、relation 和 weight，
 *              weight 影响弹簧强度和线条粗细。
 */
@Composable
fun KnowledgeGraphView(
    nodes: List<KnowledgeNode>,
    edges: List<KnowledgeEdge>
) {
    if (nodes.isEmpty()) {
        EmptyStateView(
            title = "思想基因图谱为空",
            subtitle = "开始阅读、高亮和记笔记，AI 将自动为您构建知识网络"
        )
        return
    }

    // 交互状态
    var pan by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var selectedNodeInfo by remember { mutableStateOf<KnowledgeNode?>(null) }
    var isLegendVisible by remember { mutableStateOf(true) }
    var showLabels by remember { mutableStateOf(true) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // 聚焦高亮：选中节点时，与其无直接连线的节点和边变暗；再次点击取消聚焦
    var focusNodeId by remember { mutableStateOf<String?>(null) }
    val connectedNodeIds = remember(focusNodeId, edges) {
        if (focusNodeId == null) emptySet()
        else {
            buildSet {
                add(focusNodeId)
                edges.forEach { edge ->
                    if (edge.source == focusNodeId) add(edge.target)
                    if (edge.target == focusNodeId) add(edge.source)
                }
            }
        }
    }

    val bgColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // 使用普通 List 存储物理状态，避免 StateMap 频繁重组导致的严重卡顿
    val physicsNodes = remember { mutableListOf<PhysicsNode>() }
    // 触发画布重绘的信号量
    var renderTrigger by remember { mutableIntStateOf(0) }

    // 阶段1：初始化圆形点阵布局
    LaunchedEffect(nodes, edges) {
        physicsNodes.clear()

        // 计算每个节点的度（连接数）以决定大小
        val degreeMap = mutableMapOf<String, Int>()
        edges.forEach { edge ->
            degreeMap[edge.source] = (degreeMap[edge.source] ?: 0) + 1
            degreeMap[edge.target] = (degreeMap[edge.target] ?: 0) + 1
        }

        nodes.forEachIndexed { i, node ->
            val angle = (i * 360f / nodes.size) * (Math.PI / 180f)
            val startRadius = 600f // 初始分布半径

            val degree = degreeMap[node.id] ?: 0
            val baseRadiusDp = when (node.category) {
                "Book" -> 22f
                "Concept" -> 14f
                "Note" -> 12f
                "Mindset" -> 16f
                else -> 10f
            }
            // 节点半径：基础大小 + 连接度加成
            val radiusPx = with(density) { (baseRadiusDp + degree * 1.5f).coerceAtMost(40f).dp.toPx() }

            val color = when (node.category) {
                "Book" -> Color(0xFF6750A4)
                "Concept" -> Color(0xFFFFB300)
                "Note" -> Color(0xFFE91E63)
                "Mindset" -> Color(0xFF4CAF50)
                else -> Color(0xFF625B71)
            }

            physicsNodes.add(
                PhysicsNode(
                    node = node,
                    x = (startRadius * cos(angle)).toFloat(),
                    y = (startRadius * sin(angle)).toFloat(),
                    radius = radiusPx,
                    color = color
                )
            )
        }
        renderTrigger++
    }

    // 阶段2：力导向物理模拟（每帧 2 步，用 vsync 同步展示扩散动画）
    LaunchedEffect(nodes, edges) {
        if (physicsNodes.isEmpty()) return@LaunchedEffect

        var totalEnergy = Float.MAX_VALUE

        while (isActive && totalEnergy >= MIN_ENERGY) {
            // 每帧跑 2 步模拟，平衡动画流畅度与扩散总时长
            for (step in 0 until 2) {
                totalEnergy = 0f

                // 1. 库仑排斥力
                for (i in physicsNodes.indices) {
                    val n1 = physicsNodes[i]
                    for (j in i + 1 until physicsNodes.size) {
                        val n2 = physicsNodes[j]
                        val dx = n1.x - n2.x
                        val dy = n1.y - n2.y
                        var distSq = dx * dx + dy * dy
                        if (distSq < 1f) distSq = 1f
                        val dist = sqrt(distSq)
                        val force = REPULSION_FORCE / distSq
                        val fx = force * (dx / dist)
                        val fy = force * (dy / dist)
                        n1.vx += fx; n1.vy += fy
                        n2.vx -= fx; n2.vy -= fy
                    }
                }

                // 2. 胡克弹簧力
                edges.forEach { edge ->
                    val n1 = physicsNodes.find { it.node.id == edge.source } ?: return@forEach
                    val n2 = physicsNodes.find { it.node.id == edge.target } ?: return@forEach
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val displacement = dist - SPRING_LENGTH
                    val force = displacement * SPRING_FORCE * (edge.weight ?: 1.0f)
                    val fx = force * (dx / dist)
                    val fy = force * (dy / dist)
                    n1.vx += fx; n1.vy += fy
                    n2.vx -= fx; n2.vy -= fy
                }

                // 3. 向心力与位置更新
                physicsNodes.forEach { node ->
                    node.vx -= node.x * GRAVITY
                    node.vy -= node.y * GRAVITY
                    node.vx *= DAMPING
                    node.vy *= DAMPING
                    node.x += node.vx
                    node.y += node.vy
                    totalEnergy += node.vx * node.vx + node.vy * node.vy
                }

                if (totalEnergy < MIN_ENERGY) break
            }

            // 触发重绘 + 等待下一帧 vsync（约 16ms @60fps）
            renderTrigger++
            withFrameNanos { }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { viewSize = it }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panDelta, zoomDelta, _ ->
                        pan += panDelta
                        zoom = (zoom * zoomDelta).coerceIn(0.2f, 4.0f)
                    }
                }
                .pointerInput(nodes) { // 用 nodes 为 key（不随动画变化），手势不被打断
                    detectTapGestures { tapOffset ->
                        val viewW = viewSize.width / 2f
                        val viewH = viewSize.height / 2f

                        // 倒推点击位置在世界坐标系中的位置
                        val worldClickX = (tapOffset.x - pan.x - viewW) / zoom
                        val worldClickY = (tapOffset.y - pan.y - viewH) / zoom

                        for (pNode in physicsNodes.reversed()) { // 后画的在最上层，先判断
                            val dx = pNode.x - worldClickX
                            val dy = pNode.y - worldClickY
                            val dist = sqrt(dx * dx + dy * dy)
                            // 增加 15px 的点击热区容错
                            if (dist <= pNode.radius / zoom + 15f) {
                                selectedNodeInfo = pNode.node
                                // 切换聚焦：再次点击同一节点取消聚焦
                                focusNodeId = if (focusNodeId == pNode.node.id) null else pNode.node.id
                                return@detectTapGestures
                            }
                        }
                        selectedNodeInfo = null // 点击空白处取消选中
                        focusNodeId = null // 同时取消聚焦
                    }
                }
        ) {
            // 确保依赖读取 renderTrigger 触发 Canvas 重绘
            val currentTrigger = renderTrigger
            if (currentTrigger < 0) return@Canvas

            val centerScreenX = size.width / 2f + pan.x
            val centerScreenY = size.height / 2f + pan.y

            // --- 1. 绘制边 ---
            edges.forEach { edge ->
                val n1 = physicsNodes.find { it.node.id == edge.source } ?: return@forEach
                val n2 = physicsNodes.find { it.node.id == edge.target } ?: return@forEach

                val start = Offset(
                    n1.x * zoom + centerScreenX,
                    n1.y * zoom + centerScreenY
                )
                val end = Offset(
                    n2.x * zoom + centerScreenX,
                    n2.y * zoom + centerScreenY
                )

                val weight = edge.weight ?: 1.0f
                val edgeIsDimmed = focusNodeId != null &&
                    edge.source != focusNodeId && edge.target != focusNodeId
                val edgeAlpha = if (edgeIsDimmed) 0.03f
                    else (0.15f + (weight * 0.05f).coerceAtMost(0.3f))
                drawLine(
                    color = Color(0xFF79747E).copy(alpha = edgeAlpha),
                    start = start,
                    end = end,
                    strokeWidth = (1.5f + weight).dp.toPx() * zoom,
                    // 移除虚线，实线在复杂图谱中性能更好且更美观
                )
            }

            // --- 2. 绘制节点 ---
            physicsNodes.forEach { pNode ->
                val pos = Offset(
                    pNode.x * zoom + centerScreenX,
                    pNode.y * zoom + centerScreenY
                )
                val currentRadius = pNode.radius * zoom

                // 聚焦变暗判定：聚焦模式下不在连通集合中的节点变暗
                val nodeDimmed = focusNodeId != null && pNode.node.id !in connectedNodeIds
                val nodeAlpha = if (nodeDimmed) 0.12f else 1f
                val labelAlpha = if (nodeDimmed) 0.15f else 1f

                // 外发光
                drawCircle(
                    color = pNode.color.copy(alpha = 0.2f * nodeAlpha),
                    radius = currentRadius + 8.dp.toPx() * zoom,
                    center = pos
                )
                // 核心
                drawCircle(
                    color = pNode.color.copy(alpha = nodeAlpha),
                    radius = currentRadius,
                    center = pos
                )
                // 描边
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * nodeAlpha),
                    radius = currentRadius,
                    center = pos,
                    style = Stroke(width = 1.5.dp.toPx() * zoom)
                )

                // --- 3. 绘制文字 (原生 Canvas 绘制，性能远超 Box 悬浮) ---
                if (showLabels && zoom > 0.4f) { // 缩放太小时隐藏文字防重叠
                    val textLayoutResult = textMeasurer.measure(
                        text = pNode.node.label,
                        style = TextStyle(
                            color = onSurfaceColor,
                            fontSize = (10 * zoom).coerceIn(8f, 16f).sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    val textWidth = textLayoutResult.size.width
                    val textHeight = textLayoutResult.size.height

                    // 将文字放置在节点正下方
                    val textTopLeft = Offset(
                        pos.x - textWidth / 2f,
                        pos.y + currentRadius + 4.dp.toPx() * zoom
                    )

                    // 绘制文字半透明背景底色，增加可读性（变暗的节点背景也变淡）
                    drawRoundRect(
                        color = surfaceColor.copy(alpha = 0.85f * labelAlpha),
                        topLeft = Offset(textTopLeft.x - 8f, textTopLeft.y - 4f),
                        size = Size(textWidth + 16f, textHeight + 8f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = textTopLeft
                    )
                }
            }
        }

        // --- UI 层 ---
        if (isLegendVisible) {
            GraphLegendPanel(onDismiss = { isLegendVisible = false })
        }

        selectedNodeInfo?.let { node ->
            KnowledgeGraphNodeDetailCard(
                node = node,
                onDismiss = { selectedNodeInfo = null }
            )
        }

        ZoomControls(
            scale = zoom,
            showLabels = showLabels,
            onToggleLabels = { showLabels = !showLabels },
            onZoomIn = { zoom = (zoom * 1.3f).coerceAtMost(4.0f) },
            onZoomOut = { zoom = (zoom / 1.3f).coerceAtLeast(0.2f) },
            onReset = { zoom = 1f; pan = Offset.Zero }
        )
    }
}

// ----------------------------------------------------------------------
// 下方保留您原有的 UI 组件 (图例、底部卡片、按钮)，代码保持基本一致，只略微修改颜色适应深色
// ----------------------------------------------------------------------

/**
 * 图谱标题和图例面板。固定在左上角，展示图谱标题、四类节点的颜色图例。
 *
 * 点击面板任意位置可将其收起，为图谱腾出更多可视空间。
 * 图例颜色对应关系：
 * - 原著 → PrimaryColor
 * - 概念 → 琥珀色 [#FFB300]
 * - 笔记 → 玫红 [#E91E63]
 * - 范式 → 绿色 [#4CAF50]
 *
 * @param onDismiss 点击面板时的收起回调，调用后面板消失
 */
@Composable
private fun BoxScope.GraphLegendPanel(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
            .clickable { onDismiss() }
            .padding(16.dp)
    ) {
        Text("思想基因图谱", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text("全景展示读书、感悟、批注等知识元关系", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(MaterialTheme.colorScheme.primary); Spacer(Modifier.width(4.dp)); Text("原著", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Spacer(Modifier.width(12.dp))
            LegendDot(Color(0xFFFFB300)); Spacer(Modifier.width(4.dp)); Text("概念", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Spacer(Modifier.width(12.dp))
            LegendDot(Color(0xFFE91E63)); Spacer(Modifier.width(4.dp)); Text("笔记", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp); Spacer(Modifier.width(12.dp))
            LegendDot(Color(0xFF4CAF50)); Spacer(Modifier.width(4.dp)); Text("范式", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

/**
 * 图例中的颜色圆点。
 *
 * @param color 圆点的填充颜色
 */
@Composable
private fun LegendDot(color: Color) {
    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
}

/**
 * 节点详情卡片。展示被点击节点的标签、分类、连接权重及思辨说明。
 *
 * 固定在底部居中显示，全宽半透明卡片。包含关闭按钮。
 *
 * @param node 被选中的知识节点，显示其 label、category、size 等属性
 * @param onDismiss 关闭卡片时的回调
 */
@Composable
private fun BoxScope.KnowledgeGraphNodeDetailCard(node: KnowledgeNode, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("思想基因元：${node.label}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("归属分类: ${node.category}  |  连接权重: ${(node.size * 10).toInt()}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("在您阅读期间，AI 辅助思想基因引擎提取该原子标记，并将其锚定在自动思想基因图谱中，追踪关于本期经典的全部思辨关联。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

/**
 * 缩放与标签控制按钮组。固定在右上角，可展开/收起。
 *
 * 切换按钮始终固定在右上角（展开时变为收起图标）。
 * 控制列通过 [AnimatedVisibility] 从切换按钮处向下弹出，向上收回，
 * 包含缩放百分比、标签显隐、放大、缩小、重置五个按钮。
 *
 * @param scale 当前缩放比例
 * @param showLabels 当前标签是否可见，控制眼睛图标样式
 * @param onToggleLabels 切换标签显隐的回调
 * @param onZoomIn 放大回调
 * @param onZoomOut 缩小回调
 * @param onReset 重置缩放和平移的回调
 */
@Composable
private fun BoxScope.ZoomControls(
    scale: Float, showLabels: Boolean, onToggleLabels: () -> Unit,
    onZoomIn: () -> Unit, onZoomOut: () -> Unit, onReset: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val btnColors = IconButtonDefaults.filledIconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 右上角固定的切换按钮（展开时显示收起图标，收起时显示展开图标）
    val toggleIcon = if (isExpanded) Icons.Default.Close else Icons.Default.Add
    val toggleDesc = if (isExpanded) "收起" else "展开"
    FilledIconButton(
        onClick = { isExpanded = !isExpanded },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 16.dp)
            .size(40.dp),
        colors = if (isExpanded) IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) else btnColors
    ) {
        Icon(toggleIcon, toggleDesc, modifier = Modifier.size(20.dp))
    }

    // 控制按钮列：从屏幕右侧弹出 / 向右收回
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = Modifier.align(Alignment.TopEnd)
    ) {
        Column(
            modifier = Modifier.padding(top = 56.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "${(scale * 100).toInt()}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp, fontWeight = FontWeight.Medium
            )
            FilledIconButton(onClick = onToggleLabels, modifier = Modifier.size(40.dp), colors = btnColors) {
                Icon(if (showLabels) Icons.Default.Visibility else Icons.Default.VisibilityOff, "标签", modifier = Modifier.size(20.dp))
            }
            FilledIconButton(onClick = onZoomIn, modifier = Modifier.size(40.dp), colors = btnColors) {
                Icon(Icons.Default.Add, "放大", modifier = Modifier.size(20.dp))
            }
            FilledIconButton(onClick = onZoomOut, modifier = Modifier.size(40.dp), colors = btnColors) {
                Icon(Icons.Default.Remove, "缩小", modifier = Modifier.size(20.dp))
            }
            FilledIconButton(onClick = onReset, modifier = Modifier.size(40.dp), colors = btnColors) {
                Icon(Icons.Default.FilterCenterFocus, "重置", modifier = Modifier.size(20.dp))
            }
        }
    }
}
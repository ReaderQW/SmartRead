package com.example.presentation
//


import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
//import androidx.compose.ui.input.pointer.detectDragGestures
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Book
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import kotlin.math.cos
import kotlin.math.sin

// --- DASHBOARD (Bookcase, Notebook & Complete Graph View) ---
@Composable
fun DashboardScreen(viewModel: SmartReadViewModel) {
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val nodes by viewModel.knowledgeNodes.collectAsStateWithLifecycle()
    val edges by viewModel.knowledgeEdges.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Logo icon",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SmartRead",
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Insight Mode",
                            style = TextStyle(color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("追溯思想基因、搜读书本、卡片笔记...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("dashboard_search"),
                    shape = RoundedCornerShape(25.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "BookShelf", tint = if (activeTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text("智阅书舍", color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Notes, contentDescription = "Clippings", tint = if (activeTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text("思存档案", color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Mind Map", tint = if (activeTab == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) },
                    label = { Text("思想基因图", color = if (activeTab == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> BookShelfView(
                    books = books.filter { it.title.contains(searchQuery, true) || it.author.contains(searchQuery, true) },
                    onBookClick = { viewModel.selectBook(it.id) }
                )
                1 -> NotesListView(
                    notes = notes.filter { it.userNote.contains(searchQuery, true) || it.originalText.contains(searchQuery, true) || it.tags.contains(searchQuery, true) },
                    onDelete = { viewModel.deleteNote(it) }
                )
                2 -> KnowledgeGraphView(
                    nodes = nodes,
                    edges = edges
                )
            }
        }
    }
}

// --- TAB 0: Bookcase Grid View ---
@Composable
fun BookShelfView(books: List<Book>, onBookClick: (Book) -> Unit) {
    if (books.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Empty shelf", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("智能书架目前为空", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text("在搜索框调整关键字或稍后再试。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "藏书阁 Exquisite Bookshelf",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "点击经典，即可进入智能高亮、OCR截图与苏格拉底提问伴阅空间。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            books.forEach { book ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onBookClick(book) }
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
                        Box(
                            modifier = Modifier
                                .size(width = 85.dp, height = 115.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = when (book.id) {
                                            1 -> listOf(MaterialTheme.colorScheme.primary, Color(0xFF985EFF))
                                            2 -> listOf(Color(0xFF381E72), MaterialTheme.colorScheme.primary)
                                            else -> listOf(Color(0xFF4F378B), Color(0xFFB09FFF))
                                        }
                                    )
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Book, contentDescription = "Book icon", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
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

                        Spacer(modifier = Modifier.width(16.dp))

                        // Book Infos
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(book.category, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("点击开启阅读", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(book.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                                Text("${(book.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 1: Notebook / Notes & AI summary cards ---
@Composable
fun NotesListView(notes: List<Note>, onDelete: (Note) -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary

    if (notes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = "No notes", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("尚未记录思辨卡片", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("在经典阅读器中长按选句、撰写感悟并保存，即可在此呈现AI极速剖析摘要。", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    } else {
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
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(trimmed, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Insight icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI智慧深透剖解", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(insight, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, style = TextStyle(lineHeight = 16.sp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

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
            Text("思想基因图谱", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("全景展示读书、感悟、批注等知识元关系", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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
            Text("提示：双手指腹可平移图谱。点击卡词获取洞察。", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
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
                        IconButton(onClick = { selectedNodeInfo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

package com.example.presentation

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Book
import com.example.domain.model.Highlight
import com.example.domain.model.KnowledgeEdge
import com.example.domain.model.KnowledgeNode
import com.example.domain.model.Note
import com.example.domain.model.ReadingReport
import com.example.utils.BookDummyData
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// --- Custom Theme Color Constants (Sleek Interface Palette) ---
object ScholarColors {
    val DarkCyan = Color(0xFF6750A4) // Sleek Primary Purple
    val BrightCyan = Color(0xFF985EFF) // Sleek Light Purple Accent
    val PaperWarm = Color(0xFFFDF7FF) // Sleek Soft Purple / White background
    val CharcoalAccent = Color(0xFF1D1B20) // Sleek Main Dark text/accents
    val GlassOverlay = Color(0xAAFFFFFF) // Clean Sleek Overlay glass
    val GoldDeep = Color(0xFF6750A4) // Brand deep violet
    val HighlightYellow = Color(0x99FFE082) // Elegant warm-amber highlights from Tailwind spec
    val HighlightGreen = Color(0x99E8DEF8) // Elegant lavender highlights
    val HighlightBlue = Color(0x88D0BCFF) // Elegant pale blue-purple
    val HighlightPink = Color(0x88F3EDF7) // Pale grey-purple highlights
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReadApp(
    viewModel: SmartReadViewModel,
    modifier: Modifier = Modifier
) {
    val currentBookId by viewModel.currentBookId.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentBookId == null) {
            // Main Exploration Dashboard (shelf, notes list, overall graph)
            DashboardScreen(viewModel = viewModel)
        } else {
            // Custom Immersive Reader Viewport
            ReaderScreen(viewModel = viewModel)
        }
    }
}

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
                    .background(Color(0xFFFDF7FF))
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
                                .background(Color(0xFF6750A4), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Logo icon",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SmartRead",
                            style = TextStyle(
                                color = Color(0xFF1D1B20),
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Serif
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8DEF8), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Insight Mode",
                            style = TextStyle(color = Color(0xFF1D192B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("追溯思想基因、搜读书本、卡片笔记...", color = Color(0xFF49454F), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFF79747E).copy(alpha = 0.3f),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
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
                containerColor = Color(0xFFF3EDF7),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "BookShelf", tint = if (activeTab == 0) Color(0xFF1D192B) else Color(0xFF49454F)) },
                    label = { Text("智阅书舍", color = if (activeTab == 0) Color(0xFF1D192B) else Color(0xFF49454F)) }
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Notes, contentDescription = "Clippings", tint = if (activeTab == 1) Color(0xFF1D192B) else Color(0xFF49454F)) },
                    label = { Text("思存档案", color = if (activeTab == 1) Color(0xFF1D192B) else Color(0xFF49454F)) }
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Hub, contentDescription = "Mind Map", tint = if (activeTab == 2) Color(0xFF1D192B) else Color(0xFF49454F)) },
                    label = { Text("思想基因图", color = if (activeTab == 2) Color(0xFF1D192B) else Color(0xFF49454F)) }
                )
            }
        },
        containerColor = Color(0xFFFDF7FF)
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
            Icon(Icons.Default.Warning, contentDescription = "Empty shelf", modifier = Modifier.size(48.dp), tint = Color(0xFF49454F))
            Spacer(modifier = Modifier.height(16.dp))
            Text("智能书架目前为空", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)
            Text("在搜索框调整关键字或稍后再试。", color = Color(0xFF49454F))
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
                color = Color(0xFF1D1B20),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "点击经典，即可进入智能高亮、OCR截图与苏格拉底提问伴阅空间。",
                color = Color(0xFF49454F),
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE7E0EC)),
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
                                            1 -> listOf(Color(0xFF6750A4), Color(0xFF985EFF))
                                            2 -> listOf(Color(0xFF381E72), Color(0xFF6750A4))
                                            else -> listOf(Color(0xFF4F378B), Color(0xFFB09FFF))
                                        }
                                    )
                                )
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Book, contentDescription = "Book icon", tint = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = book.title,
                                    fontSize = 11.sp,
                                    color = Color.White,
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
                                        .background(Color(0xFFE8DEF8), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(book.category, color = Color(0xFF1D192B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("点击开启阅读", color = Color(0xFF6750A4), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(book.title, color = Color(0xFF1D1B20), fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                            Text(book.author, color = Color(0xFF49454F), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = book.summaryText,
                                color = Color(0xFF49454F),
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
                                    color = Color(0xFF6750A4),
                                    trackColor = Color(0xFFE7E0EC)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${(book.progress * 100).toInt()}%", color = Color(0xFF1D1B20), fontSize = 10.sp)
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
    if (notes.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Warning, contentDescription = "No notes", modifier = Modifier.size(48.dp), tint = Color(0xFF49454F))
            Spacer(modifier = Modifier.height(16.dp))
            Text("尚未记录思辨卡片", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("在经典阅读器中长按选句、撰写感悟并保存，即可在此呈现AI极速剖析摘要。", color = Color(0xFF49454F), textAlign = TextAlign.Center, fontSize = 12.sp)
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
                    color = Color(0xFF1D1B20),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "在此可以回溯您摘录的黄金概念句，与您当时所作思考。AI智慧洞察亦将实时陪伴。",
                    color = Color(0xFF49454F),
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE7E0EC)),
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
                                                .background(Color(0xFFE8DEF8), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(trimmed, color = Color(0xFF1D192B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { onDelete(note) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note", tint = Color(0xFF49454F), modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .drawBehind {
                                    drawLine(
                                        color = Color(0xFF6750A4),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "“${note.originalText}”",
                                color = Color(0xFF1D1B20),
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                style = TextStyle(lineHeight = 18.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "您的感悟：",
                            color = Color(0xFF1D1B20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = note.userNote,
                            color = Color(0xFF49454F),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )

                        note.aiSummary?.let { insight ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = "Insight icon", tint = Color(0xFF6750A4), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI智慧深透剖解", color = Color(0xFF6750A4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(insight, color = Color(0xFF1D1B20), fontSize = 11.sp, style = TextStyle(lineHeight = 16.sp))
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
            .background(Color(0xFFFDF7FF))
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
                        color = Color(0xFF6750A4).copy(alpha = 0.25f),
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
                        "Book" -> Color(0xFF6750A4)
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
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(6.dp))
                    .clickable { selectedNodeInfo = nodeObj }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = nodeObj.label,
                    color = Color(0xFF1D1B20),
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
                .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text("思想基因图谱", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("全景展示读书、感悟、批注等知识元关系", color = Color(0xFF49454F), fontSize = 10.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(Color(0xFF6750A4), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("原著中心", color = Color(0xFF1D1B20), fontSize = 9.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB300), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("反思概念", color = Color(0xFF1D1B20), fontSize = 9.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(8.dp).background(Color(0xFFE91E63), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("笔记档案", color = Color(0xFF1D1B20), fontSize = 9.sp)
            }
            Text("提示：双手指腹可平移图谱。点击卡词获取洞察。", color = Color(0xFF6750A4), fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
        }

        // Details floating pop-up card
        selectedNodeInfo?.let { node ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "思想基因元：${node.label}",
                            color = Color(0xFF1D1B20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif
                        )
                        IconButton(onClick = { selectedNodeInfo = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF49454F))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "归属分类: ${node.category}  |  连接强度: ${node.size * 10}",
                        color = Color(0xFF6750A4),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "在您阅读期间，AI辅助思想思想基因引擎提取该原子标记，并将其锚定在您的自动思想基因图谱中，用以追踪和整合关于本期经典的全部思辨关联。这是一个端到端完全连接的关系元宇宙。",
                        color = Color(0xFF49454F),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}


// --- READER MODULE (TAB 0 Details Screen) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(viewModel: SmartReadViewModel) {
    val context = LocalContext.current
    val currentBookId by viewModel.currentBookId.collectAsStateWithLifecycle()
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    val pageIndex by viewModel.currentPageIndex.collectAsStateWithLifecycle()
    val selectedText by viewModel.selectedText.collectAsStateWithLifecycle()
    val highlights by viewModel.highlightsForCurrentBook.collectAsStateWithLifecycle()
    val isFloatingAssistantOpen by viewModel.isFloatingAssistantOpen.collectAsStateWithLifecycle()
    val isOcrScanning by viewModel.isOcrScanning.collectAsStateWithLifecycle()
    val scannedOcrText by viewModel.scannedOcrText.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val activeReport by viewModel.activeReport.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val activeBook = remember(currentBookId, books) {
        books.find { it.id == currentBookId }
    } ?: return

    val bookExcerpts = remember(currentBookId) {
        BookDummyData.excerpts[currentBookId] ?: listOf("内容缺失")
    }

    val pageContent = bookExcerpts.getOrNull(pageIndex) ?: "页码超出范围"

    // Manage sliding menus or comment dialog triggers
    var isCommentDialogShow by remember { mutableStateOf(false) }
    var commentInputText by remember { mutableStateOf("") }
    var chosenHighlightIdForComment by remember { mutableIntStateOf(0) }

    // Floating toolbar trigger coordinate layout
    var showHighlightColorPicker by remember { mutableStateOf(false) }

    // Update reader progress whenever page switches
    LaunchedEffect(pageIndex) {
        val calcProgress = (pageIndex + 1).toFloat() / bookExcerpts.size.toFloat()
        viewModel.updateBookProgress(calcProgress)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeBook.title, color = Color(0xFF1D1B20), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(activeBook.author, color = Color(0xFF49454F), fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectBook(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Shelf", tint = Color(0xFF6750A4))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateReport() },
                        modifier = Modifier.testTag("report_generate_btn")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Intelligence report", tint = Color(0xFF6750A4))
                    }
                    IconButton(onClick = { viewModel.setFloatingAssistantOpen(!isFloatingAssistantOpen) }) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Socratic bot",
                            tint = if (isFloatingAssistantOpen) Color(0xFF6750A4) else Color(0xFF49454F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDF7FF))
            )
        },
        bottomBar = {
            // Interactive page control buttons
            BottomAppBar(
                containerColor = Color(0xFFF3EDF7),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevPage() }, enabled = pageIndex > 0) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev page", tint = if (pageIndex > 0) Color(0xFF6750A4) else Color(0xFFCDC9D0))
                    }
                    
                    Text("第 ${pageIndex + 1} / ${bookExcerpts.size} 页", color = Color(0xFF1D1B20), fontSize = 13.sp)

                    Button(
                        onClick = { viewModel.startSimulatedOcrScan(pageContent) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8)),
                        border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Sim OCR", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OCR截图批注", color = Color(0xFF1D192B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { viewModel.nextPage(bookExcerpts.size) }, enabled = pageIndex < bookExcerpts.size - 1) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next page", tint = if (pageIndex < bookExcerpts.size - 1) Color(0xFF6750A4) else Color(0xFFCDC9D0))
                    }
                }
            }
        },
        containerColor = Color(0xFFFDF7FF)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Reader background layout paper
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(ScholarColors.PaperWarm)
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
                        color = Color(0xFF49454F),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                    Text(
                        text = "Page ${pageIndex + 1}",
                        color = Color(0xFF49454F),
                        fontSize = 11.sp
                    )
                }

                // Interactive paragraph text body where clicking on lines toggles HIGHLIGHTING / annotation triggers
                Text(
                    text = pageContent,
                    color = Color(0xFF1D1B20),
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Serif,
                    style = TextStyle(lineHeight = 31.sp, letterSpacing = 1.sp)
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFF79747E).copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // Section of list of Highlights on this page
                val pHighlights = highlights.filter { it.pageIndex == pageIndex }
                if (pHighlights.isNotEmpty()) {
                    Text("本页高亮句子及批注：", color = Color(0xFF625B71), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                    color = Color(0xFF1D1B20),
                                    fontSize = 12.sp,
                                    fontStyle = FontStyle.Italic
                                )
                                hl.comment?.let { comment ->
                                    Text(
                                        text = "我的思考：$comment",
                                        color = Color(0xFF49454F),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            Row {
                                IconButton(onClick = {
                                    chosenHighlightIdForComment = hl.id
                                    commentInputText = hl.comment ?: ""
                                    isCommentDialogShow = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Add comment", tint = Color(0xFF6750A4), modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteHighlight(hl) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove highlight", tint = Color(0xFF49454F), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 高亮选句提示",
                        color = Color(0xFF1D1B20),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "在下方可以直接选定代表性的‘探询点’或句子，开启高亮或AI对话。",
                        color = Color(0xFF49454F),
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetPhrases.forEach { text ->
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFE7E0EC), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectTextSelection(text)
                                        showHighlightColorPicker = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text, color = Color(0xFF6750A4), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Highlighting and text clipping floating tool bar dialog
            if (selectedText.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFF6750A4)),
                    tonalElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("已选中经典词句 passage：", color = Color(0xFF49454F), fontSize = 11.sp)
                            IconButton(onClick = { viewModel.clearTextSelection() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close selections bar", tint = Color(0xFF49454F))
                            }
                        }
                        
                        Text(
                            text = "“$selectedText”",
                            color = Color(0xFF1D1B20),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.padding(vertical = 8.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Highlight color palette circles & Socratic link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Highlights triggers
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "#FFEB3B" to ScholarColors.HighlightYellow,
                                    "#69F0AE" to ScholarColors.HighlightGreen,
                                    "#40C4FF" to ScholarColors.HighlightBlue,
                                    "#FF4081" to ScholarColors.HighlightPink
                                ).forEach { (hex, tintColor) ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(tintColor, CircleShape)
                                            .border(1.dp, Color.White, CircleShape)
                                            .clickable { viewModel.saveHighlight(hex) }
                                    )
                                }
                            }

                            // Socratic bot linkage & Notebook link
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        viewModel.setFloatingAssistantOpen(true)
                                        // Auto issue initial greeting in companion window
                                        viewModel.sendSocraticMessage("关于这一句：‘$selectedText’，该如何深度切入反思？")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = "Socratic icon", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("伴读对论", color = Color.White, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        commentInputText = ""
                                        chosenHighlightIdForComment = 0
                                        isCommentDialogShow = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8DEF8)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save and AI summary notes icon", tint = Color(0xFF1D192B), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("存为感悟", color = Color(0xFF1D192B), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Draggable Socratic floating AI tutor companion side drawer / panel
            AnimatedVisibility(
                visible = isFloatingAssistantOpen,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.82f)
            ) {
                SocraticFloatingPanel(viewModel = viewModel)
            }

            // Simulated OCR Digital Scan Scan beam HUD Overlay
            if (isOcrScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFDF7FF).copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Digital futuristic scan beam animation
                        val infiniteTransition = rememberInfiniteTransition(label = "scanning")
                        val beamY by infiniteTransition.animateFloat(
                            initialValue = -120f,
                            targetValue = 120f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scanning_beam"
                        )

                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .border(2.dp, Color(0xFF6750A4), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pageContent.take(150) + "...",
                                color = Color(0xFF1D1B20),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.alpha(0.6f)
                            )
                            // Glowing line representing scanner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .offset(y = beamY.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color(0xFF6750A4), Color.Transparent)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("SmartScan OCR 深度提取中...", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold)
                        Text("基于 ML Kit 进行端侧字符 point-cloud 抽取坐标映射..", color = Color(0xFF49454F), fontSize = 11.sp)
                    }
                }
            }

            // SCANNED OCR NOTE CREATION DIALOG
            scannedOcrText?.let { ocrRawPassage ->
                var draftUserThought by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { viewModel.clearOcrResult() },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.saveNote(ocrRawPassage, draftUserThought)
                                viewModel.clearOcrResult()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("确认存入思绪卡片", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearOcrResult() }) {
                            Text("丢弃", color = Color(0xFF49454F))
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "ocr icon", tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ML Kit 截图字符识别成功", color = Color(0xFF1D1B20), fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column {
                            Text("系统已通过数字镜头识别当前页面文字块：", color = Color(0xFF49454F), fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(Color(0xFFF3EDF7), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(ocrRawPassage, color = Color(0xFF1D1B20), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text("请记录您的当期理解或批注心得（AI将根据该反思构建思想路径）：", color = Color(0xFF1D1B20), fontSize = 11.sp)
                            OutlinedTextField(
                                value = draftUserThought,
                                onValueChange = { draftUserThought = it },
                                placeholder = { Text("例如：这个类比启示了我对于真理的认识...", color = Color(0xFF49454F), fontSize = 11.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCDC9D0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    },
                    containerColor = Color.White
                )
            }

            // HIGHLIGHT COMMENT OR SIMPLE DIRECT NOTE COMPOSER DIALOG
            if (isCommentDialogShow) {
                var localThought by remember { mutableStateOf(commentInputText) }

                AlertDialog(
                    onDismissRequest = { isCommentDialogShow = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (chosenHighlightIdForComment > 0) {
                                    // Add comment to highlight
                                    scope.launch {
                                        viewModel.saveHighlight(colorHex = "#FFEB3B")
                                    }
                                } else {
                                    // Save note directly
                                    viewModel.saveNote(selectedText, localThought)
                                    viewModel.clearTextSelection()
                                }
                                isCommentDialogShow = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Text("保存并在思维树中织入点", color = Color.White, fontSize = 11.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isCommentDialogShow = false }) {
                            Text("取消", color = Color(0xFF49454F))
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "notes icon", tint = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加即刻思忆 / 批注", color = Color(0xFF1D1B20), fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column {
                            Text("标注原句 passage：", color = Color(0xFF49454F), fontSize = 10.sp)
                            Text(
                                text = "“${selectedText.ifEmpty { "文本标注片段" }}”",
                                color = Color(0xFF1D1B20),
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 4.dp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("您的感悟(Insight Input)：", color = Color(0xFF1D1B20), fontSize = 11.sp)
                            OutlinedTextField(
                                value = localThought,
                                onValueChange = { localThought = it },
                                placeholder = { Text("记录该哲理或段落带给您的敏锐感悟或反驳方向...", color = Color(0xFF49454F), fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6750A4),
                                    unfocusedBorderColor = Color(0xFFCDC9D0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    },
                    containerColor = Color.White
                )
            }

            // BLIND BOX COGNITIVE CERTIFICATE OVERLAY DIALOG
            activeReport?.let { report ->
                ReadingReportDialog(report = report, onDismiss = { viewModel.closeReport() })
            }

            // Spinner Loading Overlay
            if (isAiLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF6750A4).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6750A4))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("大模型智慧思考反刍中...", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("正在基于RAG与思想脉络织造启发...", color = Color(0xFF49454F), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN: Socratic Dialog chat widget panels ---
@Composable
fun SocraticFloatingPanel(viewModel: SmartReadViewModel) {
    val chatMessages by viewModel.chatMessagesForCurrentBook.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    var userDraftMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp)
            .testTag("socratic_floating_panel"),
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE7E0EC))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDF7FF))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "companion socrates", tint = Color(0xFF6750A4))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("苏格拉底AI伴读", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("探问思维漏洞，引导第一性元认知", color = Color(0xFF49454F), fontSize = 9.sp)
                    }
                }
                IconButton(onClick = { viewModel.setFloatingAssistantOpen(false) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Hide socrates banner", tint = Color(0xFF49454F))
                }
            }

            // Quick suggestion presets to test
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "寻找隐含前提",
                    "提出怀疑 and 对立论点",
                    "举出具体的现实案例",
                    "分析该概念是否仍适用"
                ).forEach { option ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .clickable {
                                userDraftMsg = "从‘${option}’的逻辑视角出发，这句话有什么隐藏的局限吗？"
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(option, color = Color(0xFF1D192B), fontSize = 9.sp)
                    }
                }
            }

            // Chat lines scroll View
            Box(modifier = Modifier.weight(1f)) {
                if (chatMessages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "empty chats", modifier = Modifier.size(32.dp), tint = Color(0xFF6750A4).copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("苏格拉底伴读者就绪", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "在原句长按启动，或在此针对当前段落、疑惑直接发言挑辩。助手不会说教，而是将循循诱导提问：",
                            color = Color(0xFF49454F),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chatMessages) { msg ->
                            val isUser = msg.sender == "USER"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(
                                            if (isUser) Color(0xFFE8DEF8) else Color(0xFFF3EDF7),
                                            RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isUser) 12.dp else 0.dp,
                                                bottomEnd = if (isUser) 0.dp else 12.dp
                                            )
                                        )
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = if (isUser) "读者 reflection" else "AI 辩伴 Socrates",
                                            color = if (isUser) Color(0xFF49454F) else Color(0xFF6750A4),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.content,
                                            color = Color(0xFF1D1B20),
                                            fontSize = 12.sp,
                                            style = TextStyle(lineHeight = 17.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFDF7FF))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userDraftMsg,
                    onValueChange = { userDraftMsg = it },
                    placeholder = { Text("叩问其逻辑、质疑偏见或写下您的追问...", color = Color(0xFF49454F), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCDC9D0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        viewModel.sendSocraticMessage(userDraftMsg)
                        userDraftMsg = ""
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF6750A4), CircleShape)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "send chat", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}


// --- COGNITIVE BLIND BOX REPORT & RADAR DETAIL DIALOG ---
@Composable
fun ReadingReportDialog(
    report: ReadingReport,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出并分享这枚心智盲盒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("返回书海", color = Color(0xFF49454F))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "report icon", tint = Color(0xFF6750A4))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SmartRead 阅读思维盲盒", color = Color(0xFF1D1B20), fontSize = 17.sp, fontFamily = FontFamily.Serif)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("基于您在本期原著划线、笔记撰写、和与有批判性的苏格拉底式对话汇总分析，为您定制心智思想雷达：", color = Color(0xFF49454F), fontSize = 11.sp)
                
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
                                color = Color(0xFF6750A4).copy(alpha = 0.1f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }

                        // Coordinates axis lines
                        angleRad.forEach { rad ->
                            drawLine(
                                color = Color(0xFF6750A4).copy(alpha = 0.15f),
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
                            color = Color(0xFF6750A4),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Labels on axis
                    Text("逻辑度(${report.logic})", color = Color(0xFF1D1B20), fontSize = 9.sp, modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-10).dp))
                    Text("创见力(${report.innovation})", color = Color(0xFF1D1B20), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-5).dp))
                    Text("广博度(${report.width})", color = Color(0xFF1D1B20), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = 10.dp))
                    Text("同理心(${report.empathy})", color = Color(0xFF1D1B20), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopCenter).offset(y = 5.dp))
                    Text("批判度(${report.critical})", color = Color(0xFF1D1B20), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = 10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cognitive growth descriptions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row {
                            Icon(Icons.Default.Star, contentDescription = "grain", tint = Color(0xFF6750A4), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("核心认知增量分析 Cognitive Growth：", color = Color(0xFF6750A4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report.cognitiveIncrement,
                            color = Color(0xFF1D1B20),
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
                        .border(1.dp, Color(0xFF6750A4).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFDF5), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("【书友刻印 · 思想灯塔古训】", color = Color(0xFFBF360C), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${report.motto}”",
                            color = Color(0xFF3E2723),
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            style = TextStyle(lineHeight = 19.sp)
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    )
}

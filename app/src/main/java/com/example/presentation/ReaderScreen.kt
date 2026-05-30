package com.example.presentation
// 沉浸式阅读器`ReaderScreen`（含 OCR 动画、高亮、批注弹窗等）
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Highlight
import com.example.ui.theme.SleekHighlightBlue
import com.example.ui.theme.SleekHighlightDefault
import com.example.ui.theme.SleekHighlightGreen
import com.example.ui.theme.SleekHighlightPink
import com.example.utils.BookDummyData
import kotlinx.coroutines.launch

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
                        Text(activeBook.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(activeBook.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectBook(null) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back Shelf", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateReport() },
                        modifier = Modifier.testTag("report_generate_btn")
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Intelligence report", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.setFloatingAssistantOpen(!isFloatingAssistantOpen) }) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Socratic bot",
                            tint = if (isFloatingAssistantOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Interactive page control buttons
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevPage() }, enabled = pageIndex > 0) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev page", tint = if (pageIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f))
                    }

                    Text("第 ${pageIndex + 1} / ${bookExcerpts.size} 页", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)

                    Button(
                        onClick = { viewModel.startSimulatedOcrScan(pageContent) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Sim OCR", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OCR截图批注", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { viewModel.nextPage(bookExcerpts.size) }, enabled = pageIndex < bookExcerpts.size - 1) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next page", tint = if (pageIndex < bookExcerpts.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
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

                // Interactive paragraph text body where clicking on lines toggles HIGHLIGHTING / annotation triggers
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
                    Text("本页高亮句子及批注：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                IconButton(onClick = {
                                    chosenHighlightIdForComment = hl.id
                                    commentInputText = hl.comment ?: ""
                                    isCommentDialogShow = true
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Add comment", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteHighlight(hl) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove highlight", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetPhrases.forEach { text ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectTextSelection(text)
                                        showHighlightColorPicker = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    tonalElevation = 12.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("已选中经典词句 passage：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            IconButton(onClick = { viewModel.clearTextSelection() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close selections bar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            text = "“$selectedText”",
                            color = MaterialTheme.colorScheme.onSurface,
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
                                    "#FFEB3B" to SleekHighlightDefault,
                                    "#69F0AE" to SleekHighlightGreen,
                                    "#40C4FF" to SleekHighlightBlue,
                                    "#FF4081" to SleekHighlightPink
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
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = "Socratic icon", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("伴读对论", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        commentInputText = ""
                                        chosenHighlightIdForComment = 0
                                        isCommentDialogShow = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save and AI summary notes icon", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("存为感悟", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
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
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
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
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pageContent.take(150) + "...",
                                color = MaterialTheme.colorScheme.onSurface,
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
                                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.primary, Color.Transparent)
                                        )
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text("SmartScan OCR 深度提取中...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text("基于 ML Kit 进行端侧字符 point-cloud 抽取坐标映射..", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("确认存入思绪卡片", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearOcrResult() }) {
                            Text("丢弃", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = "ocr icon", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ML Kit 截图字符识别成功", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column {
                            Text("系统已通过数字镜头识别当前页面文字块：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                                    .verticalScroll(rememberScrollState())
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(ocrRawPassage, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text("请记录您的当期理解或批注心得（AI将根据该反思构建思想路径）：", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            OutlinedTextField(
                                value = draftUserThought,
                                onValueChange = { draftUserThought = it },
                                placeholder = { Text("例如：这个类比启示了我对于真理的认识...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("保存并在思维树中织入点", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isCommentDialogShow = false }) {
                            Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "notes icon", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("添加即刻思忆 / 批注", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                        }
                    },
                    text = {
                        Column {
                            Text("标注原句 passage：", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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
                            Text("您的感悟(Insight Input)：", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                            OutlinedTextField(
                                value = localThought,
                                onValueChange = { localThought = it },
                                placeholder = { Text("记录该哲理或段落带给您的敏锐感悟或反驳方向...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("大模型智慧思考反刍中...", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("正在基于RAG与思想脉络织造启发...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

package com.example.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Book
import com.example.domain.model.Note
import com.example.presentation.ReadingReportDialog
import com.example.presentation.SocraticFloatingPanel
import com.example.presentation.viewmodel.SmartReadViewModel
import com.example.ui.components.GiftBoxOpeningAnimation
import com.example.ui.components.LoadingOverlay
import com.example.utils.BookDummyData
import kotlinx.coroutines.launch

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
    val uiConfig by viewModel.uiConfig.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val activeBook = remember(currentBookId, books) {
        books.find { it.id == currentBookId }
    } ?: return

    // FLOATING 类型：仅展示思绪卡片数量 + 开启悬浮窗按钮
    if (activeBook.type == "FLOATING") {
        FloatingBookReaderView(
            activeBook = activeBook,
            notes = viewModel.notesForCurrentBook.collectAsStateWithLifecycle().value,
            viewModel = viewModel
        )
        return
    }

    val storedPages by viewModel.currentBookPages.collectAsStateWithLifecycle()
    val bookExcerpts = remember(currentBookId, storedPages) {
        if (storedPages.isNotEmpty()) storedPages
        else BookDummyData.excerpts[currentBookId] ?: listOf("内容缺失")
    }

    val pageContent = bookExcerpts.getOrNull(pageIndex) ?: "页码超出范围"

    var isCommentDialogShow by remember { mutableStateOf(false) }
    var commentInputText by remember { mutableStateOf("") }
    var chosenHighlightIdForComment by remember { mutableIntStateOf(0) }
    var showGiftBox by remember { mutableStateOf(false) }

    LaunchedEffect(activeReport) {
        if (activeReport != null) {
            showGiftBox = true
        }
    }

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
                        Icon(Icons.Default.ArrowBack, "Back Shelf", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateReport() }, modifier = Modifier.testTag("report_generate_btn")) {
                        Icon(Icons.Default.Star, "Intelligence report", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.setFloatingAssistantOpen(!isFloatingAssistantOpen) }) {
                        Icon(Icons.Default.Send, "Socratic bot", tint = if (isFloatingAssistantOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentPadding = PaddingValues(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.prevPage() }, enabled = pageIndex > 0) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Prev page", tint = if (pageIndex > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f))
                    }
                    Text("第 ${pageIndex + 1} / ${bookExcerpts.size} 页", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Button(onClick = { viewModel.startSimulatedOcrScan(pageContent) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Search, "Sim OCR", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OCR截图批注", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { viewModel.nextPage(bookExcerpts.size) }, enabled = pageIndex < bookExcerpts.size - 1) {
                        Icon(Icons.Default.KeyboardArrowRight, "Next page", tint = if (pageIndex < bookExcerpts.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f))
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ReaderPageContent(pageContent = pageContent, pageIndex = pageIndex, currentBookId = currentBookId, highlights = highlights, onPhraseSelected = { viewModel.selectTextSelection(it) }, onDeleteHighlight = { viewModel.deleteHighlight(it) }, onEditHighlight = { chosenHighlightIdForComment = it.id; commentInputText = it.comment ?: ""; isCommentDialogShow = true }, modifier = Modifier.fillMaxSize(), readingFontFamily = uiConfig.readingFont.toFontFamily())
            if (selectedText.isNotEmpty()) {
                ReaderSelectionToolbar(selectedText = selectedText, onClearSelection = { viewModel.clearTextSelection() }, onSaveHighlight = { viewModel.saveHighlight(it) }, onOpenSocraticWithText = { viewModel.setFloatingAssistantOpen(true); viewModel.sendSocraticMessage("关于这一句：'$it'，该如何深度切入反思？") }, onOpenCommentDialog = { commentInputText = ""; chosenHighlightIdForComment = 0; isCommentDialogShow = true }, modifier = Modifier.align(Alignment.BottomCenter))
            }
            AnimatedVisibility(visible = isFloatingAssistantOpen, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.82f)) {
                SocraticFloatingPanel(viewModel = viewModel)
            }
            OcrScanOverlay(isVisible = isOcrScanning, pageContent = pageContent)
            OcrNoteDialog(scannedOcrText = scannedOcrText, onDismiss = { viewModel.clearOcrResult() }, viewModel = viewModel)
            ReaderCommentDialog(isVisible = isCommentDialogShow, chosenHighlightId = chosenHighlightIdForComment, initialCommentText = commentInputText, selectedText = selectedText, onDismiss = { isCommentDialogShow = false }, viewModel = viewModel)
            if (showGiftBox) { GiftBoxOpeningAnimation(onFinished = { showGiftBox = false }) }
            if (!showGiftBox) { activeReport?.let { ReadingReportDialog(report = it, viewModel = viewModel, onDismiss = { viewModel.closeReport() }) } }
            if (isAiLoading) { LoadingOverlay(title = "大模型智慧思考反刍中...", subtitle = "正在基于RAG与思想脉络织造启发...") }
        }
    }
}

// FLOATING 类型书籍阅读视图

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBookReaderView(activeBook: Book, notes: List<Note>, viewModel: SmartReadViewModel) {
    val isFloatingAssistantOpen by viewModel.isFloatingAssistantOpen.collectAsStateWithLifecycle()
    var isFloatingServiceActive by remember { mutableStateOf(false) }
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val activeReport by viewModel.activeReport.collectAsStateWithLifecycle()
    var showGiftBox by remember { mutableStateOf(false) }
    LaunchedEffect(activeReport) { if (activeReport != null) showGiftBox = true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeBook.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
                        Text(activeBook.author, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                },
                navigationIcon = { IconButton(onClick = { viewModel.selectBook(null) }) { Icon(Icons.Default.ArrowBack, "返回书架", tint = MaterialTheme.colorScheme.primary) } },
                actions = {
                    IconButton(onClick = { viewModel.generateReport() }) { Icon(Icons.Default.Star, "生成报告", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { viewModel.setFloatingAssistantOpen(!isFloatingAssistantOpen) }) { Icon(Icons.Default.Send, "AI伴读", tint = if (isFloatingAssistantOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(40.dp))
                Box(modifier = Modifier.size(width = 120.dp, height = 170.dp).clip(RoundedCornerShape(12.dp)).background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(activeBook.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = MaterialTheme.colorScheme.onSurface)
                Text(activeBook.author, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖", fontSize = 40.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("已存入", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("${notes.size}", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("张思绪卡片", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clickable {
                        isFloatingServiceActive = !isFloatingServiceActive
                        viewModel.setFloatingServiceActive(isFloatingServiceActive)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFloatingServiceActive)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isFloatingServiceActive)
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.OpenInNew, null, tint = if (isFloatingServiceActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isFloatingServiceActive) "关闭悬浮阅读窗" else "开启悬浮阅读窗",
                            color = if (isFloatingServiceActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (isFloatingServiceActive) "悬浮窗已开启，退出App后悬浮球不会消失，可在其他应用上层使用。"
                    else "悬浮窗可在其他阅读软件中截图后，通过 OCR 提取文字并记录思绪卡片。退出App后悬浮窗不消失。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                if (activeBook.summaryText.isNotBlank()) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("简介", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(activeBook.summaryText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            AnimatedVisibility(visible = isFloatingAssistantOpen, enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }, modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.82f)) { SocraticFloatingPanel(viewModel = viewModel) }
            if (showGiftBox) { GiftBoxOpeningAnimation(onFinished = { showGiftBox = false }) }
            if (!showGiftBox) { activeReport?.let { ReadingReportDialog(report = it, viewModel = viewModel, onDismiss = { viewModel.closeReport() }) } }
            if (isAiLoading) { LoadingOverlay(title = "大模型智慧思考反刍中...", subtitle = "正在基于RAG与思想脉络织造启发...") }
        }
    }
}
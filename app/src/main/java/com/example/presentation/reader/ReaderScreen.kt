package com.example.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val bookExcerpts = remember(currentBookId) {
        BookDummyData.excerpts[currentBookId] ?: listOf("内容缺失")
    }

    val pageContent = bookExcerpts.getOrNull(pageIndex) ?: "页码超出范围"

    // Dialog state hoisted from child composables
    var isCommentDialogShow by remember { mutableStateOf(false) }
    var commentInputText by remember { mutableStateOf("") }
    var chosenHighlightIdForComment by remember { mutableIntStateOf(0) }
    var showGiftBox by remember { mutableStateOf(false) }

    // 当有新报告生成时，先开启礼盒动画仪式
    LaunchedEffect(activeReport) {
        if (activeReport != null) {
            showGiftBox = true
        }
    }

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
                        Text(
                            activeBook.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            activeBook.author,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.selectBook(null) }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back Shelf",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.generateReport() },
                        modifier = Modifier.testTag("report_generate_btn")
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Intelligence report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.setFloatingAssistantOpen(!isFloatingAssistantOpen) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Socratic bot",
                            tint = if (isFloatingAssistantOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.prevPage() },
                        enabled = pageIndex > 0
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Prev page",
                            tint = if (pageIndex > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f)
                            }
                        )
                    }

                    Text(
                        "第 ${pageIndex + 1} / ${bookExcerpts.size} 页",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )

                    Button(
                        onClick = { viewModel.startSimulatedOcrScan(pageContent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Sim OCR",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "OCR截图批注",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextPage(bookExcerpts.size) },
                        enabled = pageIndex < bookExcerpts.size - 1
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next page",
                            tint = if (pageIndex < bookExcerpts.size - 1) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f)
                            }
                        )
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
            // Main page content (scrollable body)
            ReaderPageContent(
                pageContent = pageContent,
                pageIndex = pageIndex,
                currentBookId = currentBookId,
                highlights = highlights,
                onPhraseSelected = { text ->
                    viewModel.selectTextSelection(text)
                },
                onDeleteHighlight = { viewModel.deleteHighlight(it) },
                onEditHighlight = { hl ->
                    chosenHighlightIdForComment = hl.id
                    commentInputText = hl.comment ?: ""
                    isCommentDialogShow = true
                },
                modifier = Modifier.fillMaxSize(),
                readingFontFamily = uiConfig.readingFont.toFontFamily()
            )

            // Floating selection toolbar when text is selected
            if (selectedText.isNotEmpty()) {
                ReaderSelectionToolbar(
                    selectedText = selectedText,
                    onClearSelection = { viewModel.clearTextSelection() },
                    onSaveHighlight = { viewModel.saveHighlight(it) },
                    onOpenSocraticWithText = { text ->
                        viewModel.setFloatingAssistantOpen(true)
                        viewModel.sendSocraticMessage(
                            "关于这一句：‘$text’，该如何深度切入反思？"
                        )
                    },
                    onOpenCommentDialog = {
                        commentInputText = ""
                        chosenHighlightIdForComment = 0
                        isCommentDialogShow = true
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Socratic AI companion side panel
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

            // OCR scan animation overlay
            OcrScanOverlay(
                isVisible = isOcrScanning,
                pageContent = pageContent
            )

            // OCR result dialog
            OcrNoteDialog(
                scannedOcrText = scannedOcrText,
                onDismiss = { viewModel.clearOcrResult() },
                viewModel = viewModel
            )

            // Highlight comment / note dialog
            ReaderCommentDialog(
                isVisible = isCommentDialogShow,
                chosenHighlightId = chosenHighlightIdForComment,
                initialCommentText = commentInputText,
                selectedText = selectedText,
                onDismiss = { isCommentDialogShow = false },
                viewModel = viewModel
            )

            // 1. 礼盒开启仪式感动效
            if (showGiftBox) {
                GiftBoxOpeningAnimation(
                    onFinished = { showGiftBox = false }
                )
            }

            // 2. Reading report dialog (仪式结束后显示)
            if (!showGiftBox) {
                activeReport?.let { report ->
                    ReadingReportDialog(
                        report = report,
                        viewModel = viewModel,
                        onDismiss = { viewModel.closeReport() }
                    )
                }
            }

            // AI loading overlay
            if (isAiLoading) {
                LoadingOverlay(
                    title = "大模型智慧思考反刍中...",
                    subtitle = "正在基于RAG与思想脉络织造启发..."
                )
            }
        }
    }
}

package com.example.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.settings.SettingsSidebar
import com.example.presentation.viewmodel.SmartReadViewModel

/**
 * 仪表盘主页入口。包含顶部搜索栏、底部 Tab 导航和内容区域。
 * 根据 [activeTab] 切换书架 / 笔记 / 知识图谱三个视图。
 *
 * @param viewModel 数据状态持有者，提供图书、笔记、知识图谱数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: SmartReadViewModel) {
    val books by viewModel.allBooks.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val nodes by viewModel.knowledgeNodes.collectAsStateWithLifecycle()
    val edges by viewModel.knowledgeEdges.collectAsStateWithLifecycle()
    val uiConfig by viewModel.uiConfig.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var selectedNoteBookId by remember { mutableStateOf<Int?>(null) }
    var isFloatingServiceActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            DashboardTopBar(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onLogoClick = { showSettings = true }
            )
        },
        bottomBar = {
            DashboardBottomBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
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
                    books = books.filter {
                        it.title.contains(searchQuery, true) ||
                            it.author.contains(searchQuery, true)
                    },
                    onBookClick = { viewModel.selectBook(it.id) },
                    onDeleteBook = { viewModel.deleteBook(it) },
                    onAddClick = { viewModel.startCreatingBook() }
                )
                1 -> NotesListView(
                    notes = notes.filter {
                        it.userNote.contains(searchQuery, true) ||
                            it.originalText.contains(searchQuery, true) ||
                            it.tags.contains(searchQuery, true)
                    },
                    books = books,
                    selectedBookId = selectedNoteBookId,
                    onSelectBook = { selectedNoteBookId = it },
                    onDelete = { viewModel.deleteNote(it) }
                )
                2 -> KnowledgeGraphView(
                    nodes = nodes,
                    edges = edges
                )
            }
        }
    }

        // 设置侧边栏覆盖层（在 Scaffold 之上，覆盖全屏包括顶栏和底栏）
        SettingsSidebar(
            isVisible = showSettings,
            uiConfig = uiConfig,
            onDismiss = { showSettings = false },
            onUpdateThemeMode = { viewModel.updateThemeMode(it) },
            onUpdateUiFont = { viewModel.updateUiFont(it) },
            onUpdateReadingFont = { viewModel.updateReadingFont(it) },
            onUpdateColorTheme = { viewModel.updateColorTheme(it) },
            isFloatingServiceActive = isFloatingServiceActive,
            onToggleFloatingService = {
                isFloatingServiceActive = !isFloatingServiceActive
                viewModel.setFloatingServiceActive(isFloatingServiceActive)
            }
        )
    }
}

/**
 * 仪表盘顶部栏。包含品牌 Logo 行和搜索输入框。
 *
 * @param searchQuery 当前搜索关键字
 * @param onSearchQueryChange 搜索关键字变更回调
 */
@Composable
private fun DashboardTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onLogoClick: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DashboardLogoRow(onLogoClick = onLogoClick)

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // 👈 在这里直接控制搜索栏的绝对高度！
                .testTag("dashboard_search"),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(25.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .padding(horizontal = 12.dp), // 左右内边距
                    verticalAlignment = Alignment.CenterVertically // 👈 核心：确保内部所有元素垂直居中
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "追溯思想基因、搜读书本、卡片笔记...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField() // 真正的输入框文本
                    }
                }
            }
        )
    }
}

/**
 * 品牌标识行。左侧为 SmartRead 图标 + 标题，右侧为 Insight Mode 徽章。
 * 点击可打开设置侧边栏。
 */
@Composable
private fun DashboardLogoRow(onLogoClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLogoClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(12.dp)
                    )
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
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(16.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Insight Mode",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/**
 * 底部导航栏。包含"智阅书舍"、"思存档案"、"思想基因图"三个 Tab。
 *
 * @param activeTab 当前选中的 Tab 索引（0 = 书架，1 = 笔记，2 = 知识图谱）
 * @param onTabSelected Tab 选中时的回调，返回选中索引
 */
@Composable
private fun DashboardBottomBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        modifier = Modifier.heightIn(max = 64.dp)
    ) {
        NavigationBarItem(
            selected = activeTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    Icons.Default.Book,
                    contentDescription = "BookShelf",
                    tint = if (activeTab == 0) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "智阅书舍",
                    color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        NavigationBarItem(
            selected = activeTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    Icons.Default.Notes,
                    contentDescription = "Clippings",
                    tint = if (activeTab == 1) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "思存档案",
                    color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
        NavigationBarItem(
            selected = activeTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    Icons.Default.Hub,
                    contentDescription = "Mind Map",
                    tint = if (activeTab == 2) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            label = {
                Text(
                    "思想基因图",
                    color = if (activeTab == 2) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

/**
 * 仪表盘顶部栏预览。展示搜索框含中文占位符的效果。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun DashboardTopBarPreview() {
    MaterialTheme {
        DashboardTopBar(
            searchQuery = "一二三四五",
            onSearchQueryChange = {}
        )
    }
}

/**
 * 仪表盘底部导航栏预览。展示"智阅书舍" Tab 选中状态。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
private fun DashboardBottomBarPreview() {
    MaterialTheme {
        DashboardBottomBar(
            activeTab = 0,
            onTabSelected = {}
        )
    }
}

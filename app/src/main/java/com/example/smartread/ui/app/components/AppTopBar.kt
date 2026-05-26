
package com.example.smartread.ui.app.components

import com.example.smartread.ui.theme.AppTheme

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 应用通用顶部标题栏（支持居中/左对齐、返回键、搜索、更多、自定义图标）
 *
 * @param title 标题文字
 * @param showBackButton 是否显示返回按钮
 * @param onBackClick 返回按钮点击事件
 * @param titleAlign 标题对齐方式：居中 / 左侧
 * @param showSearch 是否显示搜索图标
 * @param onSearchClick 搜索点击
 * @param showMoreMenu 是否显示更多菜单
 * @param onMoreClick 更多点击
 * @param customNavigationIcon 自定义导航图标（优先级高于返回键）
 * @param onCustomNavClick 自定义导航图标点击
 * @param customActions 自定义右侧操作图标组
 * @param scrollBehavior 滚动折叠行为
 * @param colors 标题栏颜色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    titleAlign: TopBarTitleAlign = TopBarTitleAlign.CENTER,
    showSearch: Boolean = false,
    onSearchClick: () -> Unit = {},
    showMoreMenu: Boolean = false,
    onMoreClick: () -> Unit = {},
    customNavigationIcon: ImageVector? = null,
    onCustomNavClick: () -> Unit = {},
    customActions: @Composable (() -> Unit) = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
) {
    val textStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        textAlign = if (titleAlign == TopBarTitleAlign.CENTER) TextAlign.Center else TextAlign.Start
    )

    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            style = textStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    val navIcon: @Composable () -> Unit = {
        when {
            customNavigationIcon != null -> {
                IconButton(onClick = onCustomNavClick) {
                    Icon(
                        imageVector = customNavigationIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            showBackButton -> {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            else -> {}
        }
    }

    val actions: @Composable () -> Unit = {
        if (showSearch) {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
        if (showMoreMenu) {
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
        customActions()
    }

    if (titleAlign == TopBarTitleAlign.CENTER) {
        CenterAlignedTopAppBar(
            title = titleContent,
            navigationIcon = navIcon,
//            actions = ,
            scrollBehavior = scrollBehavior,
            colors = colors
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navIcon,
//            actions = actions  as @Composable (RowScope.() -> Unit),
            scrollBehavior = scrollBehavior,
            colors = colors
        )
    }
}

/**
 * 简化版标题栏（仅标题 + 返回键）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppTopBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    titleAlign: TopBarTitleAlign = TopBarTitleAlign.CENTER
) {
    AppTopBar(
        title = title,
        showBackButton = showBackButton,
        onBackClick = onBackClick,
        titleAlign = titleAlign,
        showSearch = false,
        showMoreMenu = false
    )
}

/**
 * 标题对齐方式
 */
enum class TopBarTitleAlign {
    CENTER, LEFT
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "首页标题栏", showBackground = true)
@Composable
fun PreviewAppTopBarHome() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppTopBar(title = "智慧阅读")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "带返回键", showBackground = true)
@Composable
fun PreviewAppTopBarWithBack() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SimpleAppTopBar(title = "文章详情", showBackButton = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "全部功能", showBackground = true)
@Composable
fun PreviewAppTopBarFull() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppTopBar(
                title = "个人中心",
                showBackButton = true,
                showSearch = true,
                showMoreMenu = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "左对齐标题", showBackground = true)
@Composable
fun PreviewAppTopBarLeftAlign() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AppTopBar(
                title = "设置",
                showBackButton = true,
                titleAlign = TopBarTitleAlign.LEFT
            )
        }
    }
}
package com.example.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ColorTheme
import com.example.ui.theme.FontOption
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.UiConfig

/**
 * 设置侧边栏。从左侧滑入，带有半透明遮罩层。
 * 提供配色主题、主题模式、界面字体、阅读字体的切换。
 *
 * @param isVisible 是否显示
 * @param uiConfig 当前 UI 配置
 * @param onDismiss 关闭侧边栏回调
 * @param onUpdateThemeMode 更新主题模式
 * @param onUpdateUiFont 更新界面字体
 * @param onUpdateReadingFont 更新阅读字体
 * @param onUpdateColorTheme 更新配色主题
 * @param isFloatingServiceActive 悬浮窗服务是否已开启
 * @param onToggleFloatingService 切换悬浮窗服务开关
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsSidebar(
    isVisible: Boolean,
    uiConfig: UiConfig,
    onDismiss: () -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onUpdateUiFont: (FontOption) -> Unit,
    onUpdateReadingFont: (FontOption) -> Unit,
    onUpdateColorTheme: (ColorTheme) -> Unit,
    isFloatingServiceActive: Boolean = false,
    onToggleFloatingService: () -> Unit = {}
) {

    // 遮罩层 + 侧边栏容器
    Box(modifier = Modifier.fillMaxSize()) {
        // 半透明遮罩层 — 点击关闭
        if (isVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickable(onClick = onDismiss)
            )
        }

        // 侧边栏主体（从左侧滑入）
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.78f),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── 头部：标题 + 关闭按钮 ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭设置",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── 主题设置区块 ──
                    SectionHeader(title = "主题模式")
                    Spacer(modifier = Modifier.height(12.dp))

                    ThemeMode.entries.forEach { mode ->
                        val isSelected = uiConfig.themeMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateThemeMode(mode) },
                            label = {
                                Text(
                                    text = mode.displayName,
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 3.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── 配色主题区块 ──
                    SectionHeader(title = "配色主题")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "每种配色都有对应的浅色/深色模式",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ColorTheme.entries.forEach { theme ->
                            ColorThemeChip(
                                theme = theme,
                                isSelected = uiConfig.colorTheme == theme,
                                onClick = { onUpdateColorTheme(theme) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── 界面字体设置区块 ──
                    SectionHeader(title = "界面字体")
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontOption.entries.forEach { font ->
                            FontChip(
                                font = font,
                                isSelected = uiConfig.uiFont == font,
                                onClick = { onUpdateUiFont(font) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── 阅读字体设置区块 ──
                    SectionHeader(title = "阅读字体")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "仅影响图书正文内容",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontOption.entries.forEach { font ->
                            FontChip(
                                font = font,
                                isSelected = uiConfig.readingFont == font,
                                onClick = { onUpdateReadingFont(font) },
                                previewText = "预览"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── 悬浮窗设置区块 ──
                    SectionHeader(title = "悬浮窗")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "开启后可在其他应用上层显示悬浮球，退出App后不消失",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "OCR 悬浮截图",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = isFloatingServiceActive,
                            onCheckedChange = { onToggleFloatingService() }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * 设置区块的标题文字。
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 20.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * 字体选项的选择芯片。
 * 根据 [isSelected] 状态切换填充样式，并可选在芯片内预览字体效果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontChip(
    font: FontOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewText: String? = null
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = font.displayName,
                fontFamily = font.toFontFamily(),
                fontSize = 14.sp
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

/**
 * 配色主题的选择芯片。
 * 左侧带有彩色圆点指示器，右侧显示主题名称。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorThemeChip(
    theme: ColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 彩色圆点 — 展示该主题的主色
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(theme.accentColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = theme.displayName,
                    fontSize = 14.sp
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

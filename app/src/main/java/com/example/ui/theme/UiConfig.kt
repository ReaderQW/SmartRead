package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * 主题模式枚举。
 * SYSTEM = 跟随系统设置，LIGHT = 强制浅色，DARK = 强制深色。
 */
enum class ThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
}

/**
 * 字体选项枚举。
 * 提供四种选择：系统默认、衬线体、无衬线体、等宽体。
 */
enum class FontOption(val displayName: String) {
    DEFAULT("系统默认"),
    SERIF("衬线体"),
    SANS_SERIF("无衬线体"),
    MONOSPACE("等宽体");

    /**
     * 将字体选项转换为 Compose [FontFamily]。
     */
    fun toFontFamily(): FontFamily = when (this) {
        DEFAULT -> FontFamily.Default
        SERIF -> FontFamily.Serif
        SANS_SERIF -> FontFamily.SansSerif
        MONOSPACE -> FontFamily.Monospace
    }
}

/**
 * 配色主题枚举。
 * 每个主题包含对应的浅色/深色配色方案。
 */
enum class ColorTheme(val displayName: String, val accentColor: Color) {
    PURPLE("典雅紫", Color(0xFF6750A4)),
    GREEN("清新绿", Color(0xFF4A7C5F)),
    BLUE("静谧蓝", Color(0xFF3F6B9C)),
    ORANGE("暖阳橙", Color(0xFFB86E2C)),
    PINK("蔷薇粉", Color(0xFF9C4A6A))
}

/**
 * UI 全局配置状态，控制界面字体、阅读字体、配色主题和颜色主题。
 *
 * @property themeMode 颜色主题模式（跟随系统/浅色/深色）
 * @property uiFont 界面字体（影响按钮、标签、导航等所有 Material 组件）
 * @property readingFont 阅读字体（仅影响图书正文内容）
 * @property colorTheme 配色主题（典雅紫/清新绿/静谧蓝/暖阳橙/蔷薇粉）
 */
data class UiConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val uiFont: FontOption = FontOption.DEFAULT,
    val readingFont: FontOption = FontOption.SERIF,
    val colorTheme: ColorTheme = ColorTheme.PURPLE
)

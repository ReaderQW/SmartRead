package com.example.presentation

import android.graphics.drawable.GradientDrawable
import com.example.ui.theme.ColorTheme

/**
 * 悬浮球主题颜色的静态桥接。
 *
 * ViewModel 在主题变化时写入当前颜色的 ARGB int 值，
 * SmartReadFloatingService 在显示悬浮球时读取并生成渐变。
 *
 * Service 与 Compose/ViewModel 处于同一进程，静态对象通信安全且高效。
 */
object FloatingThemeColors {

    /** 渐变起始色（primary） */
    @Volatile
    var startColor: Int = 0xFF6750A4.toInt()

    /** 渐变结束色（tertiary） */
    @Volatile
    var endColor: Int = 0xFF7D5260.toInt()

    /** 边框颜色（取 primaryContainer 色系的半透明值） */
    @Volatile
    var borderColor: Int = 0xB3D0BCFF.toInt()

    /**
     * 根据 ColorTheme 更新颜色。
     * 颜色取自紫色主题 lightColorScheme 的 primary / tertiary。
     * 边框色对应 primaryContainer。
     */
    fun updateFromTheme(theme: ColorTheme) {
        val triple = when (theme) {
            ColorTheme.PURPLE  -> Triple(0xFF6750A4.toInt(), 0xFF7D5260.toInt(), 0xB3D0BCFF.toInt())
            ColorTheme.GREEN   -> Triple(0xFF2E7D5A.toInt(), 0xFF7C5F4A.toInt(), 0xB3C8F0D9.toInt())
            ColorTheme.BLUE    -> Triple(0xFF3F6B9C.toInt(), 0xFF6B587C.toInt(), 0xB3D6E4F0.toInt())
            ColorTheme.ORANGE  -> Triple(0xFFB86E2C.toInt(), 0xFF5F5C4A.toInt(), 0xB3FCE4C8.toInt())
            ColorTheme.PINK    -> Triple(0xFF9C4A6A.toInt(), 0xFF5F5C4A.toInt(), 0xB3F3D4E0.toInt())
        }
        startColor = triple.first
        endColor = triple.second
        borderColor = triple.third
    }

    /**
     * 创建 TL_BR 对角线渐变的 GradientDrawable，用于悬浮球背景。
     *
     * @param shape 图形形状，默认 OVAL
     * @param strokeWidthPx 边框宽度（px），0 表示无边框
     */
    fun createGradientDrawable(
        shape: Int = GradientDrawable.OVAL,
        strokeWidthPx: Float = 0f
    ): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(startColor, endColor)
        ).apply {
            this.shape = shape
            if (strokeWidthPx > 0f) {
                setStroke(strokeWidthPx.toInt(), borderColor)
            }
        }
    }
}

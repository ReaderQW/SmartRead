package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily

// ── Purple (典雅紫) ──
private val PurpleLightScheme =
  lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8DEF8),
    onPrimaryContainer = Color(0xFF1D192B),
    secondary = Color(0xFF49454F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    surfaceTint = Color(0xFF6750A4)
  )

private val PurpleDarkScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1B25),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF141218),
    surfaceTint = Color(0xFFD0BCFF)
  )

// ── Green (清新绿) ──
private val GreenLightScheme =
  lightColorScheme(
    primary = Color(0xFF2E7D5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F0D9),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4A7C5F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8D5),
    onSecondaryContainer = Color(0xFF092013),
    tertiary = Color(0xFF7C5F4A),
    onTertiary = Color.White,
    background = Color(0xFFFBFDF8),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFBFDF8),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5DB),
    onSurfaceVariant = Color(0xFF42493F),
    outline = Color(0xFF72796F),
    outlineVariant = Color(0xFFC2C9BE),
    inverseSurface = Color(0xFF2F312E),
    inverseOnSurface = Color(0xFFF0F1EC),
    surfaceTint = Color(0xFF2E7D5A)
  )

private val GreenDarkScheme =
  darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF1B6B3F),
    onPrimaryContainer = Color(0xFFC8F0D9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF00391E),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFCDE8D5),
    tertiary = Color(0xFFD7B59E),
    onTertiary = Color(0xFF3A2414),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DF),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DF),
    surfaceVariant = Color(0xFF42493F),
    onSurfaceVariant = Color(0xFFC2C9BE),
    outline = Color(0xFF8C9389),
    outlineVariant = Color(0xFF42493F),
    inverseSurface = Color(0xFFE2E3DF),
    inverseOnSurface = Color(0xFF1A1C19),
    surfaceTint = Color(0xFF81C784)
  )

// ── Blue (静谧蓝) ──
private val BlueLightScheme =
  lightColorScheme(
    primary = Color(0xFF3F6B9C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4F0),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFF3D5A7C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3F8),
    onSecondaryContainer = Color(0xFF001A33),
    tertiary = Color(0xFF6B587C),
    onTertiary = Color.White,
    background = Color(0xFFF8F9FD),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FD),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72787E),
    outlineVariant = Color(0xFFC2C7CF),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF0F0F4),
    surfaceTint = Color(0xFF3F6B9C)
  )

private val BlueDarkScheme =
  darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF1F5A92),
    onPrimaryContainer = Color(0xFFD6E4F0),
    secondary = Color(0xFFAAC7FF),
    onSecondary = Color(0xFF002F54),
    secondaryContainer = Color(0xFF264B73),
    onSecondaryContainer = Color(0xFFD3E3F8),
    tertiary = Color(0xFFCFBCE3),
    onTertiary = Color(0xFF362748),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9198),
    outlineVariant = Color(0xFF42474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    surfaceTint = Color(0xFF90CAF9)
  )

// ── Orange (暖阳橙) ──
private val OrangeLightScheme =
  lightColorScheme(
    primary = Color(0xFFB86E2C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4C8),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF7C6B3D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4C8),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = Color(0xFF5F5C4A),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF1E1B16),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1E1B16),
    surfaceVariant = Color(0xFFF0E8D7),
    onSurfaceVariant = Color(0xFF5A4E3E),
    outline = Color(0xFF827568),
    outlineVariant = Color(0xFFD3C7B9),
    inverseSurface = Color(0xFF33302B),
    inverseOnSurface = Color(0xFFF6F0E7),
    surfaceTint = Color(0xFFB86E2C)
  )

private val OrangeDarkScheme =
  darkColorScheme(
    primary = Color(0xFFFFB74D),
    onPrimary = Color(0xFF4A2000),
    primaryContainer = Color(0xFF8F501E),
    onPrimaryContainer = Color(0xFFFCE4C8),
    secondary = Color(0xFFDEC69A),
    onSecondary = Color(0xFF3A2C10),
    secondaryContainer = Color(0xFF534325),
    onSecondaryContainer = Color(0xFFFCE4C8),
    tertiary = Color(0xFFC3C09E),
    onTertiary = Color(0xFF2C2B1C),
    background = Color(0xFF1E1B16),
    onBackground = Color(0xFFE6E1D9),
    surface = Color(0xFF1E1B16),
    onSurface = Color(0xFFE6E1D9),
    surfaceVariant = Color(0xFF5A4E3E),
    onSurfaceVariant = Color(0xFFD3C7B9),
    outline = Color(0xFF96897A),
    outlineVariant = Color(0xFF5A4E3E),
    inverseSurface = Color(0xFFE6E1D9),
    inverseOnSurface = Color(0xFF1E1B16),
    surfaceTint = Color(0xFFFFB74D)
  )

// ── Pink (蔷薇粉) ──
private val PinkLightScheme =
  lightColorScheme(
    primary = Color(0xFF9C4A6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D4E0),
    onPrimaryContainer = Color(0xFF3D0722),
    secondary = Color(0xFF7C4A5B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3D4E0),
    onSecondaryContainer = Color(0xFF3D0722),
    tertiary = Color(0xFF5F5C4A),
    onTertiary = Color.White,
    background = Color(0xFFFFF8F9),
    onBackground = Color(0xFF201A1C),
    surface = Color(0xFFFFF8F9),
    onSurface = Color(0xFF201A1C),
    surfaceVariant = Color(0xFFF0DDE5),
    onSurfaceVariant = Color(0xFF5A4E4E),
    outline = Color(0xFF82757A),
    outlineVariant = Color(0xFFD3C5C8),
    inverseSurface = Color(0xFF352F31),
    inverseOnSurface = Color(0xFFF7EEF0),
    surfaceTint = Color(0xFF9C4A6A)
  )

private val PinkDarkScheme =
  darkColorScheme(
    primary = Color(0xFFF48FB1),
    onPrimary = Color(0xFF5E1137),
    primaryContainer = Color(0xFF7A2D50),
    onPrimaryContainer = Color(0xFFF3D4E0),
    secondary = Color(0xFFD6A0B0),
    onSecondary = Color(0xFF3D0722),
    secondaryContainer = Color(0xFF5C2440),
    onSecondaryContainer = Color(0xFFF3D4E0),
    tertiary = Color(0xFFC3C0A0),
    onTertiary = Color(0xFF2C2B1C),
    background = Color(0xFF201A1C),
    onBackground = Color(0xFFEBE0E2),
    surface = Color(0xFF201A1C),
    onSurface = Color(0xFFEBE0E2),
    surfaceVariant = Color(0xFF5A4E4E),
    onSurfaceVariant = Color(0xFFD3C5C8),
    outline = Color(0xFF96898B),
    outlineVariant = Color(0xFF5A4E4E),
    inverseSurface = Color(0xFFEBE0E2),
    inverseOnSurface = Color(0xFF201A1C),
    surfaceTint = Color(0xFFF48FB1)
  )

/**
 * 根据 [ColorTheme] 和深浅模式返回对应的 [ColorScheme]。
 */
private fun colorSchemeFor(colorTheme: ColorTheme, isDark: Boolean): ColorScheme {
  return when (colorTheme) {
    ColorTheme.PURPLE -> if (isDark) PurpleDarkScheme else PurpleLightScheme
    ColorTheme.GREEN -> if (isDark) GreenDarkScheme else GreenLightScheme
    ColorTheme.BLUE -> if (isDark) BlueDarkScheme else BlueLightScheme
    ColorTheme.ORANGE -> if (isDark) OrangeDarkScheme else OrangeLightScheme
    ColorTheme.PINK -> if (isDark) PinkDarkScheme else PinkLightScheme
  }
}

/**
 * 使用 UI 配置的全局主题。
 *
 * @param themeMode 主题模式：跟随系统 / 浅色 / 深色
 * @param uiFontFamily 界面全局字体，会应用到所有 Material 组件
 * @param colorTheme 配色主题（典雅紫/清新绿/静谧蓝/暖阳橙/蔷薇粉）
 * @param dynamicColor 是否启用 Android 12+ 动态取色（默认关闭以保持自定义设计）
 * @param content 子组合项
 */
@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  uiFontFamily: FontFamily = FontFamily.Default,
  colorTheme: ColorTheme = ColorTheme.PURPLE,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val darkTheme = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      else -> colorSchemeFor(colorTheme, darkTheme)
    }

  val typography = Typography.withFontFamily(uiFontFamily)

  MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}

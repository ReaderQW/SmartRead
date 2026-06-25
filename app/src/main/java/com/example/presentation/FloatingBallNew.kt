package com.example.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

/**
 * 新悬浮球 — Material 3 FAB 风格
 *
 * 支持可选的边框样式，通过 borderWidth / borderColor 控制。
 */
@Composable
fun FloatingBallModern(
    icon: ImageVector = Icons.Default.CameraAlt,
    size: Dp = 56.dp,
    colors: List<Color> = listOf(Color(0xFF6750A4), Color(0xFF7D5260)),
    elevation: Dp = 6.dp,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var actualBorderWidth = borderWidth
    var actualBorderColor = borderColor

    // 如果设了 borderWidth 但没指定颜色，默认半透明白色描边
    if (borderWidth > 0.dp && borderColor == Color.Transparent) {
        actualBorderColor = Color.White.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier
            .size(size)
            .scale(pulseScale)
            .shadow(elevation, CircleShape)
            .then(
                if (actualBorderWidth > 0.dp)
                    Modifier.border(actualBorderWidth, actualBorderColor, CircleShape)
                else Modifier
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(colors)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "悬浮窗",
            tint = Color.White,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

// ══════════════════════════════════════════════
//  预览 1：边框方案对比
// ══════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun FloatingBallBorderPreview() {
    MyApplicationTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "悬浮球 · 边框方案",
                    fontSize = 22.sp,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(8.dp))

                BorderVariantCard(
                    title = "无边框（当前）",
                    description = "纯渐变 + 阴影，无额外描边",
                    ball = { FloatingBallModern(size = 64.dp, elevation = 6.dp) }
                )

                BorderVariantCard(
                    title = "细白边 · 1dp",
                    description = "半透明白色描边，轻微发光感",
                    ball = {
                        FloatingBallModern(
                            size = 64.dp, elevation = 6.dp,
                            borderWidth = 1.dp, borderColor = Color.White.copy(alpha = 0.45f)
                        )
                    }
                )

                BorderVariantCard(
                    title = "粗白边 · 2.5dp",
                    description = "明显白色边框，像 FAB 凸出效果",
                    ball = {
                        FloatingBallModern(
                            size = 64.dp, elevation = 4.dp,
                            borderWidth = 2.5.dp, borderColor = Color.White.copy(alpha = 0.6f)
                        )
                    }
                )

                BorderVariantCard(
                    title = "主题色边 · 1.5dp",
                    description = "primaryContainer #E8DEF8 描边",
                    ball = {
                        FloatingBallModern(
                            size = 64.dp, elevation = 6.dp,
                            borderWidth = 1.5.dp, borderColor = Color(0xFFE8DEF8)
                        )
                    }
                )

                BorderVariantCard(
                    title = "双色边 · 2dp",
                    description = "渐变底色 + 浅色描边，层次更丰富",
                    ball = {
                        FloatingBallModern(
                            size = 64.dp, elevation = 6.dp,
                            borderWidth = 2.dp, borderColor = Color(0xFFD0BCFF).copy(alpha = 0.7f)
                        )
                    }
                )

                BorderVariantCard(
                    title = "亮边发光 · 1dp glow",
                    description = "细白边 + 高阴影，外发光效果",
                    ball = {
                        FloatingBallModern(
                            size = 64.dp, elevation = 10.dp,
                            borderWidth = 1.dp, borderColor = Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }
    }
}

// ══════════════════════════════════════════════
//  预览 2：真实场景（推荐边框方案）
// ══════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun FloatingBallPositionBorderPreview() {
    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "模拟其他 App 界面",
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFCCCCCC),
                fontSize = 18.sp
            )
            Box(modifier = Modifier.offset(x = 16.dp, y = 120.dp)) {
                FloatingBallModern(
                    borderWidth = 1.5.dp,
                    borderColor = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════
//  预览 3：大尺寸细节
// ══════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF6750A4)
@Composable
private fun FloatingBallAloneBorderPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        FloatingBallModern(
            size = 80.dp,
            elevation = 8.dp,
            borderWidth = 2.dp,
            borderColor = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ══════════════════════════════════════════════
//  辅助组件
// ══════════════════════════════════════════════

@Composable
private fun BorderVariantCard(
    title: String,
    description: String,
    ball: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ball()
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = Color(0xFF333333)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF888888),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

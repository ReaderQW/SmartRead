package com.example.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

/**
 * 新悬浮窗菜单 — Material 3 卡片样式
 * 点击悬浮球后弹出，带半透明遮罩 + 居中卡片 + Ripple 反馈
 */
@Composable
fun FloatingMenuNew(
    onOcrClick: () -> Unit = {},
    onAiChatClick: () -> Unit = {},
    onHideClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    // 遮罩透明度动画
    var visible by remember { mutableStateOf(true) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "menuAlpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = tween(250),
        label = "menuScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * alpha))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // 菜单卡片 — 点击遮罩不会穿透到卡片
        Card(
            modifier = Modifier
                .width(200.dp)
                .scale(scale)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MenuItemCard(
                    icon = Icons.Default.ScreenSearchDesktop,
                    label = "OCR 截图识别",
                    desc = "截取屏幕并提取文字",
                    onClick = {
                        visible = false
                        onOcrClick()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                MenuItemCard(
                    icon = Icons.Default.Psychology,
                    label = "AI 伴读",
                    desc = "苏格拉底式对话",
                    onClick = {
                        visible = false
                        onAiChatClick()
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
                MenuItemCard(
                    icon = Icons.Default.Close,
                    label = "隐藏悬浮窗",
                    desc = null,
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        visible = false
                        onHideClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    icon: ImageVector,
    label: String,
    desc: String?,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (desc != null) {
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── 预览 ──

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun FloatingMenuNewPreview() {
    MyApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingMenuNew()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
private fun FloatingMenuNewPreview_NoScrim() {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .padding(20.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            MenuItemCard(
                icon = Icons.Default.ScreenSearchDesktop,
                label = "OCR 截图识别",
                desc = "截取屏幕并提取文字",
                onClick = {}
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            MenuItemCard(
                icon = Icons.Default.Psychology,
                label = "AI 伴读",
                desc = "苏格拉底式对话",
                onClick = {}
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            MenuItemCard(
                icon = Icons.Default.Close,
                label = "隐藏悬浮窗",
                desc = null,
                textColor = MaterialTheme.colorScheme.error,
                onClick = {}
            )
        }
    }
}

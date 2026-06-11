package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsControlPanel(
    isPlaying: Boolean,
    volume: Int,
    currentVcn: String,
    onTogglePlay: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onVcnChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showVcnMenu by remember { mutableStateOf(false) }

    val vcnOptions = listOf(
        "俊朗男声" to "M24",
        "知性柔美" to "F245_natural",
        "电台主播" to "GAME_GIR_LTY"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 语音状态图标（总入口）
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                contentDescription = "TTS Panel",
                modifier = Modifier.size(16.dp),
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally(),
            exit = shrinkHorizontally()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                // 播放/暂停
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // 音量滑块
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 1f..100f,
                    modifier = Modifier.width(60.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                // 音色选择
                Box {
                    TextButton(
                        onClick = { showVcnMenu = true },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = vcnOptions.find { it.second == currentVcn }?.first ?: "未知",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    DropdownMenu(
                        expanded = showVcnMenu,
                        onDismissRequest = { showVcnMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        vcnOptions.forEach { (label, code) ->
                            DropdownMenuItem(
                                text = { Text(label, fontSize = 12.sp) },
                                onClick = {
                                    onVcnChange(code)
                                    showVcnMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

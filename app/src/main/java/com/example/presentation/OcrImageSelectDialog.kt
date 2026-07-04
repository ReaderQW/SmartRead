package com.example.presentation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SmartReadViewModel
import com.example.utils.OcrTextRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * OCR 图片选区全屏覆盖层（作为主 Activity 布局的一部分）
 * 显示截图 → 用户手指拖拽框选文字区域 → 裁剪 OCR → 确认存为笔记
 *
 * 使用 [BoxWithConstraints] 来保证布局正确适配系统栏 insets，
 * 不依赖 Dialog 窗口（Dialog 中无法正确获取 system bars insets）。
 */
@Composable
fun OcrImageSelectDialog(
    screenshotPath: String,
    viewModel: SmartReadViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // ── 状态 ──
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bitmapLoading by remember { mutableStateOf(true) }

    var selectionStart by remember { mutableStateOf<Offset?>(null) } // bitmap 坐标
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }
    var isOcrLoading by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf<String?>(null) }
    var ocrError by remember { mutableStateOf<String?>(null) }
    var userNoteInput by remember { mutableStateOf("") }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isSaving by remember { mutableStateOf(false) }
    var noBookSelected by remember { mutableStateOf(false) }

    // 加载 Bitmap
    LaunchedEffect(screenshotPath) {
        withContext(Dispatchers.IO) {
            bitmap = try {
                BitmapFactory.decodeFile(screenshotPath)
            } catch (_: Exception) {
                null
            }
            bitmapLoading = false
        }
    }

    // 全屏黑色遮罩 — 作为主 Activity 布局的直接子级
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(enabled = false) { /* 阻断下层点击 */ }
    ) {
        when {
            bitmapLoading -> {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            bitmap == null -> {
                Text("加载图片失败", color = Color.White, modifier = Modifier.align(Alignment.Center))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Text("关闭", color = Color.White)
                }
            }
            else -> {
                val bmp = bitmap!!

                // 计算图片显示区域（像素坐标），ContentScale.Fit 等比缩放居中
                val fitScale = if (containerSize.width > 0 && containerSize.height > 0) {
                    minOf(
                        containerSize.width.toFloat() / bmp.width,
                        containerSize.height.toFloat() / bmp.height
                    )
                } else 1f
                val displayedW = bmp.width * fitScale
                val displayedH = bmp.height * fitScale
                val imgOffsetX = (containerSize.width - displayedW) / 2f
                val imgOffsetY = (containerSize.height - displayedH) / 2f

                /** 显示坐标 → 原始 Bitmap 坐标 */
                fun dispToBmp(disp: Offset): Offset {
                    return Offset(
                        ((disp.x - imgOffsetX) / fitScale).coerceIn(0f, bmp.width.toFloat()),
                        ((disp.y - imgOffsetY) / fitScale).coerceIn(0f, bmp.height.toFloat())
                    )
                }

                /** 获取当前选区（Bitmap 坐标） */
                fun currentSelectionRect(): Rect? {
                    val s = selectionStart ?: return null
                    val e = selectionEnd ?: return null
                    if (kotlin.math.abs(e.x - s.x) < 10f && kotlin.math.abs(e.y - s.y) < 10f) return null
                    return Rect(
                        topLeft = Offset(minOf(s.x, e.x), minOf(s.y, e.y)),
                        bottomRight = Offset(maxOf(s.x, e.x), maxOf(s.y, e.y))
                    )
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // ── 顶部栏（在安全区内） ──
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消", color = Color.White)
                        }
                        Text(
                            when {
                                recognizedText != null -> "识别结果"
                                isOcrLoading -> "识别中..."
                                else -> "框选文字区域"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (recognizedText != null) {
                            TextButton(onClick = {
                                selectionStart = null
                                selectionEnd = null
                                recognizedText = null
                                ocrError = null
                                userNoteInput = ""
                                noBookSelected = false
                            }) {
                                Text("重选", color = Color.White)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(72.dp))
                        }
                    }

                    // ── 图片 + 选区 Canvas（撑满剩余空间） ──
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged { containerSize = it }
                    ) {
                        if (containerSize.width > 0 && containerSize.height > 0) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // 1. 绘制图片
                                val painter = BitmapPainter(bmp.asImageBitmap())
                                withTransform({
                                    translate(left = imgOffsetX, top = imgOffsetY)
                                    scale(scaleX = fitScale, scaleY = fitScale, pivot = Offset.Zero)
                                }) {
                                    with(painter) {
                                        draw(size = Size(bmp.width.toFloat(), bmp.height.toFloat()))
                                    }
                                }

                                // 2. 选区遮罩 + 边框
                                val selRect = currentSelectionRect()
                                if (selRect != null) {
                                    val sDispX = selRect.left * fitScale + imgOffsetX
                                    val sDispY = selRect.top * fitScale + imgOffsetY
                                    val sDispW = selRect.width * fitScale
                                    val sDispH = selRect.height * fitScale

                                    // 在图片范围内，选区外部盖暗色遮罩
                                    clipRect(
                                        left = imgOffsetX,
                                        top = imgOffsetY,
                                        right = imgOffsetX + displayedW,
                                        bottom = imgOffsetY + displayedH
                                    ) {
                                        clipRect(
                                            left = sDispX,
                                            top = sDispY,
                                            right = sDispX + sDispW,
                                            bottom = sDispY + sDispH,
                                            clipOp = ClipOp.Difference
                                        ) {
                                            drawRect(
                                                Color.Black.copy(alpha = 0.5f),
                                                size = this.size
                                            )
                                        }
                                    }

                                    // 选区白色边框
                                    drawRect(
                                        Color.White,
                                        topLeft = Offset(sDispX, sDispY),
                                        size = Size(sDispW, sDispH),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            // 触摸事件（仅在未识别时允许拖选）
                            if (recognizedText == null && !isOcrLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(containerSize) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    val bmpPt = dispToBmp(offset)
                                                    if (bmpPt.x in 0f..bmp.width.toFloat() &&
                                                        bmpPt.y in 0f..bmp.height.toFloat()
                                                    ) {
                                                        selectionStart = bmpPt
                                                        selectionEnd = bmpPt
                                                    }
                                                },
                                                onDrag = { change, _ ->
                                                    selectionEnd = dispToBmp(change.position)
                                                    change.consume()
                                                },
                                                onDragEnd = {
                                                    val rect = currentSelectionRect()
                                                    if (rect != null) {
                                                        isOcrLoading = true
                                                        scope.launch {
                                                            try {
                                                                val cropX = rect.left.toInt().coerceIn(0, bmp.width - 1)
                                                                val cropY = rect.top.toInt().coerceIn(0, bmp.height - 1)
                                                                val cropW = rect.width.toInt().coerceIn(1, bmp.width - cropX)
                                                                val cropH = rect.height.toInt().coerceIn(1, bmp.height - cropY)
                                                                val cropped = Bitmap.createBitmap(bmp, cropX, cropY, cropW, cropH)

                                                                val result = withContext(Dispatchers.IO) {
                                                                    OcrTextRecognizer.recognizeSuspend(cropped)
                                                                }
                                                                result.fold(
                                                                    onSuccess = { text ->
                                                                        recognizedText = text
                                                                    },
                                                                    onFailure = { error ->
                                                                        ocrError = error.message ?: "识别失败"
                                                                    }
                                                                )
                                                            } catch (e: Exception) {
                                                                ocrError = e.message ?: "处理失败"
                                                            } finally {
                                                                isOcrLoading = false
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                )
                            }

                            // OCR 加载指示器
                            if (isOcrLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("识别中...", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    // ── 底部面板：提示 / 识别结果（在安全区内） ──
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1C1C1E),
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .navigationBarsPadding()
                                .verticalScroll(rememberScrollState())
                        ) {
                            when {
                                // 成功识别
                                recognizedText != null -> {
                                    Text("识别文本", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = recognizedText ?: "",
                                        onValueChange = { recognizedText = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp),
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4A90D9),
                                            unfocusedBorderColor = Color(0xFF555555),
                                            cursorColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("我的批注", color = Color.Gray, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = userNoteInput,
                                        onValueChange = { userNoteInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("输入你的想法...", color = Color.Gray, fontSize = 14.sp) },
                                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4A90D9),
                                            unfocusedBorderColor = Color(0xFF555555),
                                            cursorColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            val bookId = viewModel.currentBookId.value
                                            if (bookId == null) {
                                                noBookSelected = true
                                                return@Button
                                            }
                                            val text = recognizedText?.trim()
                                            if (!text.isNullOrEmpty()) {
                                                isSaving = true
                                                viewModel.saveNote(
                                                    text,
                                                    userNoteInput.ifEmpty { "OCR 选区识别" }
                                                )
                                                onDismiss()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isSaving && recognizedText?.isNotEmpty() == true,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4A90D9)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        if (isSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("存为笔记", fontSize = 15.sp)
                                        }
                                    }
                                    if (noBookSelected) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "请先在书架选择一本书后再保存笔记",
                                            color = Color(0xFFFF6B6B),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                // 识别失败
                                ocrError != null -> {
                                    Text("识别失败: $ocrError", color = Color(0xFFFF6B6B), fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            selectionStart = null
                                            selectionEnd = null
                                            ocrError = null
                                            recognizedText = null
                                            noBookSelected = false
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                                    ) {
                                        Text("重新选择")
                                    }
                                }
                                // 提示
                                else -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👆", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "在图片上拖动手指框选要识别的文字区域",
                                            color = Color.Gray,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 治愈系：圆角撞色清新礼盒
 * 香芋紫+奶白，半透纱质丝带，盒盖向后滑动。
 */
@Composable
fun GiftBoxOpeningAnimation(
    onFinished: () -> Unit
) {
    var isOpened by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    val openProgress = animateFloatAsState(
        targetValue = if (isOpened) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "open_progress"
    )

    val bokehParticles = remember { List(25) { BokehParticle() } }
    
    LaunchedEffect(isOpened) {
        if (isOpened) {
            delay(2200)
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (!isOpened) isOpened = true
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(320.dp)) {
            val boxW = 130.dp.toPx()
            val bowSize = 65.dp.toPx() // 增大蝴蝶结尺寸
            val tilt = boxW * 0.22f
            
            translate(center.x, center.y) {
                
                // 1. 溢出的淡紫色朦胧光斑
                if (isOpened) {
                    bokehParticles.forEach { it.draw(this, openProgress.value) }
                }

                // 2. 礼盒主体
                drawHealingBox(boxW, openProgress.value)
                
                // 3. 纱带蝴蝶结 (随盖子移动并飞扬)
                if (openProgress.value < 0.95f) {
                    val slideBackY = -openProgress.value * boxW * 0.6f
                    val slideBackX = -openProgress.value * boxW * 0.3f
                    
                    withTransform({
                        // 起点精确贴合盒盖轴心 (-tilt)
                        translate(left = slideBackX, top = -tilt + slideBackY)
                        // 轻微旋转飞扬
                        rotate(openProgress.value * 20f, pivot = Offset.Zero)
                        val s = (1f - openProgress.value * 0.5f).coerceIn(0.5f, 1f)
                        scale(s, s, Offset.Zero)
                    }) {
                        drawGauzeBow(bowSize, openProgress.value)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawHealingBox(w: Float, progress: Float) {
    val h = w * 0.75f
    val tilt = w * 0.22f
    val radius = 12.dp.toPx()
    
    // 治愈系配色
    val colorTaro = Color(0xFFD1C4E9)      // 香芋紫
    val colorTaroDark = Color(0xFFB39DDB)  // 侧面阴影
    val colorMilk = Color(0xFFFDFCF0)      // 奶白色
    val colorGauze = Color(0xFFE1BEE7).copy(alpha = 0.6f) // 半透纱质
    
    // --- 1. 盒身 (奶白色圆角方形) ---
    val bodyPathLeft = Path().apply {
        moveTo(0f, tilt)
        lineTo(-w/2, 0f)
        lineTo(-w/2, h - radius)
        quadraticTo(-w/2, h, -w/2 + radius, h)
        lineTo(0f, tilt + h)
        close()
    }
    drawPath(bodyPathLeft, colorMilk)
    
    val bodyPathRight = Path().apply {
        moveTo(0f, tilt)
        lineTo(w/2, 0f)
        lineTo(w/2, h - radius)
        quadraticTo(w/2, h, w/2 - radius, h)
        lineTo(0f, tilt + h)
        close()
    }
    drawPath(bodyPathRight, colorMilk.copy(alpha = 0.9f))

    // --- 2. 盒身纱带 (细款环绕) ---
    val ribbonY = h * 0.4f
    val ribbonW = 12.dp.toPx()
    drawPath(Path().apply {
        moveTo(-w/2, ribbonY)
        lineTo(0f, tilt + ribbonY)
        lineTo(w/2, ribbonY)
        lineTo(w/2, ribbonY + ribbonW)
        lineTo(0f, tilt + ribbonY + ribbonW)
        lineTo(-w/2, ribbonY + ribbonW)
        close()
    }, colorGauze)

    // --- 3. 盒盖 (香芋紫，向后滑开) ---
    val slideBackY = -progress * w * 0.6f
    val slideBackX = -progress * w * 0.3f
    val lidAlpha = (1f - progress * 0.8f).coerceIn(0.2f, 1f)
    
    withTransform({
        translate(left = slideBackX, top = slideBackY)
    }) {
        val lidH = 15.dp.toPx()
        
        // 盖子顶部 (圆角菱形感)
        val lidTop = Path().apply {
            moveTo(0f, -tilt)
            lineTo(w/2, 0f)
            lineTo(0f, tilt)
            lineTo(-w/2, 0f)
            close()
        }
        drawPath(lidTop, colorTaro.copy(alpha = lidAlpha))
        
        // 盖子侧面 (增加圆角感)
        drawPath(Path().apply {
            moveTo(0f, tilt); lineTo(-w/2, 0f); lineTo(-w/2, lidH); lineTo(0f, tilt+lidH); close()
        }, colorTaroDark.copy(alpha = lidAlpha))
        drawPath(Path().apply {
            moveTo(0f, tilt); lineTo(w/2, 0f); lineTo(w/2, lidH); lineTo(0f, tilt+lidH); close()
        }, colorTaro.copy(alpha = lidAlpha))
        
        // 盖子上的细纱带
        drawPath(Path().apply {
            moveTo(-w/2, 0f); lineTo(0f, tilt); lineTo(w/2, 0f)
            lineTo(w/2, -5.dp.toPx()); lineTo(0f, tilt - 5.dp.toPx()); lineTo(-w/2, -5.dp.toPx())
            close()
        }, colorGauze.copy(alpha = lidAlpha))
    }
}

private fun DrawScope.drawGauzeBow(size: Float, progress: Float) {
    val color = Color(0xFFE1BEE7).copy(alpha = 0.8f) // 略微增加不透明度使线条更清晰
    val strokeW = 4.5.dp.toPx() // 进一步加粗线条
    
    // 中心小结 (确保在 Offset.Zero)
    drawCircle(color, size / 8, center = Offset.Zero)
    
    // 饱满的大蝴蝶结翼
    val wingLift = progress * 25f
    
    fun drawWing(isLeft: Boolean) {
        val sign = if (isLeft) -1f else 1f
        val path = Path().apply {
            // 起点从圆心出发
            moveTo(sign * size/12, 0f)
            cubicTo(
                sign * size * 0.85f, -size * 0.65f - wingLift, 
                sign * size * 1.15f, size * 0.45f,
                sign * size/12, size/12
            )
        }
        drawPath(path, color, style = Stroke(width = strokeW, cap = StrokeCap.Round))
    }
    
    drawWing(true)
    drawWing(false)
    
    // 自然下垂的长丝带
    val tailPath = Path().apply {
        // 从中心结下方出发
        moveTo(-size/15, size/10)
        quadraticTo(-size/2, size * 0.7f, -size/3, size * 1.1f + wingLift)
        moveTo(size/15, size/10)
        quadraticTo(size/2, size * 0.7f, size/3, size * 1.1f + wingLift)
    }
    drawPath(tailPath, color, style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round))
}

/**
 * 朦胧光斑：淡紫色、散景效果
 */
class BokehParticle {
    private val angle = Random.nextFloat() * 2 * PI
    private val speed = Random.nextFloat() * 150f + 50f
    private val color = Color(0xFFE1BEE7)
    private val radius = Random.nextFloat() * 20f + 10f

    fun draw(drawScope: DrawScope, progress: Float) {
        if (progress <= 0f) return
        val dist = speed * progress
        val x = (dist * cos(angle)).toFloat()
        val y = (dist * sin(angle)).toFloat() - (progress * 80f)
        val alpha = (0.4f * (1f - progress)).coerceIn(0f, 1f)
        
        drawScope.drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y),
            blendMode = BlendMode.Plus
        )
    }
}

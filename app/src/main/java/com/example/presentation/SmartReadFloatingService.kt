package com.example.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.OcrTextRecognizer
import com.example.utils.ScreenCaptureManager
import com.example.presentation.FloatingThemeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 悬浮阅读窗服务 — 小白圆形悬浮球，可在其他应用上层显示。
 * 点击展开菜单：OCR截图批注 / AI伴读 / 关闭悬浮窗。
 * App 退出后悬浮窗不消失，通过通知栏保持服务存活。
 */
class SmartReadFloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBall: View? = null
    private var menuPopup: View? = null
    private var ocrResultView: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "floating_service"
        const val NOTIFICATION_ID = 1001

        const val ACTION_OCR_RESULT = "com.example.action.OCR_RESULT"
        const val EXTRA_OCR_TEXT = "ocr_text"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            showFloatingBall()
        } catch (e: Exception) {
            showToast("悬浮窗启动失败: ${e.localizedMessage ?: e.javaClass.simpleName}")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeAllViews()
    }

    // ── 悬浮球 ──

    private fun showFloatingBall() {
        removeAllViews()

        val ball = ImageView(this).apply {
            // 矢量图标，任意 dpi 下不失真；CENTER_INSIDE 防止溢出边缘
            setImageResource(com.example.R.drawable.ic_camera_white)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(dpToPx(14), dpToPx(14), dpToPx(14), dpToPx(14))

            // 渐变背景 + 双色边（主题色 primaryContainer 色系）
            background = FloatingThemeColors.createGradientDrawable(
                shape = GradientDrawable.OVAL,
                strokeWidthPx = dpToPx(2).toFloat()
            )
        }

        floatingBall = ball

        val params = WindowManager.LayoutParams(
            dpToPx(56),
            dpToPx(56),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = dpToPx(16)
            y = dpToPx(120)
        }

        setupDrag(ball, params)

        windowManager.addView(ball, params)
    }

    private var isDragging = false
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private fun setupDrag(view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (kotlin.math.abs(dx) > 5 || kotlin.math.abs(dy) > 5) {
                        isDragging = true
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        showMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ── 菜单 ──

    private fun showMenu() {
        removeMenu()

        // 遮罩层 — 点击关闭菜单
        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#80000000"))
            setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                    removeMenu()
                    true
                } else false
            }
        }

        // 菜单卡片 — Material 3 风格
        val menuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(20).toFloat()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                elevation = dpToPx(3).toFloat()
            }
        }

        fun createMenuItem(
            emoji: String,
            label: String,
            desc: String?,
            textColor: Int,
            circleBg: Int = 0xFFF3E8FF.toInt(),
            onClick: () -> Unit
        ): View {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dpToPx(20), dpToPx(14), dpToPx(20), dpToPx(14))
                isClickable = true
                isFocusable = true
                // Ripple 点击反馈
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val outValue = android.util.TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    if (outValue.resourceId != 0) {
                        foreground = getDrawable(outValue.resourceId)
                    }
                }
                setOnClickListener {
                    onClick()
                    removeMenu()
                }
            }

            // 彩色圆形图标背景
            val iconCircle = TextView(this).apply {
                text = emoji
                textSize = 16f
                gravity = Gravity.CENTER
                val size = dpToPx(40)
                layoutParams = LinearLayout.LayoutParams(size, size)
                background = GradientDrawable().apply {
                    setColor(circleBg)
                    shape = GradientDrawable.OVAL
                }
            }
            row.addView(iconCircle)

            // 文字列
            val textColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding(dpToPx(14), 0, 0, 0)
            }
            textColumn.addView(TextView(this).apply {
                text = label
                textSize = 15f
                setTextColor(textColor)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            if (desc != null) {
                textColumn.addView(TextView(this).apply {
                    text = desc
                    textSize = 11f
                    setTextColor(Color.parseColor("#888888"))
                })
            }
            row.addView(textColumn)
            return row
        }

        fun createDivider(): View {
            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(dpToPx(20), 0, dpToPx(20), 0) }
            divider.setBackgroundColor(Color.parseColor("#EEEEEE"))
            return divider
        }

        // ── 组装菜单项 ──
        menuCard.addView(createMenuItem("📷", "OCR 截图识别", "截取屏幕并提取文字", Color.parseColor("#333333")) {
            performCaptureAndOcr()
        })
        menuCard.addView(createDivider())
        menuCard.addView(createMenuItem("💡", "AI 伴读", "苏格拉底式对话", Color.parseColor("#333333")) {
            Toast.makeText(this, "AI伴读已开启", Toast.LENGTH_SHORT).show()
        })
        menuCard.addView(createDivider())
        menuCard.addView(createMenuItem("✕", "隐藏悬浮窗", null, Color.parseColor("#CC3333"), circleBg = 0xFFFFE8E8.toInt()) {
            stopSelf()
        })

        // 菜单卡片放入遮罩
        container.addView(menuCard, FrameLayout.LayoutParams(
            dpToPx(200),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ))

        menuPopup = container

        val screenSize = resources.displayMetrics
        val params = WindowManager.LayoutParams(
            screenSize.widthPixels,
            screenSize.heightPixels,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(container, params)
    }

    private fun removeMenu() {
        menuPopup?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        menuPopup = null
    }

    private fun removeAllViews() {
        floatingBall?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        floatingBall = null
        removeMenu()
        removeOcrResultView()
    }

    // ── 截图 + OCR ──

    private fun performCaptureAndOcr() {
        // 检查 ScreenCaptureManager 是否已初始化
        if (!isScreenCaptureReady()) {
            showToast("请先在 App 内点击「OCR截图批注」按钮授权屏幕录制权限")
            // 发送广播通知 MainActivity 弹出授权
            val intent = Intent("com.example.action.REQUEST_SCREEN_CAPTURE")
            sendBroadcast(intent)
            return
        }

        showToast("正在截图识别...")

        ScreenCaptureManager.captureBitmapWithDelay(500) { bitmap ->
            if (bitmap == null) {
                showToast("截图失败，请重试")
                return@captureBitmapWithDelay
            }

            scope.launch {
                showOcrLoading()
                val result = withContext(Dispatchers.IO) {
                    OcrTextRecognizer.recognizeSuspend(bitmap)
                }
                hideOcrLoading()

                result.fold(
                    onSuccess = { text ->
                        val file = saveScreenshot(bitmap)
                        showOcrResult(text, file?.absolutePath)
                    },
                    onFailure = { error ->
                        showToast("OCR 识别失败: ${error.message}")
                    }
                )
            }
        }
    }

    private fun isScreenCaptureReady(): Boolean {
        return try {
            // 尝试获取一次截图，如果返回 null 说明未初始化
            val bitmap = ScreenCaptureManager.captureBitmap()
            bitmap != null
        } catch (_: Exception) {
            false
        }
    }

    private fun saveScreenshot(bitmap: Bitmap): java.io.File? {
        return try {
            val dir = java.io.File(filesDir, "ocr_screenshots")
            dir.mkdirs()
            val file = java.io.File(dir, "ocr_${System.currentTimeMillis()}.png")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    // ── OCR 加载中提示 ──

    private fun showOcrLoading() {
        handler.post {
            val loadingView = LayoutInflater.from(this).inflate(
                com.example.R.layout.layout_ocr_loading,
                null
            ) ?: return@post

            ocrResultView = loadingView

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(loadingView, params)
            } catch (_: Exception) {}
        }
    }

    private fun hideOcrLoading() {
        handler.post {
            ocrResultView?.let {
                try { windowManager.removeView(it) } catch (_: Exception) {}
            }
            ocrResultView = null
        }
    }

    // ── OCR 结果展示 ──

    private fun showOcrResult(text: String, screenshotPath: String?) {
        handler.post {
            val resultView = LayoutInflater.from(this).inflate(
                com.example.R.layout.layout_ocr_result,
                null
            ) ?: return@post

            val tvText = resultView.findViewById<TextView>(com.example.R.id.tv_ocr_text)
            val btnCreateBook = resultView.findViewById<Button>(com.example.R.id.btn_create_book)
            val btnDismiss = resultView.findViewById<Button>(com.example.R.id.btn_dismiss)

            // 显示识别文本（取前500字）
            val displayText = if (text.length > 500) text.take(500) + "..." else text
            tvText.text = "识别结果:\n$displayText"

            btnCreateBook.setOnClickListener {
                // 发送广播给 MainActivity 处理内容追加或书籍创建
                val intent = Intent(ACTION_OCR_RESULT).apply {
                    putExtra(EXTRA_OCR_TEXT, text)
                    putExtra(EXTRA_SCREENSHOT_PATH, screenshotPath)
                }
                sendBroadcast(intent)

                // 将 App 呼唤到前台，以便用户查看结果
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(launchIntent)
                } catch (_: Exception) {}

                removeOcrResultView()
                showToast("内容已发送至 App")
            }

            btnDismiss.setOnClickListener {
                removeOcrResultView()
            }

            ocrResultView = resultView

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager.addView(resultView, params)
            } catch (_: Exception) {
                showToast("显示结果失败")
            }
        }
    }

    private fun removeOcrResultView() {
        ocrResultView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        ocrResultView = null
    }

    private fun showToast(msg: String) {
        handler.post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    // ── 通知 ──

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SmartRead 悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持悬浮阅读窗在后台运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartRead 悬浮窗")
            .setContentText("正在运行，截图后可快速记录思绪")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}

/**
 * 悬浮窗菜单预览 — 使用 Compose 渲染，与 [showMenu] 的 View 实现样式一致。
 * 仅用于 UI 调试，不影响正式功能。
 */
@Preview(showBackground = true, backgroundColor = 0xFFF0F0F0)
@Composable
fun FloatingMenuPreview() {
    val DarkText = ComposeColor(0xFF333333)
    val RedText = ComposeColor(0xFFCC3333)
    val LightPurple = ComposeColor(0xFFF3E8FF)
    val LightPink = ComposeColor(0xFFFFE8E8)
    val Divider = ComposeColor(0xFFEEEEEE)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 遮罩
        Box(modifier = Modifier.fillMaxSize().background(ComposeColor.Black.copy(alpha = 0.5f)))

        // 菜单卡片
        Column(
            modifier = Modifier
                .width(200.dp)
                .background(ComposeColor.White, shape = RoundedCornerShape(20.dp))
                .shadow(3.dp, RoundedCornerShape(20.dp)),
        ) {
            MenuRow(emoji = "📷", label = "OCR 截图识别", desc = "截取屏幕并提取文字", circleBg = LightPurple, textColor = DarkText)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Divider, thickness = 1.dp)
            MenuRow(emoji = "💡", label = "AI 伴读", desc = "苏格拉底式对话", circleBg = LightPurple, textColor = DarkText)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Divider, thickness = 1.dp)
            MenuRow(emoji = "✕", label = "隐藏悬浮窗", desc = null, circleBg = LightPink, textColor = RedText)
        }
    }
}

@Composable
private fun MenuRow(
    emoji: String,
    label: String,
    desc: String?,
    circleBg: ComposeColor,
    textColor: ComposeColor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 彩色圆形图标背景
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(circleBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        // 文字列
        Column {
            Text(
                text = label,
                fontSize = 15.sp,
                color = textColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            if (desc != null) {
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = ComposeColor(0xFF888888)
                )
            }
        }
    }
}

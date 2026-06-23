package com.example.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.utils.OcrTextRecognizer
import com.example.utils.ScreenCaptureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 悬浮窗截图 OCR 服务
 * 提供可拖拽的悬浮球，点击后截图并通过 ML Kit OCR 识别文字
 * 识别结果通过广播发送给 MainActivity 处理
 */
class FloatOcrService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatView: View? = null
    private var ocrResultView: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "float_ocr_service"
        const val NOTIFICATION_ID = 1002

        const val ACTION_OCR_RESULT = "com.example.action.OCR_RESULT"
        const val EXTRA_OCR_TEXT = "ocr_text"
        const val EXTRA_SCREENSHOT_PATH = "screenshot_path"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (floatView == null) {
            createFloatBall()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeAllViews()
        OcrTextRecognizer.release()
        ScreenCaptureManager.release()
    }

    // ── 悬浮球 ──

    private fun createFloatBall() {
        removeAllViews()

        val ball = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(android.graphics.Color.WHITE)
            setPadding(12, 12, 12, 12)
            background = GradientDrawable().apply {
                setColor(0xFF4A90D9.toInt())
                shape = GradientDrawable.OVAL
            }
            contentDescription = "截图识别"
        }

        floatView = ball

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
            y = dpToPx(200)
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
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
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
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        performCaptureAndOcr()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ── 截图 + OCR ──

    private fun performCaptureAndOcr() {
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
                        // 保存截图
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

                // 将 App 呼唤到前台
                try {
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(launchIntent)
                } catch (_: Exception) {}

                removeResultView(resultView)
                showToast("内容已发送至 App")
            }

            btnDismiss.setOnClickListener {
                removeResultView(resultView)
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

    private fun removeResultView(view: View) {
        try { windowManager.removeView(view) } catch (_: Exception) {}
        ocrResultView = null
    }

    private fun removeAllViews() {
        floatView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        floatView = null
        ocrResultView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        ocrResultView = null
    }

    // ── 通知 ──

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OCR 悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持截图 OCR 服务在后台运行"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SmartRead OCR")
            .setContentText("点击悬浮球截图识别文字")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    private fun showToast(msg: String) {
        handler.post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}

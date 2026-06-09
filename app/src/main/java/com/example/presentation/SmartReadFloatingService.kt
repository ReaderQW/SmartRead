package com.example.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * 悬浮阅读窗服务 — 小白圆形悬浮球，可在其他应用上层显示。
 * 点击展开菜单：OCR截图批注 / AI伴读 / 关闭悬浮窗。
 * App 退出后悬浮窗不消失，通过通知栏保持服务存活。
 */
class SmartReadFloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBall: View? = null
    private var menuPopup: View? = null

    companion object {
        const val CHANNEL_ID = "floating_service"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        showFloatingBall()
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
            setColorFilter(Color.WHITE)
            setPadding(12, 12, 12, 12)

            // 蓝色圆形背景
            background = GradientDrawable().apply {
                setColor(0xFF4A90D9.toInt())
                shape = GradientDrawable.OVAL
            }
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

        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)

            // 白色圆角背景
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dpToPx(12).toFloat()
                setStroke(1, Color.parseColor("#E0E0E0"))
            }
        }

        fun addMenuItem(label: String, emoji: String, textColor: Int, onClick: () -> Unit) {
            val tv = TextView(this).apply {
                text = "$emoji  $label"
                textSize = 14f
                setTextColor(textColor)
                gravity = Gravity.CENTER
                setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
                setOnClickListener {
                    onClick()
                    removeMenu()
                }
            }
            menuLayout.addView(tv)

            // 分隔线
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }
            menuLayout.addView(divider)
        }

        addMenuItem("OCR截图批注", "📷", Color.parseColor("#333333")) {
            Toast.makeText(this, "请在阅读软件截图后使用 OCR", Toast.LENGTH_SHORT).show()
        }
        addMenuItem("AI伴读", "🤖", Color.parseColor("#333333")) {
            Toast.makeText(this, "AI伴读已开启", Toast.LENGTH_SHORT).show()
        }
        // 关闭按钮
        val closeTv = TextView(this).apply {
            text = "✕  关闭悬浮窗"
            textSize = 14f
            setTextColor(Color.parseColor("#CC3333"))
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12))
            setOnClickListener { stopSelf() }
        }
        menuLayout.addView(closeTv)

        menuPopup = menuLayout

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

        windowManager.addView(menuLayout, params)
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
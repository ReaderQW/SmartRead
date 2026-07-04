package com.example.presentation

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

/**
 * 无障碍截图服务 — 使用 Android 13+ 的 [takeScreenshot] API 截取屏幕。
 *
 * 与 [ScreenCaptureManager]（MediaProjection）不同，本服务不会与系统录屏冲突，
 * 因为 [takeScreenshot] 走的是无障碍通道，而非 MediaProjection。
 *
 * 用户需在 系统设置 → 无障碍 → SmartRead 中手动开启此服务。
 * 如果服务未开启，[instance] 为 null，SmartReadFloatingService 会自动回退到 MediaProjection。
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU) // API 33 = Android 13
class OcrAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "SmartReadA11ySvc"

        /** 服务实例，服务连接时设置，销毁时清空 */
        var instance: OcrAccessibilityService? = null
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val screenshotExecutor = Executors.newSingleThreadExecutor()
    private var screenshotCallback: ((Bitmap?) -> Unit)? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "onServiceConnected: 无障碍服务已连接，instance 已设置")
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: 无障碍服务销毁，instance 置 null")
        instance = null
        screenshotCallback = null
        super.onDestroy()
    }

    /**
     * 异步截图，结果通过 callback 返回（主线程回调）。
     * 如果已有截图请求进行中，直接 callback(null) 并拒绝。
     */
    fun takeScreenshotAsync(callback: (Bitmap?) -> Unit) {
        if (screenshotCallback != null) {
            callback(null)
            return
        }
        screenshotCallback = callback
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            screenshotExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val bitmap = hardwareBufferToBitmap(screenshot.hardwareBuffer)
                    mainHandler.post {
                        screenshotCallback?.invoke(bitmap)
                        screenshotCallback = null
                    }
                }

                override fun onFailure(errorCode: Int) {
                    mainHandler.post {
                        screenshotCallback?.invoke(null)
                        screenshotCallback = null
                    }
                }
            }
        )
    }

    /**
     * 将 HardwareBuffer 转为 ARGB_8888 Bitmap 副本，然后关闭 HardwareBuffer。
     */
    private fun hardwareBufferToBitmap(hb: HardwareBuffer?): Bitmap? {
        if (hb == null) return null
        val bitmap = try {
            val cs = ColorSpace.get(ColorSpace.Named.SRGB)
            Bitmap.wrapHardwareBuffer(hb, cs)
                ?.copy(Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            null
        } finally {
            hb.close()
        }
        return bitmap
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}

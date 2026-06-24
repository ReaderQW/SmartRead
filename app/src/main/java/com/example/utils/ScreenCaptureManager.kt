package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * 屏幕截图管理类
 * 基于 MediaProjection 实现全局截图
 */
object ScreenCaptureManager {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var contextRef: Context? = null

    fun init(context: Context, projection: MediaProjection?) {
        contextRef = context
        mediaProjection = projection

        // Android 14+ 要求必须先注册 Callback，否则 createVirtualDisplay() 会抛异常
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                release()
            }
        }, null)

        val metrics = context.resources.displayMetrics

        imageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )

        virtualDisplay = projection?.createVirtualDisplay(
            "screen_capture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.surface,
            null,
            null
        )
    }

    /**
     * 截图并返回 Bitmap
     */
    fun captureBitmap(): Bitmap? {
        return try {
            val image = imageReader?.acquireLatestImage() ?: return null
            val bitmap = imageToBitmap(image)
            image.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 截图并保存到文件，回调返回路径
     */
    fun capture(
        context: Context,
        callback: (Boolean, String?) -> Unit
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val image = imageReader?.acquireLatestImage()
                if (image == null) {
                    callback(false, "获取图片失败")
                    return@postDelayed
                }

                val file = saveImage(context, image)
                image.close()
                callback(true, file.absolutePath)
            } catch (e: Exception) {
                callback(false, e.message)
            }
        }, 300)
    }

    /**
     * 截图并返回 Bitmap（带延迟，确保画面稳定）
     */
    fun captureBitmapWithDelay(delayMs: Long = 300, callback: (Bitmap?) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            val bitmap = captureBitmap()
            callback(bitmap)
        }, delayMs)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun saveImage(context: Context, image: Image): File {
        val bitmap = imageToBitmap(image)
        val file = File(
            context.getExternalFilesDir(null),
            "capture_${System.currentTimeMillis()}.png"
        )
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    fun release() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (_: Exception) {
        }
        virtualDisplay = null
        imageReader = null
        mediaProjection = null
        contextRef = null
    }
}

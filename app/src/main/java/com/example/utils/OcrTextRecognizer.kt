package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlin.coroutines.resume

/**
 * ML Kit 离线 OCR 文字识别工具类
 * 使用中文文本识别模型，支持中英文混合识别
 */
object OcrTextRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    /**
     * 对 Bitmap 进行 OCR 识别
     * @param bitmap 待识别的位图
     * @param onResult 识别结果回调 (success, text)
     */
    fun recognize(
        bitmap: Bitmap,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text.trim()
                    if (recognizedText.isNotEmpty()) {
                        onResult(true, recognizedText)
                    } else {
                        onResult(false, "未识别到文字内容")
                    }
                }
                .addOnFailureListener { e ->
                    onResult(false, "OCR 识别失败: ${e.message}")
                }
        } catch (e: Exception) {
            onResult(false, "OCR 识别异常: ${e.message}")
        }
    }

    /**
     * 同步方式识别（用于协程环境）
     */
    suspend fun recognizeSuspend(bitmap: Bitmap): Result<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val text = visionText.text.trim()
                        if (text.isNotEmpty()) {
                            continuation.resume(Result.success(text))
                        } else {
                            continuation.resume(Result.failure(Exception("未识别到文字内容")))
                        }
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            recognizer.close()
        } catch (_: Exception) {
        }
    }
}

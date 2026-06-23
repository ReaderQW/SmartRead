package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.SmartReadApp
import com.example.presentation.SmartReadFloatingService
import com.example.presentation.viewmodel.SmartReadViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.ScreenCaptureManager

class MainActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private var pendingOcrText by mutableStateOf<String?>(null)
    private var pendingScreenshotPath by mutableStateOf<String?>(null)

    @RequiresApi(Build.VERSION_CODES.O)
    private val mediaProjectionLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val projection = projectionManager.getMediaProjection(result.resultCode, result.data!!)
                ScreenCaptureManager.init(this, projection)

                // 启动悬浮窗 OCR 服务
                val intent = Intent(this, SmartReadFloatingService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }

    private val ocrResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                SmartReadFloatingService.ACTION_OCR_RESULT -> {
                    val text = intent.getStringExtra(SmartReadFloatingService.EXTRA_OCR_TEXT) ?: return
                    val path = intent.getStringExtra(SmartReadFloatingService.EXTRA_SCREENSHOT_PATH)
                    pendingOcrText = text
                    pendingScreenshotPath = path
                }
                "com.example.action.REQUEST_SCREEN_CAPTURE" -> {
                    // 来自悬浮窗服务的请求：弹出屏幕录制授权
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requestScreenCapture()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 注册广播接收器（OCR 结果 + 屏幕录制授权请求）
        val filter = IntentFilter().apply {
            addAction(SmartReadFloatingService.ACTION_OCR_RESULT)
            addAction("com.example.action.REQUEST_SCREEN_CAPTURE")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ocrResultReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(ocrResultReceiver, filter)
        }

        val viewModel = ViewModelProvider(this)[SmartReadViewModel::class.java]

        setContent {
            val uiConfig by viewModel.uiConfig.collectAsStateWithLifecycle()

            MyApplicationTheme(
                themeMode = uiConfig.themeMode,
                uiFontFamily = uiConfig.uiFont.toFontFamily(),
                colorTheme = uiConfig.colorTheme
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SmartReadApp(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        pendingOcrText = pendingOcrText,
                        pendingScreenshotPath = pendingScreenshotPath,
                        onOcrHandled = {
                            pendingOcrText = null
                            pendingScreenshotPath = null
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(ocrResultReceiver)
        } catch (_: Exception) {}
    }

    /**
     * 启动录屏授权流程
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun requestScreenCapture() {
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }
}

package com.example.smartread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smartread.presentation.reader.MainAppScaffold
import com.example.smartread.ui.theme.SmartReadTheme

class SmartReadApplication : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartReadTheme {
                MainAppScaffold() // 调用你的导航
            }
        }
    }
}
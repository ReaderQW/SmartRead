package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.SmartReadApp
import com.example.presentation.viewmodel.SmartReadViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}


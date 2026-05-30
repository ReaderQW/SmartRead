package com.example.presentation

// 主入口`SmartReadApp` 仅做导航逻辑

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReadApp(
    viewModel: SmartReadViewModel,
    modifier: Modifier = Modifier
) {
    val currentBookId by viewModel.currentBookId.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (currentBookId == null) {
            // Main Exploration Dashboard (shelf, notes list, overall graph)
            DashboardScreen(viewModel = viewModel)
        } else {
            // Custom Immersive Reader Viewport
            ReaderScreen(viewModel = viewModel)
        }
    }
}

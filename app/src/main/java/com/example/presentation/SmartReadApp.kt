package com.example.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.creation.BookCreationScreen
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.reader.ReaderScreen
import com.example.presentation.viewmodel.SmartReadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartReadApp(
    viewModel: SmartReadViewModel,
    modifier: Modifier = Modifier
) {
    val currentBookId by viewModel.currentBookId.collectAsStateWithLifecycle()
    val isCreatingBook by viewModel.isCreatingBook.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            // 书籍创建流程
            isCreatingBook -> {
                BookCreationScreen(viewModel = viewModel)
            }
            // 没有选中书籍 → 仪表盘
            currentBookId == null -> {
                DashboardScreen(viewModel = viewModel)
            }
            // 已选中书籍 → 阅读器
            else -> {
                ReaderScreen(viewModel = viewModel)
            }
        }
    }
}
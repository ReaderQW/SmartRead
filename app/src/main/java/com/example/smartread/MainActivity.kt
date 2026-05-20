package com.example.smartread

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartread.ui.main.MainAppScaffold
import com.example.smartread.ui.theme.SmartReadTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartReadTheme {
                MainAppScaffold()
            }
        }
    }
}

private const val deviceScreenSize = "spec:width=360dp,height=640dp,dpi=480"

@Preview(
    showBackground = true,
    name = "SmartRead",
    group = "SmartRead",
    showSystemUi = true,
    device = deviceScreenSize
)
@Composable
fun SmartReadAppPreview() {
    SmartReadTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainAppScaffold()
        }
    }
}

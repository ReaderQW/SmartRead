package com.example.presentation.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SleekHighlightBlue
import com.example.ui.theme.SleekHighlightDefault
import com.example.ui.theme.SleekHighlightGreen
import com.example.ui.theme.SleekHighlightPink

@Composable
fun ReaderSelectionToolbar(
    selectedText: String,
    onClearSelection: () -> Unit,
    onSaveHighlight: (colorHex: String) -> Unit,
    onOpenSocraticWithText: (text: String) -> Unit,
    onOpenCommentDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        tonalElevation = 12.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "已选中经典词句 passage：",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = onClearSelection,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close selections bar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "“$selectedText”",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Highlight color palette circles & Socratic link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Highlights triggers
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "#FFEB3B" to SleekHighlightDefault,
                        "#69F0AE" to SleekHighlightGreen,
                        "#40C4FF" to SleekHighlightBlue,
                        "#FF4081" to SleekHighlightPink
                    ).forEach { (hex, tintColor) ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(tintColor, CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                                .clickable { onSaveHighlight(hex) }
                        )
                    }
                }

                // Socratic bot linkage & Notebook link
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onOpenSocraticWithText(selectedText) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Socratic icon",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("伴读对论", color = MaterialTheme.colorScheme.onPrimary, fontSize = 10.sp)
                    }

                    Button(
                        onClick = onOpenCommentDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save and AI summary notes icon",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("存为感悟", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

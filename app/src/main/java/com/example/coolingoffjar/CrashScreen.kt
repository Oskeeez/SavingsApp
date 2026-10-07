package com.example.coolingoffjar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Debug-only: shows the stack trace of the previous crash. */
@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFFF4E5)).safeDrawingPadding().padding(16.dp),
    ) {
        Text("The app crashed last time", fontSize = 20.sp, color = Color(0xFF4B4239))
        Text(
            "Screenshot this (or long-press the text to copy it) and send it over. Nothing here leaves your phone.",
            fontSize = 14.sp,
            color = Color(0xFF4B4239),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Button(onClick = onContinue) { Text("Continue to the app") }
        SelectionContainer(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
            Text(
                trace,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = Color(0xFF2B2A2E),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
}

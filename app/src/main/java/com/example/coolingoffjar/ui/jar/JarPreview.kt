package com.example.coolingoffjar.ui.jar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.coolingoffjar.ui.theme.CoolingOffJarTheme
import com.example.coolingoffjar.ui.theme.JarTheme

@Composable
private fun JarRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(JarTheme.palette.background)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        JarView(0, 5, Modifier.width(90.dp))
        JarView(1, 5, Modifier.width(90.dp))
        JarView(3, 5, Modifier.width(90.dp))
        JarView(5, 5, Modifier.width(90.dp))
    }
}

@Preview(name = "Jar light", widthDp = 420, heightDp = 160)
@Composable
private fun JarLightPreview() = CoolingOffJarTheme(darkTheme = false) { JarRow() }

@Preview(name = "Jar dark", widthDp = 420, heightDp = 160, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JarDarkPreview() = CoolingOffJarTheme(darkTheme = true) { JarRow() }

@Preview(name = "Jar large", widthDp = 300, heightDp = 400)
@Composable
private fun JarLargePreview() = CoolingOffJarTheme(darkTheme = false) {
    JarView(4, 7, Modifier.background(JarTheme.palette.background).padding(24.dp))
}

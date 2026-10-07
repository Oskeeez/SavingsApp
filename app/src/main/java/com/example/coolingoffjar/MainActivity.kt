package com.example.coolingoffjar

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.coolingoffjar.ui.navigation.CoolingOffJarNavHost
import com.example.coolingoffjar.ui.theme.CoolingOffJarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light bars (dark icons) on the cream background, regardless of the phone's dark-mode setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val debuggable = (application as CoolingOffJarApp).isDebuggable
        setContent {
            CoolingOffJarTheme {
                // Debug builds: if the last run crashed, show why before anything else.
                var crash by remember { mutableStateOf(if (debuggable) CrashReporter.pending(this) else null) }
                val trace = crash
                if (trace != null) {
                    CrashScreen(trace, onContinue = { CrashReporter.clear(this); crash = null })
                } else {
                    CoolingOffJarNavHost()
                }
            }
        }
    }
}

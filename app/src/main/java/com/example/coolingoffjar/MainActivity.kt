package com.example.coolingoffjar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.coolingoffjar.ui.navigation.CoolingOffJarNavHost
import com.example.coolingoffjar.ui.theme.CoolingOffJarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoolingOffJarTheme {
                CoolingOffJarNavHost()
            }
        }
    }
}

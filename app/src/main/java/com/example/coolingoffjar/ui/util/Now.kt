package com.example.coolingoffjar.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

private const val HOUR_MS = 3_600_000L

/**
 * "Now" for the UI. This is only a *refresh* mechanism: correctness comes from stored unlock times
 * compared with the real clock. It re-reads the clock whenever the app resumes (after reboot, long
 * absence, or a clock/timezone change), when the next unlock passes, and at least hourly.
 */
@Composable
fun rememberNowMillis(nextUnlockAt: Long?): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleResumeEffect(Unit) {
        now = System.currentTimeMillis()
        onPauseOrDispose { }
    }
    LaunchedEffect(nextUnlockAt) {
        while (true) {
            val current = System.currentTimeMillis()
            val target = minOf(nextUnlockAt ?: Long.MAX_VALUE, current + HOUR_MS)
            delay((target - current).coerceAtLeast(1_000L))
            now = System.currentTimeMillis()
        }
    }
    return now
}

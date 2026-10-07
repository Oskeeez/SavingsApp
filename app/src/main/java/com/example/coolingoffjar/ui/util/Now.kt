package com.example.coolingoffjar.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.coolingoffjar.data.AppClock
import kotlinx.coroutines.delay

private const val HOUR_MS = 3_600_000L

/**
 * "Now" for the UI, read from [AppClock]. This is only a *refresh* mechanism: correctness comes from
 * stored unlock times compared with the clock. It re-reads the clock whenever the app resumes (after
 * reboot, long absence, or a clock/timezone change), when the next unlock passes, when the debug
 * clock offset changes, and at least hourly.
 */
@Composable
fun rememberNowMillis(nextUnlockAt: Long?): Long {
    val offset by AppClock.offsetMs.collectAsState()
    var now by remember { mutableLongStateOf(AppClock.now()) }
    LifecycleResumeEffect(Unit) {
        now = AppClock.now()
        onPauseOrDispose { }
    }
    LaunchedEffect(nextUnlockAt, offset) {
        now = AppClock.now()
        while (true) {
            val current = AppClock.now()
            val target = minOf(nextUnlockAt ?: Long.MAX_VALUE, current + HOUR_MS)
            delay((target - current).coerceAtLeast(1_000L))
            now = AppClock.now()
        }
    }
    return now
}

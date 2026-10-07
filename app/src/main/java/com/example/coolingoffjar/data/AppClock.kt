package com.example.coolingoffjar.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The one clock the app reads. In normal use it is just the system clock. Debug builds can set an
 * offset (Settings > Developer) to time-travel; release builds never apply one.
 */
object AppClock {
    private val _offsetMs = MutableStateFlow(0L)
    val offsetMs: StateFlow<Long> = _offsetMs

    fun now(): Long = System.currentTimeMillis() + _offsetMs.value

    fun setOffset(offsetMs: Long) {
        _offsetMs.value = offsetMs
    }
}

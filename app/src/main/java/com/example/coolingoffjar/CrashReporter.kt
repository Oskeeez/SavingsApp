package com.example.coolingoffjar

import android.content.Context
import java.io.File

/**
 * Debug builds only: if the app crashes, the stack trace is saved to a private file and shown on screen at the next
 * launch, so it can be read (and screenshotted) without Logcat. Nothing is sent anywhere.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(app.filesDir, FILE_NAME).writeText("Thread: ${thread.name}\n\n" + error.stackTraceToString().take(14_000))
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun pending(context: Context): String? =
        File(context.filesDir, FILE_NAME).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}

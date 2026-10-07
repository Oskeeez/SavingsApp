package com.example.coolingoffjar

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.coolingoffjar.data.AppClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CoolingOffJarApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** True for debug builds only: gates the Developer settings and the time-travel clock. */
    val isDebuggable: Boolean
        get() = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onCreate() {
        super.onCreate()
        if (isDebuggable) CrashReporter.install(this)
        container = AppContainer(this)
        appScope.launch { container.repository.ensureCurrentJar() }
        if (isDebuggable) {
            appScope.launch {
                container.settingsRepository.debugTimeOffsetMs.collect { AppClock.setOffset(it) }
            }
        }
    }
}

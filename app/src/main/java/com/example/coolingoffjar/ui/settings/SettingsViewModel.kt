package com.example.coolingoffjar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.data.settings.SettingsRepository
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.DebugTime
import com.example.coolingoffjar.domain.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class SettingsUiState(
    val isLoaded: Boolean = false,
    val settings: Settings = Settings(),
    /** Debug clock offset in millis (0 = real time). */
    val debugOffsetMs: Long = 0L,
)

class SettingsViewModel(
    private val repository: CoolingOffRepository,
    private val settingsRepository: SettingsRepository,
    val isDebuggable: Boolean,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repository.settings,
        settingsRepository.debugTimeOffsetMs,
    ) { settings, offset -> SettingsUiState(true, settings, offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setCoolOffDays(days: Int) {
        viewModelScope.launch { repository.setCoolOffDays(days) }
    }

    /** Applies to the current jar straight away; may complete it (the freebie card then waits on Home). */
    fun setNotBuysPerJar(n: Int) {
        viewModelScope.launch { repository.setNotBuysPerJar(n) }
    }

    // --- Developer tools (only reachable in debug builds) ---

    fun shiftClockDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setDebugTimeOffsetMs(AppClock.offsetMs.value + days * CoolOffRules.DAY_MS)
        }
    }

    fun resetClock() {
        viewModelScope.launch { settingsRepository.setDebugTimeOffsetMs(0L) }
    }

    /** [pickedUtcMidnight] is the Material date picker's result. */
    fun setClockToDate(pickedUtcMidnight: Long) {
        viewModelScope.launch {
            val offset = DebugTime.offsetForPickedDate(pickedUtcMidnight, System.currentTimeMillis(), ZoneId.systemDefault())
            settingsRepository.setDebugTimeOffsetMs(offset)
        }
    }

    fun addSampleWants() = viewModelScope.launch { repository.debugAddSampleWants() }
    fun makeAllReady() = viewModelScope.launch { repository.debugMakeAllReady() }
    fun addCoin() = viewModelScope.launch { repository.debugAddCoin() }
    fun completeJar() = viewModelScope.launch { repository.debugCompleteJar() }
    fun resetAllData() = viewModelScope.launch { repository.debugResetAll() }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CoolingOffJarApp
                SettingsViewModel(app.container.repository, app.container.settingsRepository, app.isDebuggable)
            }
        }
    }
}

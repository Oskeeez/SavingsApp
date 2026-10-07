package com.example.coolingoffjar.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.data.repo.DecisionResult
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.Settings
import com.example.coolingoffjar.domain.Want
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val isLoaded: Boolean = false,
    val jar: Jar = Jar(),
    val wants: List<Want> = emptyList(),
    val settings: Settings = Settings(),
    /** A just-completed jar whose "Freebie unlocked" card is waiting for Use freebie / Later. */
    val celebration: Jar? = null,
)

sealed interface HomeEvent {
    data class WantAdded(val want: Want) : HomeEvent
    data object BoughtNoted : HomeEvent
}

class HomeViewModel(private val repository: CoolingOffRepository) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        repository.currentJar,
        repository.openWants,
        repository.settings,
        repository.pendingCelebration,
    ) { jar, wants, settings, celebration -> HomeState(true, jar, wants, settings, celebration) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun addWant(name: String) {
        viewModelScope.launch {
            repository.addWant(name)?.let { _events.emit(HomeEvent.WantAdded(it)) }
        }
    }

    /** "Not buying": the coin(s) drop in via the jar's own animation when the new state arrives. */
    fun notBuying(wantId: Long) {
        viewModelScope.launch { repository.decideNotBuying(wantId) }
    }

    /** "Still want it": no coin, neutral acknowledgement. */
    fun stillWant(wantId: Long) {
        viewModelScope.launch {
            if (repository.decideStillWant(wantId) is DecisionResult.Done) _events.emit(HomeEvent.BoughtNoted)
        }
    }

    fun useFreebie(jarId: Long) {
        viewModelScope.launch { repository.useFreebie(jarId) }
    }

    fun dismissCelebration() {
        viewModelScope.launch { repository.dismissCelebration() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CoolingOffJarApp
                HomeViewModel(app.container.repository)
            }
        }
    }
}

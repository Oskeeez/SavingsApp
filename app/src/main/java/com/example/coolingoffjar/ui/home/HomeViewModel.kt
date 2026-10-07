package com.example.coolingoffjar.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.repo.CoolingOffRepository
import com.example.coolingoffjar.data.repo.DecisionResult
import com.example.coolingoffjar.data.repo.ShelfRepository
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.OwnedItem
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
    /** Completed jars, newest first: they stand on the shelf as memories. */
    val completedJars: List<Jar> = emptyList(),
    /** Everything bought for the shelf. */
    val owned: List<OwnedItem> = emptyList(),
)

sealed interface HomeEvent {
    data class WantAdded(val want: Want) : HomeEvent
    data object BoughtNoted : HomeEvent
    data class WantRemoved(val want: Want) : HomeEvent
}

class HomeViewModel(
    private val repository: CoolingOffRepository,
    shelfRepository: ShelfRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        combine(
            repository.currentJar,
            repository.openWants,
            repository.settings,
            repository.pendingCelebration,
            repository.completedJars,
        ) { jar, wants, settings, celebration, completed -> HomeState(true, jar, wants, settings, celebration, completed) },
        shelfRepository.owned,
    ) { base, owned -> base.copy(owned = owned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _events = MutableSharedFlow<HomeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun addWant(name: String, iconKey: String) {
        viewModelScope.launch {
            repository.addWant(name, iconKey)?.let { _events.emit(HomeEvent.WantAdded(it)) }
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

    /** Remove an item from the waiting list; the UI offers Undo via [restoreWant]. */
    fun removeWant(want: Want) {
        viewModelScope.launch {
            repository.deleteWant(want.id)?.let { _events.emit(HomeEvent.WantRemoved(it)) }
        }
    }

    fun restoreWant(want: Want) {
        viewModelScope.launch { repository.restoreWant(want) }
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
                HomeViewModel(app.container.repository, app.container.shelfRepository)
            }
        }
    }
}

package com.example.coolingoffjar.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.repo.PurchaseResult
import com.example.coolingoffjar.data.repo.ShelfRepository
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.PurchaseCheck
import com.example.coolingoffjar.domain.RoomLook
import com.example.coolingoffjar.domain.ScenePoint
import com.example.coolingoffjar.domain.SlotRef
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopState(
    val isLoaded: Boolean = false,
    /** Coins available to spend. */
    val balance: Int = 0,
    val ownedIds: Set<String> = emptySet(),
    val owned: List<OwnedItem> = emptyList(),
    /** The most recent purchases, newest last: shown on the shelf strip above the shop. */
    val recentOwnedIds: List<String> = emptyList(),
    /** The wall, floor and shelf currently in use. */
    val look: RoomLook = RoomLook.DEFAULT,
)

sealed interface ShopEvent {
    data class Bought(val itemId: String) : ShopEvent
    data class Refused(val reason: PurchaseCheck) : ShopEvent
}

class ShopViewModel(private val shelfRepository: ShelfRepository) : ViewModel() {

    val state: StateFlow<ShopState> = combine(shelfRepository.balance, shelfRepository.owned, shelfRepository.look) { balance, owned, look ->
        ShopState(true, balance, owned.map { it.itemId }.toSet(), owned, owned.map { it.itemId }, look)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopState())

    private val _events = MutableSharedFlow<ShopEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ShopEvent> = _events.asSharedFlow()

    /** Put an owned wall, floor or shelf to use. */
    fun use(itemId: String) {
        viewModelScope.launch { shelfRepository.selectDecor(itemId) }
    }

    /** Buy [itemId] and put it at [slot] / [point] (null = next free spot). */
    fun purchase(itemId: String, slot: SlotRef? = null, point: ScenePoint? = null) {
        viewModelScope.launch {
            _events.emit(
                when (val result = shelfRepository.purchase(itemId, slot, point)) {
                    is PurchaseResult.Bought -> ShopEvent.Bought(itemId)
                    is PurchaseResult.Refused -> ShopEvent.Refused(result.reason)
                },
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CoolingOffJarApp
                ShopViewModel(app.container.shelfRepository)
            }
        }
    }
}

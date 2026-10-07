package com.example.coolingoffjar.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.coolingoffjar.CoolingOffJarApp
import com.example.coolingoffjar.data.repo.PurchaseResult
import com.example.coolingoffjar.data.repo.ShelfRepository
import com.example.coolingoffjar.domain.PurchaseCheck
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
    /** The most recent purchases, newest last: shown on the shelf strip above the shop. */
    val recentOwnedIds: List<String> = emptyList(),
)

sealed interface ShopEvent {
    data class Bought(val itemId: String) : ShopEvent
    data class Refused(val reason: PurchaseCheck) : ShopEvent
}

class ShopViewModel(private val shelfRepository: ShelfRepository) : ViewModel() {

    val state: StateFlow<ShopState> = combine(shelfRepository.balance, shelfRepository.owned) { balance, owned ->
        ShopState(true, balance, owned.map { it.itemId }.toSet(), owned.map { it.itemId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopState())

    private val _events = MutableSharedFlow<ShopEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ShopEvent> = _events.asSharedFlow()

    fun purchase(itemId: String) {
        viewModelScope.launch {
            _events.emit(
                when (val result = shelfRepository.purchase(itemId)) {
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

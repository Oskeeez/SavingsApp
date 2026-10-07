package com.example.coolingoffjar.data.repo

import androidx.room.withTransaction
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.db.AppDatabase
import com.example.coolingoffjar.data.db.toDomain
import com.example.coolingoffjar.data.db.toEntity
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.PurchaseCheck
import com.example.coolingoffjar.domain.ShelfEconomy
import com.example.coolingoffjar.domain.SlotRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

sealed interface PurchaseResult {
    data class Bought(val item: OwnedItem) : PurchaseResult
    data class Refused(val reason: PurchaseCheck) : PurchaseResult
}

/**
 * The shelf: what the user owns, how many coins they have, and buying. Coins are never stored: the balance
 * is always (every coin ever put in a jar) minus (the cost of everything owned), so it cannot drift.
 */
class ShelfRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = AppClock::now,
) {
    private val shelfDao = db.shelfDao()
    private val jarDao = db.jarDao()

    val owned: Flow<List<OwnedItem>> = shelfDao.observeOwned().map { list -> list.map { it.toDomain() } }

    /** Coins available to spend right now. */
    val balance: Flow<Int> = combine(jarDao.observeTotalCoins(), owned) { earned, owned ->
        ShelfEconomy.balance(earned, owned)
    }.distinctUntilChanged()

    /**
     * Checks and buys in one transaction, so two quick taps can never buy twice or overspend. [slot] is where the
     * user chose to put it (null = the next free spot); a spot that is reserved or taken is refused.
     */
    suspend fun purchase(itemId: String, slot: SlotRef? = null): PurchaseResult = db.withTransaction {
        val owned = shelfDao.getOwned().map { it.toDomain() }
        when (val check = ShelfEconomy.check(itemId, owned, jarDao.totalCoins(), slot)) {
            is PurchaseCheck.Ok -> {
                val item = OwnedItem(itemId, check.slot.tier, check.slot.slot, clock())
                shelfDao.insert(item.toEntity())
                PurchaseResult.Bought(item)
            }
            else -> PurchaseResult.Refused(check)
        }
    }
}

package com.example.coolingoffjar.data.repo

import androidx.room.withTransaction
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.db.AppDatabase
import com.example.coolingoffjar.data.db.DecorChoiceEntity
import com.example.coolingoffjar.data.db.toDomain
import com.example.coolingoffjar.data.db.toEntity
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.PurchaseCheck
import com.example.coolingoffjar.domain.RoomLook
import com.example.coolingoffjar.domain.ScenePoint
import com.example.coolingoffjar.domain.ShelfCatalog
import com.example.coolingoffjar.domain.ShelfCategory
import com.example.coolingoffjar.domain.ShelfEconomy
import com.example.coolingoffjar.domain.ShelfLayout
import com.example.coolingoffjar.domain.ShelfSurface
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
 * The shelf: what the user owns, how many coins they have, buying, moving things and choosing how the room looks.
 * Coins are never stored: the balance is always (every coin ever put in a jar) minus (the cost of everything owned),
 * so it cannot drift.
 */
class ShelfRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = AppClock::now,
) {
    private val shelfDao = db.shelfDao()
    private val jarDao = db.jarDao()

    /** Everything owned: bought items, the always-there objects, and the walls/floors/shelves. */
    val owned: Flow<List<OwnedItem>> = shelfDao.observeOwned().map { list -> list.map { it.toDomain() } }

    /** The wall, floor and shelf currently shown. */
    val look: Flow<RoomLook> = combine(owned, shelfDao.observeChoices()) { owned, choices ->
        RoomLook.from(owned.map { it.itemId }.toSet(), choices.associate { it.kind to it.itemId })
    }.distinctUntilChanged()

    /** Coins available to spend right now. */
    val balance: Flow<Int> = combine(jarDao.observeTotalCoins(), owned) { earned, owned ->
        ShelfEconomy.balance(earned, owned)
    }.distinctUntilChanged()

    /**
     * Makes sure the always-there objects exist (the clock, jar, gacha machine and one memory jar per finished jar,
     * up to two) and that the starting wall, floor and shelf are owned. Safe to call any number of times.
     */
    suspend fun ensureDefaults(finishedJars: Int) = db.withTransaction {
        val existing = shelfDao.getOwned().map { it.toDomain() }
        val have = existing.map { it.itemId }.toSet()
        val taken = ShelfLayout.taken(ShelfLayout.resolve(existing)).toMutableSet()
        val wantedCore = listOf("clock", "jar", "gacha") + (1..finishedJars.coerceAtMost(2)).map { "memory_$it" }
        for (id in wantedCore) {
            if (id in have) continue
            val preferred = ShelfLayout.DEFAULT_SLOTS.getValue(id)
            val slot = if (preferred !in taken) preferred else ShelfLayout.nextFreeSlot(taken) ?: preferred
            taken += slot
            shelfDao.insert(OwnedItem(id, slot.tier, slot.slot, clock()).toEntity())
        }
        for (category in listOf(ShelfCategory.WALLS, ShelfCategory.FLOORS, ShelfCategory.SHELVES)) {
            val id = ShelfCatalog.defaultDecor(category) ?: continue
            if (id !in have) shelfDao.insert(OwnedItem(id, 0, 0, clock()).toEntity())
        }
    }

    /**
     * Checks and buys in one transaction, so two quick taps can never buy twice or overspend. [slot] / [point] is
     * where the user chose to put it (null = the next free spot); a spot that is taken is refused. A new wall, floor
     * or shelf is put to use straight away.
     */
    suspend fun purchase(itemId: String, slot: SlotRef? = null, point: ScenePoint? = null): PurchaseResult = db.withTransaction {
        val owned = shelfDao.getOwned().map { it.toDomain() }
        when (val check = ShelfEconomy.check(itemId, owned, jarDao.totalCoins(), slot, point)) {
            is PurchaseCheck.Ok -> {
                val item = OwnedItem(itemId, check.slot.tier, check.slot.slot, clock(), check.point?.x, check.point?.y)
                shelfDao.insert(item.toEntity())
                val entry = ShelfCatalog.find(itemId)
                if (entry?.surface == ShelfSurface.DECOR) shelfDao.setChoice(DecorChoiceEntity(entry.category.name, itemId))
                PurchaseResult.Bought(item)
            }
            else -> PurchaseResult.Refused(check)
        }
    }

    /** Moves a bookcase item to a new slot. Refused (false) if the slot is taken or the item is not a bookcase item. */
    suspend fun move(itemId: String, target: SlotRef): Boolean = db.withTransaction {
        val owned = shelfDao.getOwned().map { it.toDomain() }
        if (!ShelfLayout.canMove(owned, itemId, target)) return@withTransaction false
        shelfDao.move(itemId, target.tier, target.slot)
        true
    }

    /** Moves a note to an exact spot on the wall (kept wholly on the wall). */
    suspend fun moveFree(itemId: String, point: ScenePoint): Boolean = db.withTransaction {
        val entry = ShelfCatalog.find(itemId)
        if (entry == null || entry.surface != ShelfSurface.WALL || shelfDao.getOwned().none { it.itemId == itemId }) {
            return@withTransaction false
        }
        val p = ShelfLayout.clampWall(entry, point)
        shelfDao.moveFree(itemId, p.x, p.y)
        true
    }

    /** Puts an owned wall, floor or shelf to use. */
    suspend fun selectDecor(itemId: String): Boolean = db.withTransaction {
        val entry = ShelfCatalog.find(itemId)
        if (entry == null || entry.surface != ShelfSurface.DECOR || shelfDao.getOwned().none { it.itemId == itemId }) {
            return@withTransaction false
        }
        shelfDao.setChoice(DecorChoiceEntity(entry.category.name, itemId))
        true
    }
}

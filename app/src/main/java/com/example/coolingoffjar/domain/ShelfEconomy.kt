package com.example.coolingoffjar.domain

sealed interface PurchaseCheck {
    /** Can buy; it will stand at [slot]. */
    data class Ok(val slot: SlotRef) : PurchaseCheck
    data object UnknownItem : PurchaseCheck
    data object AlreadyOwned : PurchaseCheck
    data class NotEnoughCoins(val shortBy: Int) : PurchaseCheck

    /** The spot asked for is reserved, already taken, or not on the shelf. */
    data object SlotUnavailable : PurchaseCheck
}

/**
 * Coins are earned by waiting: every "Not buying" puts one coin in a jar, and every coin that has ever
 * gone into any jar counts, so emptying a jar into the shelf never undoes your patience. Nothing here is
 * money, and nothing is random: you pick exactly what you want, and exactly where it goes.
 */
object ShelfEconomy {
    fun coinsEarned(jarFilledCounts: List<Int>): Int = jarFilledCounts.sum()

    fun coinsSpent(owned: List<OwnedItem>): Int = owned.sumOf { ShelfCatalog.find(it.itemId)?.cost ?: 0 }

    fun balance(coinsEarned: Int, owned: List<OwnedItem>): Int = coinsEarned - coinsSpent(owned)

    /** [wantedSlot] is where the user chose to put it; null means "the next free spot". */
    fun check(itemId: String, owned: List<OwnedItem>, coinsEarned: Int, wantedSlot: SlotRef? = null): PurchaseCheck {
        val item = ShelfCatalog.find(itemId) ?: return PurchaseCheck.UnknownItem
        if (owned.any { it.itemId == itemId }) return PurchaseCheck.AlreadyOwned
        val balance = balance(coinsEarned, owned)
        if (balance < item.cost) return PurchaseCheck.NotEnoughCoins(item.cost - balance)
        val taken = ShelfLayout.resolve(owned).map { it.slotRef }.toSet()
        if (wantedSlot == null) {
            val next = ShelfLayout.nextFreeSlot(taken, item.surface) ?: return PurchaseCheck.SlotUnavailable
            return PurchaseCheck.Ok(next)
        }
        return if (ShelfLayout.isUsable(wantedSlot, item.surface, taken)) PurchaseCheck.Ok(wantedSlot) else PurchaseCheck.SlotUnavailable
    }
}

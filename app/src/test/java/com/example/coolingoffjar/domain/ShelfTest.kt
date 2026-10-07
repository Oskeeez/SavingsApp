package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfTest {
    private fun owned(id: String, tier: Int, slot: Int) = OwnedItem(id, tier, slot, purchasedAt = 0)

    // ---- catalogue ----
    @Test fun `catalogue ids are unique and costs are sensible`() {
        val items = ShelfCatalog.items
        assertEquals(items.size, items.map { it.id }.toSet().size)
        assertTrue(items.all { it.cost in 1..10 })
        assertTrue(items.size >= 20)
    }

    @Test fun `the functional objects are never for sale`() {
        for (id in listOf("jar_empty", "gacha_machine", "desk_clock", "coin", "coin_pile")) assertNull(ShelfCatalog.find(id))
    }

    @Test fun `every category has something to buy`() {
        for (c in ShelfCategory.entries) assertTrue(c.name, ShelfCatalog.items.any { it.category == c })
    }

    // ---- reserved slots and placement ----
    @Test fun `the clock is on the top row and the jar and gacha machine share the second`() {
        assertEquals(0, ShelfLayout.CLOCK.tier)
        assertEquals(1, ShelfLayout.JAR.tier)
        assertEquals(1, ShelfLayout.GACHA.tier)
        assertTrue(ShelfLayout.JAR.slot != ShelfLayout.GACHA.slot)
    }

    @Test fun `jar gacha and clock slots are reserved`() {
        for (s in listOf(ShelfLayout.JAR, ShelfLayout.GACHA, ShelfLayout.CLOCK) + ShelfLayout.MEMORY_JARS) {
            assertTrue(ShelfLayout.isReserved(s))
        }
    }

    @Test fun `purchases never land on a reserved slot and never overlap`() {
        val taken = HashSet<SlotRef>()
        repeat(60) {
            val slot = ShelfLayout.nextFreeSlot(taken)
            assertFalse("reserved $slot", ShelfLayout.isReserved(slot))
            assertTrue("duplicate $slot", taken.add(slot))
            assertTrue(slot.slot < ShelfLayout.slotXs(slot.tier).size)
        }
    }

    @Test fun `first purchases fill the shelf top to bottom, left to right`() {
        val first = ShelfLayout.nextFreeSlot(emptySet())
        assertEquals(SlotRef(0, 1), first) // the clock holds the top row's first slot
        val second = ShelfLayout.nextFreeSlot(setOf(first))
        assertEquals(SlotRef(0, 2), second)
        // the second row belongs to the jar and the gacha machine, so the next stop is the third row
        assertEquals(SlotRef(2, 0), ShelfLayout.nextFreeSlot(setOf(first, second)))
    }

    @Test fun `placement is deterministic`() {
        val occupied = setOf(SlotRef(1, 1), SlotRef(2, 0))
        assertEquals(ShelfLayout.nextFreeSlot(occupied), ShelfLayout.nextFreeSlot(occupied))
    }

    @Test fun `the shelf always has room because tiers are added`() {
        val many = (0 until 40).scan(emptySet<SlotRef>()) { acc, _ -> acc + ShelfLayout.nextFreeSlot(acc) }.last()
        assertEquals(40, many.size)
        val tiers = ShelfLayout.tiersNeeded(many.map { owned("x", it.tier, it.slot) })
        assertTrue(tiers > ShelfGeometry.MIN_TIERS)
    }

    @Test fun `an empty shelf still shows the minimum number of tiers`() {
        assertEquals(ShelfGeometry.MIN_TIERS, ShelfLayout.tiersNeeded(emptyList()))
    }

    @Test fun `items are sized to fit their slot and compartment`() {
        val tiers = ShelfGeometry.MIN_TIERS
        for (item in ShelfCatalog.items) for (tier in 1 until tiers) {
            val h = ShelfLayout.heightFor(item, tier, tiers)
            val w = h * item.aspect
            assertTrue("${item.id} h=$h", h > 20f && h <= ShelfGeometry.compartmentHeight(tier, tiers))
            assertTrue("${item.id} w=$w", w <= 92.5f)
        }
    }

    // ---- geometry ----
    @Test fun `slices are contiguous and stand lines run downwards`() {
        for (tiers in 2..9) {
            val s = ShelfGeometry.slices(tiers)
            for (i in 1 until s.size) assertEquals(s[i - 1].dstTop + s[i - 1].srcHeight, s[i].dstTop)
            assertEquals(ShelfGeometry.composedHeight(tiers), s.last().dstTop + s.last().srcHeight)
            val stands = (0 until tiers).map { ShelfGeometry.standLine(it, tiers) }
            assertEquals(stands.sorted(), stands)
            assertEquals(stands.size, stands.toSet().size)
            assertTrue(stands.last() < ShelfGeometry.composedHeight(tiers))
        }
    }

    @Test fun `the five-tier shelf is close to the original artwork`() {
        // top 196 + 3 middles of 143 + bottom 396
        assertEquals(196 + 3 * 143 + 396, ShelfGeometry.composedHeight(5))
        assertEquals(180, ShelfGeometry.standLine(0, 5))
    }

    // ---- economy ----
    @Test fun `coins earned is every coin ever put in any jar`() {
        assertEquals(0, ShelfEconomy.coinsEarned(emptyList()))
        assertEquals(12, ShelfEconomy.coinsEarned(listOf(5, 5, 2)))
    }

    @Test fun `balance is earned minus the cost of what you own`() {
        val cat = ShelfCatalog.find("cat_calico")!!
        val lamp = ShelfCatalog.find("mushroom_lamp")!!
        val own = listOf(owned(cat.id, 1, 1), owned(lamp.id, 1, 2))
        assertEquals(cat.cost + lamp.cost, ShelfEconomy.coinsSpent(own))
        assertEquals(20 - cat.cost - lamp.cost, ShelfEconomy.balance(20, own))
    }

    @Test fun `can buy when affordable and it gets the next free slot`() {
        val item = ShelfCatalog.find("pen_cup")!!
        val result = ShelfEconomy.check(item.id, emptyList(), coinsEarned = item.cost)
        assertEquals(PurchaseCheck.Ok(ShelfLayout.nextFreeSlot(emptySet())), result)
    }

    @Test fun `cannot buy without enough coins and it says how many short`() {
        val item = ShelfCatalog.find("cat_calico")!!
        assertEquals(PurchaseCheck.NotEnoughCoins(2), ShelfEconomy.check(item.id, emptyList(), item.cost - 2))
        assertEquals(PurchaseCheck.NotEnoughCoins(item.cost), ShelfEconomy.check(item.id, emptyList(), 0))
    }

    @Test fun `cannot buy the same thing twice or something that does not exist`() {
        val item = ShelfCatalog.find("rabbit")!!
        val own = listOf(owned(item.id, 1, 1))
        assertEquals(PurchaseCheck.AlreadyOwned, ShelfEconomy.check(item.id, own, 100))
        assertEquals(PurchaseCheck.UnknownItem, ShelfEconomy.check("gacha_machine", own, 100))
    }

    @Test fun `spending can never make the balance negative through the check`() {
        // Buy as many cheap things as 10 coins allow; the balance never goes below zero.
        var own = emptyList<OwnedItem>()
        for (item in ShelfCatalog.items.sortedBy { it.cost }) {
            val r = ShelfEconomy.check(item.id, own, 10)
            if (r is PurchaseCheck.Ok) own = own + OwnedItem(item.id, r.slot.tier, r.slot.slot, 0)
            assertTrue(ShelfEconomy.balance(10, own) >= 0)
        }
    }

    // ---- choosing where it goes ----
    @Test fun `a chosen free slot is used`() {
        val item = ShelfCatalog.find("pen_cup")!!
        val chosen = SlotRef(3, 1)
        assertEquals(PurchaseCheck.Ok(chosen), ShelfEconomy.check(item.id, emptyList(), 10, chosen))
    }

    @Test fun `a chosen slot that is reserved, taken or off the shelf is refused`() {
        val item = ShelfCatalog.find("pen_cup")!!
        val own = listOf(owned("rabbit", 3, 0))
        for (bad in listOf(ShelfLayout.JAR, ShelfLayout.GACHA, ShelfLayout.CLOCK, ShelfLayout.MEMORY_JARS[0], SlotRef(3, 0), SlotRef(0, 5), SlotRef(-1, 0), SlotRef(2, 9))) {
            assertEquals("$bad", PurchaseCheck.SlotUnavailable, ShelfEconomy.check(item.id, own, 100, bad))
        }
    }

    @Test fun `choosing a slot does not skip the coin check`() {
        val item = ShelfCatalog.find("cat_calico")!!
        assertEquals(PurchaseCheck.NotEnoughCoins(item.cost), ShelfEconomy.check(item.id, emptyList(), 0, SlotRef(3, 1)))
    }

    @Test fun `free slots exclude reserved and occupied ones`() {
        val taken = setOf(SlotRef(0, 1))
        val free = ShelfLayout.freeSlots(taken, 5)
        assertFalse(SlotRef(0, 1) in free)
        assertTrue(free.none { ShelfLayout.isReserved(it) })
        assertTrue(SlotRef(0, 2) in free)
        assertEquals(free.size, free.toSet().size)
    }

    // ---- old saved positions ----
    @Test fun `items on slots that are now reserved move to a free spot and the rest stay put`() {
        // Under the old layout the jar and gacha stood on row 1's neighbours: these two collide with them.
        val old = listOf(
            OwnedItem("cat_calico", 1, 0, purchasedAt = 1),
            OwnedItem("rabbit", 1, 1, purchasedAt = 2),
            OwnedItem("pen_cup", 3, 1, purchasedAt = 3),
        )
        val resolved = ShelfLayout.resolve(old).associateBy { it.itemId }
        assertEquals(SlotRef(3, 1), resolved.getValue("pen_cup").slotRef) // a valid choice is never moved
        val slots = resolved.values.map { it.slotRef }
        assertEquals(3, slots.toSet().size)
        assertTrue(slots.none { ShelfLayout.isReserved(it) })
        assertTrue(slots.all { ShelfLayout.isValid(it) })
    }

    @Test fun `resolving is stable and leaves a clean shelf alone`() {
        val clean = listOf(OwnedItem("cat_calico", 0, 1, 1), OwnedItem("rabbit", 3, 0, 2))
        assertEquals(clean, ShelfLayout.resolve(clean))
        val clash = listOf(OwnedItem("cat_calico", 3, 0, 1), OwnedItem("rabbit", 3, 0, 2))
        assertEquals(ShelfLayout.resolve(clash), ShelfLayout.resolve(clash))
        assertEquals(SlotRef(3, 0), ShelfLayout.resolve(clash).first { it.itemId == "cat_calico" }.slotRef) // earlier purchase wins
    }

    @Test fun `a new purchase never lands on an item that was relocated`() {
        val old = listOf(OwnedItem("cat_calico", 1, 0, 1)) // collides with the jar
        val relocated = ShelfLayout.resolve(old).single().slotRef
        val r = ShelfEconomy.check("pen_cup", old, 100)
        assertTrue(r is PurchaseCheck.Ok && r.slot != relocated)
    }
}

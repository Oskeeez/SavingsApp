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
        val filled = mutableSetOf(first)
        assertEquals(SlotRef(0, 2), ShelfLayout.nextFreeSlot(filled).also { filled += it })
        assertEquals(SlotRef(0, 3), ShelfLayout.nextFreeSlot(filled).also { filled += it })
        // the second row keeps its two outer spots; the jar and the gacha machine hold the middle
        assertEquals(SlotRef(1, 0), ShelfLayout.nextFreeSlot(filled).also { filled += it })
        assertEquals(SlotRef(1, 3), ShelfLayout.nextFreeSlot(filled).also { filled += it })
        assertEquals(SlotRef(2, 0), ShelfLayout.nextFreeSlot(filled))
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
        for (item in ShelfCatalog.items.filter { it.surface == ShelfSurface.SHELF }) for (tier in 0 until tiers) {
            val h = ShelfLayout.heightFor(item, tier, tiers)
            val w = h * item.aspect
            assertTrue("${item.id} h=$h", h > 20f && h <= ShelfGeometry.compartmentHeight(tier, tiers))
            assertTrue("${item.id} w=$w", w <= ShelfLayout.MAX_ITEM_WIDTH + 0.5f)
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

    @Test fun `the five-row shelf is as tall as the artwork it is cut from, minus floor`() {
        // top 562 + 3 middles of 154 + bottom 370
        assertEquals(562 + 3 * 154 + 370, ShelfGeometry.composedHeight(5))
        assertEquals(546, ShelfGeometry.standLine(0, 5))
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
        for (bad in listOf(ShelfLayout.JAR, ShelfLayout.GACHA, ShelfLayout.CLOCK, ShelfLayout.MEMORY_JARS[0], SlotRef(3, 0), SlotRef(0, 5), SlotRef(-1, 0), SlotRef(2, 9), SlotRef(-3, 0))) {
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
            OwnedItem("cat_calico", 1, 1, purchasedAt = 1), // the jar's spot now
            OwnedItem("rabbit", 1, 2, purchasedAt = 2), // the gacha machine's spot now
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
        val old = listOf(OwnedItem("cat_calico", 1, 1, 1)) // collides with the jar
        val relocated = ShelfLayout.resolve(old).single().slotRef
        val r = ShelfEconomy.check("pen_cup", old, 100)
        assertTrue(r is PurchaseCheck.Ok && r.slot != relocated)
    }

    // ---- notes on the wall ----
    private val note get() = ShelfCatalog.items.first { it.surface == ShelfSurface.WALL }

    @Test fun `notes are their own category and go on the wall`() {
        val notes = ShelfCatalog.items.filter { it.category == ShelfCategory.NOTES }
        assertEquals(5, notes.size)
        assertTrue(notes.all { it.surface == ShelfSurface.WALL })
        assertTrue(ShelfCatalog.items.filter { it.category != ShelfCategory.NOTES }.all { it.surface == ShelfSurface.SHELF })
    }

    @Test fun `a note goes to the first free wall spot, upper row first`() {
        assertEquals(PurchaseCheck.Ok(SlotRef(-1, 0)), ShelfEconomy.check(note.id, emptyList(), 100))
        val one = listOf(OwnedItem("note_small_progress", -1, 0, 1))
        val r = ShelfEconomy.check(note.id, one, 100)
        assertEquals(PurchaseCheck.Ok(SlotRef(-1, 1)), r)
    }

    @Test fun `a note can be placed on a chosen wall spot but not on the shelf, and a shelf item not on the wall`() {
        assertEquals(PurchaseCheck.Ok(SlotRef(-2, 2)), ShelfEconomy.check(note.id, emptyList(), 100, SlotRef(-2, 2)))
        assertEquals(PurchaseCheck.SlotUnavailable, ShelfEconomy.check(note.id, emptyList(), 100, SlotRef(2, 0)))
        assertEquals(PurchaseCheck.SlotUnavailable, ShelfEconomy.check("pen_cup", emptyList(), 100, SlotRef(-1, 0)))
    }

    @Test fun `free wall spots never include shelf spots and the wall has room for every note`() {
        val free = ShelfLayout.freeSlots(emptySet(), tiers = 5, surface = ShelfSurface.WALL)
        assertTrue(free.all { it.isWall })
        assertTrue(free.size >= ShelfCatalog.items.count { it.surface == ShelfSurface.WALL })
        assertTrue(ShelfLayout.freeSlots(emptySet(), 5).none { it.isWall })
    }

    @Test fun `wall notes do not add shelf rows and fit the wall`() {
        val resolved = ShelfLayout.resolve(listOf(OwnedItem(note.id, -2, 2, 1)))
        assertEquals(ShelfGeometry.MIN_TIERS, ShelfLayout.tiersNeeded(resolved))
        for (n in ShelfCatalog.items.filter { it.surface == ShelfSurface.WALL }) {
            val h = ShelfLayout.heightFor(n, -1, 5)
            assertTrue("${n.id} h=$h", h in 40f..120.5f)
            assertTrue("${n.id} w=${h * n.aspect}", h * n.aspect <= ShelfLayout.MAX_NOTE_WIDTH + 0.5f)
        }
    }

    // ---- dragging things to new places ----
    @Test fun `an item can move to a free spot on its own surface`() {
        val own = listOf(OwnedItem("pen_cup", 0, 1, 1), OwnedItem("note_small_progress", -1, 0, 2))
        assertTrue(ShelfLayout.canMove(own, "pen_cup", SlotRef(3, 2)))
        assertTrue(ShelfLayout.canMove(own, "note_small_progress", SlotRef(-2, 1)))
    }

    @Test fun `an item cannot move onto a reserved, taken, off-shelf or wrong-surface spot`() {
        val own = listOf(OwnedItem("pen_cup", 0, 1, 1), OwnedItem("rabbit", 3, 0, 2), OwnedItem("note_small_progress", -1, 0, 3))
        for (bad in listOf(ShelfLayout.JAR, ShelfLayout.GACHA, ShelfLayout.CLOCK, ShelfLayout.MEMORY_JARS[1], SlotRef(3, 0), SlotRef(0, 9), SlotRef(-1, 0).copy(slot = 7), SlotRef(-1, 1))) {
            if (bad == SlotRef(-1, 1)) continue // that one is valid for a note; checked below
            assertFalse("$bad", ShelfLayout.canMove(own, "pen_cup", bad))
        }
        assertFalse(ShelfLayout.canMove(own, "note_small_progress", SlotRef(3, 1))) // a note cannot stand on the shelf
        assertTrue(ShelfLayout.canMove(own, "note_small_progress", SlotRef(-1, 1)))
    }

    @Test fun `moving to where it already is is fine, and unknown items cannot move`() {
        val own = listOf(OwnedItem("pen_cup", 0, 1, 1))
        assertTrue(ShelfLayout.canMove(own, "pen_cup", SlotRef(0, 1)))
        assertFalse(ShelfLayout.canMove(own, "rabbit", SlotRef(3, 1)))
    }

    // ---- the drag maths ----
    private val cup get() = ShelfCatalog.find("pen_cup")!!

    @Test fun `dropping an item close to where it started leaves it there`() {
        val own = listOf(OwnedItem("pen_cup", 2, 0, 1))
        val (x, y) = ShelfDrag.anchor(cup, SlotRef(2, 0), 5)
        assertEquals(SlotRef(2, 0), ShelfDrag.dropTarget(own, "pen_cup", 5, x + 6f, y - 4f))
    }

    @Test fun `dropping near a free spot moves it there`() {
        val own = listOf(OwnedItem("pen_cup", 2, 0, 1))
        val (x, y) = ShelfDrag.anchor(cup, SlotRef(3, 1), 5)
        assertEquals(SlotRef(3, 1), ShelfDrag.dropTarget(own, "pen_cup", 5, x, y))
    }

    @Test fun `dropping on the jar or another item never lands there`() {
        val own = listOf(OwnedItem("pen_cup", 2, 0, 1), OwnedItem("rabbit", 3, 0, 2))
        val (jx, jy) = ShelfDrag.anchor(cup, ShelfLayout.JAR, 5)
        val atJar = ShelfDrag.dropTarget(own, "pen_cup", 5, jx, jy)
        assertTrue(atJar != ShelfLayout.JAR)
        val (rx, ry) = ShelfDrag.anchor(cup, SlotRef(3, 0), 5)
        assertTrue(ShelfDrag.dropTarget(own, "pen_cup", 5, rx, ry) != SlotRef(3, 0))
    }

    @Test fun `dropping far from every spot cancels the move`() {
        val own = listOf(OwnedItem("pen_cup", 2, 0, 1))
        assertNull(ShelfDrag.dropTarget(own, "pen_cup", 5, -2000f, -2000f))
    }

    @Test fun `a note dropped on the shelf snaps to a wall spot or cancels, never to the shelf`() {
        val own = listOf(OwnedItem("note_small_progress", -1, 0, 1))
        val (x, y) = ShelfDrag.anchor(ShelfCatalog.find("note_small_progress")!!, SlotRef(-2, 2), 5)
        assertEquals(SlotRef(-2, 2), ShelfDrag.dropTarget(own, "note_small_progress", 5, x, y))
        val onShelf = ShelfDrag.dropTarget(own, "note_small_progress", 5, 300f, 700f)
        assertTrue(onShelf == null || onShelf.isWall)
    }

    @Test fun `unknown items have no drop target`() {
        assertNull(ShelfDrag.dropTarget(emptyList(), "pen_cup", 5, 100f, 100f))
    }
}

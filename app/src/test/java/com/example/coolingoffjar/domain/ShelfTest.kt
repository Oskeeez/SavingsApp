package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfTest {
    private fun owned(id: String, tier: Int, slot: Int, at: Long = 0) = OwnedItem(id, tier, slot, at)
    private fun core() = ShelfLayout.DEFAULT_SLOTS.map { (id, s) -> OwnedItem(id, s.tier, s.slot, 0) }
    private val shopShelfItems = ShelfCatalog.items.filter { it.surface == ShelfSurface.SHELF }
    private val notes = ShelfCatalog.items.filter { it.surface == ShelfSurface.WALL }

    // ---- catalogue ----
    @Test fun `catalogue ids are unique, paid items cost 1 to 10 and only the starting look is free`() {
        val all = ShelfCatalog.items + ShelfCatalog.core
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (i in ShelfCatalog.items) {
            if (i.surface == ShelfSurface.DECOR && i.cost == 0) continue
            assertTrue(i.id, i.cost in 1..10)
        }
        for (c in listOf(ShelfCategory.WALLS, ShelfCategory.FLOORS, ShelfCategory.SHELVES)) {
            assertEquals(c.name, 1, ShelfCatalog.items.count { it.category == c && it.cost == 0 })
            assertTrue(c.name, ShelfCatalog.items.count { it.category == c } >= 4)
        }
        assertEquals(5, ShelfCatalog.items.count { it.category == ShelfCategory.SHELVES })
    }

    @Test fun `the always-there objects are never for sale`() {
        for (c in ShelfCatalog.core) {
            assertTrue(ShelfCatalog.items.none { it.id == c.id })
            assertEquals(PurchaseCheck.UnknownItem, ShelfEconomy.check(c.id, emptyList(), 99))
        }
    }

    @Test fun `every category that is sold has something to buy`() {
        for (c in ShelfCategory.entries.filter { it.inShop }) assertTrue(c.name, ShelfCatalog.items.any { it.category == c })
    }

    // ---- geometry ----
    @Test fun `five levels, boards below each other, and room for things to stand`() {
        val lines = (0 until ShelfGeometry.LEVELS).map { ShelfGeometry.standLine(it) }
        assertEquals(lines.sorted(), lines)
        for (t in 0 until ShelfGeometry.LEVELS) {
            assertTrue(ShelfGeometry.compartmentHeight(t) in 80f..120f)
            assertTrue(ShelfGeometry.slotWidth(t) > ShelfLayout.MAX_ITEM_WIDTH)
        }
        assertTrue(lines.last() < ShelfGeometry.SHELF_BOTTOM)
    }

    @Test fun `the shelf stands on the floor and leaves wall above it for a heading and a picture frame`() {
        assertTrue(ShelfGeometry.SHELF_BOTTOM > ShelfGeometry.WALL_HEIGHT)
        assertTrue(ShelfGeometry.SHELF_BOTTOM < ShelfGeometry.WALL_HEIGHT + 60f) // against the wall, not out in the room
        assertTrue(ShelfGeometry.SHELF_TOP > 180f)
        assertTrue(ShelfGeometry.SHELF_LEFT > 0f)
        assertTrue(ShelfGeometry.SHELF_BOTTOM < ShelfGeometry.SCENE_HEIGHT)
    }

    @Test fun `every item fits its slot`() {
        for (item in shopShelfItems + ShelfCatalog.core) for (t in 0 until ShelfGeometry.LEVELS) {
            val h = ShelfLayout.heightFor(item, t)
            assertTrue("${item.id} tier $t", h * item.aspect <= ShelfGeometry.slotWidth(t) + 0.5f)
            assertTrue("${item.id} tier $t", h <= ShelfGeometry.compartmentHeight(t) + 0.5f)
        }
    }

    @Test fun `the jar is the biggest thing on the shelf`() {
        val jar = ShelfCatalog.find("jar")!!
        for (item in shopShelfItems) assertTrue(item.id, ShelfLayout.heightFor(item, 2) < ShelfLayout.heightFor(jar, 2))
    }

    // ---- layout ----
    @Test fun `the always-there objects start on the top two rows and the memory rows`() {
        val d = ShelfLayout.DEFAULT_SLOTS
        assertEquals(0, d.getValue("clock").tier)
        assertEquals(1, d.getValue("jar").tier)
        assertEquals(1, d.getValue("gacha").tier)
        assertEquals(d.values.size, d.values.toSet().size)
    }

    @Test fun `purchases never overlap and the bookcase eventually reports it is full`() {
        val taken = HashSet<SlotRef>()
        while (true) {
            val slot = ShelfLayout.nextFreeSlot(taken) ?: break
            assertTrue(ShelfLayout.isValid(slot))
            assertTrue("duplicate $slot", taken.add(slot))
        }
        assertEquals(ShelfGeometry.LEVELS * ShelfGeometry.SLOTS_PER_LEVEL, taken.size)
    }

    @Test fun `first purchases fill the bookcase top to bottom, left to right around the always-there objects`() {
        val taken = ShelfLayout.taken(ShelfLayout.resolve(core())).toMutableSet()
        assertEquals(SlotRef(0, 1), ShelfLayout.nextFreeSlot(taken).also { taken += it!! })
        assertEquals(SlotRef(0, 2), ShelfLayout.nextFreeSlot(taken))
    }

    @Test fun `purchase takes the wanted slot, refuses a taken one, and refuses when full`() {
        val own = core()
        assertEquals(PurchaseCheck.Ok(SlotRef(4, 0)), ShelfEconomy.check("bonsai", own, 99, SlotRef(4, 0)))
        assertEquals(PurchaseCheck.SlotUnavailable, ShelfEconomy.check("bonsai", own, 99, ShelfLayout.DEFAULT_SLOTS.getValue("jar")))
        assertEquals(PurchaseCheck.SlotUnavailable, ShelfEconomy.check("bonsai", own, 99, SlotRef(5, 0)))
        val full = (0 until 5).flatMap { t -> (0 until 4).map { owned("filler$it$t", t, it) } }
        assertEquals(PurchaseCheck.SlotUnavailable, ShelfEconomy.check("bonsai", full, 99))
    }

    @Test fun `economy counts decor and notes but never the always-there objects`() {
        val own = core() + owned("bonsai", 0, 1) + owned("wall_sage", 0, 0)
        val spent = ShelfEconomy.coinsSpent(own)
        assertEquals(ShelfCatalog.find("bonsai")!!.cost + ShelfCatalog.find("wall_sage")!!.cost, spent)
        assertEquals(20 - spent, ShelfEconomy.balance(20, own))
    }

    @Test fun `affordability, ownership and unknown items are checked`() {
        assertEquals(PurchaseCheck.NotEnoughCoins(ShelfCatalog.find("bonsai")!!.cost - 1), ShelfEconomy.check("bonsai", emptyList(), 1))
        assertEquals(PurchaseCheck.AlreadyOwned, ShelfEconomy.check("bonsai", listOf(owned("bonsai", 0, 1)), 99))
        assertEquals(PurchaseCheck.UnknownItem, ShelfEconomy.check("nope", emptyList(), 99))
    }

    @Test fun `walls floors and shelves are bought without a spot and then appear in the room look`() {
        assertEquals(PurchaseCheck.Ok(SlotRef(0, 0)), ShelfEconomy.check("wall_sage", emptyList(), 99))
        assertEquals(RoomLook.DEFAULT, RoomLook.from(emptySet(), emptyMap()))
        val look = RoomLook.from(setOf("wall_sage", "floor_oak"), mapOf("WALLS" to "wall_sage", "FLOORS" to "floor_walnut", "SHELVES" to "wall_sage"))
        assertEquals("wall_sage", look.wall)
        assertEquals("floor_oak", look.floor) // chosen but not owned -> starting floor
        assertEquals(RoomLook.DEFAULT.shelf, look.shelf) // wrong kind of thing -> starting shelf
    }

    @Test fun `decor never takes a slot`() {
        val own = core() + owned("wall_sage", 0, 0) + owned("shelf_walnut", 0, 0)
        assertTrue(ShelfLayout.resolve(own).none { it.itemId.startsWith("wall_") || it.itemId.startsWith("shelf_") })
    }

    @Test fun `resolve keeps clean positions and moves clashes to a free slot, earlier things win and core wins`() {
        val clean = core() + owned("cat_calico", 0, 1, 1) + owned("rabbit", 4, 0, 2)
        assertEquals(clean.sortedBy { it.itemId }, ShelfLayout.resolve(clean).sortedBy { it.itemId })
        val jar = ShelfLayout.DEFAULT_SLOTS.getValue("jar")
        val clash = core() + owned("cat_calico", jar.tier, jar.slot, 1)
        val resolved = ShelfLayout.resolve(clash).associateBy { it.itemId }
        assertEquals(jar, resolved.getValue("jar").slotRef)
        assertTrue(resolved.getValue("cat_calico").slotRef != jar)
        assertEquals(resolved.size, resolved.values.map { it.slotRef }.toSet().size)
    }

    @Test fun `old rows beyond the five levels are brought back onto the bookcase`() {
        val old = core() + owned("cat_calico", 7, 2, 1)
        val cat = ShelfLayout.resolve(old).first { it.itemId == "cat_calico" }
        assertTrue(ShelfLayout.isValid(cat.slotRef))
    }

    @Test fun `anything can be moved to an empty slot, the jar included, but not onto another thing`() {
        val own = core() + owned("cat_calico", 0, 1, 1)
        assertTrue(ShelfLayout.canMove(own, "jar", SlotRef(4, 3)))
        assertTrue(ShelfLayout.canMove(own, "clock", SlotRef(2, 0)))
        assertTrue(ShelfLayout.canMove(own, "gacha", SlotRef(3, 1)))
        assertFalse(ShelfLayout.canMove(own, "jar", SlotRef(0, 1))) // the cat
        assertFalse(ShelfLayout.canMove(own, "jar", SlotRef(5, 0)))
        assertFalse(ShelfLayout.canMove(own, "ghost", SlotRef(4, 3)))
    }

    // ---- notes: placed anywhere on the wall ----
    @Test fun `notes keep exactly the position they were given`() {
        val note = ShelfCatalog.find("note_good_days")!!
        val own = core() + OwnedItem(note.id, 0, 0, 1, 0.37f, 0.18f)
        val r = ShelfLayout.resolve(own).first { it.itemId == note.id }
        assertEquals(0.37f, r.x!!, 1e-6f)
        assertEquals(0.18f, r.y!!, 1e-6f)
    }

    @Test fun `a note without a position gets a default one and notes cannot leave the wall or the picture`() {
        for (note in notes) {
            val r = ShelfLayout.resolve(listOf(owned(note.id, 0, 0))).first()
            assertNotNull(r.x); assertNotNull(r.y)
            val (w, h) = ShelfLayout.wallSize(note)
            for (p in listOf(ScenePoint(-5f, -5f), ScenePoint(9f, 9f), ScenePoint(0f, 0.99f), ScenePoint(1f, 0f))) {
                val c = ShelfLayout.clampWall(note, p)
                assertTrue(c.x * ShelfGeometry.SCENE_WIDTH - w / 2 >= -0.01f)
                assertTrue(c.x * ShelfGeometry.SCENE_WIDTH + w / 2 <= ShelfGeometry.SCENE_WIDTH + 0.01f)
                assertTrue(c.y * ShelfGeometry.SCENE_HEIGHT - h / 2 >= -0.01f)
                assertTrue(c.y * ShelfGeometry.SCENE_HEIGHT + h / 2 <= ShelfGeometry.WALL_HEIGHT)
            }
        }
    }

    @Test fun `a note can be bought for an exact spot on the wall`() {
        val p = ScenePoint(0.3f, 0.2f)
        val ok = ShelfEconomy.check("note_good_days", emptyList(), 99, null, p) as PurchaseCheck.Ok
        assertEquals(p.x, ok.point!!.x, 1e-6f)
        assertEquals(p.y, ok.point!!.y, 1e-6f)
    }

    @Test fun `notes are not dropped into bookcase slots and bookcase items are not dropped on the wall`() {
        val own = core() + OwnedItem("note_good_days", 0, 0, 1, 0.3f, 0.2f)
        assertFalse(ShelfLayout.canMove(own, "note_good_days", SlotRef(4, 3)))
        assertNull(ShelfDrag.dropTarget(own, "note_good_days", 100f, 100f))
    }

    // ---- dragging ----
    @Test fun `dropping an item on top of its own slot keeps it there`() {
        val own = core() + owned("bonsai", 3, 0, 1)
        val item = ShelfCatalog.find("bonsai")!!
        val (x, y) = ShelfDrag.anchor(item, SlotRef(3, 0))
        assertEquals(SlotRef(3, 0), ShelfDrag.dropTarget(own, "bonsai", x, y))
    }

    @Test fun `dropping near an empty slot goes to it, and a drop onto another item goes to the nearest free slot`() {
        val own = core() + owned("bonsai", 3, 0, 1) + owned("cat_calico", 3, 1, 2)
        val item = ShelfCatalog.find("bonsai")!!
        val (tx, ty) = ShelfDrag.anchor(item, SlotRef(4, 2))
        assertEquals(SlotRef(4, 2), ShelfDrag.dropTarget(own, "bonsai", tx + 6, ty - 4))
        val (cx, cy) = ShelfDrag.anchor(item, SlotRef(3, 1))
        val t = ShelfDrag.dropTarget(own, "bonsai", cx, cy)
        assertNotNull(t)
        assertTrue(t != SlotRef(3, 1))
    }

    @Test fun `the jar can be dragged to another level`() {
        val own = core()
        val jar = ShelfCatalog.find("jar")!!
        val (x, y) = ShelfDrag.anchor(jar, SlotRef(4, 3))
        assertEquals(SlotRef(4, 3), ShelfDrag.dropTarget(own, "jar", x, y))
    }

    @Test fun `a drop far from everywhere is cancelled`() {
        val own = core() + owned("bonsai", 3, 0, 1)
        assertNull(ShelfDrag.dropTarget(own, "bonsai", -400f, -400f))
        assertNull(ShelfDrag.dropTarget(own, "unknown", 100f, 100f))
    }

    // ---- camera: never shows past the picture ----
    private fun checkCovers(f: SceneCamera.Frame, w: Float, h: Float) {
        val left = f.centerX - w / 2f / f.scale
        val right = f.centerX + w / 2f / f.scale
        val top = f.centerY - h / 2f / f.scale
        val bottom = f.centerY + h / 2f / f.scale
        assertTrue("left $left", left >= -0.01f)
        assertTrue("right $right", right <= ShelfGeometry.SCENE_WIDTH + 0.01f)
        assertTrue("top $top", top >= -0.01f)
        assertTrue("bottom $bottom", bottom <= ShelfGeometry.SCENE_HEIGHT + 0.01f)
    }

    @Test fun `the camera never shows the edge of the picture, on any screen, at any point of any zoom`() {
        val screens = listOf(1080f to 2400f, 1080f to 1920f, 1440f to 3120f, 800f to 1280f, 1200f to 1200f, 600f to 2400f)
        val targets = listOf(null) + ShelfCatalog.core.map { c ->
            ShelfLayout.resolve(core()).first { it.itemId == c.id }.let { SceneCamera.targetFor(it) }
        } + ZoomTarget(5f, 5f, 40f) + ZoomTarget(660f, 1180f, 100f) + ZoomTarget(332f, 100f, 100f)
        for ((w, h) in screens) for (t in targets) for (i in 0..20) checkCovers(SceneCamera.frame(i / 20f, t, w, h), w, h)
    }

    @Test fun `at rest the whole picture is shown scaled to cover the screen, centred`() {
        val f = SceneCamera.frame(0f, ZoomTarget(300f, 600f, 90f), 1080f, 2400f)
        assertEquals(SceneCamera.coverScale(1080f, 2400f), f.scale, 1e-4f)
        assertEquals(ShelfGeometry.SCENE_WIDTH / 2f, f.centerX, 1e-3f)
        assertEquals(ShelfGeometry.SCENE_HEIGHT / 2f, f.centerY, 1e-3f)
    }

    @Test fun `zoom moves smoothly and ends with the object about a third of the screen tall`() {
        val t = SceneCamera.targetFor(ShelfLayout.resolve(core()).first { it.itemId == "jar" })!!
        var prev = SceneCamera.frame(0f, t, 1080f, 2400f).scale
        for (i in 1..20) {
            val s = SceneCamera.frame(i / 20f, t, 1080f, 2400f).scale
            assertTrue(s >= prev); prev = s
        }
        assertEquals(SceneCamera.TARGET_HEIGHT * 2400f, prev * t.height, 2f)
    }

    // ---- lighting ----
    @Test fun `lower levels are a little darker, warmer and shadowier, but only a little`() {
        val item = ShelfCatalog.find("cat_calico")!!
        val l = (0 until 5).map { ShelfLighting.forObject(item, it, 0.5f) }
        for (i in 1 until 5) {
            assertTrue(l[i].brightness < l[i - 1].brightness)
            assertTrue(l[i].warmth >= l[i - 1].warmth)
            assertTrue(l[i].shadowOpacity >= l[i - 1].shadowOpacity)
        }
        assertTrue(l.all { it.brightness in 0.88f..1.08f })
        assertTrue(l.all { it.shadowOpacity in 0.05f..0.45f })
    }

    @Test fun `light comes from the left so shadows fall down and right and the left is slightly brighter`() {
        val item = ShelfCatalog.find("bonsai")!!
        for (t in 0 until 5) {
            val left = ShelfLighting.forObject(item, t, 0.2f)
            val right = ShelfLighting.forObject(item, t, 0.8f)
            assertTrue(left.shadowOffsetX > 0f && left.shadowOffsetY > 0f)
            assertTrue(left.brightness > right.brightness)
            assertTrue(left.brightness - right.brightness < 0.06f)
        }
    }

    @Test fun `shadows follow the object type`() {
        fun p(id: String) = ShelfLighting.profileFor(ShelfCatalog.find(id)!!)
        assertTrue(p("gacha").thickness > p("clock").thickness)
        assertTrue(p("gacha").strength > p("clock").strength)
        assertTrue(p("books_stack").thickness < p("anthurium").thickness)
        assertTrue(p("cat_calico").width > p("film_camera").width)
        assertTrue(p("anthurium").width > p("books_standing").width)
    }
}

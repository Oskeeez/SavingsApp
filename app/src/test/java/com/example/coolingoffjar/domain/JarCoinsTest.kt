package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class JarCoinsTest {
    private val slots = JarCoins.slots

    @Test fun `there are enough slots for a satisfying pile`() {
        assertTrue("slots=${slots.size}", slots.size in 10..40)
    }

    @Test fun `layout is stable between runs and seeds differ`() {
        assertEquals(slots, JarCoins.generate(JarCoins.SEED))
        assertTrue(JarCoins.generate(1L) != JarCoins.generate(2L))
    }

    @Test fun `every coin sits inside the jar picture`() {
        for (s in slots) {
            assertTrue("x=${s.x}", s.x in 0.17f..0.83f)
            assertTrue("y=${s.y}", s.y in 0.30f..0.94f)
        }
    }

    @Test fun `slots build up from the bottom of the jar`() {
        val q = slots.size / 3
        assertTrue(slots.take(q).map { it.y }.average() > slots.takeLast(q).map { it.y }.average() + 0.15)
    }

    @Test fun `coins overlap but never sit on top of each other`() {
        var min = Float.MAX_VALUE
        for (i in slots.indices) for (j in i + 1 until slots.size) {
            // compare in width units: jar is 154 wide by 211 tall
            min = minOf(min, hypot(slots[i].x - slots[j].x, (slots[i].y - slots[j].y) * 211f / 154f))
        }
        assertTrue("min=$min", min >= JarCoins.COIN_DIAMETER * 0.35f)
    }

    @Test fun `visible count follows round of F times S and is full exactly at k equals N`() {
        assertEquals(0, JarCoins.visibleCoinCount(0f))
        assertEquals(slots.size, JarCoins.visibleCoinCount(1f))
        for (n in 1..50) {
            assertEquals(slots.size, JarCoins.visibleCoinCount(JarRules.fillLevel(n, n)))
            var previous = 0
            for (k in 1..n) {
                val now = JarCoins.visibleCoinCount(JarRules.fillLevel(k, n))
                assertTrue(now >= previous)
                previous = now
            }
        }
    }
}

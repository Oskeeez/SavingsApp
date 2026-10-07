package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class CoinLayoutTest {
    private val slots = CoinLayout.slots
    private val d = CoinLayout.COIN_DIAMETER

    @Test fun `there are a sensible number of slots`() {
        assertTrue("slots=${slots.size}", slots.size in 60..200)
    }

    @Test fun `layout is stable between runs`() {
        assertEquals(CoinLayout.generate(CoinLayout.SEED), CoinLayout.generate(CoinLayout.SEED))
        assertEquals(slots, CoinLayout.generate(CoinLayout.SEED))
    }

    @Test fun `a different seed gives a different jitter`() {
        assertTrue(CoinLayout.generate(1L) != CoinLayout.generate(2L))
    }

    @Test fun `every coin sits inside the glass`() {
        for (s in slots) {
            val half = JarGeometry.interiorHalfWidth(s.y + d / 2 * 0.8f)
            assertTrue("x=${s.x} y=${s.y} half=$half", abs(s.x - 0.5f) + d / 2 * 0.85f <= half + 0.001f)
            assertTrue(s.y + d / 2 <= JarGeometry.INNER_BOTTOM + 0.001f)
            assertTrue(s.y - d / 2 >= JarGeometry.NECK_BOTTOM)
        }
    }

    @Test fun `coins overlap only slightly and never stack`() {
        var minDist = Float.MAX_VALUE
        for (i in slots.indices) for (j in i + 1 until slots.size) {
            minDist = minOf(minDist, hypot(slots[i].x - slots[j].x, slots[i].y - slots[j].y))
        }
        assertTrue("minDist=$minDist d=$d", minDist >= d * 0.55f)
    }

    @Test fun `slots are ordered from the bottom of the jar upwards`() {
        val q = slots.size / 4
        val bottom = slots.take(q).map { it.y }.average()
        val top = slots.takeLast(q).map { it.y }.average()
        assertTrue(bottom > top + 0.2)
    }

    @Test fun `some coins are tilted ellipses and most are front-on`() {
        val tilted = slots.count { it.squashY < 1f }
        assertTrue(tilted > slots.size / 10)
        assertTrue(tilted < slots.size / 2)
    }

    @Test fun `visible count is round of F times S`() {
        val s = slots.size
        assertEquals(0, CoinLayout.visibleCoinCount(0f))
        assertEquals(s, CoinLayout.visibleCoinCount(1f))
        assertEquals(s, CoinLayout.visibleCoinCount(2f)) // F is capped at 1 upstream, stay safe anyway
        assertEquals(Math.round(0.6f * s), CoinLayout.visibleCoinCount(0.6f))
    }

    @Test fun `jar is exactly full at k equals N, for any N`() {
        for (n in 1..50) {
            assertEquals(slots.size, CoinLayout.visibleCoinCount(JarRules.fillLevel(n, n)))
            assertEquals(0, CoinLayout.visibleCoinCount(JarRules.fillLevel(0, n)))
        }
    }

    @Test fun `each not-buy adds about S over N coins and the count never goes down`() {
        for (n in listOf(1, 3, 5, 10, 25, 50)) {
            var previous = 0
            for (k in 1..n) {
                val now = CoinLayout.visibleCoinCount(JarRules.fillLevel(k, n))
                val added = now - previous
                assertTrue("n=$n k=$k added=$added", added >= 0)
                assertTrue("n=$n k=$k added=$added", added <= Math.ceil(slots.size.toDouble() / n).toInt() + 1)
                previous = now
            }
        }
    }

    @Test fun `interior width is narrow at the neck and widest in the body`() {
        assertTrue(JarGeometry.interiorHalfWidth(0.15f) < JarGeometry.interiorHalfWidth(0.3f))
        assertTrue(JarGeometry.interiorHalfWidth(0.3f) < JarGeometry.interiorHalfWidth(0.6f))
        assertEquals(0f, JarGeometry.interiorHalfWidth(1.19f), 0f)
    }
}

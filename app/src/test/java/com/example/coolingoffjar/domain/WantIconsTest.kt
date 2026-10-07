package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WantIconsTest {
    @Test fun `icons are unique and the default is one of them`() {
        assertEquals(WantIcons.all.size, WantIcons.all.toSet().size)
        assertTrue(WantIcons.DEFAULT in WantIcons.all)
        assertEquals(24, WantIcons.all.size)
    }

    @Test fun `unknown or missing icons fall back to the default`() {
        assertEquals(WantIcons.DEFAULT, WantIcons.normalize(null))
        assertEquals(WantIcons.DEFAULT, WantIcons.normalize(""))
        assertEquals(WantIcons.DEFAULT, WantIcons.normalize("spaceship"))
        assertEquals("cat", WantIcons.normalize("cat"))
    }

    @Test fun `progress art is empty when empty and full exactly when the jar is full`() {
        for (n in 1..50) {
            assertEquals(0, ProgressArt.stateFor(0, n))
            assertEquals(ProgressArt.FULL, ProgressArt.stateFor(n, n))
            if (n > 1) assertTrue("n=$n", ProgressArt.stateFor(n - 1, n) < ProgressArt.FULL)
        }
    }

    @Test fun `progress art never goes backwards and the first coin always shows`() {
        for (n in 2..50) {
            var previous = 0
            for (k in 1 until n) {
                val s = ProgressArt.stateFor(k, n)
                assertTrue("n=$n k=$k", s in 1..4)
                assertTrue(s >= previous)
                previous = s
            }
        }
    }

    @Test fun `a jar of five lines up with the five dots one to one`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), (0..5).map { ProgressArt.stateFor(it, 5) })
    }
}

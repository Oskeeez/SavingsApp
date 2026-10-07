package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarArtTest {
    @Test fun `empty jar shows the empty picture`() {
        for (n in 1..50) assertEquals(JarArt.EMPTY, JarArt.stateFor(0, n))
    }

    @Test fun `full picture appears exactly when the jar is full, for any N`() {
        for (n in 1..50) {
            assertEquals(JarArt.FULL, JarArt.stateFor(n, n))
            assertEquals(JarArt.FULL, JarArt.stateFor(n + 3, n)) // over-full (N lowered) is still full
            if (n in 2..5) assertTrue("n=$n", JarArt.stateFor(n - 1, n) < JarArt.FULL)
        }
    }

    @Test fun `a bigger jar shows one coin per coin up to five and keeps the five coin picture until it is full`() {
        for (n in 6..50) {
            assertEquals((0..5).toList(), (0..5).map { JarArt.stateFor(it, n) })
            for (k in 5..n) assertEquals("n=$n k=$k", JarArt.FULL, JarArt.stateFor(k, n))
        }
    }

    @Test fun `anything in between uses the 1 to 4 coin pictures and never goes backwards`() {
        for (n in 2..5) {
            var previous = JarArt.EMPTY
            for (k in 1 until n) {
                val s = JarArt.stateFor(k, n)
                assertTrue("n=$n k=$k s=$s", s in 1..4)
                assertTrue(s >= previous)
                previous = s
            }
        }
    }

    @Test fun `the default jar of five steps through every picture in order`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5), (0..5).map { JarArt.stateFor(it, 5) })
    }

    @Test fun `a one-coin jar goes straight from empty to full`() {
        assertEquals(JarArt.EMPTY, JarArt.stateFor(0, 1))
        assertEquals(JarArt.FULL, JarArt.stateFor(1, 1))
    }
}

package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class JarRulesTest {
    // --- fill level ---
    @Test fun `fill level is k over N`() {
        assertEquals(0f, JarRules.fillLevel(0, 5), 0.0001f)
        assertEquals(0.6f, JarRules.fillLevel(3, 5), 0.0001f)
        assertEquals(1f, JarRules.fillLevel(5, 5), 0.0001f)
    }

    @Test fun `fill level is capped at 1 and safe for bad N`() {
        assertEquals(1f, JarRules.fillLevel(9, 5), 0.0001f)
        assertEquals(0f, JarRules.fillLevel(3, 0), 0.0001f)
    }

    // --- not buying / completion / rollover ---
    @Test fun `not buy adds one coin and does not complete below N`() {
        val t = JarRules.recordNotBuy(Jar(id = 7, filledCount = 2), notBuysPerJar = 5, now = 100)
        assertEquals(3, t.current.filledCount)
        assertFalse(t.current.isComplete)
        assertNull(t.next)
        assertNull(t.completedJar)
    }

    @Test fun `the Nth not buy completes the jar and starts a fresh empty one`() {
        val t = JarRules.recordNotBuy(Jar(id = 7, filledCount = 4), notBuysPerJar = 5, now = 100)
        assertEquals(5, t.current.filledCount)
        assertEquals(100L, t.current.completedAt)
        assertEquals(7L, t.current.id)
        assertTrue(t.current.hasUnusedFreebie)
        assertNotNull(t.next)
        assertEquals(0, t.next!!.filledCount)
        assertFalse(t.next!!.isComplete)
        assertSame(t.current, t.completedJar)
    }

    @Test fun `N of 1 completes on the first not buy`() {
        assertNotNull(JarRules.recordNotBuy(Jar(filledCount = 0), 1, now = 1).next)
    }

    @Test fun `a full sequence of N not-buys completes exactly once, at N`() {
        val n = 5
        var jar = Jar(id = 1)
        var completions = 0
        repeat(n) { i ->
            val t = JarRules.recordNotBuy(jar, n, now = 1000L + i)
            if (t.next != null) { completions++; assertEquals(n - 1, i); jar = t.next!! } else jar = t.current
        }
        assertEquals(1, completions)
        assertEquals(0, jar.filledCount) // rolled over to a fresh jar
    }

    // --- freebie ---
    @Test fun `freebie can be used once and records only a time`() {
        val completed = Jar(id = 1, filledCount = 5, completedAt = 10)
        val used = JarRules.useFreebie(completed, now = 50)
        assertTrue(used.freebieUsed)
        assertEquals(50L, used.freebieUsedAt)
        assertFalse(used.hasUnusedFreebie)
        assertSame(used, JarRules.useFreebie(used, now = 99)) // second press changes nothing
    }

    @Test fun `freebie cannot be used on a jar that is not complete`() {
        val open = Jar(id = 1, filledCount = 2)
        assertSame(open, JarRules.useFreebie(open, now = 5))
    }

    // --- settings-change rules for N ---
    @Test fun `raising N never completes the jar`() {
        assertNull(JarRules.reconcile(Jar(filledCount = 3), 10, now = 1).next)
    }

    @Test fun `lowering N above k does not complete the jar`() {
        assertNull(JarRules.reconcile(Jar(filledCount = 3), 4, now = 1).next)
    }

    @Test fun `lowering N to exactly k completes the jar`() {
        val t = JarRules.reconcile(Jar(id = 2, filledCount = 3), 3, now = 77)
        assertEquals(77L, t.current.completedAt)
        assertEquals(3, t.current.filledCount)
        assertNotNull(t.next)
    }

    @Test fun `lowering N below k completes the jar and keeps its coins`() {
        val t = JarRules.reconcile(Jar(id = 2, filledCount = 4), 2, now = 77)
        assertEquals(4, t.current.filledCount)
        assertNotNull(t.completedJar)
        assertEquals(0, t.next!!.filledCount)
    }

    @Test fun `reconcile never touches an already completed jar`() {
        val done = Jar(id = 1, filledCount = 5, completedAt = 10)
        val t = JarRules.reconcile(done, 1, now = 99)
        assertSame(done, t.current)
        assertNull(t.next)
    }
}

package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CoolOffRulesTest {
    private val day = CoolOffRules.DAY_MS
    private fun want(unlockAt: Long, status: WantStatus = WantStatus.COOLING) =
        Want(id = 1, name = "Lamp", createdAt = 0, unlockAt = unlockAt, status = status)

    @Test fun `unlock time is createdAt plus days in millis`() {
        assertEquals(1_000L + 30 * 86_400_000L, CoolOffRules.unlockAt(1_000L, 30))
        assertEquals(86_400_000L, CoolOffRules.unlockAt(0, 1))
    }

    @Test fun `unlock time for 365 days does not overflow int math`() {
        assertEquals(365L * 86_400_000L, CoolOffRules.unlockAt(0, 365))
    }

    @Test fun `cooling before unlock`() {
        assertEquals(WantStatus.COOLING, CoolOffRules.effectiveStatus(want(10 * day), now = 10 * day - 1))
    }

    @Test fun `ready exactly at unlock`() {
        assertEquals(WantStatus.READY, CoolOffRules.effectiveStatus(want(10 * day), now = 10 * day))
    }

    @Test fun `ready waits indefinitely with no expiry`() {
        assertEquals(WantStatus.READY, CoolOffRules.effectiveStatus(want(10 * day), now = 10 * day + 100_000L * day))
    }

    @Test fun `stored READY is not trusted if the clock moves back`() {
        val stored = want(10 * day, WantStatus.READY)
        assertEquals(WantStatus.COOLING, CoolOffRules.effectiveStatus(stored, now = 5 * day))
    }

    @Test fun `stored COOLING becomes READY from time alone`() {
        // Simulates reboot / long absence: nothing ran, only the clock moved.
        assertEquals(WantStatus.READY, CoolOffRules.effectiveStatus(want(10 * day), now = 11 * day))
    }

    @Test fun `decided wants keep their decision whatever the time`() {
        assertEquals(WantStatus.SKIPPED, CoolOffRules.effectiveStatus(want(10 * day, WantStatus.SKIPPED), now = 0))
        assertEquals(WantStatus.BOUGHT, CoolOffRules.effectiveStatus(want(10 * day, WantStatus.BOUGHT), now = 99 * day))
    }

    @Test fun `days left rounds up and bottoms out at zero`() {
        assertEquals(10, CoolOffRules.daysLeft(10 * day, now = 0))
        assertEquals(1, CoolOffRules.daysLeft(10 * day, now = 9 * day + 1))
        assertEquals(1, CoolOffRules.daysLeft(10 * day, now = 10 * day - 1))
        assertEquals(0, CoolOffRules.daysLeft(10 * day, now = 10 * day))
        assertEquals(0, CoolOffRules.daysLeft(10 * day, now = 20 * day))
    }
}

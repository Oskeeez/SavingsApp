package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRulesTest {
    @Test fun `defaults are 30 days and 5 coins`() {
        val s = Settings()
        assertEquals(30, s.coolOffDays)
        assertEquals(5, s.notBuysPerJar)
    }

    @Test fun `cool-off days are clamped to 1 through 365`() {
        assertEquals(1, SettingsRules.clampCoolOffDays(0))
        assertEquals(1, SettingsRules.clampCoolOffDays(-40))
        assertEquals(1, SettingsRules.clampCoolOffDays(1))
        assertEquals(30, SettingsRules.clampCoolOffDays(30))
        assertEquals(365, SettingsRules.clampCoolOffDays(365))
        assertEquals(365, SettingsRules.clampCoolOffDays(10_000))
    }

    @Test fun `not-buys per jar is clamped to 1 through 50`() {
        assertEquals(1, SettingsRules.clampNotBuysPerJar(0))
        assertEquals(1, SettingsRules.clampNotBuysPerJar(1))
        assertEquals(50, SettingsRules.clampNotBuysPerJar(50))
        assertEquals(50, SettingsRules.clampNotBuysPerJar(51))
    }

    @Test fun `changing cool-off days only affects wants added afterwards`() {
        val day = CoolOffRules.DAY_MS
        val created = 1_000L
        val before = Want(1, "Old", created, CoolOffRules.unlockAt(created, 30), WantStatus.COOLING)
        // Setting changes from 30 to 7 days; the stored unlockAt of the existing want is what counts.
        val after = Want(2, "New", created, CoolOffRules.unlockAt(created, 7), WantStatus.COOLING)
        val now = created + 10 * day
        assertEquals(WantStatus.COOLING, CoolOffRules.effectiveStatus(before, now))
        assertEquals(WantStatus.READY, CoolOffRules.effectiveStatus(after, now))
        assertEquals(created + 30 * day, before.unlockAt)
    }
}

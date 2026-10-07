package com.example.coolingoffjar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class DebugTimeTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private fun millis(z: ZonedDateTime) = z.toInstant().toEpochMilli()

    @Test fun `picking today gives no offset`() {
        val now = millis(ZonedDateTime.of(2026, 10, 7, 14, 30, 0, 0, utc))
        val picked = DebugTime.pickerMillisFor(now, utc)
        assertEquals(0L, DebugTime.offsetForPickedDate(picked, now, utc))
    }

    @Test fun `picking ten days ahead shifts by exactly ten days in UTC and keeps time of day`() {
        val now = millis(ZonedDateTime.of(2026, 10, 7, 14, 30, 0, 0, utc))
        val picked = DebugTime.pickerMillisFor(now + 10 * CoolOffRules.DAY_MS, utc)
        assertEquals(10 * CoolOffRules.DAY_MS, DebugTime.offsetForPickedDate(picked, now, utc))
    }

    @Test fun `picking a past date gives a negative offset`() {
        val now = millis(ZonedDateTime.of(2026, 10, 7, 14, 30, 0, 0, utc))
        val picked = DebugTime.pickerMillisFor(now - 3 * CoolOffRules.DAY_MS, utc)
        assertEquals(-3 * CoolOffRules.DAY_MS, DebugTime.offsetForPickedDate(picked, now, utc))
    }

    @Test fun `across a DST change the offset is within an hour of whole days`() {
        val zone = ZoneId.of("Europe/London")
        val now = millis(ZonedDateTime.of(2026, 10, 20, 12, 0, 0, 0, zone)) // clocks go back on 25 Oct 2026
        val picked = DebugTime.pickerMillisFor(now + 10 * CoolOffRules.DAY_MS, zone)
        val offset = DebugTime.offsetForPickedDate(picked, now, zone)
        assertTrue(Math.abs(offset - 10 * CoolOffRules.DAY_MS) <= 3_600_000L)
        // And the simulated moment still reads 12:00 local.
        val simulated = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(now + offset), zone)
        assertEquals(12, simulated.hour)
    }
}

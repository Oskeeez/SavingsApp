package com.example.coolingoffjar.domain

/** Time rules. Everything is a pure function of stored epoch millis and a `now` passed in: no timers. */
object CoolOffRules {
    const val DAY_MS = 86_400_000L

    /** Fixed 24h days (not calendar days), so DST and timezone changes can never shift an unlock time. */
    fun unlockAt(createdAt: Long, coolOffDays: Int): Long = createdAt + coolOffDays.toLong() * DAY_MS

    /**
     * COOLING vs READY comes from the stored [Want.unlockAt] and [now]. A decided want keeps its decision.
     * A READY want stays READY indefinitely; there is no expiry.
     */
    fun effectiveStatus(want: Want, now: Long): WantStatus = when (want.status) {
        WantStatus.BOUGHT, WantStatus.SKIPPED -> want.status
        WantStatus.COOLING, WantStatus.READY ->
            if (now >= want.unlockAt) WantStatus.READY else WantStatus.COOLING
    }

    /** Whole days remaining, rounded up so "1 day left" shows until the very last moment. 0 once ready. */
    fun daysLeft(unlockAt: Long, now: Long): Int =
        if (now >= unlockAt) 0 else ((unlockAt - now + DAY_MS - 1) / DAY_MS).toInt()
}

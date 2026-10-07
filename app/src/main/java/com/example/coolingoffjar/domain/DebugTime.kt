package com.example.coolingoffjar.domain

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Pure helpers for the debug "change date" feature. */
object DebugTime {
    /**
     * Offset (millis) that makes "today" equal the picked date, keeping the real time of day.
     * [pickedUtcMidnight] is what Material's DatePicker returns: the date at 00:00 UTC.
     */
    fun offsetForPickedDate(pickedUtcMidnight: Long, realNow: Long, zone: ZoneId): Long {
        val date = Instant.ofEpochMilli(pickedUtcMidnight).atZone(ZoneOffset.UTC).toLocalDate()
        val timeOfDay = Instant.ofEpochMilli(realNow).atZone(zone).toLocalTime()
        val target = date.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
        return target - realNow
    }

    /** Inverse for pre-selecting the picker: the local date of [now], expressed as 00:00 UTC. */
    fun pickerMillisFor(now: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

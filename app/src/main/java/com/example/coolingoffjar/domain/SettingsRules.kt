package com.example.coolingoffjar.domain

object SettingsRules {
    const val DEFAULT_COOL_OFF_DAYS = 30
    const val MIN_COOL_OFF_DAYS = 1
    const val MAX_COOL_OFF_DAYS = 365

    const val DEFAULT_NOT_BUYS_PER_JAR = 5
    const val MIN_NOT_BUYS_PER_JAR = 1
    const val MAX_NOT_BUYS_PER_JAR = 50

    fun clampCoolOffDays(days: Int): Int = days.coerceIn(MIN_COOL_OFF_DAYS, MAX_COOL_OFF_DAYS)
    fun clampNotBuysPerJar(n: Int): Int = n.coerceIn(MIN_NOT_BUYS_PER_JAR, MAX_NOT_BUYS_PER_JAR)
}

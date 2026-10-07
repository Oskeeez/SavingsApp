package com.example.coolingoffjar.domain

/** Lifecycle of a want. READY is derived from time (see [CoolOffRules]); it is never trusted from storage. */
enum class WantStatus { COOLING, READY, BOUGHT, SKIPPED }

/** Something the user wants to buy. Deliberately has no price/amount field. */
data class Want(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val unlockAt: Long,
    val status: WantStatus,
    val decidedAt: Long? = null,
)

/** A jar of "not buy" coins. The current jar is the one with no [completedAt]. */
data class Jar(
    val id: Long = 0,
    val filledCount: Int = 0,
    val completedAt: Long? = null,
    val freebieUsed: Boolean = false,
    val freebieUsedAt: Long? = null,
) {
    val isComplete: Boolean get() = completedAt != null
    val hasUnusedFreebie: Boolean get() = isComplete && !freebieUsed
}

data class Settings(
    val coolOffDays: Int = SettingsRules.DEFAULT_COOL_OFF_DAYS,
    val notBuysPerJar: Int = SettingsRules.DEFAULT_NOT_BUYS_PER_JAR,
)

package com.example.coolingoffjar.domain

/**
 * Result of applying a change to the current jar.
 * If [next] is non-null, [current] was just completed (freebie unlocked) and [next] is the fresh empty jar to insert.
 */
data class JarTransition(val current: Jar, val next: Jar?) {
    val completedJar: Jar? get() = if (next != null) current else null
}

object JarRules {
    /** F = min(1, k / N). */
    fun fillLevel(filledCount: Int, notBuysPerJar: Int): Float =
        if (notBuysPerJar <= 0) 0f else (filledCount.toFloat() / notBuysPerJar).coerceIn(0f, 1f)

    /** A "Not buying" decision: one more coin, then complete and roll over if the jar is full. */
    fun recordNotBuy(jar: Jar, notBuysPerJar: Int, now: Long): JarTransition =
        reconcile(jar.copy(filledCount = jar.filledCount + 1), notBuysPerJar, now)

    /**
     * Completes [jar] when k >= N, otherwise leaves it alone. Also the rule for changing N:
     * the new N applies to the current jar immediately, so lowering it can complete the jar.
     * A jar that is already complete is never touched.
     */
    fun reconcile(jar: Jar, notBuysPerJar: Int, now: Long): JarTransition =
        if (!jar.isComplete && jar.filledCount >= notBuysPerJar) {
            JarTransition(jar.copy(completedAt = now), Jar())
        } else {
            JarTransition(jar, null)
        }

    /** Honour-based: marks the freebie spent once. No amount is recorded. No-op if unavailable or already used. */
    fun useFreebie(jar: Jar, now: Long): Jar =
        if (jar.hasUnusedFreebie) jar.copy(freebieUsed = true, freebieUsedAt = now) else jar
}

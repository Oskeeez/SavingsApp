package com.example.coolingoffjar.data.repo

import androidx.room.withTransaction
import com.example.coolingoffjar.data.AppClock
import com.example.coolingoffjar.data.db.AppDatabase
import com.example.coolingoffjar.data.db.JarEntity
import com.example.coolingoffjar.data.db.toDomain
import com.example.coolingoffjar.data.db.toEntity
import com.example.coolingoffjar.data.settings.SettingsRepository
import com.example.coolingoffjar.domain.CoolOffRules
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.JarRules
import com.example.coolingoffjar.domain.JarTransition
import com.example.coolingoffjar.domain.Settings
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantIcons
import com.example.coolingoffjar.domain.WantStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface DecisionResult {
    /** The want is missing, already decided, or still cooling. Nothing changed. */
    data object NotAvailable : DecisionResult

    /** Decision saved. [completedJar] is non-null when this "not buy" filled the jar (freebie unlocked, new jar started). */
    data class Done(val completedJar: Jar? = null) : DecisionResult
}

/** Single entry point for the UI. Rules live in `domain`; this class only does storage and transactions. */
class CoolingOffRepository(
    private val db: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: () -> Long = AppClock::now,
) {
    private val wantDao = db.wantDao()
    private val jarDao = db.jarDao()

    /** Undecided wants. Use [CoolOffRules.effectiveStatus] with the current time to split READY from COOLING. */
    val openWants: Flow<List<Want>> = wantDao.observeOpen().map { list -> list.map { it.toDomain() } }

    /** Emits once the current jar exists (see [ensureCurrentJar]). */
    val currentJar: Flow<Jar> = jarDao.observeCurrent().filterNotNull().map { it.toDomain() }

    val completedJars: Flow<List<Jar>> = jarDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    val settings: Flow<Settings> = settingsRepository.settings

    /** The just-completed jar whose "Freebie unlocked" card is still waiting for Use freebie / Later. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pendingCelebration: Flow<Jar?> = settingsRepository.pendingCelebrationJarId.flatMapLatest { id ->
        if (id == null) flowOf(null) else jarDao.observeById(id).map { it?.toDomain() }
    }

    suspend fun getOpenWants(): List<Want> = wantDao.getOpen().map { it.toDomain() }

    suspend fun getWant(id: Long): Want? = wantDao.getById(id)?.toDomain()

    /** Safe to call any time; creates the first jar on a fresh install. */
    suspend fun ensureCurrentJar() {
        db.withTransaction { currentJarOrCreate() }
    }

    /** Returns the saved want, or null if [name] is blank. unlockAt uses the cool-off setting as of now. */
    suspend fun addWant(name: String, iconKey: String = WantIcons.DEFAULT): Want? {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        val now = clock()
        val days = settingsRepository.settings.first().coolOffDays
        val want = Want(
            name = trimmed,
            createdAt = now,
            unlockAt = CoolOffRules.unlockAt(now, days),
            status = WantStatus.COOLING,
            iconKey = WantIcons.normalize(iconKey),
        )
        return want.copy(id = wantDao.insert(want.toEntity()))
    }

    /** "Not buying": mark SKIPPED, add a coin, and complete + roll over the jar if it is now full. */
    suspend fun decideNotBuying(wantId: Long): DecisionResult {
        val completed = db.withTransaction {
            val now = clock()
            val want = readyWantOrNull(wantId, now) ?: return@withTransaction null
            wantDao.setDecision(want.id, WantStatus.SKIPPED, now)
            val n = settingsRepository.settings.first().notBuysPerJar
            Decided(persist(JarRules.recordNotBuy(currentJarOrCreate().toDomain(), n, now)))
        } ?: return DecisionResult.NotAvailable
        completed.jar?.let { announceCompletion(it) }
        return DecisionResult.Done(completed.jar)
    }

    /** "Still want it": neutral, no coin. */
    suspend fun decideStillWant(wantId: Long): DecisionResult = db.withTransaction {
        val now = clock()
        val want = readyWantOrNull(wantId, now) ?: return@withTransaction DecisionResult.NotAvailable
        wantDao.setDecision(want.id, WantStatus.BOUGHT, now)
        DecisionResult.Done()
    }

    /** Returns the deleted want so the UI can offer undo via [restoreWant]. */
    suspend fun deleteWant(id: Long): Want? = db.withTransaction {
        val want = wantDao.getById(id)?.toDomain()
        if (want != null) wantDao.delete(id)
        want
    }

    suspend fun restoreWant(want: Want) {
        wantDao.insert(want.toEntity()) // REPLACE keeps the original id, so the notification can be rescheduled
    }

    /**
     * Honour-based: records that the freebie was spent, nothing more (no amounts). If it was spent on one of the things
     * being waited for ([spentOnWantId]), that item is marked as bought: a freebie is a guilt-free "yes", even before the
     * cooling-off is over. Also clears the celebration card if it was showing.
     */
    suspend fun useFreebie(jarId: Long, spentOnWantId: Long? = null) {
        db.withTransaction {
            val now = clock()
            val jar = jarDao.getById(jarId)?.toDomain() ?: return@withTransaction
            val updated = JarRules.useFreebie(jar, now)
            if (updated != jar) {
                jarDao.update(updated.toEntity())
                if (spentOnWantId != null) {
                    val want = wantDao.getById(spentOnWantId)?.toDomain()
                    if (want != null && (want.status == WantStatus.COOLING || want.status == WantStatus.READY)) {
                        wantDao.setDecision(want.id, WantStatus.BOUGHT, now)
                    }
                }
            }
        }
        dismissCelebration(jarId)
    }

    /** "Later": the freebie stays unused (badge on the Shelf) but the card goes away. */
    suspend fun dismissCelebration(jarId: Long? = null) {
        val pending = settingsRepository.pendingCelebrationJarId.first()
        if (jarId == null || pending == jarId) settingsRepository.setPendingCelebrationJarId(null)
    }

    /** Only affects wants added afterwards: existing wants keep their stored unlockAt. */
    suspend fun setCoolOffDays(days: Int) = settingsRepository.setCoolOffDays(days)

    /**
     * Applies to the current jar immediately. Returns the completed jar if the new N is already reached
     * (k >= N), in which case the jar is completed and a fresh one started.
     */
    suspend fun setNotBuysPerJar(n: Int): Jar? {
        val completed = db.withTransaction {
            settingsRepository.setNotBuysPerJar(n)
            val effectiveN = settingsRepository.settings.first().notBuysPerJar
            persist(JarRules.reconcile(currentJarOrCreate().toDomain(), effectiveN, clock()))
        }
        completed?.let { announceCompletion(it) }
        return completed
    }

    // --- Debug-only helpers, used by Settings > Developer (hidden in release builds) ---

    suspend fun debugAddSampleWants() {
        val now = clock()
        val samples = listOf("Ready now (sample)" to 0L, "Ready in 2 days (sample)" to 2L, "Ready in 20 days (sample)" to 20L)
        for ((name, days) in samples) {
            val unlockAt = if (days == 0L) now - 1 else now + days * CoolOffRules.DAY_MS
            wantDao.insert(Want(name = name, createdAt = now, unlockAt = unlockAt, status = WantStatus.COOLING).toEntity())
        }
    }

    suspend fun debugMakeAllReady() = wantDao.setAllOpenUnlockAt(clock() - 1)

    /** Adds one coin without a want. May complete the jar. */
    suspend fun debugAddCoin() {
        val completed = db.withTransaction {
            val n = settingsRepository.settings.first().notBuysPerJar
            persist(JarRules.recordNotBuy(currentJarOrCreate().toDomain(), n, clock()))
        }
        completed?.let { announceCompletion(it) }
    }

    /** Adds [count] coins one after another (completing jars as they fill). Handy for trying the shop. */
    suspend fun debugAddCoins(count: Int) {
        repeat(count) { debugAddCoin() }
    }

    /** Fills the current jar to N and completes it. */
    suspend fun debugCompleteJar() {
        val completed = db.withTransaction {
            val n = settingsRepository.settings.first().notBuysPerJar
            val jar = currentJarOrCreate().toDomain()
            persist(JarRules.reconcile(jar.copy(filledCount = maxOf(jar.filledCount, n)), n, clock()))
        }
        completed?.let { announceCompletion(it) }
    }

    /** Deletes every want and jar and starts afresh. Settings are kept. */
    suspend fun debugResetAll() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        settingsRepository.setPendingCelebrationJarId(null)
        ensureCurrentJar()
    }

    // --- internals ---

    private class Decided(val jar: Jar?)

    private suspend fun readyWantOrNull(id: Long, now: Long): Want? {
        val want = wantDao.getById(id)?.toDomain() ?: return null
        return want.takeIf { CoolOffRules.effectiveStatus(it, now) == WantStatus.READY }
    }

    private suspend fun currentJarOrCreate(): JarEntity =
        jarDao.getCurrent() ?: JarEntity(0, 0, null, false, null).let { it.copy(id = jarDao.insert(it)) }

    /** Saves the transition; returns the jar that was just completed, if any. Call inside a transaction. */
    private suspend fun persist(transition: JarTransition): Jar? {
        jarDao.update(transition.current.toEntity())
        transition.next?.let { jarDao.insert(it.toEntity()) }
        return transition.completedJar
    }

    /** Remember that this jar's "Freebie unlocked" card still needs showing. Call after the transaction. */
    private suspend fun announceCompletion(jar: Jar) {
        settingsRepository.setPendingCelebrationJarId(jar.id)
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
    }
}

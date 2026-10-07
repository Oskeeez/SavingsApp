package com.example.coolingoffjar.data.repo

import androidx.room.withTransaction
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
import com.example.coolingoffjar.domain.WantStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val wantDao = db.wantDao()
    private val jarDao = db.jarDao()

    /** Undecided wants. Use [CoolOffRules.effectiveStatus] with the current time to split READY from COOLING. */
    val openWants: Flow<List<Want>> = wantDao.observeOpen().map { list -> list.map { it.toDomain() } }

    /** Emits once the current jar exists (see [ensureCurrentJar]). */
    val currentJar: Flow<Jar> = jarDao.observeCurrent().filterNotNull().map { it.toDomain() }

    val completedJars: Flow<List<Jar>> = jarDao.observeCompleted().map { list -> list.map { it.toDomain() } }

    val settings: Flow<Settings> = settingsRepository.settings

    suspend fun getOpenWants(): List<Want> = wantDao.getOpen().map { it.toDomain() }

    suspend fun getWant(id: Long): Want? = wantDao.getById(id)?.toDomain()

    /** Safe to call any time; creates the first jar on a fresh install. */
    suspend fun ensureCurrentJar() {
        db.withTransaction { currentJarOrCreate() }
    }

    /** Returns the saved want, or null if [name] is blank. unlockAt uses the cool-off setting as of now. */
    suspend fun addWant(name: String): Want? {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return null
        val now = clock()
        val days = settingsRepository.settings.first().coolOffDays
        val want = Want(
            name = trimmed,
            createdAt = now,
            unlockAt = CoolOffRules.unlockAt(now, days),
            status = WantStatus.COOLING,
        )
        return want.copy(id = wantDao.insert(want.toEntity()))
    }

    /** "Not buying": mark SKIPPED, add a coin, and complete + roll over the jar if it is now full. */
    suspend fun decideNotBuying(wantId: Long): DecisionResult = db.withTransaction {
        val now = clock()
        val want = readyWantOrNull(wantId, now) ?: return@withTransaction DecisionResult.NotAvailable
        wantDao.setDecision(want.id, WantStatus.SKIPPED, now)
        val n = settingsRepository.settings.first().notBuysPerJar
        val transition = JarRules.recordNotBuy(currentJarOrCreate().toDomain(), n, now)
        persist(transition)
        DecisionResult.Done(transition.completedJar)
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

    suspend fun useFreebie(jarId: Long) {
        db.withTransaction {
            val jar = jarDao.getById(jarId)?.toDomain() ?: return@withTransaction
            val updated = JarRules.useFreebie(jar, clock())
            if (updated != jar) jarDao.update(updated.toEntity())
        }
    }

    /** Only affects wants added afterwards: existing wants keep their stored unlockAt. */
    suspend fun setCoolOffDays(days: Int) = settingsRepository.setCoolOffDays(days)

    /**
     * Applies to the current jar immediately. Returns the completed jar if the new N is already reached
     * (k >= N), in which case the jar is completed and a fresh one started.
     */
    suspend fun setNotBuysPerJar(n: Int): Jar? = db.withTransaction {
        settingsRepository.setNotBuysPerJar(n)
        val effectiveN = settingsRepository.settings.first().notBuysPerJar
        val transition = JarRules.reconcile(currentJarOrCreate().toDomain(), effectiveN, clock())
        persist(transition)
        transition.completedJar
    }

    // --- internals (call inside a transaction) ---

    private suspend fun readyWantOrNull(id: Long, now: Long): Want? {
        val want = wantDao.getById(id)?.toDomain() ?: return null
        return want.takeIf { CoolOffRules.effectiveStatus(it, now) == WantStatus.READY }
    }

    private suspend fun currentJarOrCreate(): JarEntity =
        jarDao.getCurrent() ?: JarEntity(0, 0, null, false, null).let { it.copy(id = jarDao.insert(it)) }

    private suspend fun persist(transition: JarTransition) {
        jarDao.update(transition.current.toEntity())
        transition.next?.let { jarDao.insert(it.toEntity()) }
    }


    companion object {
        const val MAX_NAME_LENGTH = 80
    }
}

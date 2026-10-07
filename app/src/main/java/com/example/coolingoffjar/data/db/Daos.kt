package com.example.coolingoffjar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.coolingoffjar.domain.WantStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WantDao {
    /** Undecided wants, soonest unlock first. COOLING vs READY is derived from unlockAt in the domain layer. */
    @Query("SELECT * FROM wants WHERE status IN ('COOLING', 'READY') ORDER BY unlockAt ASC, id ASC")
    fun observeOpen(): Flow<List<WantEntity>>

    @Query("SELECT * FROM wants WHERE status IN ('COOLING', 'READY') ORDER BY unlockAt ASC, id ASC")
    suspend fun getOpen(): List<WantEntity>

    @Query("SELECT * FROM wants WHERE id = :id")
    suspend fun getById(id: Long): WantEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(want: WantEntity): Long

    @Query("UPDATE wants SET status = :status, decidedAt = :decidedAt WHERE id = :id")
    suspend fun setDecision(id: Long, status: WantStatus, decidedAt: Long)

    /** Debug only: pretend every open want's cooling-off just ended. */
    @Query("UPDATE wants SET unlockAt = :unlockAt WHERE status IN ('COOLING', 'READY')")
    suspend fun setAllOpenUnlockAt(unlockAt: Long)

    @Query("DELETE FROM wants WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface JarDao {
    /** The current jar is the one that has not been completed. */
    @Query("SELECT * FROM jars WHERE completedAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeCurrent(): Flow<JarEntity?>

    @Query("SELECT * FROM jars WHERE completedAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getCurrent(): JarEntity?

    @Query("SELECT * FROM jars WHERE completedAt IS NOT NULL ORDER BY completedAt DESC, id DESC")
    fun observeCompleted(): Flow<List<JarEntity>>

    @Query("SELECT * FROM jars WHERE id = :id")
    suspend fun getById(id: Long): JarEntity?

    @Query("SELECT * FROM jars WHERE id = :id")
    fun observeById(id: Long): Flow<JarEntity?>

    @Insert
    suspend fun insert(jar: JarEntity): Long

    @Update
    suspend fun update(jar: JarEntity)
}

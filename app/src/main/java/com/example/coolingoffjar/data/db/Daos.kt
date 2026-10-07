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

    /** Every coin that has ever gone into any jar: the coins earned so far. */
    @Query("SELECT COALESCE(SUM(filledCount), 0) FROM jars")
    fun observeTotalCoins(): Flow<Int>

    @Query("SELECT COALESCE(SUM(filledCount), 0) FROM jars")
    suspend fun totalCoins(): Int

    @Insert
    suspend fun insert(jar: JarEntity): Long

    @Update
    suspend fun update(jar: JarEntity)
}

@Dao
interface ShelfDao {
    @Query("SELECT * FROM owned_items ORDER BY purchasedAt ASC")
    fun observeOwned(): Flow<List<OwnedItemEntity>>

    @Query("SELECT * FROM owned_items ORDER BY purchasedAt ASC")
    suspend fun getOwned(): List<OwnedItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: OwnedItemEntity)

    @Query("UPDATE owned_items SET tier = :tier, slot = :slot WHERE itemId = :itemId")
    suspend fun move(itemId: String, tier: Int, slot: Int)
}

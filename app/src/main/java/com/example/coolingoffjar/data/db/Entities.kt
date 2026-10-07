package com.example.coolingoffjar.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantStatus

// No price, amount or currency column anywhere, by design.
@Entity(tableName = "wants")
data class WantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val unlockAt: Long,
    val status: WantStatus,
    val decidedAt: Long?,
)

@Entity(tableName = "jars")
data class JarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filledCount: Int,
    val completedAt: Long?,
    val freebieUsed: Boolean,
    val freebieUsedAt: Long?,
)

fun WantEntity.toDomain() = Want(id, name, createdAt, unlockAt, status, decidedAt)
fun Want.toEntity() = WantEntity(id, name, createdAt, unlockAt, status, decidedAt)
fun JarEntity.toDomain() = Jar(id, filledCount, completedAt, freebieUsed, freebieUsedAt)
fun Jar.toEntity() = JarEntity(id, filledCount, completedAt, freebieUsed, freebieUsedAt)

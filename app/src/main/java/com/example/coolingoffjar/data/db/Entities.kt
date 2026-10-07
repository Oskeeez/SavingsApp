package com.example.coolingoffjar.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.coolingoffjar.domain.Jar
import com.example.coolingoffjar.domain.OwnedItem
import com.example.coolingoffjar.domain.RoomText
import com.example.coolingoffjar.domain.Want
import com.example.coolingoffjar.domain.WantIcons
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
    val iconKey: String,
)

@Entity(tableName = "jars")
data class JarEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filledCount: Int,
    val completedAt: Long?,
    val freebieUsed: Boolean,
    val freebieUsedAt: Long?,
)

/** A shelf item the user has bought, and the slot it stands in. */
@Entity(tableName = "owned_items")
data class OwnedItemEntity(
    @PrimaryKey val itemId: String,
    val tier: Int,
    val slot: Int,
    val purchasedAt: Long,
    /** Free position on the wall (scene fractions), for notes. Null for everything else. */
    val posX: Float? = null,
    val posY: Float? = null,
    /** Put away in a storage box. */
    val stored: Boolean = false,
)

/** The heading on the wall (one row, id 1). */
@Entity(tableName = "room_text")
data class RoomTextEntity(
    @PrimaryKey val id: Int = 1,
    val title: String,
    val body: String,
    val posX: Float?,
    val posY: Float?,
    val visible: Boolean,
)

fun RoomTextEntity.toDomain() = RoomText(title, body, posX, posY, visible)
fun RoomText.toEntity() = RoomTextEntity(1, title, body, x, y, visible)

/** Which wall, floor and shelf the user has chosen: one row per kind (the category name). */
@Entity(tableName = "decor_choice")
data class DecorChoiceEntity(
    @PrimaryKey val kind: String,
    val itemId: String,
)

fun OwnedItemEntity.toDomain() = OwnedItem(itemId, tier, slot, purchasedAt, posX, posY, stored)
fun OwnedItem.toEntity() = OwnedItemEntity(itemId, tier, slot, purchasedAt, x, y, stored)

fun WantEntity.toDomain() = Want(id, name, createdAt, unlockAt, status, decidedAt, WantIcons.normalize(iconKey))
fun Want.toEntity() = WantEntity(id, name, createdAt, unlockAt, status, decidedAt, iconKey)
fun JarEntity.toDomain() = Jar(id, filledCount, completedAt, freebieUsed, freebieUsedAt)
fun Jar.toEntity() = JarEntity(id, filledCount, completedAt, freebieUsed, freebieUsedAt)

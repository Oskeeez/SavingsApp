package com.example.coolingoffjar.domain

import kotlin.math.max

/** What the camera zooms in on: a point in the scene and the object's height there (scene px). */
data class ZoomTarget(val centerX: Float, val centerY: Float, val height: Float)

/**
 * The camera over the scene. At rest it shows the whole picture scaled to cover the screen (so the sides or the top and
 * bottom may be cropped, but the edge of the picture is never shown). Zooming moves smoothly to a close-up of an
 * object; the camera is clamped at every step so the picture always covers the screen, even close to the walls.
 * Pure maths, so it can be tested.
 */
object SceneCamera {
    /** Where on the screen a close-up object ends up, and how tall it is, as fractions of the screen height. */
    const val TARGET_Y = 0.24f
    const val TARGET_HEIGHT = 0.30f
    const val MAX_ZOOM_FACTOR = 6f

    /** [scale]: screen px per scene px. ([centerX], [centerY]): the scene point shown at the middle of the screen. */
    data class Frame(val scale: Float, val centerX: Float, val centerY: Float)

    /** Screen px per scene px so the scene covers the whole screen. */
    fun coverScale(screenW: Float, screenH: Float): Float =
        max(screenW / ShelfGeometry.SCENE_WIDTH, screenH / ShelfGeometry.SCENE_HEIGHT)

    private fun clampAxis(c: Float, sceneSize: Float, halfScreenInScene: Float): Float =
        if (halfScreenInScene * 2f >= sceneSize) sceneSize / 2f else c.coerceIn(halfScreenInScene, sceneSize - halfScreenInScene)

    /** Keeps the screen entirely inside the picture. */
    fun clamp(frame: Frame, screenW: Float, screenH: Float): Frame = Frame(
        frame.scale,
        clampAxis(frame.centerX, ShelfGeometry.SCENE_WIDTH.toFloat(), screenW / 2f / frame.scale),
        clampAxis(frame.centerY, ShelfGeometry.SCENE_HEIGHT.toFloat(), screenH / 2f / frame.scale),
    )

    /** The camera [progress] of the way (0 = whole room, 1 = close up on [target]). */
    fun frame(progress: Float, target: ZoomTarget?, screenW: Float, screenH: Float): Frame {
        val k0 = coverScale(screenW, screenH)
        val rest = clamp(Frame(k0, ShelfGeometry.SCENE_WIDTH / 2f, ShelfGeometry.SCENE_HEIGHT / 2f), screenW, screenH)
        if (target == null || progress <= 0f) return rest
        val p = progress.coerceIn(0f, 1f)
        val kT = (TARGET_HEIGHT * screenH / max(target.height, 1f)).coerceIn(k0, k0 * MAX_ZOOM_FACTOR)
        // The scene point at the middle of the screen that puts the object at TARGET_Y.
        val close = Frame(kT, target.centerX, target.centerY + (0.5f - TARGET_Y) * screenH / kT)
        val k = rest.scale + (close.scale - rest.scale) * p
        return clamp(
            Frame(
                k,
                rest.centerX + (close.centerX - rest.centerX) * p,
                rest.centerY + (close.centerY - rest.centerY) * p,
            ),
            screenW, screenH,
        )
    }

    /** The target for an item standing or hanging where [item] is (already resolved), or null if it is not placed. */
    fun targetFor(item: OwnedItem): ZoomTarget? {
        val entry = ShelfCatalog.find(item.itemId) ?: return null
        return when (entry.surface) {
            ShelfSurface.SHELF -> {
                val h = ShelfLayout.heightFor(entry, item.tier)
                ZoomTarget(ShelfGeometry.slotCenterX(item.tier, item.slot), ShelfGeometry.standLine(item.tier) - h / 2f, h)
            }
            ShelfSurface.WALL -> {
                val h = ShelfLayout.heightFor(entry, 0)
                ZoomTarget((item.x ?: 0.5f) * ShelfGeometry.SCENE_WIDTH, (item.y ?: 0.2f) * ShelfGeometry.SCENE_HEIGHT, h)
            }
            ShelfSurface.DECOR -> null
        }
    }

}

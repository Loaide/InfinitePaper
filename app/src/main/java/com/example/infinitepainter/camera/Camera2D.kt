
package com.example.infinitepainter.camera

/**
 * Immutable 2D camera.
 *
 * World and screen Y both increase downward.
 * zoom = screen pixels per world unit.
 *
 * No Android or OpenGL dependencies.
 */
data class Camera2D(
    val center: Vec2,
    val zoom: Double,
    val viewportWidth: Int,
    val viewportHeight: Int
) {
    init {
        require(zoom.isFinite() && zoom > 0.0) {
            "Zoom must be finite and positive"
        }
        require(viewportWidth > 0 && viewportHeight > 0) {
            "Viewport dimensions must be positive"
        }
    }

    private val halfWidth: Double
        get() = viewportWidth.toDouble() / 2.0

    private val halfHeight: Double
        get() = viewportHeight.toDouble() / 2.0

    fun worldToScreen(world: Vec2): Vec2 {
        return Vec2(
            checked(halfWidth + (world.x - center.x) * zoom),
            checked(halfHeight + (world.y - center.y) * zoom)
        )
    }

    fun screenToWorld(screen: Vec2): Vec2 {
        return Vec2(
            checked(center.x + (screen.x - halfWidth) / zoom),
            checked(center.y + (screen.y - halfHeight) / zoom)
        )
    }

    /**
     * Move the canvas in the same direction as the drag.
     * Positive screen dx moves visible content right.
     */
    fun panBy(screenDx: Double, screenDy: Double): Camera2D {
        require(screenDx.isFinite() && screenDy.isFinite()) {
            "Pan displacement must be finite"
        }

        return copy(
            center = Vec2(
                checked(center.x - screenDx / zoom),
                checked(center.y - screenDy / zoom)
            )
        )
    }

    /**
     * Zoom around a screen-space focal point.
     *
     * Uses a relative-offset formula to avoid first computing
     * an unnecessarily large absolute world-space focal point.
     */
    fun zoomAt(factor: Double, focalPoint: Vec2): Camera2D {
        require(factor.isFinite() && factor > 0.0) {
            "Zoom factor must be finite and positive"
        }

        val newZoom = checked(zoom * factor)
        require(newZoom > 0.0) {
            "Resulting zoom must be positive"
        }

        val offsetX = focalPoint.x - halfWidth
        val offsetY = focalPoint.y - halfHeight

        val inverseDifference = checked(1.0 / zoom - 1.0 / newZoom)

        return copy(
            center = Vec2(
                checked(center.x + offsetX * inverseDifference),
                checked(center.y + offsetY * inverseDifference)
            ),
            zoom = newZoom
        )
    }

    /**
     * Change viewport dimensions without moving the camera center
     * or changing zoom.
     */
    fun resized(width: Int, height: Int): Camera2D {
        return copy(
            viewportWidth = width,
            viewportHeight = height
        )
    }

    /**
     * World position expressed relative to the camera center.
     * The renderer may later convert these values to Float,
     * after checking that they fit its precision budget.
     */
    fun worldToCameraRelative(world: Vec2): Vec2 {
        return Vec2(
            checked(world.x - center.x),
            checked(world.y - center.y)
        )
    }

    private fun checked(value: Double): Double {
        require(value.isFinite()) {
            "Camera operation exceeded finite Double range"
        }
        return value
    }

    companion object {
        fun initial(width: Int, height: Int): Camera2D {
            return Camera2D(
                center = Vec2(0.0, 0.0),
                zoom = 1.0,
                viewportWidth = width,
                viewportHeight = height
            )
        }
    }
}

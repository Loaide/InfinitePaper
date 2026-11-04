
package com.example.infinitepainter.camera

import kotlin.math.hypot

/**
 * A two-finger gesture sample in screen pixels.
 */
data class PinchSample(
    val first: Vec2,
    val second: Vec2
) {
    val midpoint: Vec2
        get() = Vec2(
            first.x / 2.0 + second.x / 2.0,
            first.y / 2.0 + second.y / 2.0
        )

    val distance: Double
        get() = hypot(
            second.x - first.x,
            second.y - first.y
        )
}

/**
 * Apply translation and zoom together.
 *
 * The world point under the previous midpoint follows
 * the gesture to its new midpoint.
 */
fun Camera2D.applyPinch(
    previous: PinchSample,
    current: PinchSample
): Camera2D {
    val oldDistance = previous.distance
    val newDistance = current.distance

    // Ignore degenerate pinches rather than dividing by zero.
    if (oldDistance < 1.0 || newDistance < 1.0) {
        return this
    }

    val oldMid = previous.midpoint
    val newMid = current.midpoint

    val moved = panBy(
        newMid.x - oldMid.x,
        newMid.y - oldMid.y
    )

    return moved.zoomAt(
        newDistance / oldDistance,
        newMid
    )
}

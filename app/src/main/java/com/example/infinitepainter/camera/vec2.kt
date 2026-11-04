
package com.example.infinitepainter.camera

/**
 * A finite 2D coordinate or displacement.
 * Units depend on the operation: world units or screen pixels.
 */
data class Vec2(val x: Double, val y: Double) {
    init {
        require(x.isFinite() && y.isFinite()) {
            "Coordinates must be finite"
        }
    }
}

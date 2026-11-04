
package com.example.infinitepainter.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraGestureTest {

    @Test
    fun pinchTranslationMovesAnchorWithFingers() {
        val camera = Camera2D.initial(1000, 800)

        val before = PinchSample(
            Vec2(300.0, 400.0),
            Vec2(500.0, 400.0)
        )
        val after = PinchSample(
            Vec2(350.0, 430.0),
            Vec2(550.0, 430.0)
        )

        val anchor = camera.screenToWorld(before.midpoint)
        val result = camera.applyPinch(before, after)
        val screen = result.worldToScreen(anchor)

        assertEquals(450.0, screen.x, 1e-7)
        assertEquals(430.0, screen.y, 1e-7)
        assertEquals(1.0, result.zoom, 1e-12)
    }

    @Test
    fun pinchZoomKeepsWorldAnchorUnderNewMidpoint() {
        val camera = Camera2D(
            Vec2(100.0, -50.0), 2.0, 1000, 800
        )

        val before = PinchSample(
            Vec2(300.0, 400.0),
            Vec2(500.0, 400.0)
        )
        val after = PinchSample(
            Vec2(250.0, 420.0),
            Vec2(650.0, 420.0)
        )

        val anchor = camera.screenToWorld(before.midpoint)
        val result = camera.applyPinch(before, after)
        val screen = result.worldToScreen(anchor)

        assertEquals(450.0, screen.x, 1e-7)
        assertEquals(420.0, screen.y, 1e-7)
        assertEquals(4.0, result.zoom, 1e-12)
    }

    @Test
    fun degeneratePinchDoesNotChangeCamera() {
        val camera = Camera2D.initial(1000, 800)
        val before = PinchSample(
            Vec2(100.0, 100.0),
            Vec2(100.0, 100.0)
        )
        val after = PinchSample(
            Vec2(90.0, 100.0),
            Vec2(110.0, 100.0)
        )

        assertEquals(camera, camera.applyPinch(before, after))
    }
}

package com.example.infinitepainter.camera

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class Camera2DTest {

    private val pixelTolerance = 1e-7
    private val worldTolerance = 1e-9

    private fun assertNear(
        expected: Double,
        actual: Double,
        tolerance: Double
    ) {
        assertTrue(
            "Expected $expected, got $actual (tolerance $tolerance)",
            abs(expected - actual) <= tolerance
        )
    }

    private fun assertPointNear(
        expected: Vec2,
        actual: Vec2,
        tolerance: Double
    ) {
        assertNear(expected.x, actual.x, tolerance)
        assertNear(expected.y, actual.y, tolerance)
    }

    private fun camera() = Camera2D.initial(2880, 1800)

    @Test
    fun cameraCenterMapsToViewportCenter() {
        assertEquals(
            Vec2(1440.0, 900.0),
            camera().worldToScreen(Vec2(0.0, 0.0))
        )
    }

    @Test
    fun worldScreenRoundTrip() {
        val cam = Camera2D(
            center = Vec2(1234.5, -987.25),
            zoom = 3.75,
            viewportWidth = 2880,
            viewportHeight = 1800
        )

        val points = listOf(
            Vec2(1234.5, -987.25),
            Vec2(1300.0, -900.0),
            Vec2(1000.0, -1100.0),
            Vec2(0.0, 0.0)
        )

        for (point in points) {
            val result = cam.screenToWorld(
                cam.worldToScreen(point)
            )
            assertPointNear(point, result, worldTolerance)
        }
    }

    @Test
    fun panMovesCanvasInDragDirection() {
        val original = camera()
        val moved = original.panBy(120.0, -80.0)

        val before = original.worldToScreen(Vec2(0.0, 0.0))
        val after = moved.worldToScreen(Vec2(0.0, 0.0))

        assertNear(before.x + 120.0, after.x, pixelTolerance)
        assertNear(before.y - 80.0, after.y, pixelTolerance)
    }

    @Test
    fun zoomPreservesOffCenterFocalPoint() {
        val original = Camera2D(
            Vec2(125.0, -40.0), 2.0, 2880, 1800
        )
        val focal = Vec2(250.0, 300.0)

        val worldBefore = original.screenToWorld(focal)
        val zoomed = original.zoomAt(2.5, focal)
        val worldAfter = zoomed.screenToWorld(focal)

        assertPointNear(worldBefore, worldAfter, worldTolerance)
        assertPointNear(
            focal,
            zoomed.worldToScreen(worldBefore),
            pixelTolerance
        )
    }

    @Test
    fun zoomAtViewportCenterDoesNotMoveCameraCenter() {
        val original = Camera2D(
            Vec2(100.0, 200.0), 1.0, 2880, 1800
        )
        val result = original.zoomAt(
            4.0, Vec2(1440.0, 900.0)
        )

        assertEquals(original.center, result.center)
        assertEquals(4.0, result.zoom, 0.0)
    }

    @Test
    fun repeatedZoomCyclesHaveBoundedDrift() {
        var cam = Camera2D(
            Vec2(100.0, -200.0), 1.5, 2880, 1800
        )
        val original = cam
        val focal = Vec2(800.0, 500.0)

        repeat(1000) {
            cam = cam.zoomAt(1.01, focal)
            cam = cam.zoomAt(1.0 / 1.01, focal)
        }

        assertPointNear(
            original.center, cam.center, 1e-6
        )
        assertNear(original.zoom, cam.zoom, 1e-9)
    }

    @Test
    fun viewportResizeKeepsCameraCenterAndZoom() {
        val original = Camera2D(
            Vec2(20.0, 30.0), 2.0, 2880, 1800
        )
        val resized = original.resized(1800, 2880)

        assertEquals(original.center, resized.center)
        assertEquals(original.zoom, resized.zoom, 0.0)
        assertEquals(1800, resized.viewportWidth)
        assertEquals(2880, resized.viewportHeight)

        assertEquals(
            Vec2(900.0, 1440.0),
            resized.worldToScreen(original.center)
        )
    }

    @Test
    fun largeWorldOffsetRetainsSmallVisibleDisplacements() {
        val cam = Camera2D(
            Vec2(1e9, -1e9), 4.0, 2880, 1800
        )
        val centerScreen = cam.worldToScreen(cam.center)
        val nearby = cam.worldToScreen(
            Vec2(1e9 + 0.25, -1e9 + 0.5)
        )

        assertNear(
            centerScreen.x + 1.0, nearby.x, pixelTolerance
        )
        assertNear(
            centerScreen.y + 2.0, nearby.y, pixelTolerance
        )
    }

    @Test
    fun cameraRelativeCoordinatesRemainSmall() {
        val cam = Camera2D(
            Vec2(1e9, 1e9), 1.0, 2880, 1800
        )

        assertEquals(
            Vec2(10.0, -20.0),
            cam.worldToCameraRelative(
                Vec2(1e9 + 10.0, 1e9 - 20.0)
            )
        )
    }

    @Test
    fun randomizedFocalZoomInvariance() {
        val random = Random(20261104)

        repeat(1000) {
            val cam = Camera2D(
                center = Vec2(
                    random.nextDouble(-10000.0, 10000.0),
                    random.nextDouble(-10000.0, 10000.0)
                ),
                zoom = random.nextDouble(0.1, 100.0),
                viewportWidth = 2880,
                viewportHeight = 1800
            )

            val focal = Vec2(
                random.nextDouble(0.0, 2880.0),
                random.nextDouble(0.0, 1800.0)
            )

            val anchor = cam.screenToWorld(focal)
            val changed = cam.zoomAt(
                random.nextDouble(0.5, 2.0), focal
            )

            assertPointNear(
                focal,
                changed.worldToScreen(anchor),
                1e-7
            )
        }
    }

    @Test
    fun invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Camera2D.initial(0, 1800)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Camera2D(
                Vec2(0.0, 0.0), 0.0, 2880, 1800
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            camera().zoomAt(-2.0, Vec2(100.0, 100.0))
        }
        assertThrows(IllegalArgumentException::class.java) {
            camera().panBy(Double.NaN, 0.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Camera2D(
                Vec2(0.0, 0.0),
                2.0,
                2880,
                1800
            ).zoomAt(
                Double.MAX_VALUE,
                Vec2(100.0, 100.0)
            )
        }
    }

    @Test
    fun originalCameraRemainsUnchanged() {
        val original = camera()
        val changed = original
            .panBy(100.0, 200.0)
            .zoomAt(2.0, Vec2(500.0, 400.0))

        assertEquals(Vec2(0.0, 0.0), original.center)
        assertEquals(1.0, original.zoom, 0.0)
        assertNotEquals(original, changed)
    }
}

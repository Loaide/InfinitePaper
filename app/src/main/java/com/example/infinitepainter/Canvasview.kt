
package com.example.infinitepainter

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.infinitepainter.camera.Camera2D
import com.example.infinitepainter.camera.PinchSample
import com.example.infinitepainter.camera.Vec2
import com.example.infinitepainter.camera.applyPinch

class CanvasView(context: Context) : GLSurfaceView(context) {

    private val canvasRenderer = CanvasRenderer()

    // Camera state is owned exclusively by the UI thread.
    private var camera = Camera2D.initial(1, 1)

    private var trackedIds = emptyList<Int>()
    private var previousPoints = emptyList<Vec2>()

    init {
        setEGLContextClientVersion(3)
        setRenderer(canvasRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int
    ) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            camera = camera.resized(w, h)
            resetGesture()
            publishCamera()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP -> {
                // Pointer membership changed; establish a new
                // baseline on the next MOVE.
                resetGesture()
                return true
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                resetGesture()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                processMove(event)
                return true
            }
        }

        return true
    }

    private fun processMove(event: MotionEvent) {
        // M0: fingers navigate; stylus does not navigate.
        val fingerIndices = (0 until event.pointerCount)
            .filter {
                event.getToolType(it) ==
                        MotionEvent.TOOL_TYPE_FINGER
            }

        // Do not navigate during mixed stylus/finger input.
        if (fingerIndices.size != event.pointerCount) {
            resetGesture()
            return
        }

        if (fingerIndices.size !in 1..2) {
            resetGesture()
            return
        }

        val ids = fingerIndices.map(event::getPointerId)
        val points = fingerIndices.map {
            Vec2(
                event.getX(it).toDouble(),
                event.getY(it).toDouble()
            )
        }

        if (trackedIds != ids ||
            previousPoints.size != points.size
        ) {
            trackedIds = ids
            previousPoints = points
            return
        }

        val nextCamera = try {
            when (points.size) {
                1 -> {
                    val before = previousPoints[0]
                    val now = points[0]

                    camera.panBy(
                        now.x - before.x,
                        now.y - before.y
                    )
                }

                2 -> {
                    camera.applyPinch(
                        PinchSample(
                            previousPoints[0],
                            previousPoints[1]
                        ),
                        PinchSample(
                            points[0],
                            points[1]
                        )
                    )
                }

                else -> camera
            }
        } catch (_: IllegalArgumentException) {
            // An invalid numerical gesture must not corrupt
            // the last valid camera state.
            resetGesture()
            return
        }

        previousPoints = points

        if (nextCamera != camera) {
            camera = nextCamera
            publishCamera()
        }
    }

    private fun publishCamera() {
        val snapshot = camera

        // The renderer's camera is only changed on the GL thread.
        queueEvent {
            canvasRenderer.setCamera(snapshot)
        }

        requestRender()
    }

    private fun resetGesture() {
        trackedIds = emptyList()
        previousPoints = emptyList()
    }
}

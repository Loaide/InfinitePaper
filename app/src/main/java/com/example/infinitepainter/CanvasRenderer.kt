
package com.example.infinitepainter

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import com.example.infinitepainter.camera.Camera2D
import com.example.infinitepainter.camera.Vec2
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CanvasRenderer : GLSurfaceView.Renderer {

    // GL thread owns this reference.
    private var camera = Camera2D.initial(1, 1)
    private var program = 0
    private var positionLocation = -1
    private var colorLocation = -1

    fun setCamera(snapshot: Camera2D) {
        // Must only be called through GLSurfaceView.queueEvent.
        camera = snapshot
    }

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        Log.i(TAG, "Vendor: ${GLES20.glGetString(GLES20.GL_VENDOR)}")
        Log.i(TAG, "Renderer: ${GLES20.glGetString(GLES20.GL_RENDERER)}")
        Log.i(TAG, "Version: ${GLES20.glGetString(GLES20.GL_VERSION)}")
        Log.i(TAG, "GLSL: ${GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION)}")

        GLES30.glClearColor(0.96f, 0.96f, 0.96f, 1f)

        val vertex = compileShader(
            GLES30.GL_VERTEX_SHADER,
            """
            #version 300 es
            layout(location = 0) in vec2 aPosition;
            void main() {
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
            """.trimIndent()
        )

        val fragment = compileShader(
            GLES30.GL_FRAGMENT_SHADER,
            """
            #version 300 es
            precision mediump float;
            uniform vec4 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = uColor;
            }
            """.trimIndent()
        )

        program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertex)
        GLES30.glAttachShader(program, fragment)
        GLES30.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(
            program, GLES30.GL_LINK_STATUS, linkStatus, 0
        )
        check(linkStatus[0] == GLES30.GL_TRUE) {
            "GL program link failed: ${
                GLES30.glGetProgramInfoLog(program)
            }"
        }

        GLES30.glDeleteShader(vertex)
        GLES30.glDeleteShader(fragment)

        positionLocation = 0
        colorLocation = GLES30.glGetUniformLocation(
            program, "uColor"
        )
        check(colorLocation >= 0) {
            "uColor uniform not found"
        }
    }

    override fun onSurfaceChanged(
        gl: GL10?,
        width: Int,
        height: Int
    ) {
        GLES30.glViewport(0, 0, width, height)
        Log.i(TAG, "Canvas size: $width x $height")
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        if (program == 0) return

        GLES30.glUseProgram(program)

        drawGrid(camera)
        drawAxes(camera)
    }

    private fun drawGrid(cam: Camera2D) {
        val step = gridStep(cam.zoom) ?: return
        val width = cam.viewportWidth.toDouble()
        val height = cam.viewportHeight.toDouble()

        // Use a camera-relative grid index to avoid
        // multiplying enormous absolute world coordinates.
        val centerGridX = floor(cam.center.x / step)
        val centerGridY = floor(cam.center.y / step)

        val lines = ArrayList<Float>(512)

        for (offset in -50..50) {
            val worldX = (centerGridX + offset) * step
            if (worldX.isFinite()) {
                val x = (worldX - cam.center.x) * cam.zoom +
                        width / 2.0
                if (x.isFinite() && x in 0.0..width) {
                    addLine(lines, x, 0.0, x, height, cam)
                }
            }

            val worldY = (centerGridY + offset) * step
            if (worldY.isFinite()) {
                val y = (worldY - cam.center.y) * cam.zoom +
                        height / 2.0
                if (y.isFinite() && y in 0.0..height) {
                    addLine(lines, 0.0, y, width, y, cam)
                }
            }
        }

        drawLines(lines, 0.83f, 0.85f, 0.87f)
    }

    private fun drawAxes(cam: Camera2D) {
        val width = cam.viewportWidth.toDouble()
        val height = cam.viewportHeight.toDouble()
        val lines = ArrayList<Float>(8)

        val originX = (-cam.center.x) * cam.zoom + width / 2.0
        val originY = (-cam.center.y) * cam.zoom + height / 2.0

        if (originX.isFinite() && originX in 0.0..width) {
            addLine(lines, originX, 0.0, originX, height, cam)
        }
        if (originY.isFinite() && originY in 0.0..height) {
            addLine(lines, 0.0, originY, width, originY, cam)
        }

        drawLines(lines, 0.22f, 0.43f, 0.68f)
    }

    private fun gridStep(zoom: Double): Double? {
        // Target approximately 100 pixels between grid lines.
        val desired = 100.0 / zoom
        if (!desired.isFinite() || desired <= 0.0) return null

        val base = 10.0.pow(floor(log10(desired)))
        if (!base.isFinite() || base <= 0.0) return null

        val multiplier = when {
            base * 2.0 >= desired -> 2.0
            base * 5.0 >= desired -> 5.0
            else -> 10.0
        }
        val result = base * multiplier
        return result.takeIf { it.isFinite() && it > 0.0 }
    }

    private fun addLine(
        output: MutableList<Float>,
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
        cam: Camera2D
    ) {
        val w = cam.viewportWidth.toDouble()
        val h = cam.viewportHeight.toDouble()

        val values = doubleArrayOf(
            2.0 * x1 / w - 1.0,
            1.0 - 2.0 * y1 / h,
            2.0 * x2 / w - 1.0,
            1.0 - 2.0 * y2 / h
        )

        if (values.all { it.isFinite() && it in -1.01..1.01 }) {
            values.forEach { output.add(it.toFloat()) }
        }
    }

    private fun drawLines(
        vertices: List<Float>,
        r: Float,
        g: Float,
        b: Float
    ) {
        if (vertices.isEmpty()) return

        val buffer = ByteBuffer
            .allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        vertices.forEach { buffer.put(it) }
        buffer.position(0)

        GLES30.glUniform4f(colorLocation, r, g, b, 1f)
        GLES30.glEnableVertexAttribArray(positionLocation)
        GLES30.glVertexAttribPointer(
            positionLocation,
            2,
            GLES30.GL_FLOAT,
            false,
            0,
            buffer
        )
        GLES30.glDrawArrays(
            GLES30.GL_LINES,
            0,
            vertices.size / 2
        )
        GLES30.glDisableVertexAttribArray(positionLocation)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        check(shader != 0) { "Failed to create GL shader" }

        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)

        val status = IntArray(1)
        GLES30.glGetShaderiv(
            shader, GLES30.GL_COMPILE_STATUS, status, 0
        )
        check(status[0] == GLES30.GL_TRUE) {
            val log = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            "GL shader compilation failed: $log"
        }
        return shader
    }

    companion object {
        private const val TAG = "InfinitePaperGL"
    }
}

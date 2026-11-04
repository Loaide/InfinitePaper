package com.example.infinitepainter

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CanvasRenderer : GLSurfaceView.Renderer {

    override fun onSurfaceCreated(
        gl: GL10?,
        config: EGLConfig?
    ) {
        Log.i(TAG, "Vendor: ${GLES20.glGetString(GLES20.GL_VENDOR)}")
        Log.i(TAG, "Renderer: ${GLES20.glGetString(GLES20.GL_RENDERER)}")
        Log.i(TAG, "Version: ${GLES20.glGetString(GLES20.GL_VERSION)}")
        Log.i(TAG, "GLSL: ${GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION)}")

        // Set the canvas background to a soft white.
        GLES30.glClearColor(0.96f, 0.96f, 0.96f, 1.0f)
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
    }

    companion object {
        private const val TAG = "InfinitePaperGL"
    }
}
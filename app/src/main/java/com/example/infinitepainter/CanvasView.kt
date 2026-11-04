package com.example.infinitepainter

import android.content.Context
import android.opengl.GLSurfaceView

class CanvasView(context: Context) : GLSurfaceView(context) {

    init {
        // Request an OpenGL ES 3.0 context.
        setEGLContextClientVersion(3)

        // Connect the renderer to the GPU surface.
        setRenderer(CanvasRenderer())

        // Draw only when explicitly requested.
        renderMode = RENDERMODE_WHEN_DIRTY
    }
}
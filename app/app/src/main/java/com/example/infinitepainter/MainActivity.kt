package com.example.infinitepainter

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var canvasView: CanvasView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check whether the device supports OpenGL ES 3.0.
        val activityManager = getSystemService(ACTIVITY_SERVICE)
                as android.app.ActivityManager

        val version = activityManager.deviceConfigurationInfo.reqGlEsVersion

        if (version < 0x30000) {
            setContentView(
                TextView(this).apply {
                    text = "OpenGL ES 3.0 is not supported on this device."
                }
            )
            return
        }

        canvasView = CanvasView(this)
        setContentView(canvasView)
    }

    override fun onResume() {
        super.onResume()
        if (::canvasView.isInitialized) canvasView.onResume()
    }

    override fun onPause() {
        if (::canvasView.isInitialized) canvasView.onPause()
        super.onPause()
    }
}
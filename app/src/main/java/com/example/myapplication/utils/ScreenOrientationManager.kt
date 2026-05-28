package com.example.myapplication.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ForcePortraitOrientation() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(Unit) {
        // Save the current orientation setup
        val originalOrientation = activity.requestedOrientation

        // Force portrait mode
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onDispose {
            // Restore original orientation when dialog closes
            activity.requestedOrientation = originalOrientation
        }

    }
}
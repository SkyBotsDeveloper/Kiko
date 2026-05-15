package com.skybots.kiko.orbit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class FloatingOrbitPermissionHelper(
    private val context: Context,
) {
    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun overlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
}

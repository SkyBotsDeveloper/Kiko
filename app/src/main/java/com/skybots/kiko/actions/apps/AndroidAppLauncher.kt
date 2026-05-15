package com.skybots.kiko.actions.apps

import android.content.Context
import android.content.Intent

class AndroidAppLauncher(context: Context) : AppLauncher {
    private val appContext = context.applicationContext

    override fun launch(packageName: String): Boolean {
        val launchIntent = appContext.packageManager
            .getLaunchIntentForPackage(packageName)
            ?: return false

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(launchIntent)
        }.isSuccess
    }
}

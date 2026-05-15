package com.skybots.kiko.actions.apps

interface AppLauncher {
    fun launch(packageName: String): Boolean
}

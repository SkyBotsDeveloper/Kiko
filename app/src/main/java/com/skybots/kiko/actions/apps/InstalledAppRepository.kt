package com.skybots.kiko.actions.apps

interface InstalledAppRepository {
    fun getLaunchableApps(): List<InstalledApp>

    fun refresh()
}

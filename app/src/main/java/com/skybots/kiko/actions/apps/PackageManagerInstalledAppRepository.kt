package com.skybots.kiko.actions.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class PackageManagerInstalledAppRepository(context: Context) : InstalledAppRepository {
    private val appContext = context.applicationContext
    private var cachedApps: List<InstalledApp>? = null

    override fun getLaunchableApps(): List<InstalledApp> =
        cachedApps ?: loadLaunchableApps().also { apps ->
            cachedApps = apps
        }

    override fun refresh() {
        cachedApps = loadLaunchableApps()
    }

    private fun loadLaunchableApps(): List<InstalledApp> {
        val packageManager = appContext.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolveInfos
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null

                InstalledApp(
                    label = label,
                    packageName = packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

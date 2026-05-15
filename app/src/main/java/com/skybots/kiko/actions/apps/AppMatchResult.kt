package com.skybots.kiko.actions.apps

sealed interface AppMatchResult {
    data class Single(val app: InstalledApp) : AppMatchResult
    data class Multiple(val candidates: List<InstalledApp>) : AppMatchResult
    data object None : AppMatchResult
}

package com.skybots.kiko.permissions

data class PermissionStatus(
    val permission: KikoPermission,
    val isGranted: Boolean,
) {
    val stateLabel: String
        get() = if (isGranted) "Granted" else "Not granted"
}

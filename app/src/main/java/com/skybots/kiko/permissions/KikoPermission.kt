package com.skybots.kiko.permissions

import android.Manifest

enum class KikoPermission(
    val androidPermission: String,
    val displayName: String,
) {
    RECORD_AUDIO(
        androidPermission = Manifest.permission.RECORD_AUDIO,
        displayName = "Microphone",
    ),
    READ_CONTACTS(
        androidPermission = Manifest.permission.READ_CONTACTS,
        displayName = "Contacts",
    ),
    CALL_PHONE(
        androidPermission = Manifest.permission.CALL_PHONE,
        displayName = "Phone calls",
    ),
    CAMERA(
        androidPermission = Manifest.permission.CAMERA,
        displayName = "Camera / flashlight",
    ),
    POST_NOTIFICATIONS(
        androidPermission = Manifest.permission.POST_NOTIFICATIONS,
        displayName = "Notifications",
    ),
}

package com.skybots.kiko.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class PermissionManager(context: Context) {
    private val appContext = context.applicationContext

    fun hasRecordAudioPermission(): Boolean = hasPermission(KikoPermission.RECORD_AUDIO)

    fun hasReadContactsPermission(): Boolean = hasPermission(KikoPermission.READ_CONTACTS)

    fun hasCallPhonePermission(): Boolean = hasPermission(KikoPermission.CALL_PHONE)

    fun hasCameraPermission(): Boolean = hasPermission(KikoPermission.CAMERA)

    fun getPermissionStatuses(): List<PermissionStatus> =
        KikoPermission.entries.map { permission ->
            PermissionStatus(
                permission = permission,
                isGranted = hasPermission(permission),
            )
        }

    private fun hasPermission(permission: KikoPermission): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            permission.androidPermission,
        ) == PackageManager.PERMISSION_GRANTED
}

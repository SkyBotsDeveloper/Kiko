package com.skybots.kiko.permissions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionManager(context: Context) : PermissionChecker {
    private val appContext = context.applicationContext

    fun hasRecordAudioPermission(): Boolean = hasPermission(KikoPermission.RECORD_AUDIO)

    override fun hasReadContactsPermission(): Boolean = hasPermission(KikoPermission.READ_CONTACTS)

    override fun hasCallPhonePermission(): Boolean = hasPermission(KikoPermission.CALL_PHONE)

    override fun hasPostNotificationsPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(KikoPermission.POST_NOTIFICATIONS)

    fun hasCameraPermission(): Boolean = hasPermission(KikoPermission.CAMERA)

    fun getPermissionStatuses(): List<PermissionStatus> =
        KikoPermission.entries.map { permission ->
            PermissionStatus(
                permission = permission,
                isGranted = if (permission == KikoPermission.POST_NOTIFICATIONS) {
                    hasPostNotificationsPermission()
                } else {
                    hasPermission(permission)
                },
            )
        }

    private fun hasPermission(permission: KikoPermission): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            permission.androidPermission,
        ) == PackageManager.PERMISSION_GRANTED
}

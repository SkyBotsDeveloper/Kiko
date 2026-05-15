package com.skybots.kiko.permissions

interface PermissionChecker {
    fun hasReadContactsPermission(): Boolean

    fun hasCallPhonePermission(): Boolean

    fun hasPostNotificationsPermission(): Boolean
}

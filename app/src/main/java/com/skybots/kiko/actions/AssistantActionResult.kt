package com.skybots.kiko.actions

import com.skybots.kiko.permissions.KikoPermission

data class AssistantActionResult(
    val response: String,
    val requestedPermission: KikoPermission? = null,
)

package com.skybots.kiko.actions.contacts

interface PhoneActionLauncher {
    fun call(phoneNumber: String): Boolean

    fun dial(phoneNumber: String): Boolean
}

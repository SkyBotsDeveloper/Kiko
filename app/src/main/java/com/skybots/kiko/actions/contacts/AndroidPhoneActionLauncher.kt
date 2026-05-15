package com.skybots.kiko.actions.contacts

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidPhoneActionLauncher(context: Context) : PhoneActionLauncher {
    private val appContext = context.applicationContext

    @SuppressLint("MissingPermission")
    override fun call(phoneNumber: String): Boolean =
        startPhoneIntent(Intent.ACTION_CALL, phoneNumber)

    override fun dial(phoneNumber: String): Boolean =
        startPhoneIntent(Intent.ACTION_DIAL, phoneNumber)

    private fun startPhoneIntent(
        action: String,
        phoneNumber: String,
    ): Boolean {
        val intent = Intent(action).apply {
            data = Uri.parse("tel:$phoneNumber")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return runCatching {
            appContext.startActivity(intent)
        }.isSuccess
    }
}

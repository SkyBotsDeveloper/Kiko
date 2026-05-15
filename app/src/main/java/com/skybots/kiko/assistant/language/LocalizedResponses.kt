package com.skybots.kiko.assistant.language

import com.skybots.kiko.assistant.clarification.PendingActionType

object LocalizedResponses {
    fun openingApp(
        appName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$appName khol raha hoon."
            LanguageHint.HINDI -> "$appName खोल रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Opening $appName."
        }

    fun multipleApps(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe multiple apps mile. Kaunsa open karu?"
            LanguageHint.HINDI -> "मुझे multiple apps मिले। कौन सा खोलूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found multiple apps. Which one should I open?"
        }

    fun appNotFound(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ye app phone me nahi mila."
            LanguageHint.HINDI -> "मुझे यह app phone में नहीं मिला।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find that app on this phone."
        }

    fun appLaunchFailed(
        appName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$appName open nahi ho paya."
            LanguageHint.HINDI -> "$appName open नहीं हो पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't open $appName."
        }

    fun contactsPermission(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Contacts find karne ke liye mujhe contacts permission chahiye."
            LanguageHint.HINDI -> "Contacts ढूँढने के लिए मुझे contacts permission चाहिए।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I need contacts permission to find and call people."
        }

    fun callingContact(
        contactName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ko call kar raha hoon."
            LanguageHint.HINDI -> "$contactName को call कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Calling $contactName."
        }

    fun openingDialer(
        contactName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye dialer open kar raha hoon."
            LanguageHint.HINDI -> "$contactName के लिए dialer खोल रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Opening dialer for $contactName."
        }

    fun contactNotFound(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ye contact phone me nahi mila."
            LanguageHint.HINDI -> "मुझे यह contact phone में नहीं मिला।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find that contact on this phone."
        }

    fun contactHasNoNumber(
        contactName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone number nahi mila."
            LanguageHint.HINDI -> "$contactName के लिए phone number नहीं मिला।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find a phone number for $contactName."
        }

    fun callLaunchFailed(
        contactName: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone app open nahi ho paya."
            LanguageHint.HINDI -> "$contactName के लिए phone app open नहीं हो पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't open the phone app for $contactName."
        }

    fun multipleContacts(
        contactNames: List<String>,
        languageHint: LanguageHint,
    ): String {
        val names = contactNames.take(MAX_NAMES_IN_RESPONSE)
        val listText = joinNames(names, languageHint)
        return when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ${contactNames.size} contacts mile: $listText. Kisko call karu?"
            LanguageHint.HINDI -> "मुझे ${contactNames.size} contacts मिले: $listText। किसको call करूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found ${contactNames.size} contacts: $listText. Which one should I call?"
        }
    }

    fun clarificationRetry(
        type: PendingActionType,
        candidateNames: List<String>,
        languageHint: LanguageHint,
    ): String =
        when (type) {
            PendingActionType.OPEN_APP -> multipleApps(languageHint)
            PendingActionType.CALL_CONTACT -> multipleContacts(candidateNames, languageHint)
        }

    fun clarificationCleared(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main is selection ko samajh nahi paya. Command dobara boliye."
            LanguageHint.HINDI -> "मैं यह selection समझ नहीं पाया। Command दोबारा बोलिए।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't match that choice. Please say the command again."
        }

    private fun joinNames(
        names: List<String>,
        languageHint: LanguageHint,
    ): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) return names.first()

        val conjunction = when (languageHint) {
            LanguageHint.HINDI -> "और"
            LanguageHint.HINGLISH -> "aur"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "and"
        }
        if (names.size == 2) {
            return "${names.first()} $conjunction ${names.last()}"
        }

        return when (languageHint) {
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "${names.dropLast(1).joinToString(", ")}, $conjunction ${names.last()}"
            LanguageHint.HINDI,
            LanguageHint.HINGLISH -> "${names.dropLast(1).joinToString(", ")} $conjunction ${names.last()}"
        }
    }

    private const val MAX_NAMES_IN_RESPONSE = 5
}

package com.skybots.kiko.assistant.language

import com.skybots.kiko.assistant.clarification.PendingActionType

object LocalizedResponses {
    fun openingApp(appName: String, languageHint: LanguageHint): String =
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

    fun appLaunchFailed(appName: String, languageHint: LanguageHint): String =
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

    fun callingContact(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ko call kar raha hoon."
            LanguageHint.HINDI -> "$contactName को call कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Calling $contactName."
        }

    fun openingDialer(contactName: String, languageHint: LanguageHint): String =
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

    fun contactHasNoNumber(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone number nahi mila."
            LanguageHint.HINDI -> "$contactName के लिए phone number नहीं मिला।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find a phone number for $contactName."
        }

    fun callLaunchFailed(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone app open nahi ho paya."
            LanguageHint.HINDI -> "$contactName के लिए phone app open नहीं हो पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't open the phone app for $contactName."
        }

    fun multipleContacts(contactNames: List<String>, languageHint: LanguageHint): String {
        val names = contactNames.take(MAX_NAMES_IN_RESPONSE)
        val listText = joinNames(names, languageHint)
        return when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ${contactNames.size} contacts mile: $listText. Kisko call karu?"
            LanguageHint.HINDI -> "मुझे ${contactNames.size} contacts मिले: $listText। किसको call करूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found ${contactNames.size} contacts: $listText. Which one should I call?"
        }
    }

    fun multipleContactNumbers(
        contactName: String,
        numberLabels: List<String>,
        languageHint: LanguageHint,
    ): String {
        val listText = joinNames(numberLabels, languageHint)
        return when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke ${numberLabels.size} numbers mile: $listText. Kaunsa call karu?"
            LanguageHint.HINDI -> "$contactName के ${numberLabels.size} numbers मिले: $listText। कौन सा call करूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found ${numberLabels.size} numbers for $contactName: $listText. Which one should I call?"
        }
    }

    fun flashlightOn(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight on kar raha hoon."
            LanguageHint.HINDI -> "Flashlight चालू कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Turning flashlight on."
        }

    fun flashlightOff(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight off kar raha hoon."
            LanguageHint.HINDI -> "Flashlight बंद कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Turning flashlight off."
        }

    fun flashlightUnavailable(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Is phone me available flashlight nahi mili."
            LanguageHint.HINDI -> "इस phone में available flashlight नहीं मिली।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "This phone does not have an available flashlight."
        }

    fun flashlightError(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight abhi control nahi ho payi."
            LanguageHint.HINDI -> "Flashlight अभी control नहीं हो पाई।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I could not control the flashlight right now."
        }

    fun volumePercent(percent: Int, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume $percent percent kar raha hoon."
            LanguageHint.HINDI -> "Volume $percent percent कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Setting volume to $percent percent."
        }

    fun volumeIncrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume badha raha hoon."
            LanguageHint.HINDI -> "Volume बढ़ा रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Increasing volume."
        }

    fun volumeDecrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume kam kar raha hoon."
            LanguageHint.HINDI -> "Volume कम कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Decreasing volume."
        }

    fun volumeFixed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Is device par volume fixed hai."
            LanguageHint.HINDI -> "इस device पर volume fixed है।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "This device has fixed volume."
        }

    fun brightnessPercent(percent: Int, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness $percent percent kar raha hoon."
            LanguageHint.HINDI -> "Brightness $percent percent कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Setting brightness to $percent percent."
        }

    fun brightnessIncrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness badha raha hoon."
            LanguageHint.HINDI -> "Brightness बढ़ा रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Increasing brightness."
        }

    fun brightnessDecrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness kam kar raha hoon."
            LanguageHint.HINDI -> "Brightness कम कर रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Decreasing brightness."
        }

    fun appBrightnessOnly(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Abhi main Kiko screen ki brightness adjust kar sakta hoon. Puri phone brightness ke liye extra permission chahiye."
            LanguageHint.HINDI -> "अभी मैं Kiko screen की brightness adjust कर सकता हूँ। पूरी phone brightness के लिए extra permission चाहिए।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I can adjust Kiko's screen brightness now. System-wide brightness needs extra permission."
        }

    fun alarmSet(displayTime: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$displayTime ke liye alarm laga raha hoon."
            LanguageHint.HINDI -> "$displayTime के लिए alarm लगा रहा हूँ।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Setting alarm for $displayTime."
        }

    fun alarmNeedsTime(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Alarm kis time ka lagana hai?"
            LanguageHint.HINDI -> "Alarm किस time का लगाना है?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What time should I set the alarm for?"
        }

    fun alarmLaunchFailed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Alarm app open nahi ho paya."
            LanguageHint.HINDI -> "Alarm app open नहीं हो पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I could not open the alarm app."
        }

    fun reminderSaved(displayTime: String, message: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$displayTime ke liye reminder save kar diya: $message."
            LanguageHint.HINDI -> "$displayTime के लिए reminder save कर दिया: $message."
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Reminder saved for $displayTime: $message."
        }

    fun reminderNeedsMessage(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kis cheez ka reminder lagana hai?"
            LanguageHint.HINDI -> "किस चीज़ का reminder लगाना है?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What should I remind you about?"
        }

    fun reminderNeedsTime(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Reminder kis time ka lagana hai?"
            LanguageHint.HINDI -> "Reminder किस time का लगाना है?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What time should I set the reminder for?"
        }

    fun notificationPermissionNeeded(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> " Notification ke liye permission chahiye."
            LanguageHint.HINDI -> " Notification के लिए permission चाहिए।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> " Notification permission is needed to alert you."
        }

    fun rememberContactAliasPrompt(
        contactName: String,
        alias: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kya main next time ke liye $contactName ko $alias ke naam se yaad rakhun?"
            LanguageHint.HINDI -> "क्या मैं अगली बार के लिए $contactName को $alias के नाम से याद रखूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Should I remember $contactName as $alias for next time?"
        }

    fun rememberAppAliasPrompt(
        appName: String,
        alias: String,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kya main next time ke liye $appName ko $alias ke naam se yaad rakhun?"
            LanguageHint.HINDI -> "क्या मैं अगली बार के लिए $appName को $alias के नाम से याद रखूँ?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Should I remember $appName as $alias for next time?"
        }

    fun aliasRemembered(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Theek hai, main yaad rakhunga."
            LanguageHint.HINDI -> "ठीक है, मैं याद रखूँगा।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Okay, I will remember that."
        }

    fun aliasNotRemembered(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Theek hai, main yaad nahi rakhunga."
            LanguageHint.HINDI -> "ठीक है, मैं याद नहीं रखूँगा।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Okay, I will not remember that."
        }

    fun unknown(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main ye command abhi samajh nahi paya."
            LanguageHint.HINDI -> "मैं यह command अभी समझ नहीं पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I did not understand that yet."
        }

    fun internetRequired(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kiko V1 offline kaam karta hai. Internet answers is version me use nahi honge."
            LanguageHint.HINDI -> "Kiko V1 offline काम करता है। Internet answers इस version में use नहीं होंगे।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Kiko V1 works offline. Internet answers will not be used in this version."
        }

    fun processingFailed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main is request ko abhi process nahi kar paya."
            LanguageHint.HINDI -> "मैं इस request को अभी process नहीं कर पाया।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I could not process that yet."
        }

    fun clarificationRetry(
        type: PendingActionType,
        candidateNames: List<String>,
        languageHint: LanguageHint,
    ): String =
        when (type) {
            PendingActionType.OPEN_APP -> multipleApps(languageHint)
            PendingActionType.CALL_CONTACT -> multipleContacts(candidateNames, languageHint)
            PendingActionType.CALL_CONTACT_NUMBER -> multipleContactNumbers(
                contactName = candidateNames.firstOrNull()?.substringBefore(" ") ?: "Contact",
                numberLabels = candidateNames,
                languageHint = languageHint,
            )
            PendingActionType.REMEMBER_APP_ALIAS,
            PendingActionType.REMEMBER_CONTACT_ALIAS -> when (languageHint) {
                LanguageHint.HINGLISH -> "Haan ya nahi boliye."
                LanguageHint.HINDI -> "हाँ या नहीं बोलिए।"
                LanguageHint.ENGLISH,
                LanguageHint.SYSTEM_DEFAULT -> "Please say yes or no."
            }
        }

    fun clarificationCleared(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main is selection ko samajh nahi paya. Command dobara boliye."
            LanguageHint.HINDI -> "मैं यह selection समझ नहीं पाया। Command दोबारा बोलिए।"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't match that choice. Please say the command again."
        }

    private fun joinNames(names: List<String>, languageHint: LanguageHint): String {
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

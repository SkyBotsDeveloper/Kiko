package com.skybots.kiko.assistant.language

import com.skybots.kiko.assistant.clarification.PendingActionType

object LocalizedResponses {
    fun openingApp(appName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$appName khol raha hoon."
            LanguageHint.HINDI -> "$appName \u0916\u094b\u0932 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Opening $appName."
        }

    fun multipleApps(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe multiple apps mile. Kaunsa open karu?"
            LanguageHint.HINDI -> "\u092e\u0941\u091d\u0947 multiple apps \u092e\u093f\u0932\u0947\u0964 \u0915\u094c\u0928 \u0938\u093e \u0916\u094b\u0932\u0942\u0901?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found multiple apps. Which one should I open?"
        }

    fun appNotFound(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ye app phone me nahi mila."
            LanguageHint.HINDI -> "\u092e\u0941\u091d\u0947 \u092f\u0939 app phone \u092e\u0947\u0902 \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find that app on this phone."
        }

    fun appLaunchFailed(appName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$appName open nahi ho paya."
            LanguageHint.HINDI -> "$appName open \u0928\u0939\u0940\u0902 \u0939\u094b \u092a\u093e\u092f\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't open $appName."
        }

    fun contactsPermission(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Contacts find karne ke liye mujhe contacts permission chahiye."
            LanguageHint.HINDI -> "Contacts \u0922\u0942\u0901\u0922\u0928\u0947 \u0915\u0947 \u0932\u093f\u090f \u092e\u0941\u091d\u0947 contacts permission \u091a\u093e\u0939\u093f\u090f\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I need contacts permission to find and call people."
        }

    fun callingContact(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ko call kar raha hoon."
            LanguageHint.HINDI -> "$contactName \u0915\u094b call \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Calling $contactName."
        }

    fun openingDialer(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye dialer open kar raha hoon."
            LanguageHint.HINDI -> "$contactName \u0915\u0947 \u0932\u093f\u090f dialer \u0916\u094b\u0932 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Opening dialer for $contactName."
        }

    fun contactNotFound(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ye contact phone me nahi mila."
            LanguageHint.HINDI -> "\u092e\u0941\u091d\u0947 \u092f\u0939 contact phone \u092e\u0947\u0902 \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find that contact on this phone."
        }

    fun contactHasNoNumber(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone number nahi mila."
            LanguageHint.HINDI -> "$contactName \u0915\u0947 \u0932\u093f\u090f phone number \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't find a phone number for $contactName."
        }

    fun callLaunchFailed(contactName: String, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "$contactName ke liye phone app open nahi ho paya."
            LanguageHint.HINDI -> "$contactName \u0915\u0947 \u0932\u093f\u090f phone app open \u0928\u0939\u0940\u0902 \u0939\u094b \u092a\u093e\u092f\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't open the phone app for $contactName."
        }

    fun multipleContacts(contactNames: List<String>, languageHint: LanguageHint): String {
        val names = contactNames.take(MAX_NAMES_IN_RESPONSE)
        val listText = joinNames(names, languageHint)
        return when (languageHint) {
            LanguageHint.HINGLISH -> "Mujhe ${contactNames.size} contacts mile: $listText. Kisko call karu?"
            LanguageHint.HINDI -> "\u092e\u0941\u091d\u0947 ${contactNames.size} contacts \u092e\u093f\u0932\u0947: $listText\u0964 \u0915\u093f\u0938\u0915\u094b call \u0915\u0930\u0942\u0901?"
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
            LanguageHint.HINDI -> "$contactName \u0915\u0947 ${numberLabels.size} numbers \u092e\u093f\u0932\u0947: $listText\u0964 \u0915\u094c\u0928 \u0938\u093e call \u0915\u0930\u0942\u0901?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I found ${numberLabels.size} numbers for $contactName: $listText. Which one should I call?"
        }
    }

    fun flashlightOn(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight on kar raha hoon."
            LanguageHint.HINDI -> "Flashlight \u091a\u093e\u0932\u0942 \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Turning flashlight on."
        }

    fun flashlightOff(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight off kar raha hoon."
            LanguageHint.HINDI -> "Flashlight \u092c\u0902\u0926 \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Turning flashlight off."
        }

    fun flashlightUnavailable(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Is phone me available flashlight nahi mili."
            LanguageHint.HINDI -> "\u0907\u0938 phone \u092e\u0947\u0902 available flashlight \u0928\u0939\u0940\u0902 \u092e\u093f\u0932\u0940\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "This phone does not have an available flashlight."
        }

    fun flashlightError(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Flashlight abhi control nahi ho payi."
            LanguageHint.HINDI -> "Flashlight \u0905\u092d\u0940 control \u0928\u0939\u0940\u0902 \u0939\u094b \u092a\u093e\u0908\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I could not control the flashlight right now."
        }

    fun volumePercent(percent: Int, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume $percent percent kar raha hoon."
            LanguageHint.HINDI -> "Volume $percent percent \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Setting volume to $percent percent."
        }

    fun volumeIncrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume badha raha hoon."
            LanguageHint.HINDI -> "Volume \u092c\u0922\u093c\u093e \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Increasing volume."
        }

    fun volumeDecrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Volume kam kar raha hoon."
            LanguageHint.HINDI -> "Volume \u0915\u092e \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Decreasing volume."
        }

    fun volumeFixed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Is device par volume fixed hai."
            LanguageHint.HINDI -> "\u0907\u0938 device \u092a\u0930 volume fixed \u0939\u0948\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "This device has fixed volume."
        }

    fun brightnessPercent(percent: Int, languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness $percent percent kar raha hoon."
            LanguageHint.HINDI -> "Brightness $percent percent \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Setting brightness to $percent percent."
        }

    fun brightnessIncrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness badha raha hoon."
            LanguageHint.HINDI -> "Brightness \u092c\u0922\u093c\u093e \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Increasing brightness."
        }

    fun brightnessDecrease(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Brightness kam kar raha hoon."
            LanguageHint.HINDI -> "Brightness \u0915\u092e \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Decreasing brightness."
        }

    fun appBrightnessOnly(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Abhi main Kiko screen ki brightness adjust kar sakta hoon. Puri phone brightness ke liye extra permission chahiye."
            LanguageHint.HINDI -> "\u0905\u092d\u0940 \u092e\u0948\u0902 Kiko screen \u0915\u0940 brightness adjust \u0915\u0930 \u0938\u0915\u0924\u093e \u0939\u0942\u0901\u0964 \u092a\u0942\u0930\u0940 phone brightness \u0915\u0947 \u0932\u093f\u090f extra permission \u091a\u093e\u0939\u093f\u090f\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I can adjust Kiko's screen brightness now. System-wide brightness needs extra permission."
        }

    fun alarmSet(
        displayTime: String,
        dayOffset: Int = 0,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "${dayPrefix(dayOffset, languageHint)}$displayTime ke liye alarm laga raha hoon."
            LanguageHint.HINDI -> "${dayPrefix(dayOffset, languageHint)}$displayTime \u0915\u0947 \u0932\u093f\u090f alarm \u0932\u0917\u093e \u0930\u0939\u093e \u0939\u0942\u0901\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> if (dayOffset == 1) {
                "Setting alarm for tomorrow at $displayTime."
            } else {
                "Setting alarm for $displayTime."
            }
        }

    fun alarmNeedsTime(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Alarm kis time ka lagana hai?"
            LanguageHint.HINDI -> "Alarm \u0915\u093f\u0938 time \u0915\u093e \u0932\u0917\u093e\u0928\u093e \u0939\u0948?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What time should I set the alarm for?"
        }

    fun alarmLaunchFailed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Alarm app open nahi ho paya."
            LanguageHint.HINDI -> "Alarm app open \u0928\u0939\u0940\u0902 \u0939\u094b \u092a\u093e\u092f\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I could not open the alarm app."
        }

    fun reminderSaved(
        displayTime: String,
        message: String,
        dayOffset: Int = 0,
        languageHint: LanguageHint,
    ): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "${dayPrefix(dayOffset, languageHint)}$displayTime ke liye reminder save kar diya: $message."
            LanguageHint.HINDI -> "${dayPrefix(dayOffset, languageHint)}$displayTime \u0915\u0947 \u0932\u093f\u090f reminder save \u0915\u0930 \u0926\u093f\u092f\u093e: $message."
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> if (dayOffset == 1) {
                "Reminder saved for tomorrow at $displayTime: $message."
            } else {
                "Reminder saved for $displayTime: $message."
            }
        }

    fun reminderNeedsMessage(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kis cheez ka reminder lagana hai?"
            LanguageHint.HINDI -> "\u0915\u093f\u0938 \u091a\u0940\u091c\u093c \u0915\u093e reminder \u0932\u0917\u093e\u0928\u093e \u0939\u0948?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What should I remind you about?"
        }

    fun reminderNeedsTime(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Reminder kis time ka lagana hai?"
            LanguageHint.HINDI -> "Reminder \u0915\u093f\u0938 time \u0915\u093e \u0932\u0917\u093e\u0928\u093e \u0939\u0948?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "What time should I set the reminder for?"
        }

    fun notificationPermissionNeeded(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> " Notification ke liye permission chahiye."
            LanguageHint.HINDI -> " Notification \u0915\u0947 \u0932\u093f\u090f permission \u091a\u093e\u0939\u093f\u090f\u0964"
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
            LanguageHint.HINDI -> "\u0915\u094d\u092f\u093e \u092e\u0948\u0902 \u0905\u0917\u0932\u0940 \u092c\u093e\u0930 \u0915\u0947 \u0932\u093f\u090f $contactName \u0915\u094b $alias \u0915\u0947 \u0928\u093e\u092e \u0938\u0947 \u092f\u093e\u0926 \u0930\u0916\u0942\u0901?"
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
            LanguageHint.HINDI -> "\u0915\u094d\u092f\u093e \u092e\u0948\u0902 \u0905\u0917\u0932\u0940 \u092c\u093e\u0930 \u0915\u0947 \u0932\u093f\u090f $appName \u0915\u094b $alias \u0915\u0947 \u0928\u093e\u092e \u0938\u0947 \u092f\u093e\u0926 \u0930\u0916\u0942\u0901?"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Should I remember $appName as $alias for next time?"
        }

    fun aliasRemembered(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Theek hai, main yaad rakhunga."
            LanguageHint.HINDI -> "\u0920\u0940\u0915 \u0939\u0948, \u092e\u0948\u0902 \u092f\u093e\u0926 \u0930\u0916\u0942\u0901\u0917\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Okay, I will remember that."
        }

    fun aliasNotRemembered(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Theek hai, main yaad nahi rakhunga."
            LanguageHint.HINDI -> "\u0920\u0940\u0915 \u0939\u0948, \u092e\u0948\u0902 \u092f\u093e\u0926 \u0928\u0939\u0940\u0902 \u0930\u0916\u0942\u0901\u0917\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Okay, I will not remember that."
        }

    fun unknown(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main ye command abhi samajh nahi paya."
            LanguageHint.HINDI -> "\u092e\u0948\u0902 \u092f\u0939 command \u0905\u092d\u0940 \u0938\u092e\u091d \u0928\u0939\u0940\u0902 \u092a\u093e\u092f\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I did not understand that yet."
        }

    fun internetRequired(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Kiko V1 offline kaam karta hai. Internet answers is version me use nahi honge."
            LanguageHint.HINDI -> "Kiko V1 offline \u0915\u093e\u092e \u0915\u0930\u0924\u093e \u0939\u0948\u0964 Internet answers \u0907\u0938 version \u092e\u0947\u0902 use \u0928\u0939\u0940\u0902 \u0939\u094b\u0902\u0917\u0947\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Kiko V1 works offline. Internet answers will not be used in this version."
        }

    fun processingFailed(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main is request ko abhi process nahi kar paya."
            LanguageHint.HINDI -> "\u092e\u0948\u0902 \u0907\u0938 request \u0915\u094b \u0905\u092d\u0940 process \u0928\u0939\u0940\u0902 \u0915\u0930 \u092a\u093e\u092f\u093e\u0964"
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
                LanguageHint.HINDI -> "\u0939\u093e\u0901 \u092f\u093e \u0928\u0939\u0940\u0902 \u092c\u094b\u0932\u093f\u090f\u0964"
                LanguageHint.ENGLISH,
                LanguageHint.SYSTEM_DEFAULT -> "Please say yes or no."
            }
        }

    fun clarificationWaiting(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main abhi pichle action ke liye clarification wait kar raha hoon. Aap 'cancel' bol sakte ho ya option choose kar sakte ho."
            LanguageHint.HINDI -> "\u092e\u0948\u0902 \u0905\u092d\u0940 \u092a\u093f\u091b\u0932\u0947 action \u0915\u0947 \u0932\u093f\u090f clarification wait \u0915\u0930 \u0930\u0939\u093e \u0939\u0942\u0901\u0964 \u0906\u092a 'cancel' \u092c\u094b\u0932 \u0938\u0915\u0924\u0947 \u0939\u0948\u0902 \u092f\u093e option choose \u0915\u0930 \u0938\u0915\u0924\u0947 \u0939\u0948\u0902\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I am waiting for clarification on the previous action. Say 'cancel' or choose an option."
        }

    fun clarificationCancelled(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Theek hai, maine pichla action cancel kar diya."
            LanguageHint.HINDI -> "\u0920\u0940\u0915 \u0939\u0948, \u092e\u0948\u0902\u0928\u0947 \u092a\u093f\u091b\u0932\u093e action cancel \u0915\u0930 \u0926\u093f\u092f\u093e\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "Okay, I cancelled the previous action."
        }

    fun clarificationCleared(languageHint: LanguageHint): String =
        when (languageHint) {
            LanguageHint.HINGLISH -> "Main is selection ko samajh nahi paya. Command dobara boliye."
            LanguageHint.HINDI -> "\u092e\u0948\u0902 \u092f\u0939 selection \u0938\u092e\u091d \u0928\u0939\u0940\u0902 \u092a\u093e\u092f\u093e\u0964 Command \u0926\u094b\u092c\u093e\u0930\u093e \u092c\u094b\u0932\u093f\u090f\u0964"
            LanguageHint.ENGLISH,
            LanguageHint.SYSTEM_DEFAULT -> "I couldn't match that choice. Please say the command again."
        }

    private fun dayPrefix(
        dayOffset: Int,
        languageHint: LanguageHint,
    ): String =
        if (dayOffset == 1) {
            when (languageHint) {
                LanguageHint.HINGLISH -> "Kal "
                LanguageHint.HINDI -> "\u0915\u0932 "
                LanguageHint.ENGLISH,
                LanguageHint.SYSTEM_DEFAULT -> "Tomorrow "
            }
        } else {
            ""
        }

    private fun joinNames(names: List<String>, languageHint: LanguageHint): String {
        if (names.isEmpty()) return ""
        if (names.size == 1) return names.first()

        val conjunction = when (languageHint) {
            LanguageHint.HINDI -> "\u0914\u0930"
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

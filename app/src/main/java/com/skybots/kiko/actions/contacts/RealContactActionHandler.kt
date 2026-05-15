package com.skybots.kiko.actions.contacts

import com.skybots.kiko.actions.AssistantActionResult
import com.skybots.kiko.assistant.clarification.ClarificationCandidate
import com.skybots.kiko.assistant.clarification.ClarificationManager
import com.skybots.kiko.assistant.clarification.PendingActionType
import com.skybots.kiko.assistant.language.LanguageHint
import com.skybots.kiko.assistant.language.LocalizedResponses
import com.skybots.kiko.assistant.parser.AssistantIntent
import com.skybots.kiko.permissions.KikoPermission
import com.skybots.kiko.permissions.PermissionChecker

class RealContactActionHandler(
    private val contactsRepository: ContactsRepository,
    private val contactMatcher: ContactMatcher,
    private val permissionChecker: PermissionChecker,
    private val phoneActionLauncher: PhoneActionLauncher,
    private val clarificationManager: ClarificationManager,
) : ContactActionHandler {
    override fun handle(intent: AssistantIntent): AssistantActionResult {
        if (!permissionChecker.hasReadContactsPermission()) {
            return AssistantActionResult(
                response = LocalizedResponses.contactsPermission(intent.languageHint),
                requestedPermission = KikoPermission.READ_CONTACTS,
            )
        }

        val query = intent.contactQuery ?: intent.target ?: intent.rawText
        return when (val match = contactMatcher.match(query, contactsRepository.getContacts())) {
            is ContactMatchResult.Single -> startCallOrDial(match.contact, intent.languageHint)
            is ContactMatchResult.Multiple -> {
                clarificationManager.setPending(
                    type = PendingActionType.CALL_CONTACT,
                    candidates = match.candidates.map { contact ->
                        ClarificationCandidate(
                            id = contact.idForClarification(),
                            label = contact.displayName,
                            subtitle = contact.primaryNumber(),
                        )
                    },
                    languageHint = intent.languageHint,
                )
                AssistantActionResult(
                    response = LocalizedResponses.multipleContacts(
                        contactNames = match.candidates.map { it.displayName },
                        languageHint = intent.languageHint,
                    ),
                )
            }
            ContactMatchResult.None -> AssistantActionResult(
                response = LocalizedResponses.contactNotFound(intent.languageHint),
            )
        }
    }

    override fun handleClarification(
        candidate: ClarificationCandidate,
        languageHint: LanguageHint,
    ): AssistantActionResult {
        if (!permissionChecker.hasReadContactsPermission()) {
            return AssistantActionResult(
                response = LocalizedResponses.contactsPermission(languageHint),
                requestedPermission = KikoPermission.READ_CONTACTS,
            )
        }

        val contact = contactsRepository.getContacts()
            .firstOrNull { it.idForClarification() == candidate.id }
            ?: ContactModel(
                contactId = null,
                displayName = candidate.label,
                phoneNumbers = listOfNotNull(
                    candidate.subtitle?.let { number ->
                        ContactPhoneNumber(number = number)
                    },
                ),
            )

        return startCallOrDial(contact, languageHint)
    }

    private fun startCallOrDial(
        contact: ContactModel,
        languageHint: LanguageHint,
    ): AssistantActionResult {
        val number = contact.primaryNumber()
            ?: return AssistantActionResult(
                response = LocalizedResponses.contactHasNoNumber(contact.displayName, languageHint),
            )

        val launched = if (permissionChecker.hasCallPhonePermission()) {
            phoneActionLauncher.call(number)
        } else {
            phoneActionLauncher.dial(number)
        }

        if (!launched) {
            return AssistantActionResult(
                response = LocalizedResponses.callLaunchFailed(contact.displayName, languageHint),
            )
        }

        return AssistantActionResult(
            response = if (permissionChecker.hasCallPhonePermission()) {
                LocalizedResponses.callingContact(contact.displayName, languageHint)
            } else {
                LocalizedResponses.openingDialer(contact.displayName, languageHint)
            },
        )
    }

    private fun ContactModel.primaryNumber(): String? =
        phoneNumbers.firstOrNull()?.number

    private fun ContactModel.idForClarification(): String =
        listOfNotNull(contactId, displayName, primaryNumber()).joinToString("|")
}

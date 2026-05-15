package com.skybots.kiko.actions.contacts

data class ContactModel(
    val contactId: String?,
    val displayName: String,
    val phoneNumbers: List<ContactPhoneNumber>,
)

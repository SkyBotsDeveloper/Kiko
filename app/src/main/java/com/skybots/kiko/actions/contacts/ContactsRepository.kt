package com.skybots.kiko.actions.contacts

interface ContactsRepository {
    fun getContacts(): List<ContactModel>

    fun refresh()
}

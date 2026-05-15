package com.skybots.kiko.actions.contacts

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.skybots.kiko.permissions.PermissionChecker

class AndroidContactsRepository(
    context: Context,
    private val permissionChecker: PermissionChecker,
) : ContactsRepository {
    private val appContext = context.applicationContext
    private var cachedContacts: List<ContactModel>? = null

    override fun getContacts(): List<ContactModel> {
        if (!permissionChecker.hasReadContactsPermission()) return emptyList()

        return cachedContacts ?: loadContacts().also { contacts ->
            cachedContacts = contacts
        }
    }

    override fun refresh() {
        cachedContacts = if (permissionChecker.hasReadContactsPermission()) {
            loadContacts()
        } else {
            null
        }
    }

    private fun loadContacts(): List<ContactModel> {
        val contactsByKey = linkedMapOf<String, MutableContact>()
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.TYPE,
        )

        appContext.contentResolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${Phone.DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(Phone.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(Phone.NUMBER)
            val typeIndex = cursor.getColumnIndex(Phone.TYPE)

            while (cursor.moveToNext()) {
                val contactId = cursor.getStringOrNull(idIndex)
                val displayName = cursor.getStringOrNull(nameIndex)?.trim().orEmpty()
                val number = cursor.getStringOrNull(numberIndex)?.trim().orEmpty()
                if (displayName.isBlank() || number.isBlank()) continue

                val key = contactId ?: displayName
                val label = cursor.getIntOrNull(typeIndex)?.let { type ->
                    Phone.getTypeLabel(
                        appContext.resources,
                        type,
                        "",
                    ).toString().ifBlank { null }
                }
                val mutableContact = contactsByKey.getOrPut(key) {
                    MutableContact(
                        contactId = contactId,
                        displayName = displayName,
                    )
                }
                if (mutableContact.phoneNumbers.none { it.number == number }) {
                    mutableContact.phoneNumbers += ContactPhoneNumber(
                        number = number,
                        label = label,
                    )
                }
            }
        }

        return contactsByKey.values
            .map { mutableContact ->
                ContactModel(
                    contactId = mutableContact.contactId,
                    displayName = mutableContact.displayName,
                    phoneNumbers = mutableContact.phoneNumbers,
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (index >= 0 && !isNull(index)) getString(index) else null

    private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
        if (index >= 0 && !isNull(index)) getInt(index) else null

    private data class MutableContact(
        val contactId: String?,
        val displayName: String,
        val phoneNumbers: MutableList<ContactPhoneNumber> = mutableListOf(),
    )
}

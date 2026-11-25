package com.johnmanko.app.steamboat.data.contacts

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.domain.PhoneNumbers

class ContactSync(private val context: Context) {
    fun read(): List<ContactEntity> {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        ) ?: return emptyList()

        val seen = HashSet<Pair<String, String>>()
        val rows = ArrayList<ContactEntity>()
        cursor.use {
            val lookupIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (it.moveToNext()) {
                val number = it.getString(numberIndex) ?: continue
                val matchKey = PhoneNumbers.matchKey(number)
                if (matchKey.isEmpty()) continue
                val lookupKey = it.getString(lookupIndex) ?: "phone:$matchKey"
                if (!seen.add(lookupKey to matchKey)) continue
                val name = it.getString(nameIndex)?.takeIf { value -> value.isNotBlank() } ?: number
                rows += ContactEntity(
                    lookupKey = lookupKey,
                    displayName = name,
                    phoneNumber = number,
                    matchKey = matchKey,
                    photoUri = it.getString(photoIndex),
                )
            }
        }
        return rows
    }

    /** Saves a phone-only contact. The display name is the formatted number. */
    fun insert(number: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) return false
        val display = PhoneNumbers.display(number)
        val operations = arrayListOf(
            ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, display)
                .build(),
            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, display)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                )
                .build(),
        )
        return runCatching {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        }.isSuccess
    }
}

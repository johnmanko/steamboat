package com.johnmanko.app.steamboat.data.sample

import android.Manifest
import android.content.ContentProviderOperation
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.johnmanko.app.steamboat.data.db.ContactEntity
import com.johnmanko.app.steamboat.data.repo.ImportedSms
import com.johnmanko.app.steamboat.data.repo.MessageStore
import com.johnmanko.app.steamboat.domain.MessageStatus
import com.johnmanko.app.steamboat.domain.PhoneNumbers

/**
 * Debug inbox so the emulator has people to text. Contacts are written into
 * the device address book when allowed, and messages are stored for the app
 * to page through. A later real text to the same number joins that thread.
 */
class SampleInbox(
    private val context: Context,
    private val store: MessageStore,
) {
    suspend fun install(): Boolean {
        if (!emulator()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_INSTALLED, false)) {
            return prefs.getBoolean(KEY_PROVIDER_CONTACTS, false)
        }
        val wroteContacts = writeContacts()
        val messages = sampleMessages()
        store.upsertContacts(localContacts())
        store.importMessages(messages)
        store.relinkContacts()
        prefs.edit()
            .putBoolean(KEY_INSTALLED, true)
            .putBoolean(KEY_PROVIDER_CONTACTS, wroteContacts)
            .apply()
        Log.i(TAG, "Installed ${PEOPLE.size} sample contacts and ${messages.size} messages")
        return wroteContacts
    }

    private fun writeContacts(): Boolean {
        if (!granted(Manifest.permission.WRITE_CONTACTS)) return false
        val operations = ArrayList<ContentProviderOperation>()
        for (person in PEOPLE) {
            if (contactExists(person.number)) continue
            val raw = operations.size
            operations += ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()
            operations += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                )
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, person.name)
                .build()
            operations += ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                )
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, person.number)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                )
                .build()
        }
        if (operations.isEmpty()) return true
        return runCatching {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        }.isSuccess
    }

    private fun contactExists(number: String): Boolean {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null,
        ) ?: return false
        val key = PhoneNumbers.matchKey(number)
        cursor.use {
            val numberIndex = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (PhoneNumbers.matchKey(it.getString(numberIndex).orEmpty()) == key) return true
            }
        }
        return false
    }

    private fun localContacts(): List<ContactEntity> = PEOPLE.map { person ->
        val key = PhoneNumbers.matchKey(person.number)
        ContactEntity(
            lookupKey = "sample:$key",
            displayName = person.name,
            phoneNumber = person.number,
            matchKey = key,
            photoUri = null,
        )
    }

    private fun sampleMessages(): List<ImportedSms> {
        val now = System.currentTimeMillis()
        fun line(
            person: Person,
            body: String,
            minutesAgo: Int,
            outgoing: Boolean,
            seen: Boolean,
        ) = ImportedSms(
            providerKey = "sample:${PhoneNumbers.matchKey(person.number)}:$minutesAgo",
            address = person.number,
            body = body,
            sentAt = now - minutesAgo * 60_000L,
            outgoing = outgoing,
            seen = seen || outgoing,
            status = if (outgoing) MessageStatus.SENT else MessageStatus.RECEIVED,
        )
        return listOf(
            line(MAYA, "The bakery on Pine saved you a slice.", minutesAgo = 60 * 26, outgoing = false, seen = true),
            line(MAYA, "I'll swing by after work.", minutesAgo = 60 * 25, outgoing = true, seen = true),
            line(MAYA, "They close at 6. Want me to pick it up?", minutesAgo = 95, outgoing = false, seen = false),
            line(JONAH, "Are we still on for the river trail?", minutesAgo = 60 * 20, outgoing = false, seen = true),
            line(JONAH, "Yes. I'll bring the thermos.", minutesAgo = 60 * 18, outgoing = true, seen = true),
            line(JONAH, "Perfect. Trailhead at 9.", minutesAgo = 60 * 3, outgoing = false, seen = true),
            line(PRIYA, "The mockups look soft and lovely.", minutesAgo = 60 * 5, outgoing = false, seen = false),
            line(PRIYA, "I'll send the dark theme tonight.", minutesAgo = 60 * 4, outgoing = true, seen = true),
            line(LUIS, "Did the tickets go through?", minutesAgo = 60 * 30, outgoing = true, seen = true),
            line(LUIS, "Two seats, row D. See you there.", minutesAgo = 60 * 28, outgoing = false, seen = true),
        )
    }

    private fun emulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val hardware = Build.HARDWARE
        return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || model.contains("google_sdk", ignoreCase = true)
            || model.contains("Emulator", ignoreCase = true)
            || model.contains("Android SDK built for", ignoreCase = true)
            || hardware.contains("goldfish", ignoreCase = true)
            || hardware.contains("ranchu", ignoreCase = true)
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private data class Person(val name: String, val number: String)

    private companion object {
        const val PREFS = "sample_inbox"
        const val KEY_INSTALLED = "installed"
        const val KEY_PROVIDER_CONTACTS = "provider_contacts"
        const val TAG = "Steamboat"

        val MAYA = Person("Maya Chen", "(555) 010-1001")
        val JONAH = Person("Jonah Ellis", "(555) 010-1002")
        val PRIYA = Person("Priya Shah", "(555) 010-1003")
        val LUIS = Person("Luis Ortega", "(555) 010-1004")
        val PEOPLE = listOf(MAYA, JONAH, PRIYA, LUIS)
    }
}

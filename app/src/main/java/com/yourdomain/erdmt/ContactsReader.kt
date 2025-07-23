package com.yourdomain.erdmt

import android.content.Context
import android.provider.ContactsContract

object ContactsReader {
    fun getContacts(context: Context): List<String> {
        val contacts = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                contacts.add("$name: $number")
            }
        }
        return contacts
    }
}
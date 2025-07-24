
package com.yourdomain.erdmt

import android.content.Context
import android.provider.ContactsContract

object ContactsReader {
    data class Contact(
        val name: String,
        val phone: String
    )
    
    fun getAllContacts(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "Unknown"
                    val phone = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    
                    contacts.add(Contact(name = name, phone = phone))
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read contacts: ${e.message}")
        }
        
        return contacts
    }
}
package com.yourdomain.erdmt

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactsReader {
    private const val TAG = "ContactsReader"
    
    data class Contact(
        val name: String,
        val phoneNumber: String,
        val email: String?
    )
    
    fun getAllContacts(context: Context): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "Unknown"
                    val phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                    val email = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS))
                    
                    contacts.add(Contact(name, phoneNumber, email))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading contacts", e)
        }
        
        return contacts
    }
}

package com.yourdomain.erdmt

import android.content.Context
import android.database.Cursor
import android.net.Uri

object SmsReader {
    fun getRecentSms(context: Context, limit: Int = 20): List<String> {
        val uri = Uri.parse("content://sms/inbox")
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, "date DESC")
        val smsList = mutableListOf<String>()
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < limit) {
                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                smsList.add("From: $address\n$body")
                count++
            }
        }
        return smsList
    }
}

package com.yourdomain.erdmt

import android.content.Context
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.*

object SmsReader {
    data class SmsMessage(
        val sender: String,
        val body: String,
        val date: String,
        val type: String
    )
    
    fun getRecentSms(context: Context, limit: Int = 20): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        try {
            val cursor = context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)) ?: "Unknown"
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY)) ?: ""
                    val date = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                    val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                    
                    val typeString = when (type) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> "Received"
                        Telephony.Sms.MESSAGE_TYPE_SENT -> "Sent"
                        else -> "Other"
                    }
                    
                    messages.add(
                        SmsMessage(
                            sender = address,
                            body = body,
                            date = dateFormat.format(Date(date)),
                            type = typeString
                        )
                    )
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read SMS: ${e.message}")
        }
        
        return messages
    }
}

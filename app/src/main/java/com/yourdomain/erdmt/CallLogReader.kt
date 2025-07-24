
package com.yourdomain.erdmt

import android.content.Context
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.*

object CallLogReader {
    data class CallLogEntry(
        val number: String,
        val type: String,
        val date: String,
        val duration: Long
    )
    
    fun getRecentCalls(context: Context, limit: Int = 20): List<CallLogEntry> {
        val calls = mutableListOf<CallLogEntry>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown"
                    val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                    val duration = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    
                    val typeString = when (type) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        else -> "Other"
                    }
                    
                    calls.add(
                        CallLogEntry(
                            number = number,
                            type = typeString,
                            date = dateFormat.format(Date(date)),
                            duration = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to read call logs: ${e.message}")
        }
        
        return calls
    }
}

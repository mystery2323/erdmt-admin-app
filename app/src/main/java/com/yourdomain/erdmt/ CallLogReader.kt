package com.yourdomain.erdmt

import android.content.Context
import android.provider.CallLog

object CallLogReader {
    fun getCallLogs(context: Context, limit: Int = 20): List<String> {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null, null, null, "date DESC"
        )
        val logs = mutableListOf<String>()
        cursor?.use {
            var count = 0
            while (it.moveToNext() && count < limit) {
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val duration = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                logs.add("Number: $number Type: $type Date: $date Duration: $duration")
                count++
            }
        }
        return logs
    }
}
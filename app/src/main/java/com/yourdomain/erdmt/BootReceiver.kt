
package com.yourdomain.erdmt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Boot completed or package replaced, starting ERDMT service")
            
            // Start the background service
            val serviceIntent = Intent(context, BackgroundService::class.java)
            context.startForegroundService(serviceIntent)
            
            // Log to Firebase (if possible)
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance()
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver, 
                    android.provider.Settings.Secure.ANDROID_ID
                )
                
                val log = mapOf(
                    "level" to "info",
                    "message" to "Device booted, ERDMT service auto-started",
                    "timestamp" to com.google.firebase.database.ServerValue.TIMESTAMP,
                    "deviceId" to deviceId,
                    "source" to "boot_receiver"
                )
                
                database.getReference("logs").push().setValue(log)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log boot event to Firebase", e)
            }
        }
    }
}

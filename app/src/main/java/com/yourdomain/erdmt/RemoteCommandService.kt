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
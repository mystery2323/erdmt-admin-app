
package com.yourdomain.erdmt

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object AppsLister {
    data class AppInfo(
        val name: String,
        val packageName: String
    )
    
    fun getInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val packageManager = context.packageManager
        
        try {
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in installedApps) {
                // Only include user-installed apps
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    apps.add(AppInfo(name = appName, packageName = appInfo.packageName))
                }
            }
            
            apps.sortBy { it.name }
        } catch (e: Exception) {
            throw RuntimeException("Failed to list apps: ${e.message}")
        }
        
        return apps
    }
}
package com.yourdomain.erdmt

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log

object AppsLister {
    private const val TAG = "AppsLister"
    
    data class AppInfo(
        val name: String,
        val packageName: String,
        val versionName: String,
        val isSystemApp: Boolean
    )
    
    fun getInstalledApps(context: Context): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        
        try {
            val packageManager = context.packageManager
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in installedApps) {
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) { // Only user apps
                    try {
                        val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        
                        apps.add(
                            AppInfo(
                                name = appName,
                                packageName = appInfo.packageName,
                                versionName = packageInfo.versionName ?: "Unknown",
                                isSystemApp = false
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error getting app info for ${appInfo.packageName}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing apps", e)
        }
        
        return apps.sortedBy { it.name }
    }
}

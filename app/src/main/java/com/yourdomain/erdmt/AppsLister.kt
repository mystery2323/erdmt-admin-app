
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

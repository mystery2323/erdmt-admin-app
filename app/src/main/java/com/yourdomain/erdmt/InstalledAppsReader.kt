package com.yourdomain.erdmt

import android.content.Context
import android.content.pm.ApplicationInfo

object InstalledAppsReader {
    fun getInstalledApps(context: Context): List<String> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        return packages.filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { "${it.loadLabel(pm)} (${it.packageName})" }
    }
}
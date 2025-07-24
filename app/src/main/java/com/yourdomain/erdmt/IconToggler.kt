
package com.yourdomain.erdmt

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconToggler {
    fun hideIcon(context: Context) {
        setIconVisibility(context, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }
    
    fun showIcon(context: Context) {
        setIconVisibility(context, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }
    
    private fun setIconVisibility(context: Context, state: Int) {
        val componentName = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            state,
            PackageManager.DONT_KILL_APP
        )
    }
}
package com.yourdomain.erdmt

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

object IconToggler {
    private const val TAG = "IconToggler"
    
    fun toggleAppIcon(context: Context, show: Boolean): Boolean {
        return try {
            val componentName = ComponentName(context, MainActivity::class.java)
            val newState = if (show) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            
            context.packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP
            )
            
            Log.d(TAG, "App icon ${if (show) "shown" else "hidden"}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle app icon", e)
            false
        }
    }
}

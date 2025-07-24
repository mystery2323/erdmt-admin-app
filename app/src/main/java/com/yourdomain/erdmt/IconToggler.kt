
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

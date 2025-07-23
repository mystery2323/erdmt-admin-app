package com.yourdomain.erdmt

import android.content.Context
import android.content.pm.PackageManager

object IconToggleUtil {
    fun setIconVisible(context: Context, visible: Boolean) {
        val pm = context.packageManager
        val componentName = context.packageName + ".MainActivity"
        pm.setComponentEnabledSetting(
            android.content.ComponentName(context, componentName),
            if (visible) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
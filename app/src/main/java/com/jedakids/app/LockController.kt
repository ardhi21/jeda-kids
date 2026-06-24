package com.jedakids.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

object LockController {
    fun adminComponent(context: Context): ComponentName {
        return ComponentName(context, JedaDeviceAdminReceiver::class.java)
    }

    fun isAdminActive(context: Context): Boolean {
        val policyManager = context.getSystemService(DevicePolicyManager::class.java)
        return policyManager.isAdminActive(adminComponent(context))
    }

    fun lockNow(context: Context): Boolean {
        if (!isAdminActive(context)) {
            return false
        }

        val policyManager = context.getSystemService(DevicePolicyManager::class.java)
        policyManager.lockNow()
        return true
    }
}
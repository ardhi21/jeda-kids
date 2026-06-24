package com.jedakids.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = TimerPreferences(context)
        val endAtMillis = preferences.getEndAtMillis()
        val nowMillis = System.currentTimeMillis()

        if (endAtMillis == TimerPreferences.NO_ACTIVE_SESSION) {
            return
        }

        if (endAtMillis > nowMillis + EARLY_DELIVERY_TOLERANCE_MILLIS) {
            if (TimerScheduler.canScheduleExactAlarms(context)) {
                TimerScheduler.schedule(context, endAtMillis)
            }
            return
        }

        preferences.clearSession()
        LockController.lockNow(context)
    }

    companion object {
        private const val EARLY_DELIVERY_TOLERANCE_MILLIS = 1_000L
    }
}
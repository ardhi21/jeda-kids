package com.jedakids.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isSupportedAction =
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == ACTION_EXACT_ALARM_PERMISSION_CHANGED

        if (!isSupportedAction) {
            return
        }

        val preferences = TimerPreferences(context)
        val endAtMillis = preferences.getEndAtMillis()

        if (endAtMillis == TimerPreferences.NO_ACTIVE_SESSION) {
            return
        }

        if (endAtMillis <= System.currentTimeMillis()) {
            preferences.clearSession()
            LockController.lockNow(context)
            return
        }

        if (TimerScheduler.canScheduleExactAlarms(context)) {
            TimerScheduler.schedule(context, endAtMillis)
        }
    }

    companion object {
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.aciton.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
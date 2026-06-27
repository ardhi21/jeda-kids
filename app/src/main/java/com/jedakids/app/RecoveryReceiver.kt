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

        val nowMillis = System.currentTimeMillis()
        val lockAtMillis = endAtMillis + TimerScheduler.LOCK_GRACE_PERIOD_MILLIS

        when {
            endAtMillis > nowMillis -> {
                if (TimerScheduler.canScheduleExactAlarms(context)) {
                    TimerScheduler.schedule(context, endAtMillis)
                }
            }

            lockAtMillis > nowMillis -> {
                TimeUpNotifier.showTimeUpNotification(context, lockAtMillis)
                if (TimerScheduler.canScheduleExactAlarms(context)) {
                    TimerScheduler.scheduleLock(context, lockAtMillis)
                } else {
                    finishExpiredSession(context, preferences)
                }
            }

            else -> finishExpiredSession(context, preferences)
        }
    }

    private fun finishExpiredSession(
        context: Context,
        preferences: TimerPreferences,
    ) {
        preferences.clearSession()
        TimerScheduler.cancel(context)
        TimeUpNotifier.cancel(context)
        LockController.lockNow(context)
    }

    companion object {
        private const val ACTION_EXACT_ALARM_PERMISSION_CHANGED =
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    }
}
package com.jedakids.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerScheduler.ACTION_TIME_UP, null -> handleTimeUp(context)
            TimerScheduler.ACTION_LOCK_DEVICE -> handleLock(context)
        }
    }

    private fun handleTimeUp(context: Context) {
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

        val lockAtMillis = endAtMillis + TimerScheduler.LOCK_GRACE_PERIOD_MILLIS
        if (lockAtMillis <= nowMillis) {
            completeLock(context, preferences)
            return
        }

        TimeUpNotifier.showTimeUpNotification(context, lockAtMillis)

        if (TimerScheduler.canScheduleExactAlarms(context)) {
            TimerScheduler.scheduleLock(context, lockAtMillis)
        } else {
            completeLock(context, preferences)
        }
    }

    private fun handleLock(context: Context) {
        val preferences = TimerPreferences(context)
        val endAtMillis = preferences.getEndAtMillis()

        if (endAtMillis == TimerPreferences.NO_ACTIVE_SESSION) {
            TimeUpNotifier.cancel(context)
            return
        }

        val lockAtMillis = endAtMillis + TimerScheduler.LOCK_GRACE_PERIOD_MILLIS
        val nowMillis = System.currentTimeMillis()

        if (lockAtMillis > nowMillis + EARLY_DELIVERY_TOLERANCE_MILLIS) {
            if (TimerScheduler.canScheduleExactAlarms(context)) {
                TimerScheduler.scheduleLock(context, lockAtMillis)
            }
            return
        }

        completeLock(context, preferences)
    }

    private fun completeLock(
        context: Context,
        preferences: TimerPreferences,
    ) {
        preferences.clearSession()
        TimerScheduler.cancel(context)
        TimeUpNotifier.cancel(context)
        LockController.lockNow(context)
    }

    companion object {
        private const val EARLY_DELIVERY_TOLERANCE_MILLIS = 1_000L
    }
}
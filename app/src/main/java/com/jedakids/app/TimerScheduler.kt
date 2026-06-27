package com.jedakids.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerScheduler {
    const val ACTION_TIME_UP = "com.jedakids.app.action.TIME_UP"
    const val ACTION_LOCK_DEVICE = "com.jedakids.app.action.LOCK_DEVICE"
    const val LOCK_GRACE_PERIOD_MILLIS = 10_000L

    private const val TIME_UP_REQUEST_CODE = 4107
    private const val LOCK_REQUEST_CODE = 4108

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, endAtMillis: Long) {
        check(canScheduleExactAlarms(context)) {
            "Alarms & reminder access has not been granted."
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        cancel(context)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endAtMillis,
            timeUpPendingIntent(context),
        )
    }

    fun scheduleLock(context: Context, lockAtMillis: Long) {
        check(canScheduleExactAlarms(context)) {
            "Alarms & reminders access has not been granted."
        }

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            lockAtMillis,
            lockPendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val timeUpPendingIntent = timeUpPendingIntent(context)
        alarmManager.cancel(timeUpPendingIntent)
        timeUpPendingIntent.cancel()

        val lockPendingIntent = lockPendingIntent(context)
        alarmManager.cancel(lockPendingIntent)
        lockPendingIntent.cancel()

        cancelLegacyAlarm(context, alarmManager)
    }

    private fun timeUpPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TIME_UP
        }
        return PendingIntent.getBroadcast(
            context,
            TIME_UP_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun lockPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_LOCK_DEVICE
        }
        return PendingIntent.getBroadcast(
            context,
            LOCK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelLegacyAlarm(
        context: Context,
        alarmManager: AlarmManager,
    ) {
        val legacyIntent = Intent(context, AlarmReceiver::class.java)
        val legacyPendingIntent = PendingIntent.getBroadcast(
            context,
            TIME_UP_REQUEST_CODE,
            legacyIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

        if (legacyPendingIntent != null) {
            alarmManager.cancel(legacyPendingIntent)
            legacyPendingIntent.cancel()
        }
    }
}
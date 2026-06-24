package com.jedakids.app

import android.content.Context
import androidx.core.content.edit

class TimerPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun getEndAtMillis(): Long {
        return preferences.getLong(KEY_END_AT_MILLIS, NO_ACTIVE_SESSION)
    }

    fun saveEndAtMillis(endAtMillis: Long) {
        preferences.edit {
            putLong(KEY_END_AT_MILLIS, endAtMillis)
        }
    }

    fun clearSession() {
        preferences.edit {
            remove(KEY_END_AT_MILLIS)
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "jeda_kids_timer"
        private const val KEY_END_AT_MILLIS = "end_at_millis"
        const val NO_ACTIVE_SESSION = 0L
    }
}
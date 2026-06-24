package com.jedakids.app

object TimerMath {
    const val MIN_SESSION_MINUTES = 1
    const val MAX_SESSION_MINUTES = 480

    fun durationMillis(minutes: Int): Long {
        require(minutes in MIN_SESSION_MINUTES..MAX_SESSION_MINUTES) {
            "Session length must be between $MIN_SESSION_MINUTES and $MAX_SESSION_MINUTES minutes."
        }
        return minutes * 60_000L
    }

    fun remainingMillis(endAtMillis: Long, nowMillis: Long): Long {
        return (endAtMillis - nowMillis).coerceAtLeast(0L)
    }

    fun formatRemaining(remainingMillis: Long): String {
        val roundedSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
        val hours = roundedSeconds / 3_600L
        val minutes = (roundedSeconds % 3_600L) / 60L
        val seconds = roundedSeconds % 60L

        return if (hours > 0L) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}
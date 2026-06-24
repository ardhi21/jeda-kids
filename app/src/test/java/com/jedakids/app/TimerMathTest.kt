package com.jedakids.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimerMathTest {
    @Test
    fun durationMillis_convertsMinutesToMilliseconds() {
        assertEquals(60_000L, TimerMath.durationMillis(1))
        assertEquals(3_600_000L, TimerMath.durationMillis(60))
        assertEquals(28_800_000L, TimerMath.durationMillis(480))
    }

    @Test
    fun durationMillis_rejectsValuesOutsideAllowedRange() {
        assertThrows(IllegalArgumentException::class.java) {
            TimerMath.durationMillis(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            TimerMath.durationMillis(481)
        }
    }

    @Test
    fun remainingMillis_neverReturnsNegativeTime() {
        assertEquals(5_000L, TimerMath.remainingMillis(15_000L, 10_000L))
        assertEquals(0L, TimerMath.remainingMillis(10_000L, 15_000L))
    }

    @Test
    fun formatRemaining_formatsMinutesAndSeconds() {
        assertEquals("00:00", TimerMath.formatRemaining(0L))
        assertEquals("00:01", TimerMath.formatRemaining(1L))
        assertEquals("01:00", TimerMath.formatRemaining(60_000L))
        assertEquals("01:01", TimerMath.formatRemaining(60_001L))
    }

    @Test
    fun formatRemaining_formatsHours() {
        assertEquals("01:00:00", TimerMath.formatRemaining(3_600_000L))
        assertEquals("02:05:09", TimerMath.formatRemaining(7_509_000L))
    }
}
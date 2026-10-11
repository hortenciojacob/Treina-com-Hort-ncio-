package com.hortencio.treinacomhortencio.data

import com.hortencio.treinacomhortencio.ui.EnergyLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckInTest {
    @Test
    fun checkInIsRestoredOnTheDayItWasSaved() {
        val checkIn = DailyCheckIn("2026-10-10", EnergyLevel.LOW, 8)

        assertTrue(checkIn.isForDay("2026-10-10"))
        assertEquals(EnergyLevel.LOW, checkIn.energy)
        assertEquals(8, checkIn.availableMinutes)
    }

    @Test
    fun checkInExpiresWhenTheLocalDayChanges() {
        val checkIn = DailyCheckIn("2026-10-10", EnergyLevel.HIGH, 25)

        assertFalse(checkIn.isForDay("2026-10-11"))
    }

    @Test
    fun habitStatusDistinguishesNoProgressPartialAndCompletedSessions() {
        assertEquals(HabitStatus.NONE, habitStatusFor(0, 3))
        assertEquals(HabitStatus.PARTIAL, habitStatusFor(1, 3))
        assertEquals(HabitStatus.COMPLETED, habitStatusFor(3, 3))
    }

    @Test(expected = IllegalArgumentException::class)
    fun habitStatusRejectsInvalidExerciseCounts() {
        habitStatusFor(4, 3)
    }

    @Test
    fun streakContinuesFromYesterdayUntilTheCurrentDayIsCompleted() {
        val completedDays = setOf("2026-10-08", "2026-10-09", "2026-10-10")

        assertEquals(3, currentStreakFor(completedDays, "2026-10-11"))
        assertEquals(0, currentStreakFor(completedDays, "2026-10-12"))
    }

    @Test
    fun streakIncludesTodayWhenThereIsACompletedSessionToday() {
        val completedDays = setOf("2026-10-09", "2026-10-10", "2026-10-11")

        assertEquals(3, currentStreakFor(completedDays, "2026-10-11"))
    }

    @Test
    fun undoRemovesTheMostRecentWaterAdditionIncludingCappedAmounts() {
        val afterAddition = addWaterToLog(currentMl = 400, amountMl = 500)

        assertEquals(WaterLogState(900, 500), afterAddition)
        assertEquals(WaterLogState(400, 0), undoWaterLogAdd(afterAddition))
        assertEquals(WaterLogState(10_000, 100), addWaterToLog(9_900, 500))
    }
}
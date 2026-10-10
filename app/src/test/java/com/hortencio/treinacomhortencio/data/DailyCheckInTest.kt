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
}
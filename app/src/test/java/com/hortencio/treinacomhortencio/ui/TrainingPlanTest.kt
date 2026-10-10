package com.hortencio.treinacomhortencio.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingPlanTest {
    private val profile = TrainingProfile(
        goal = TrainingGoal.STRENGTH,
        experience = ExperienceLevel.NEW,
        hasChair = false,
        hasBackpack = false
    )

    @Test
    fun lowEnergyOffersShortGentlePlanEvenWhenMoreTimeIsAvailable() {
        val plan = createWorkoutPlan(profile, availableMinutes = 25, energy = EnergyLevel.LOW)

        assertEquals("Movimento leve", plan.title)
        assertEquals(8, plan.durationMinutes)
        assertEquals(2, plan.exercises.size)
        assertTrue(plan.exercises.all { it.prescription.contains("confortável") })
    }

    @Test
    fun availableTimeChangesTheNumberOfExercises() {
        val shortPlan = createWorkoutPlan(profile, availableMinutes = 8, energy = EnergyLevel.STEADY)
        val longerPlan = createWorkoutPlan(profile, availableMinutes = 25, energy = EnergyLevel.STEADY)

        assertEquals(2, shortPlan.exercises.size)
        assertEquals(5, longerPlan.exercises.size)
    }

    @Test
    fun availableEquipmentChangesExerciseVariations() {
        val equippedProfile = profile.copy(hasChair = true, hasBackpack = true)
        val plan = createWorkoutPlan(equippedProfile, availableMinutes = 25, energy = EnergyLevel.STEADY)

        assertTrue(plan.exercises.any { it.name == "Agachamento até à cadeira" })
        assertTrue(plan.exercises.any { it.name == "Remada com mochila leve" })
    }

    @Test
    fun regularExperienceGetsMoreRepetitions() {
        val regularProfile = profile.copy(experience = ExperienceLevel.REGULAR)
        val newPlan = createWorkoutPlan(profile, availableMinutes = 25, energy = EnergyLevel.STEADY)
        val regularPlan = createWorkoutPlan(regularProfile, availableMinutes = 25, energy = EnergyLevel.STEADY)

        assertTrue(newPlan.exercises.first().prescription.contains("8 repetições"))
        assertTrue(regularPlan.exercises.first().prescription.contains("10 repetições"))
    }

    @Test
    fun highEnergyIncreasesDoseForRegularExperience() {
        val regularProfile = profile.copy(experience = ExperienceLevel.REGULAR)
        val plan = createWorkoutPlan(regularProfile, availableMinutes = 25, energy = EnergyLevel.HIGH)

        assertTrue(plan.exercises.first().prescription.contains("3 séries · 12 repetições"))
    }

    @Test
    fun exerciseVariationCanBeSwappedAndRestored() {
        val exercise = createWorkoutPlan(profile, availableMinutes = 15, energy = EnergyLevel.STEADY).exercises.first()
        val alternate = exercise.toggleAlternative()!!

        assertNotEquals(exercise.name, alternate.name)
        assertEquals(exercise, alternate.toggleAlternative())
    }

    @Test
    fun difficultFeedbackReducesTheNextStrengthDose() {
        val plan = createWorkoutPlan(
            profile,
            availableMinutes = 25,
            energy = EnergyLevel.STEADY,
            previousFeedback = SessionFeedback.TOO_DEMANDING
        )

        assertTrue(plan.exercises.first().prescription.contains("1 séries · 6 repetições"))
    }

    @Test
    fun easyFeedbackIncreasesTheNextStrengthDoseGradually() {
        val plan = createWorkoutPlan(
            profile,
            availableMinutes = 25,
            energy = EnergyLevel.STEADY,
            previousFeedback = SessionFeedback.COULD_DO_MORE
        )

        assertTrue(plan.exercises.first().prescription.contains("3 séries · 9 repetições"))
    }

    @Test
    fun lowEnergyStillUsesTheGentlePlanAfterEasyFeedback() {
        val plan = createWorkoutPlan(
            profile,
            availableMinutes = 25,
            energy = EnergyLevel.LOW,
            previousFeedback = SessionFeedback.COULD_DO_MORE
        )

        assertEquals("Movimento leve", plan.title)
        assertEquals(8, plan.durationMinutes)
        assertTrue(plan.exercises.all { it.prescription.contains("ritmo confortável") })
    }
}
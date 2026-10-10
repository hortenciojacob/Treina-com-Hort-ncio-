package com.hortencio.treinacomhortencio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.hortencio.treinacomhortencio.ui.ExperienceLevel
import com.hortencio.treinacomhortencio.ui.ExerciseVariation
import com.hortencio.treinacomhortencio.ui.EnergyLevel
import com.hortencio.treinacomhortencio.ui.PlannedExercise
import com.hortencio.treinacomhortencio.ui.SessionFeedback
import com.hortencio.treinacomhortencio.ui.TrainingGoal
import com.hortencio.treinacomhortencio.ui.TrainingProfile
import com.hortencio.treinacomhortencio.ui.WorkoutPlan

data class SessionHistoryEntry(
    val completedAt: Long,
    val title: String,
    val completedExercises: Int,
    val totalExercises: Int,
    val goal: TrainingGoal? = null,
    val feedback: SessionFeedback? = null
)

data class PausedWorkout(
    val plan: WorkoutPlan,
    val completedExerciseIds: Set<String>
)

data class DailyCheckIn(
    val dayKey: String,
    val energy: EnergyLevel,
    val availableMinutes: Int
) {
    fun isForDay(today: String): Boolean = dayKey == today
}

class TrainingProfileStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "training_profile",
        Context.MODE_PRIVATE
    )

    fun loadProfile(): TrainingProfile? {
        val goalName = preferences.getString(KEY_GOAL, null) ?: return null
        val experienceName = preferences.getString(KEY_EXPERIENCE, null) ?: return null
        val goal = runCatching { TrainingGoal.valueOf(goalName) }.getOrNull() ?: return null
        val experience = runCatching { ExperienceLevel.valueOf(experienceName) }.getOrNull() ?: return null

        return TrainingProfile(
            goal = goal,
            experience = experience,
            hasChair = preferences.getBoolean(KEY_HAS_CHAIR, false),
            hasBackpack = preferences.getBoolean(KEY_HAS_BACKPACK, false)
        )
    }

    fun saveProfile(profile: TrainingProfile) {
        preferences.edit()
            .putString(KEY_GOAL, profile.goal.name)
            .putString(KEY_EXPERIENCE, profile.experience.name)
            .putBoolean(KEY_HAS_CHAIR, profile.hasChair)
            .putBoolean(KEY_HAS_BACKPACK, profile.hasBackpack)
            .apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    fun loadDailyCheckIn(today: String = currentDayKey()): DailyCheckIn? {
        val savedDay = preferences.getString(KEY_CHECK_IN_DAY, null) ?: return null
        val energyName = preferences.getString(KEY_CHECK_IN_ENERGY, null) ?: return null
        val energy = runCatching { EnergyLevel.valueOf(energyName) }.getOrNull() ?: return null
        val availableMinutes = preferences.getInt(KEY_CHECK_IN_MINUTES, 0)
        if (availableMinutes !in 5..60) return null
        return DailyCheckIn(savedDay, energy, availableMinutes).takeIf { it.isForDay(today) }
    }

    fun saveDailyCheckIn(energy: EnergyLevel, availableMinutes: Int, today: String = currentDayKey()) {
        require(availableMinutes in 5..60)
        preferences.edit()
            .putString(KEY_CHECK_IN_DAY, today)
            .putString(KEY_CHECK_IN_ENERGY, energy.name)
            .putInt(KEY_CHECK_IN_MINUTES, availableMinutes)
            .apply()
    }

    fun completedSessionCount(): Int = preferences.getInt(KEY_COMPLETED_SESSIONS, 0)

    fun recordSession(
        title: String,
        completedExercises: Int,
        totalExercises: Int,
        goal: TrainingGoal? = null,
        feedback: SessionFeedback? = null,
        completedAt: Long = System.currentTimeMillis()
    ) {
        require(totalExercises > 0)
        require(completedExercises in 0..totalExercises)

        val entry = SessionHistoryEntry(completedAt, title, completedExercises, totalExercises, goal, feedback)
        val updatedHistory = (readHistory() + entry).takeLast(MAX_HISTORY_SIZE)
        val historyJson = JSONArray().apply {
            updatedHistory.forEach { session ->
                put(
                    JSONObject()
                        .put(KEY_TIMESTAMP, session.completedAt)
                        .put(KEY_TITLE, session.title)
                        .put(KEY_DONE, session.completedExercises)
                        .put(KEY_TOTAL, session.totalExercises)
                        .put(KEY_GOAL, session.goal?.name ?: JSONObject.NULL)
                        .put(KEY_FEEDBACK, session.feedback?.name ?: JSONObject.NULL)
                )
            }
        }
        val editor = preferences.edit().putString(KEY_SESSION_HISTORY, historyJson.toString())
        if (completedExercises == totalExercises) {
            editor.putInt(KEY_COMPLETED_SESSIONS, completedSessionCount() + 1)
        }
        editor.apply()
    }

    fun sessionsInLastSevenDays(now: Long = System.currentTimeMillis()): Int {
        val weekAgo = now - SEVEN_DAYS_MILLIS
        return readHistory().count { it.completedAt in weekAgo..now }
    }

    fun recentSessions(limit: Int = 3): List<SessionHistoryEntry> =
        readHistory().sortedByDescending { it.completedAt }.take(limit)

    fun latestCompletedFeedback(goal: TrainingGoal): SessionFeedback? = readHistory()
        .filter { it.completedExercises == it.totalExercises && it.goal == goal }
        .maxByOrNull { it.completedAt }
        ?.feedback

    fun savePausedWorkout(plan: WorkoutPlan, completedExerciseIds: Set<String>) {
        val exercisesJson = JSONArray().apply {
            plan.exercises.forEach { exercise ->
                val alternativeJson = exercise.alternative?.let { alternative ->
                    JSONObject()
                        .put(KEY_NAME, alternative.name)
                        .put(KEY_INSTRUCTION, alternative.instruction)
                }
                put(
                    JSONObject()
                        .put(KEY_ID, exercise.id)
                        .put(KEY_NAME, exercise.name)
                        .put(KEY_PRESCRIPTION, exercise.prescription)
                        .put(KEY_INSTRUCTION, exercise.instruction)
                        .put(KEY_ALTERNATIVE, alternativeJson ?: JSONObject.NULL)
                )
            }
        }
        val pausedJson = JSONObject()
            .put(KEY_TITLE, plan.title)
            .put(KEY_DESCRIPTION, plan.description)
            .put(KEY_DURATION, plan.durationMinutes)
            .put(KEY_GOAL, plan.goal.name)
            .put(KEY_EXERCISES, exercisesJson)
            .put(KEY_COMPLETED_IDS, JSONArray().apply {
                completedExerciseIds.forEach(::put)
            })
        preferences.edit().putString(KEY_PAUSED_WORKOUT, pausedJson.toString()).apply()
    }

    fun loadPausedWorkout(): PausedWorkout? {
        val pausedValue = preferences.getString(KEY_PAUSED_WORKOUT, null) ?: return null
        return runCatching {
            val pausedJson = JSONObject(pausedValue)
            val exercisesJson = pausedJson.optJSONArray(KEY_EXERCISES) ?: return@runCatching null
            val exercises = buildList {
                for (index in 0 until exercisesJson.length()) {
                    val exerciseJson = exercisesJson.optJSONObject(index) ?: continue
                    val id = exerciseJson.optString(KEY_ID).takeIf { it.isNotBlank() } ?: continue
                    val name = exerciseJson.optString(KEY_NAME).takeIf { it.isNotBlank() } ?: continue
                    val alternativeJson = exerciseJson.optJSONObject(KEY_ALTERNATIVE)
                    val alternative = alternativeJson?.let {
                        val alternativeName = it.optString(KEY_NAME).takeIf(String::isNotBlank)
                        val alternativeInstruction = it.optString(KEY_INSTRUCTION)
                        alternativeName?.let { validName -> ExerciseVariation(validName, alternativeInstruction) }
                    }
                    add(
                        PlannedExercise(
                            id = id,
                            name = name,
                            prescription = exerciseJson.optString(KEY_PRESCRIPTION),
                            instruction = exerciseJson.optString(KEY_INSTRUCTION),
                            alternative = alternative
                        )
                    )
                }
            }
            if (exercises.isEmpty()) return@runCatching null
            val completedJson = pausedJson.optJSONArray(KEY_COMPLETED_IDS) ?: JSONArray()
            val completedIds = buildSet {
                for (index in 0 until completedJson.length()) {
                    completedJson.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
            PausedWorkout(
                plan = WorkoutPlan(
                    title = pausedJson.optString(KEY_TITLE),
                    description = pausedJson.optString(KEY_DESCRIPTION),
                    durationMinutes = pausedJson.optInt(KEY_DURATION, 0),
                    goal = runCatching { TrainingGoal.valueOf(pausedJson.optString(KEY_GOAL)) }
                        .getOrDefault(TrainingGoal.STRENGTH),
                    exercises = exercises
                ),
                completedExerciseIds = completedIds.intersect(exercises.map { it.id }.toSet())
            )
        }.getOrNull()
    }

    fun clearPausedWorkout() {
        preferences.edit().remove(KEY_PAUSED_WORKOUT).apply()
    }

    private fun readHistory(): List<SessionHistoryEntry> {
        val historyJson = preferences.getString(KEY_SESSION_HISTORY, null) ?: return emptyList()
        val array = runCatching { JSONArray(historyJson) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val title = item.optString(KEY_TITLE).takeIf { it.isNotBlank() } ?: continue
                val total = item.optInt(KEY_TOTAL, 0)
                val completed = item.optInt(KEY_DONE, -1)
                if (total <= 0 || completed !in 0..total) continue
                val goalName = item.optString(KEY_GOAL).takeUnless { it.isBlank() || it == "null" }
                val goal = goalName?.let { runCatching { TrainingGoal.valueOf(it) }.getOrNull() }
                val feedbackName = item.optString(KEY_FEEDBACK).takeUnless { it.isBlank() || it == "null" }
                val feedback = feedbackName?.let { runCatching { SessionFeedback.valueOf(it) }.getOrNull() }
                add(
                    SessionHistoryEntry(
                        completedAt = item.optLong(KEY_TIMESTAMP, 0L),
                        title = title,
                        completedExercises = completed,
                        totalExercises = total,
                        goal = goal,
                        feedback = feedback
                    )
                )
            }
        }
    }

    private fun currentDayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())

    private companion object {
        const val KEY_GOAL = "goal"
        const val KEY_EXPERIENCE = "experience"
        const val KEY_HAS_CHAIR = "has_chair"
        const val KEY_HAS_BACKPACK = "has_backpack"
        const val KEY_CHECK_IN_DAY = "check_in_day"
        const val KEY_CHECK_IN_ENERGY = "check_in_energy"
        const val KEY_CHECK_IN_MINUTES = "check_in_minutes"
        const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        const val KEY_SESSION_HISTORY = "session_history"
        const val KEY_TIMESTAMP = "completed_at"
        const val KEY_TITLE = "title"
        const val KEY_DONE = "completed_exercises"
        const val KEY_TOTAL = "total_exercises"
        const val KEY_FEEDBACK = "feedback"
        const val KEY_PAUSED_WORKOUT = "paused_workout"
        const val KEY_DESCRIPTION = "description"
        const val KEY_DURATION = "duration"
        const val KEY_EXERCISES = "exercises"
        const val KEY_COMPLETED_IDS = "completed_ids"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_INSTRUCTION = "instruction"
        const val KEY_PRESCRIPTION = "prescription"
        const val KEY_ALTERNATIVE = "alternative"
        const val MAX_HISTORY_SIZE = 100
        const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}
package com.hortencio.treinacomhortencio.data

import android.content.Context
import androidx.core.content.edit
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
import com.hortencio.treinacomhortencio.ui.SessionMood
import com.hortencio.treinacomhortencio.ui.TrainingGoal
import com.hortencio.treinacomhortencio.ui.TrainingProfile
import com.hortencio.treinacomhortencio.ui.WorkoutPlan

data class SessionHistoryEntry(
    val completedAt: Long,
    val title: String,
    val completedExercises: Int,
    val totalExercises: Int,
    val goal: TrainingGoal? = null,
    val feedback: SessionFeedback? = null,
    val mood: SessionMood? = null
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

enum class HabitStatus {
    NONE,
    PARTIAL,
    COMPLETED
}

fun habitStatusFor(completedExercises: Int, totalExercises: Int): HabitStatus {
    require(totalExercises > 0)
    require(completedExercises in 0..totalExercises)
    return when {
        completedExercises == totalExercises -> HabitStatus.COMPLETED
        completedExercises > 0 -> HabitStatus.PARTIAL
        else -> HabitStatus.NONE
    }
}

data class WaterLogState(val intakeMl: Int, val lastAddedMl: Int)

fun addWaterToLog(currentMl: Int, amountMl: Int): WaterLogState {
    require(currentMl in 0..10_000)
    require(amountMl in 50..1000)
    val total = (currentMl + amountMl).coerceAtMost(10_000)
    return WaterLogState(total, total - currentMl)
}

fun undoWaterLogAdd(state: WaterLogState): WaterLogState {
    require(state.intakeMl in 0..10_000)
    require(state.lastAddedMl in 0..state.intakeMl)
    return WaterLogState(state.intakeMl - state.lastAddedMl, 0)
}

fun currentStreakFor(completedDayKeys: Set<String>, todayKey: String): Int {
    val yesterdayKey = previousDayKey(todayKey)
    require(yesterdayKey != todayKey) { "Today must be a valid yyyy-MM-dd day key" }
    var cursor = when {
        todayKey in completedDayKeys -> todayKey
        yesterdayKey in completedDayKeys -> yesterdayKey
        else -> return 0
    }
    var streak = 0
    while (cursor in completedDayKeys) {
        streak += 1
        cursor = previousDayKey(cursor)
    }
    return streak
}

private fun previousDayKey(dayKey: String): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
    val date = formatter.parse(dayKey) ?: return dayKey
    val calendar = java.util.Calendar.getInstance().apply {
        time = date
        add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    return formatter.format(calendar.time)
}

data class HabitDay(val label: String, val status: HabitStatus)

data class MoodDay(val label: String, val mood: SessionMood?)

data class MonthActivity(val label: String, val sessions: Int)

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
            hasBackpack = preferences.getBoolean(KEY_HAS_BACKPACK, false),
            prefersNoFloorExercises = preferences.getBoolean(KEY_PREFERS_NO_FLOOR_EXERCISES, false)
        )
    }

    fun saveProfile(profile: TrainingProfile) {
        preferences.edit {
            putString(KEY_GOAL, profile.goal.name)
            putString(KEY_EXPERIENCE, profile.experience.name)
            putBoolean(KEY_HAS_CHAIR, profile.hasChair)
            putBoolean(KEY_HAS_BACKPACK, profile.hasBackpack)
            putBoolean(KEY_PREFERS_NO_FLOOR_EXERCISES, profile.prefersNoFloorExercises)
        }
    }

    fun clearAll() {
        preferences.edit { clear() }
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
        preferences.edit {
            putString(KEY_CHECK_IN_DAY, today)
            putString(KEY_CHECK_IN_ENERGY, energy.name)
            putInt(KEY_CHECK_IN_MINUTES, availableMinutes)
        }
    }

    fun waterIntake(today: String = currentDayKey()): Int {
        if (preferences.getString(KEY_WATER_DAY, null) != today) return 0
        return preferences.getInt(KEY_WATER_INTAKE, 0).coerceAtLeast(0)
    }

    fun addWater(amountMl: Int, today: String = currentDayKey()): Int {
        val current = waterIntake(today)
        val updated = addWaterToLog(current, amountMl)
        preferences.edit {
            putString(KEY_WATER_DAY, today)
            putInt(KEY_WATER_INTAKE, updated.intakeMl)
            putInt(KEY_WATER_LAST_ADD, updated.lastAddedMl)
        }
        return updated.intakeMl
    }

    fun setWaterIntake(amountMl: Int, today: String = currentDayKey()) {
        require(amountMl in 0..10_000)
        preferences.edit {
            putString(KEY_WATER_DAY, today)
            putInt(KEY_WATER_INTAKE, amountMl)
            remove(KEY_WATER_LAST_ADD)
        }
    }

    fun undoLastWaterAdd(today: String = currentDayKey()): Int {
        val isToday = preferences.getString(KEY_WATER_DAY, null) == today
        val current = waterIntake(today)
        val lastAdded = if (isToday) {
            preferences.getInt(KEY_WATER_LAST_ADD, 0).coerceIn(0, current)
        } else {
            0
        }
        val updated = undoWaterLogAdd(WaterLogState(current, lastAdded))
        preferences.edit {
            putInt(KEY_WATER_INTAKE, updated.intakeMl)
            remove(KEY_WATER_LAST_ADD)
        }
        return updated.intakeMl
    }

    fun canUndoLastWaterAdd(today: String = currentDayKey()): Boolean =
        preferences.getString(KEY_WATER_DAY, null) == today &&
            preferences.getInt(KEY_WATER_LAST_ADD, 0) > 0

    fun waterGoal(): Int = preferences.getInt(KEY_WATER_GOAL, 2000).coerceIn(500, 5000)

    fun saveWaterGoal(amountMl: Int) {
        require(amountMl in 500..5000)
        preferences.edit { putInt(KEY_WATER_GOAL, amountMl) }
    }

    fun waterReminderIntervalMinutes(): Int = preferences.getInt(KEY_WATER_REMINDER_INTERVAL, 0)
        .takeIf { it in WATER_REMINDER_INTERVALS } ?: 0

    fun saveWaterReminderInterval(minutes: Int) {
        require(minutes == 0 || minutes in WATER_REMINDER_INTERVALS)
        preferences.edit { putInt(KEY_WATER_REMINDER_INTERVAL, minutes) }
    }

    fun completedSessionCount(): Int = preferences.getInt(KEY_COMPLETED_SESSIONS, 0)

    fun loadWeeklyGoal(): Int = preferences.getInt(KEY_WEEKLY_GOAL, 3)

    fun saveWeeklyGoal(target: Int) {
        require(target in 1..7)
        preferences.edit { putInt(KEY_WEEKLY_GOAL, target) }
    }

    fun loadMonthlyGoal(): Int = preferences.getInt(KEY_MONTHLY_GOAL, 12)

    fun saveMonthlyGoal(target: Int) {
        require(target in 1..31)
        preferences.edit { putInt(KEY_MONTHLY_GOAL, target) }
    }

    fun weeklyGoalProgress(now: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val weekStart = startOfWeek(now)
        val sessionsThisWeek = readHistory()
            .count { it.completedExercises == it.totalExercises && it.completedAt >= weekStart && it.completedAt <= now }
        return sessionsThisWeek to loadWeeklyGoal()
    }

    fun monthProgress(now: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val monthStart = startOfMonth(now)
        val sessionsThisMonth = readHistory()
            .count { it.completedExercises == it.totalExercises && it.completedAt >= monthStart && it.completedAt <= now }
        return sessionsThisMonth to loadMonthlyGoal()
    }

    fun monthActivity(now: Long = System.currentTimeMillis(), monthCount: Int = 6): List<MonthActivity> {
        require(monthCount > 0)
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val months = mutableListOf<Pair<String, Pair<Long, Long>>>()
        repeat(monthCount) {
            val monthStart = calendar.timeInMillis
            val nextMonth = (calendar.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.MONTH, 1)
            }.timeInMillis
            val label = SimpleDateFormat("MMM", Locale.forLanguageTag("pt-PT"))
                .format(Date(monthStart))
                .replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-PT")) }
            months.add(label to (monthStart to nextMonth))
            calendar.add(java.util.Calendar.MONTH, -1)
        }
        val completedSessions = readHistory()
            .filter { it.completedExercises == it.totalExercises }
        return months.asReversed().map { (label, range) ->
            MonthActivity(label, completedSessions.count { it.completedAt >= range.first && it.completedAt < range.second })
        }
    }

    fun habitCalendarDays(now: Long = System.currentTimeMillis()): List<HabitDay> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val days = mutableListOf<Pair<String, String>>()
        for (offset in 6 downTo 0) {
            val day = (calendar.clone() as java.util.Calendar).apply {
                add(java.util.Calendar.DAY_OF_YEAR, -offset)
            }
            val key = dayKey(day.timeInMillis)
            val label = SimpleDateFormat("EEEEE", Locale.forLanguageTag("pt-PT"))
                .format(day.time)
                .replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-PT")) }
            days.add(key to label)
        }
        val dailyStatus = readHistory()
            .groupBy { dayKey(it.completedAt) }
            .mapValues { (_, sessions) ->
                sessions
                    .map { habitStatusFor(it.completedExercises, it.totalExercises) }
                    .maxByOrNull { it.ordinal }
                    ?: HabitStatus.NONE
            }
        return days.map { (key, label) -> HabitDay(label, dailyStatus[key] ?: HabitStatus.NONE) }
    }

    fun weeklyMoodHistory(now: Long = System.currentTimeMillis()): List<MoodDay> {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = startOfWeek(now)
        val moodsByDay = readHistory()
            .filter { it.completedExercises == it.totalExercises && it.mood != null }
            .groupBy { dayKey(it.completedAt) }
            .mapValues { (_, sessions) -> sessions.maxByOrNull { it.completedAt }?.mood }
        return (0..6).map {
            val date = calendar.time
            val key = dayKey(date.time)
            val label = SimpleDateFormat("EEEEE", Locale.forLanguageTag("pt-PT"))
                .format(date)
                .replaceFirstChar { it.uppercase(Locale.forLanguageTag("pt-PT")) }
            MoodDay(label, moodsByDay[key])
                .also { calendar.add(java.util.Calendar.DAY_OF_YEAR, 1) }
        }
    }

    fun recordSession(
        title: String,
        completedExercises: Int,
        totalExercises: Int,
        goal: TrainingGoal? = null,
        feedback: SessionFeedback? = null,
        mood: SessionMood? = null,
        completedAt: Long = System.currentTimeMillis()
    ) {
        require(totalExercises > 0)
        require(completedExercises in 0..totalExercises)

        val entry = SessionHistoryEntry(completedAt, title, completedExercises, totalExercises, goal, feedback, mood)
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
                        .put(KEY_MOOD, session.mood?.name ?: JSONObject.NULL)
                )
            }
        }
        preferences.edit {
            putString(KEY_SESSION_HISTORY, historyJson.toString())
            if (completedExercises == totalExercises) {
                putInt(KEY_COMPLETED_SESSIONS, completedSessionCount() + 1)
            }
        }
    }

    fun sessionsInLastSevenDays(now: Long = System.currentTimeMillis()): Int {
        val weekAgo = now - SEVEN_DAYS_MILLIS
        return readHistory().count { it.completedAt in weekAgo..now }
    }

    fun weeklyHabitScore(now: Long = System.currentTimeMillis()): Int {
        val recent = readHistory().filter { it.completedAt in now - SEVEN_DAYS_MILLIS..now }
        val completed = recent.count { it.completedExercises == it.totalExercises }
        val strength = if (recent.isEmpty()) 0 else ((completed.toDouble() / recent.size.toDouble()) * 100).toInt()
        val streak = currentStreakDays(now)
        val streakBonus = minOf(streak * 7, 25)
        return (strength * 0.7 + streakBonus).toInt().coerceIn(0, 100)
    }

    fun currentStreakDays(now: Long = System.currentTimeMillis()): Int {
        val completedDays = readHistory()
            .filter { it.completedExercises == it.totalExercises }
            .map { dayKey(it.completedAt) }
            .toSet()
        return currentStreakFor(completedDays, dayKey(now))
    }

    fun recentSessions(limit: Int = 3): List<SessionHistoryEntry> =
        readHistory().sortedByDescending { it.completedAt }.take(limit)

    fun latestCompletedFeedback(goal: TrainingGoal): SessionFeedback? = readHistory()
        .filter { it.completedExercises == it.totalExercises && it.goal == goal }
        .maxByOrNull { it.completedAt }
        ?.feedback

    fun latestCompletedMood(goal: TrainingGoal): SessionMood? = readHistory()
        .filter { it.completedExercises == it.totalExercises && it.goal == goal }
        .maxByOrNull { it.completedAt }
        ?.mood

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
            .put(KEY_REASON, plan.reason)
            .put(KEY_EXERCISES, exercisesJson)
            .put(KEY_COMPLETED_IDS, JSONArray().apply {
                completedExerciseIds.forEach(::put)
            })
        preferences.edit { putString(KEY_PAUSED_WORKOUT, pausedJson.toString()) }
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
                    reason = pausedJson.optString(KEY_REASON),
                    exercises = exercises
                ),
                completedExerciseIds = completedIds.intersect(exercises.map { it.id }.toSet())
            )
        }.getOrNull()
    }

    fun clearPausedWorkout() {
        preferences.edit { remove(KEY_PAUSED_WORKOUT) }
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
                val moodName = item.optString(KEY_MOOD).takeUnless { it.isBlank() || it == "null" }
                val mood = moodName?.let { runCatching { SessionMood.valueOf(it) }.getOrNull() }
                add(
                    SessionHistoryEntry(
                        completedAt = item.optLong(KEY_TIMESTAMP, 0L),
                        title = title,
                        completedExercises = completed,
                        totalExercises = total,
                        goal = goal,
                        feedback = feedback,
                        mood = mood
                    )
                )
            }
        }
    }

    private fun currentDayKey(): String = dayKey(System.currentTimeMillis())

    private fun dayKey(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(timestamp))

    private fun startOfWeek(now: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.firstDayOfWeek = java.util.Calendar.MONDAY
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        return calendar.timeInMillis
    }

    private fun startOfMonth(now: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private companion object {
        const val KEY_GOAL = "goal"
        const val KEY_EXPERIENCE = "experience"
        const val KEY_HAS_CHAIR = "has_chair"
        const val KEY_HAS_BACKPACK = "has_backpack"
        const val KEY_PREFERS_NO_FLOOR_EXERCISES = "prefers_no_floor_exercises"
        const val KEY_CHECK_IN_DAY = "check_in_day"
        const val KEY_CHECK_IN_ENERGY = "check_in_energy"
        const val KEY_CHECK_IN_MINUTES = "check_in_minutes"
        const val KEY_WATER_DAY = "water_day"
        const val KEY_WATER_INTAKE = "water_intake"
        const val KEY_WATER_LAST_ADD = "water_last_add"
        const val KEY_WATER_GOAL = "water_goal"
        const val KEY_WATER_REMINDER_INTERVAL = "water_reminder_interval"
        const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        const val KEY_WEEKLY_GOAL = "weekly_goal"
        const val KEY_MONTHLY_GOAL = "monthly_goal"
        const val KEY_SESSION_HISTORY = "session_history"
        const val KEY_TIMESTAMP = "completed_at"
        const val KEY_TITLE = "title"
        const val KEY_DONE = "completed_exercises"
        const val KEY_TOTAL = "total_exercises"
        const val KEY_FEEDBACK = "feedback"
        const val KEY_MOOD = "mood"
        const val KEY_PAUSED_WORKOUT = "paused_workout"
        const val KEY_DESCRIPTION = "description"
        const val KEY_REASON = "reason"
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
        val WATER_REMINDER_INTERVALS = setOf(60, 90, 120, 180)
    }
}
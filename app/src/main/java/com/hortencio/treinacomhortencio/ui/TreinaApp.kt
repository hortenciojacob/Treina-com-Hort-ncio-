package com.hortencio.treinacomhortencio.ui

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.hortencio.treinacomhortencio.data.SessionHistoryEntry
import com.hortencio.treinacomhortencio.data.HabitDay
import com.hortencio.treinacomhortencio.data.HabitStatus
import com.hortencio.treinacomhortencio.data.MoodDay
import com.hortencio.treinacomhortencio.data.MonthActivity
import com.hortencio.treinacomhortencio.data.TrainingProfileStore
import com.hortencio.treinacomhortencio.notifications.WaterReminderScheduler
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

private enum class AppPage { SETUP, HOME, COACH, WATER, NUTRITION, SESSION, FEEDBACK }
private val PortugueseLocale = Locale.forLanguageTag("pt-PT")

@Composable
fun TreinaApp() {
    val context = LocalContext.current
    val profileStore = remember(context) { TrainingProfileStore(context) }
    val pausedWorkoutAtLaunch = remember(profileStore) { profileStore.loadPausedWorkout() }
    val dailyCheckInAtLaunch = remember(profileStore) { profileStore.loadDailyCheckIn() }
    var profile by remember(profileStore) { mutableStateOf(profileStore.loadProfile()) }
    var page by remember(profileStore) {
        mutableStateOf(if (profile == null) AppPage.SETUP else AppPage.HOME)
    }
    var energy by remember { mutableStateOf(dailyCheckInAtLaunch?.energy ?: EnergyLevel.STEADY) }
    var availableMinutes by remember { mutableIntStateOf(dailyCheckInAtLaunch?.availableMinutes ?: 15) }
    var waterIntake by remember(profileStore) { mutableIntStateOf(profileStore.waterIntake()) }
    var canUndoWaterAdd by remember(profileStore) { mutableStateOf(profileStore.canUndoLastWaterAdd()) }
    var waterGoal by remember(profileStore) { mutableIntStateOf(profileStore.waterGoal()) }
    var waterReminderInterval by remember(profileStore) { mutableIntStateOf(profileStore.waterReminderIntervalMinutes()) }
    var completedSessions by remember(profileStore) { mutableIntStateOf(profileStore.completedSessionCount()) }
    var sessionsLastSevenDays by remember(profileStore) { mutableIntStateOf(profileStore.sessionsInLastSevenDays()) }
    var streakDays by remember(profileStore) { mutableIntStateOf(profileStore.currentStreakDays()) }
    var habitScore by remember(profileStore) { mutableIntStateOf(profileStore.weeklyHabitScore()) }
    var weeklyGoal by remember(profileStore) { mutableIntStateOf(profileStore.loadWeeklyGoal()) }
    var weeklyProgress by remember(profileStore) { mutableStateOf(profileStore.weeklyGoalProgress()) }
    var monthlyGoal by remember(profileStore) { mutableIntStateOf(profileStore.loadMonthlyGoal()) }
    var monthProgress by remember(profileStore) { mutableStateOf(profileStore.monthProgress()) }
    var habitCalendar by remember(profileStore) { mutableStateOf(profileStore.habitCalendarDays()) }
    var weeklyMood by remember(profileStore) { mutableStateOf(profileStore.weeklyMoodHistory()) }
    var monthActivity by remember(profileStore) { mutableStateOf(profileStore.monthActivity()) }
    var recentSessions by remember(profileStore) { mutableStateOf(profileStore.recentSessions()) }
    var activePlan by remember { mutableStateOf(pausedWorkoutAtLaunch?.plan) }
    var pendingFeedbackPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var sessionPaused by remember { mutableStateOf(pausedWorkoutAtLaunch != null) }
    var restSecondsRemaining by remember { mutableIntStateOf(0) }
    var completionMessage by remember { mutableStateOf<String?>(null) }
    val completedExerciseIds = remember {
        mutableStateListOf<String>().apply {
            pausedWorkoutAtLaunch?.completedExerciseIds?.forEach { add(it) }
        }
    }
    val latestFeedback = remember(profileStore, profile?.goal, recentSessions) {
        profile?.goal?.let(profileStore::latestCompletedFeedback)
    }
    val latestMood = remember(profileStore, profile?.goal, recentSessions) {
        profile?.goal?.let(profileStore::latestCompletedMood)
    }
    fun storeSession(plan: WorkoutPlan, feedback: SessionFeedback?, mood: SessionMood? = null) {
        profileStore.recordSession(
            title = plan.title,
            completedExercises = completedExerciseIds.size,
            totalExercises = plan.exercises.size,
            goal = plan.goal,
            feedback = feedback,
            mood = mood
        )
        profileStore.clearPausedWorkout()
        completedSessions = profileStore.completedSessionCount()
        sessionsLastSevenDays = profileStore.sessionsInLastSevenDays()
        streakDays = profileStore.currentStreakDays()
        habitScore = profileStore.weeklyHabitScore()
        weeklyProgress = profileStore.weeklyGoalProgress()
        monthProgress = profileStore.monthProgress()
        habitCalendar = profileStore.habitCalendarDays()
        weeklyMood = profileStore.weeklyMoodHistory()
        monthActivity = profileStore.monthActivity()
        recentSessions = profileStore.recentSessions()
        completionMessage = if (completedExerciseIds.size == plan.exercises.size) {
            "Sessão registada. Obrigado por cuidares de ti."
        } else {
            "Progresso guardado. Cada parte conta."
        }
        activePlan = null
        pendingFeedbackPlan = null
        completedExerciseIds.clear()
        sessionPaused = false
        restSecondsRemaining = 0
    }
    val recommendation = remember(profile, availableMinutes, energy, latestFeedback) {
        profile?.let { createWorkoutPlan(it, availableMinutes, energy, latestFeedback) }
    }
    fun startChallenge(userProfile: TrainingProfile, challenge: ShortChallenge) {
        val challengePlan = createWorkoutPlan(
            userProfile,
            availableMinutes = 8,
            energy = energy,
            previousFeedback = latestFeedback
        ).let { base ->
            base.copy(
                title = challenge.title,
                description = challenge.encouragement,
                exercises = base.exercises.take(2)
            )
        }
        completedExerciseIds.clear()
        activePlan = challengePlan
        profileStore.savePausedWorkout(challengePlan, emptySet())
        sessionPaused = false
        restSecondsRemaining = 0
        completionMessage = null
        page = AppPage.SESSION
    }

    Scaffold(containerColor = Color(0xFFF6F8F5)) { padding ->
        when (page) {
            AppPage.SETUP -> SetupScreen(
                modifier = Modifier.padding(padding),
                initialProfile = profile,
                onCancel = { page = AppPage.HOME },
                onDeleteData = {
                    profileStore.clearAll()
                    profile = null
                    energy = EnergyLevel.STEADY
                    availableMinutes = 15
                    completedSessions = 0
                    sessionsLastSevenDays = 0
                    streakDays = 0
                    habitScore = 0
                    weeklyGoal = 3
                    weeklyProgress = 0 to 3
                    monthlyGoal = 12
                    monthProgress = 0 to 12
                    habitCalendar = emptyList()
                    weeklyMood = emptyList()
                    monthActivity = emptyList()
                    recentSessions = emptyList()
                    waterIntake = 0
                    canUndoWaterAdd = false
                    waterGoal = 2000
                    waterReminderInterval = 0
                    WaterReminderScheduler.cancel(context)
                    activePlan = null
                    pendingFeedbackPlan = null
                    completedExerciseIds.clear()
                    sessionPaused = false
                    restSecondsRemaining = 0
                    completionMessage = null
                    page = AppPage.SETUP
                },
                onSave = { updatedProfile ->
                    profileStore.saveProfile(updatedProfile)
                    profile = updatedProfile
                    page = AppPage.HOME
                }
            )
            AppPage.HOME -> {
                val currentProfile = profile
                val currentPlan = recommendation
                if (currentProfile != null && currentPlan != null) {
                    Dashboard(
                        modifier = Modifier.padding(padding),
                        profile = currentProfile,
                        plan = currentPlan,
                        energy = energy,
                        availableMinutes = availableMinutes,
                        completedSessions = completedSessions,
                        sessionsLastSevenDays = sessionsLastSevenDays,
                        streakDays = streakDays,
                        habitScore = habitScore,
                        weeklyGoal = weeklyGoal,
                        weeklyProgress = weeklyProgress,
                        monthlyGoal = monthlyGoal,
                        monthProgress = monthProgress,
                        habitCalendar = habitCalendar,
                        weeklyMood = weeklyMood,
                        monthActivity = monthActivity,
                        recentMood = latestMood,
                        recentSessions = recentSessions,
                        waterIntake = waterIntake,
                        waterGoal = waterGoal,
                        pausedPlan = if (sessionPaused) activePlan else null,
                        pausedCompletedCount = completedExerciseIds.size,
                        completionMessage = completionMessage,
                        onEnergySelected = {
                            energy = it
                            profileStore.saveDailyCheckIn(it, availableMinutes)
                        },
                        onTimeSelected = {
                            availableMinutes = it
                            profileStore.saveDailyCheckIn(energy, it)
                        },
                        onWeeklyGoalSelected = {
                            weeklyGoal = it
                            profileStore.saveWeeklyGoal(it)
                            weeklyProgress = profileStore.weeklyGoalProgress()
                            monthProgress = profileStore.monthProgress()
                        },
                        onMonthlyGoalSelected = {
                            monthlyGoal = it
                            profileStore.saveMonthlyGoal(it)
                            monthProgress = profileStore.monthProgress()
                        },
                        onEditProfile = { page = AppPage.SETUP },
                        onOpenCoach = { page = AppPage.COACH },
                        onOpenWater = { page = AppPage.WATER },
                        onOpenNutrition = { page = AppPage.NUTRITION },
                        onResumeWorkout = {
                            sessionPaused = false
                            page = AppPage.SESSION
                        },
                        onStartWorkout = {
                            completedExerciseIds.clear()
                            activePlan = currentPlan
                            profileStore.savePausedWorkout(currentPlan, emptySet())
                            sessionPaused = false
                            restSecondsRemaining = 0
                            completionMessage = null
                            page = AppPage.SESSION
                        },
                        onStartChallenge = { challenge -> startChallenge(currentProfile, challenge) }
                    )
                }
            }
            AppPage.COACH -> profile?.let { currentProfile ->
                CoachScreen(
                    modifier = Modifier.padding(padding),
                    profile = currentProfile,
                    energy = energy,
                    availableMinutes = availableMinutes,
                    streakDays = streakDays,
                    recentMood = latestMood,
                    challenge = shortChallengeFor(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), currentProfile.goal),
                    canStartChallenge = !sessionPaused,
                    onBack = { page = AppPage.HOME },
                    onStartChallenge = { challenge -> startChallenge(currentProfile, challenge) }
                )
            }
            AppPage.WATER -> HydrationScreen(
                modifier = Modifier.padding(padding),
                intakeMl = waterIntake,
                canUndoLastAdd = canUndoWaterAdd,
                goalMl = waterGoal,
                reminderIntervalMinutes = waterReminderInterval,
                onBack = { page = AppPage.HOME },
                onAddWater = { amount ->
                    waterIntake = profileStore.addWater(amount)
                    canUndoWaterAdd = profileStore.canUndoLastWaterAdd()
                },
                onUndoWater = {
                    waterIntake = profileStore.undoLastWaterAdd()
                    canUndoWaterAdd = profileStore.canUndoLastWaterAdd()
                },
                onGoalSelected = { goal ->
                    waterGoal = goal
                    profileStore.saveWaterGoal(goal)
                },
                onReminderIntervalSelected = { interval ->
                    profileStore.saveWaterReminderInterval(interval)
                    waterReminderInterval = interval
                    WaterReminderScheduler.schedule(context, interval)
                }
            )
            AppPage.NUTRITION -> NutritionScreen(
                modifier = Modifier.padding(padding),
                onBack = { page = AppPage.HOME }
            )
            AppPage.SESSION -> activePlan?.let { plan ->
                WorkoutSession(
                    modifier = Modifier.padding(padding),
                    plan = plan,
                    completedExerciseIds = completedExerciseIds,
                    restSecondsRemaining = restSecondsRemaining,
                    onToggleExercise = { exerciseId ->
                        if (exerciseId in completedExerciseIds) completedExerciseIds.remove(exerciseId)
                        else completedExerciseIds.add(exerciseId)
                        activePlan?.let { profileStore.savePausedWorkout(it, completedExerciseIds.toSet()) }
                    },
                    onRestTimeChanged = { restSecondsRemaining = it },
                    onSubstituteExercise = { exerciseId ->
                        activePlan = activePlan?.let { currentPlan ->
                            currentPlan.copy(
                                exercises = currentPlan.exercises.map { exercise ->
                                    if (exercise.id == exerciseId) exercise.toggleAlternative() ?: exercise else exercise
                                }
                            )
                        }
                        completedExerciseIds.remove(exerciseId)
                        activePlan?.let { profileStore.savePausedWorkout(it, completedExerciseIds.toSet()) }
                        restSecondsRemaining = 0
                    },
                    onPause = {
                        activePlan?.let { profileStore.savePausedWorkout(it, completedExerciseIds.toSet()) }
                        sessionPaused = true
                        page = AppPage.HOME
                    },
                    onSaveSession = { plan ->
                        if (completedExerciseIds.size == plan.exercises.size) {
                            pendingFeedbackPlan = plan
                            page = AppPage.FEEDBACK
                        } else {
                            storeSession(plan, null)
                            page = AppPage.HOME
                        }
                    }
                )
            }
            AppPage.FEEDBACK -> pendingFeedbackPlan?.let { plan ->
                SessionFeedbackScreen(
                    modifier = Modifier.padding(padding),
                    plan = plan,
                    onSubmit = { feedback, mood ->
                        storeSession(plan, feedback, mood)
                        page = AppPage.HOME
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    modifier: Modifier = Modifier,
    initialProfile: TrainingProfile?,
    onCancel: () -> Unit,
    onDeleteData: () -> Unit,
    onSave: (TrainingProfile) -> Unit
) {
    var goal by remember(initialProfile) { mutableStateOf(initialProfile?.goal) }
    var experience by remember(initialProfile) { mutableStateOf(initialProfile?.experience) }
    var hasChair by remember(initialProfile) { mutableStateOf(initialProfile?.hasChair ?: false) }
    var hasBackpack by remember(initialProfile) { mutableStateOf(initialProfile?.hasBackpack ?: false) }
    var prefersNoFloorExercises by remember(initialProfile) {
        mutableStateOf(initialProfile?.prefersNoFloorExercises ?: false)
    }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (initialProfile != null) TextButton(onClick = onCancel) { Text("Voltar") }
        Text(
            if (initialProfile == null) "Vamos começar pelo teu ritmo." else "O teu perfil",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text("O que gostarias de trabalhar?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ChoiceCard("Ganhar força", "Movimentos simples e progressivos", goal == TrainingGoal.STRENGTH) {
            goal = TrainingGoal.STRENGTH
        }
        ChoiceCard("Mover com mais facilidade", "Mobilidade e movimento confortável", goal == TrainingGoal.MOBILITY) {
            goal = TrainingGoal.MOBILITY
        }
        ChoiceCard("Criar consistência", "Pequenas sessões que cabem no dia", goal == TrainingGoal.CONSISTENCY) {
            goal = TrainingGoal.CONSISTENCY
        }

        Text("Como descreves a tua experiência?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ChoiceCard("Estou a começar", "Prefiro movimentos simples", experience == ExperienceLevel.NEW) {
            experience = ExperienceLevel.NEW
        }
        ChoiceCard("Estou a regressar", "Quero retomar com calma", experience == ExperienceLevel.RETURNING) {
            experience = ExperienceLevel.RETURNING
        }
        ChoiceCard("Já treino regularmente", "Posso fazer mais repetições", experience == ExperienceLevel.REGULAR) {
            experience = ExperienceLevel.REGULAR
        }

        Text("O que tens por perto?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ChoiceCard("Tenho uma cadeira estável", "Opcional", hasChair) { hasChair = !hasChair }
        ChoiceCard("Posso usar uma mochila", "Opcional, sem peso obrigatório", hasBackpack) { hasBackpack = !hasBackpack }
        ChoiceCard(
            "Prefiro movimentos de pé ou sentado",
            "Adapta o treino para evitar exercícios no chão. É uma preferência, não uma avaliação médica.",
            prefersNoFloorExercises
        ) { prefersNoFloorExercises = !prefersNoFloorExercises }

        Button(
            onClick = {
                val selectedGoal = goal ?: return@Button
                val selectedExperience = experience ?: return@Button
                onSave(
                    TrainingProfile(
                        selectedGoal,
                        selectedExperience,
                        hasChair,
                        hasBackpack,
                        prefersNoFloorExercises
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = goal != null && experience != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Guardar perfil")
        }
        if (initialProfile != null) {
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apagar perfil e histórico", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar todos os dados locais?") },
            text = { Text("O perfil, check-in, histórico e sessão em pausa serão removidos deste dispositivo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteData()
                    }
                ) {
                    Text("Apagar tudo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Dashboard(
    modifier: Modifier = Modifier,
    profile: TrainingProfile,
    plan: WorkoutPlan,
    energy: EnergyLevel,
    availableMinutes: Int,
    completedSessions: Int,
    sessionsLastSevenDays: Int,
    streakDays: Int,
    habitScore: Int,
    weeklyGoal: Int,
    weeklyProgress: Pair<Int, Int>,
    monthlyGoal: Int,
    monthProgress: Pair<Int, Int>,
    habitCalendar: List<HabitDay>,
    weeklyMood: List<MoodDay>,
    monthActivity: List<MonthActivity>,
    recentMood: SessionMood?,
    recentSessions: List<SessionHistoryEntry>,
    waterIntake: Int,
    waterGoal: Int,
    pausedPlan: WorkoutPlan?,
    pausedCompletedCount: Int,
    completionMessage: String?,
    onEnergySelected: (EnergyLevel) -> Unit,
    onTimeSelected: (Int) -> Unit,
    onWeeklyGoalSelected: (Int) -> Unit,
    onMonthlyGoalSelected: (Int) -> Unit,
    onEditProfile: () -> Unit,
    onOpenCoach: () -> Unit,
    onOpenWater: () -> Unit,
    onOpenNutrition: () -> Unit,
    onResumeWorkout: () -> Unit,
    onStartWorkout: () -> Unit,
    onStartChallenge: (ShortChallenge) -> Unit
) {
    val challenge = remember(profile.goal) {
        shortChallengeFor(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), profile.goal)
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF123D2B), Color(0xFF1C6042))),
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(15.dp), color = Color(0xFFB5D9C5)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("H", color = Color(0xFF123D2B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("MOVIMENTO COM PROPÓSITO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB5D9C5), fontWeight = FontWeight.Bold)
                        Text("Hortêncio", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onEditProfile) { Text("Perfil", color = Color.White) }
                }
                Text("O teu ritmo. A tua jornada.", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "Bem-estar que cabe na vida real — sem perfeição, um dia de cada vez.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE1EEE6)
                )
            }
        }

        Text("Como está a tua energia hoje?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EnergyLevel.entries.forEach { option ->
                SelectablePill(
                    modifier = Modifier.weight(1f),
                    label = option.label(),
                    selected = energy == option,
                    onClick = { onEnergySelected(option) }
                )
            }
        }

        Text("Quanto tempo tens?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(8, 15, 25).forEach { minutes ->
                SelectablePill(
                    modifier = Modifier.weight(1f),
                    label = "$minutes min",
                    selected = availableMinutes == minutes,
                    onClick = { onTimeSelected(minutes) }
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.weight(1f).clickable(onClick = onOpenWater),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F3F5))
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Hidratação", style = MaterialTheme.typography.labelLarge, color = Color(0xFF176477), fontWeight = FontWeight.Bold)
                    Text("${waterIntake} / ${waterGoal} ml", style = MaterialTheme.typography.titleMedium, color = Color(0xFF173640), fontWeight = FontWeight.Bold)
                    Text("Registar água", style = MaterialTheme.typography.bodySmall, color = Color(0xFF526A70))
                }
            }
            Card(
                modifier = Modifier.weight(1f).clickable(onClick = onOpenNutrition),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E6))
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Nutrição", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9B5B2A), fontWeight = FontWeight.Bold)
                    Text("Pequenas escolhas", style = MaterialTheme.typography.titleMedium, color = Color(0xFF35271D), fontWeight = FontWeight.Bold)
                    Text("Ideias sem dietas rígidas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6655))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF1F6F3)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meta semanal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${weeklyProgress.first}/${weeklyProgress.second}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF087A4A), fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 5).forEach { goal ->
                        SelectablePill(
                            modifier = Modifier.weight(1f),
                            label = "$goal",
                            selected = weeklyGoal == goal,
                            onClick = { onWeeklyGoalSelected(goal) }
                        )
                    }
                }
                Text(
                    if (weeklyProgress.first >= weeklyProgress.second) "Meta cumprida. Mantém o ritmo e deixa este momentum crescer."
                    else "Faltam ${weeklyProgress.second - weeklyProgress.first} sessões para atingires a meta da semana.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFFFF3E6)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("DESAFIO DE HOJE · 8 MIN", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9B5B2A), fontWeight = FontWeight.Bold)
                Text(challenge.title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF35271D), fontWeight = FontWeight.Bold)
                Text(challenge.prompt, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4A3B))
                Text(challenge.encouragement, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6655))
                Button(
                    onClick = { onStartChallenge(challenge) },
                    enabled = pausedPlan == null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (pausedPlan == null) "Aceitar desafio" else "Retoma a sessão em pausa")
                }
            }
        }

        if (pausedPlan != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2EC))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sessão em pausa", style = MaterialTheme.typography.labelLarge, color = Color(0xFF087A4A), fontWeight = FontWeight.Bold)
                    Text(pausedPlan.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$pausedCompletedCount de ${pausedPlan.exercises.size} movimentos concluídos", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onResumeWorkout, modifier = Modifier.fillMaxWidth()) { Text("Retomar sessão") }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF123D2B))
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("A TUA SUGESTÃO", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB5D9C5), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${plan.durationMinutes} MIN", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB5D9C5))
                }
                Text(plan.title, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE1EEE6))
                Text("PORQUÊ ESTA SUGESTÃO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB5D9C5), fontWeight = FontWeight.Bold)
                Text(plan.reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE1EEE6))
                Text(
                    "${plan.exercises.size} movimentos · foco em ${profile.goal.label().lowercase(PortugueseLocale)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB5D9C5)
                )
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = pausedPlan == null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (pausedPlan == null) "Começar este treino" else "Retoma a sessão em pausa", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (completionMessage != null) {
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color(0xFFEAF2EC)) {
                Text(completionMessage, modifier = Modifier.padding(16.dp), color = Color(0xFF123D2B))
            }
        }

        val dailyCoachNote = coachMessage(profile, energy, availableMinutes, streakDays, recentMood)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEAF2EC)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Ritmo do dia", style = MaterialTheme.typography.labelLarge, color = Color(0xFF087A4A), fontWeight = FontWeight.Bold)
                Text(dailyCoachNote, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF123D2B))
                TextButton(onClick = onOpenCoach) { Text("Abrir espaço do coach") }
            }
        }

        Text("A tua jornada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(streakDays.toString(), "dias seguidos", Modifier.weight(1f))
            SummaryTile(sessionsLastSevenDays.toString(), "últimos 7 dias", Modifier.weight(1f))
        }
        SummaryTile("${habitScore}%", "score de consistência", modifier = Modifier.fillMaxWidth())
        SummaryTile(completedSessions.toString(), "sessões concluídas", modifier = Modifier.fillMaxWidth())

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Calendário de hábitos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    habitCalendar.forEach { day ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "${day.label}: ${day.status.label()}"
                                    },
                                shape = RoundedCornerShape(11.dp),
                                color = when (day.status) {
                                    HabitStatus.COMPLETED -> Color(0xFF087A4A)
                                    HabitStatus.PARTIAL -> Color(0xFFE5A04D)
                                    HabitStatus.NONE -> Color(0xFFEAF2EC)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (day.status) {
                                            HabitStatus.COMPLETED -> "✓"
                                            HabitStatus.PARTIAL -> "•"
                                            HabitStatus.NONE -> "·"
                                        },
                                        color = when (day.status) {
                                            HabitStatus.COMPLETED -> Color.White
                                            HabitStatus.PARTIAL -> Color(0xFF3D2A11)
                                            HabitStatus.NONE -> Color(0xFF5D6B62)
                                        },
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }
                            Text(day.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
                        }
                    }
                }
                Text(
                    "Verde: sessão concluída · âmbar: progresso parcial. Cada passo conta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
        }

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Meta mensal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("${monthProgress.first}/${monthProgress.second}", style = MaterialTheme.typography.labelLarge, color = Color(0xFF087A4A), fontWeight = FontWeight.Bold)
                }
                Text("Escolhe um alvo que respeite a tua rotina.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(6, 8, 10, 12, 16).forEach { goal ->
                        SelectablePill(
                            modifier = Modifier.weight(1f),
                            label = "$goal",
                            selected = monthlyGoal == goal,
                            onClick = { onMonthlyGoalSelected(goal) }
                        )
                    }
                }
                MonthProgressChart(monthActivity, monthlyGoal)
                Text(
                    if (monthProgress.first >= monthlyGoal) {
                        "Meta mensal alcançada. Celebra o caminho que construíste."
                    } else {
                        "Este mês: ${monthProgress.first} de $monthlyGoal sessões concluídas."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF1F6F3)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Como te tens sentido?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("esta semana", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    weeklyMood.forEach { day ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(day.mood?.emoji() ?: "·", style = MaterialTheme.typography.titleLarge)
                            Text(day.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
                        }
                    }
                }
                val moodCount = weeklyMood.count { it.mood != null }
                Text(
                    if (moodCount == 0) "Regista como te sentes no fim de uma sessão para veres o teu padrão."
                    else "$moodCount ${if (moodCount == 1) "registo" else "registos"} de bem-estar esta semana. O teu ritmo não precisa de ser igual todos os dias.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
        }

        if (recentSessions.isNotEmpty()) {
            Text("Sessões recentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recentSessions.forEach { session -> RecentSessionItem(session) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun CoachScreen(
    modifier: Modifier = Modifier,
    profile: TrainingProfile,
    energy: EnergyLevel,
    availableMinutes: Int,
    streakDays: Int,
    recentMood: SessionMood?,
    challenge: ShortChallenge,
    canStartChallenge: Boolean,
    onBack: () -> Unit,
    onStartChallenge: (ShortChallenge) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Voltar à jornada") }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF123D2B), Color(0xFF1C6042))),
                    RoundedCornerShape(24.dp)
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("O TEU ESPAÇO", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB5D9C5), fontWeight = FontWeight.Bold)
                Text("Olá. Vamos ao teu ritmo.", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    coachMessage(profile, energy, availableMinutes, streakDays, recentMood),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE1EEE6)
                )
            }
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFF1F6F3)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("O teu sinal mais recente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    recentMood?.let { "${it.emoji()} ${it.label()}" }
                        ?: "Ainda não há registos de como te sentiste depois de treinar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF5D6B62)
                )
                Text(
                    "O teu feedback ajusta as sugestões; não é uma nota nem uma avaliação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = Color(0xFFFFF3E6)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DESAFIO DE HOJE · 8 MIN", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9B5B2A), fontWeight = FontWeight.Bold)
                Text(challenge.title, style = MaterialTheme.typography.headlineSmall, color = Color(0xFF35271D), fontWeight = FontWeight.Bold)
                Text(challenge.prompt, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4A3B))
                Text(challenge.encouragement, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A6655))
                Button(
                    onClick = { onStartChallenge(challenge) },
                    enabled = canStartChallenge,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (canStartChallenge) "Começar desafio" else "Retoma primeiro a sessão em pausa")
                }
            }
        }
        Text(
            "A consistência não é uma sequência perfeita. Também é saber abrandar, recomeçar e continuar a cuidar de ti.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5D6B62)
        )
    }
}

@Composable
private fun HydrationScreen(
    modifier: Modifier = Modifier,
    intakeMl: Int,
    canUndoLastAdd: Boolean,
    goalMl: Int,
    reminderIntervalMinutes: Int,
    onBack: () -> Unit,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    onGoalSelected: (Int) -> Unit,
    onReminderIntervalSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    var pendingReminderInterval by remember { mutableIntStateOf(0) }
    var reminderMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val interval = pendingReminderInterval
        pendingReminderInterval = 0
        if (granted && interval > 0) {
            onReminderIntervalSelected(interval)
            reminderMessage = "Lembretes ativados. Podes desligá-los quando quiseres."
        } else if (!granted) {
            reminderMessage = "As notificações estão desativadas. Permite-as nas definições do Android para receber lembretes."
        }
    }

    fun chooseReminder(interval: Int) {
        if (interval == 0) {
            pendingReminderInterval = 0
            onReminderIntervalSelected(0)
            reminderMessage = "Lembretes desligados."
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            pendingReminderInterval = interval
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminderIntervalSelected(interval)
            reminderMessage = "Lembretes ativados. Podes desligá-los quando quiseres."
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Voltar à jornada") }
        Text("Hidratação ao teu ritmo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Regista o que bebes e escolhe um lembrete opcional. O objetivo é pessoal, não uma recomendação médica.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5D6B62)
        )
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color(0xFFE7F3F5)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("HOJE", style = MaterialTheme.typography.labelLarge, color = Color(0xFF176477), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$intakeMl", style = MaterialTheme.typography.displaySmall, color = Color(0xFF173640), fontWeight = FontWeight.Bold)
                    Text("ml de $goalMl ml", modifier = Modifier.padding(bottom = 7.dp), color = Color(0xFF526A70))
                }
                LinearProgressIndicator(
                    progress = { hydrationProgress(intakeMl, goalMl) },
                    modifier = Modifier.fillMaxWidth().height(9.dp),
                    color = Color(0xFF25849A),
                    trackColor = Color(0xFFC8E1E5)
                )
                Text(
                    if (intakeMl >= goalMl) "Atingiste o teu objetivo pessoal. Podes continuar a registar se quiseres."
                    else "O progresso serve para acompanhar o teu registo, não para avaliar o teu corpo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF526A70)
                )
            }
        }
        Text("Adicionar ao registo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(150, 250, 500).forEach { amount ->
                Button(
                    onClick = { onAddWater(amount) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+${amount}ml")
                }
            }
        }
        TextButton(
            onClick = onUndoWater,
            enabled = canUndoLastAdd,
            modifier = Modifier.align(Alignment.End)
        ) { Text("Desfazer última adição") }

        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color.White) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("O teu objetivo diário", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Ajusta, ou deixa como está. As necessidades variam de pessoa para pessoa.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1500, 2000, 2500, 3000).forEach { goal ->
                        SelectablePill(
                            modifier = Modifier.weight(1f),
                            label = if (goal % 1000 == 0) "${goal / 1000}L" else "${goal / 1000},${goal % 1000 / 100}L",
                            selected = goalMl == goal,
                            onClick = { onGoalSelected(goal) }
                        )
                    }
                }
            }
        }
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFFF1F6F3)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Lembretes tranquilos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Notificações locais durante o dia (aprox. 09h–21h). Escolhe a frequência ou desliga.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(0, 60, 90, 120, 180).forEach { interval ->
                        SelectablePill(
                            modifier = Modifier.weight(1f),
                            label = when (interval) {
                                0 -> "Off"
                                60 -> "60m"
                                90 -> "90m"
                                120 -> "2h"
                                else -> "3h"
                            },
                            selected = reminderIntervalMinutes == interval,
                            onClick = { chooseReminder(interval) }
                        )
                    }
                }
                reminderMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62)) }
            }
        }
        Text(
            "Os lembretes são aproximados e podem ser adiados pelo Android. Considera orientações profissionais se tens uma condição que afete a hidratação.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF5D6B62)
        )
    }
}

@Composable
private fun NutritionScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Voltar à jornada") }
        Text("Nutrição sem extremos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Ideias gerais para acompanhar o movimento — sem contar calorias, proibir alimentos ou substituir aconselhamento profissional.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF5D6B62)
        )
        NutritionTip(
            "01 · Monta uma refeição completa",
            "Quando fizer sentido para ti, combina uma fonte de energia, alimentos com proteína e fruta ou vegetais. Não precisa de ser perfeito em todas as refeições."
        )
        NutritionTip(
            "02 · Torna o simples acessível",
            "Iogurte com fruta, pão com ovo, sopa com leguminosas ou arroz com feijão são exemplos; adapta aos teus gostos, orçamento e cultura."
        )
        NutritionTip(
            "03 · Antes e depois do movimento",
            "Escolhe algo que te seja confortável. Não é obrigatório comer num horário específico para que uma sessão curta conte."
        )
        NutritionTip(
            "04 · Respeita as tuas necessidades",
            "Alergias, condições clínicas, gravidez, medicação e histórico alimentar podem mudar o que é adequado. Um/a nutricionista pode ajudar com orientação individual."
        )
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF3E6)) {
            Text(
                "A Treina com Hortêncio não faz diagnóstico nem cria dietas clínicas. Estas sugestões são educativas e não substituem aconselhamento de saúde.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF5D4A3B)
            )
        }
    }
}

@Composable
private fun NutritionTip(title: String, text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF123D2B), fontWeight = FontWeight.Bold)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D6B62))
        }
    }
}

@Composable
private fun SessionFeedbackScreen(
    modifier: Modifier = Modifier,
    plan: WorkoutPlan,
    onSubmit: (SessionFeedback?, SessionMood?) -> Unit
) {
    var selectedFeedback by remember(plan) { mutableStateOf<SessionFeedback?>(null) }
    var selectedMood by remember(plan) { mutableStateOf<SessionMood?>(null) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Como sentiste este treino?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(plan.title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF5D6B62))
        ChoiceCard(
            "Podia fazer mais",
            "A próxima sessão pode subir um pouco.",
            selectedFeedback == SessionFeedback.COULD_DO_MORE
        ) { selectedFeedback = SessionFeedback.COULD_DO_MORE }
        ChoiceCard(
            "Na medida certa",
            "Mantemos um ritmo semelhante.",
            selectedFeedback == SessionFeedback.JUST_RIGHT
        ) { selectedFeedback = SessionFeedback.JUST_RIGHT }
        ChoiceCard(
            "Foi demasiado exigente",
            "A próxima sessão será mais leve.",
            selectedFeedback == SessionFeedback.TOO_DEMANDING
        ) { selectedFeedback = SessionFeedback.TOO_DEMANDING }

        Text("Como te sentiste no corpo hoje?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ChoiceCard(
            "Exausto/a",
            "Precisei de respeitar os limites.",
            selectedMood == SessionMood.EXHAUSTED
        ) { selectedMood = SessionMood.EXHAUSTED }
        ChoiceCard(
            "No ponto certo",
            "Senti-me bem e estável.",
            selectedMood == SessionMood.OKAY
        ) { selectedMood = SessionMood.OKAY }
        ChoiceCard(
            "Com força",
            "Senti-me presente e capaz.",
            selectedMood == SessionMood.STRONG
        ) { selectedMood = SessionMood.STRONG }

        Button(
            onClick = { onSubmit(selectedFeedback, selectedMood) },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedFeedback != null || selectedMood != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Guardar e terminar")
        }
        TextButton(onClick = { onSubmit(null, null) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Saltar")
        }
    }
}

@Composable
private fun ChoiceCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFEAF2EC) else Color.White,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(6.dp),
                color = if (selected) Color(0xFF087A4A) else Color(0xFFDCE7DF)
            ) {}
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
            }
        }
    }
}

@Composable
private fun SelectablePill(modifier: Modifier = Modifier, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(44.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFF123D2B) else Color(0xFFEAF2EC)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Color(0xFF123D2B), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SummaryTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Color(0xFFEAF2EC)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF123D2B))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
        }
    }
}

@Composable
private fun WorkoutSession(
    modifier: Modifier = Modifier,
    plan: WorkoutPlan,
    completedExerciseIds: List<String>,
    restSecondsRemaining: Int,
    onToggleExercise: (String) -> Unit,
    onRestTimeChanged: (Int) -> Unit,
    onSubstituteExercise: (String) -> Unit,
    onPause: () -> Unit,
    onSaveSession: (WorkoutPlan) -> Unit
) {
    val completedCount = completedExerciseIds.size
    var restTimerPaused by remember(plan) { mutableStateOf(false) }
    LaunchedEffect(restSecondsRemaining, restTimerPaused) {
        if (restSecondsRemaining > 0 && !restTimerPaused) {
            delay(1_000)
            onRestTimeChanged(restSecondsRemaining - 1)
        }
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onPause) { Text("Pausar treino") }
        Text(plan.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("${plan.durationMinutes} minutos · ${plan.exercises.size} movimentos", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF5D6B62))
        LinearProgressIndicator(
            progress = { completedCount / plan.exercises.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF087A4A),
            trackColor = Color(0xFFDCE7DF)
        )
        Text("$completedCount de ${plan.exercises.size} concluídos", style = MaterialTheme.typography.labelLarge, color = Color(0xFF5D6B62))

        if (restSecondsRemaining > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2EC))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Descanso · ${restSecondsRemaining}s", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { restTimerPaused = !restTimerPaused }) {
                        Text(if (restTimerPaused) "Retomar" else "Pausar")
                    }
                    TextButton(onClick = { onRestTimeChanged(0) }) { Text("Saltar") }
                }
            }
        }

        plan.exercises.forEachIndexed { index, exercise ->
            val isComplete = exercise.id in completedExerciseIds
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isComplete) Color(0xFFEAF2EC) else Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Checkbox(
                        checked = isComplete,
                        onCheckedChange = { checked ->
                            onToggleExercise(exercise.id)
                            if (checked && index < plan.exercises.lastIndex) onRestTimeChanged(45)
                            else if (!checked) onRestTimeChanged(0)
                        }
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(exercise.prescription, style = MaterialTheme.typography.labelMedium, color = Color(0xFF087A4A))
                        Text(exercise.instruction, style = MaterialTheme.typography.bodySmall, color = Color(0xFF5D6B62))
                        ExerciseHowTo(exercise.id, exercise.name)
                        if (exercise.alternative != null) {
                            TextButton(onClick = { onSubstituteExercise(exercise.id) }) { Text("Trocar variação") }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onSaveSession(plan) },
            modifier = Modifier.fillMaxWidth(),
            enabled = completedCount > 0,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(if (completedCount == plan.exercises.size) "Concluir sessão" else "Guardar progresso e sair")
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecentSessionItem(session: SessionHistoryEntry) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(session.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    listOfNotNull(
                        if (session.completedExercises == session.totalExercises) "Sessão concluída"
                        else "${session.completedExercises} de ${session.totalExercises} movimentos feitos",
                        session.feedback?.label(),
                        session.mood?.label()
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5D6B62)
                )
            }
            Text(
                SimpleDateFormat("d MMM", PortugueseLocale).format(Date(session.completedAt)),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF5D6B62)
            )
        }
    }
}

@Composable
private fun ExerciseHowTo(exerciseId: String, exerciseName: String) {
    val guide = remember(exerciseId, exerciseName) { exerciseGuideFor(exerciseId, exerciseName) }
    var isExpanded by remember(exerciseId, exerciseName) { mutableStateOf(false) }
    var movementAtEnd by remember(exerciseId, exerciseName) { mutableStateOf(false) }
    val phase by animateFloatAsState(
        targetValue = if (movementAtEnd) 1f else 0f,
        animationSpec = tween(650),
        label = "exercise-movement"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        TextButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(if (isExpanded) "Ocultar guia do movimento" else "Ver guia do movimento")
        }
        if (isExpanded) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F6F3)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Demonstração esquemática",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF176477),
                                fontWeight = FontWeight.Bold
                            )
                            Text(guide.focus, style = MaterialTheme.typography.bodySmall, color = Color(0xFF526A70))
                        }
                        TextButton(onClick = { movementAtEnd = !movementAtEnd }) {
                            Text(if (movementAtEnd) "Repor" else "Ver movimento")
                        }
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                        val centerX = size.width / 2f
                        val groundY = size.height * 0.94f
                        val isSquat = exerciseId == "squat" || exerciseId == "sit-stand"
                        val isMarch = exerciseId == "march" || exerciseId == "easy-march"
                        val isPush = exerciseId == "push" || exerciseId == "wall-push"
                        val isPull = exerciseId == "pull"
                        val isBridge = exerciseId == "bridge" &&
                            (exerciseName == "Ponte de glúteos" || exerciseName == "Ponte curta")
                        val isSeated = exerciseId == "breathing" ||
                            exerciseName.contains("sentada", ignoreCase = true) ||
                            exerciseName.contains("sentado", ignoreCase = true)
                        val bend = if (isSquat) phase else 0f
                        val shoulderY = size.height * (0.28f + bend * 0.07f)
                        val hipY = size.height * (if (isBridge) 0.67f else 0.55f + bend * 0.12f)
                        val kneeY = size.height * (0.75f + bend * 0.10f)
                        val head = Offset(centerX, shoulderY - size.height * 0.11f)
                        val shoulder = Offset(centerX, shoulderY)
                        val hip = Offset(centerX, hipY)
                        val legSwing = if (isMarch || exerciseId == "dead-bug") {
                            (phase - 0.5f) * size.height * 0.24f
                        } else {
                            0f
                        }
                        val leftKnee = Offset(centerX - size.width * 0.13f, kneeY - legSwing)
                        val rightKnee = Offset(centerX + size.width * 0.13f, kneeY + legSwing)
                        val leftFoot = Offset(centerX - size.width * 0.17f, groundY - if (isMarch) legSwing else 0f)
                        val rightFoot = Offset(centerX + size.width * 0.17f, groundY + if (isMarch) legSwing else 0f)
                        val armMove = when {
                            isPush -> size.width * 0.07f * phase
                            isPull -> size.width * 0.06f * phase
                            exerciseId == "reach" || exerciseId == "shoulder-roll" -> size.height * 0.14f * phase
                            else -> 0f
                        }
                        val leftHand = Offset(
                            centerX - size.width * 0.25f + if (isPush) armMove else 0f,
                            shoulderY + size.height * 0.16f - armMove
                        )
                        val rightHand = Offset(
                            centerX + size.width * 0.25f - if (isPush) armMove else 0f,
                            shoulderY + size.height * 0.16f + armMove
                        )
                        val stroke = size.minDimension * 0.027f
                        val figure = Color(0xFF176477)
                        val jointColor = Color(0xFF25849A)
                        val seatTop = Offset(centerX + size.width * 0.19f, size.height * 0.52f)

                        drawLine(
                            Color(0xFFCADDD9),
                            Offset(size.width * 0.16f, groundY),
                            Offset(size.width * 0.84f, groundY),
                            stroke
                        )
                        if (isSeated || isSquat) {
                            drawLine(Color(0xFFC47C43), seatTop, Offset(seatTop.x, groundY), stroke, cap = StrokeCap.Round)
                            drawLine(
                                Color(0xFFC47C43),
                                seatTop,
                                Offset(seatTop.x + size.width * 0.12f, seatTop.y),
                                stroke,
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(figure, radius = size.minDimension * 0.065f, center = head)
                        drawLine(figure, shoulder, if (isBridge) hip.copy(x = centerX) else hip, stroke, cap = StrokeCap.Round)
                        if (isBridge) {
                            drawLine(figure, hip, Offset(centerX - size.width * 0.15f, groundY), stroke, cap = StrokeCap.Round)
                            drawLine(figure, hip, Offset(centerX + size.width * 0.15f, groundY), stroke, cap = StrokeCap.Round)
                        } else if (isSeated) {
                            drawLine(figure, hip, Offset(centerX - size.width * 0.10f, size.height * 0.78f), stroke, cap = StrokeCap.Round)
                            drawLine(figure, hip, Offset(centerX + size.width * 0.10f, size.height * 0.78f), stroke, cap = StrokeCap.Round)
                        } else {
                            drawLine(figure, hip, leftKnee, stroke, cap = StrokeCap.Round)
                            drawLine(figure, hip, rightKnee, stroke, cap = StrokeCap.Round)
                            drawLine(figure, leftKnee, leftFoot, stroke, cap = StrokeCap.Round)
                            drawLine(figure, rightKnee, rightFoot, stroke, cap = StrokeCap.Round)
                        }
                        drawLine(figure, shoulder, leftHand, stroke, cap = StrokeCap.Round)
                        drawLine(figure, shoulder, rightHand, stroke, cap = StrokeCap.Round)
                        listOf(shoulder, hip, leftKnee, rightKnee, leftHand, rightHand).forEach {
                            drawCircle(jointColor, radius = stroke * 1.3f, center = it)
                        }
                        if (isPush) {
                            val wallX = centerX + size.width * 0.29f
                            drawLine(Color(0xFFC47C43), Offset(wallX, size.height * 0.22f), Offset(wallX, groundY), stroke)
                        }
                    }
                    guide.steps.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", style = MaterialTheme.typography.bodySmall, color = Color(0xFF344C44))
                    }
                    Text("Atenção: ${guide.watchOut}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A4B2B))
                    Text(
                        "Ilustração educativa, não substitui orientação profissional. Pára se houver dor.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF526A70)
                    )
                }
            }
        }
    }
}

private fun TrainingGoal.label() = when (this) {
    TrainingGoal.STRENGTH -> "força"
    TrainingGoal.MOBILITY -> "mobilidade"
    TrainingGoal.CONSISTENCY -> "consistência"
}

private fun ExperienceLevel.label() = when (this) {
    ExperienceLevel.NEW -> "A começar"
    ExperienceLevel.RETURNING -> "A regressar"
    ExperienceLevel.REGULAR -> "Regular"
}

private fun EnergyLevel.label() = when (this) {
    EnergyLevel.LOW -> "Pouca"
    EnergyLevel.STEADY -> "Normal"
    EnergyLevel.HIGH -> "Boa"
}

private fun HabitStatus.label() = when (this) {
    HabitStatus.NONE -> "sem sessão"
    HabitStatus.PARTIAL -> "progresso parcial"
    HabitStatus.COMPLETED -> "sessão concluída"
}

private fun SessionFeedback.label() = when (this) {
    SessionFeedback.COULD_DO_MORE -> "Leve"
    SessionFeedback.JUST_RIGHT -> "Adequado"
    SessionFeedback.TOO_DEMANDING -> "Exigente"
}

@Composable
private fun MonthProgressChart(activity: List<MonthActivity>, monthlyGoal: Int) {
    if (activity.isEmpty()) return
    val maximum = maxOf(monthlyGoal, activity.maxOf { it.sessions }, 1)
    Canvas(modifier = Modifier.fillMaxWidth().height(128.dp)) {
        val top = 8f
        val bottom = size.height - 8f
        val chartHeight = bottom - top
        val slotWidth = size.width / activity.size
        val barWidth = slotWidth * 0.42f
        val goalY = bottom - chartHeight * monthlyGoal / maximum

        activity.forEachIndexed { index, month ->
            val left = slotWidth * index + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = Color(0xFFEAF2EC),
                topLeft = Offset(left, top),
                size = Size(barWidth, chartHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            val filledHeight = chartHeight * month.sessions.coerceAtMost(maximum) / maximum
            if (filledHeight > 0f) {
                drawRoundRect(
                    color = if (index == activity.lastIndex) Color(0xFF087A4A) else Color(0xFF70A98A),
                    topLeft = Offset(left, bottom - filledHeight),
                    size = Size(barWidth, filledHeight),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }
        }
        drawLine(
            color = Color(0xFFC47C43),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2f
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        activity.forEach { month ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${month.sessions}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF123D2B), fontWeight = FontWeight.SemiBold)
                Text(month.label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(modifier = Modifier.size(8.dp), shape = RoundedCornerShape(4.dp), color = Color(0xFFC47C43)) {}
        Text("meta mensal · sessões concluídas por mês", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
    }
}

private fun SessionMood.emoji() = when (this) {
    SessionMood.EXHAUSTED -> "😮‍💨"
    SessionMood.OKAY -> "🌿"
    SessionMood.STRONG -> "✨"
}

private fun SessionMood.label() = when (this) {
    SessionMood.EXHAUSTED -> "Exausto/a"
    SessionMood.OKAY -> "No ponto certo"
    SessionMood.STRONG -> "Com força"
}

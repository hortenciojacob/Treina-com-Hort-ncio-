package com.hortencio.treinacomhortencio.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.hortencio.treinacomhortencio.data.SessionHistoryEntry
import com.hortencio.treinacomhortencio.data.TrainingProfileStore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppPage { SETUP, HOME, SESSION, FEEDBACK }
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
    var availableMinutes by remember { mutableStateOf(dailyCheckInAtLaunch?.availableMinutes ?: 15) }
    var completedSessions by remember(profileStore) { mutableStateOf(profileStore.completedSessionCount()) }
    var sessionsLastSevenDays by remember(profileStore) { mutableStateOf(profileStore.sessionsInLastSevenDays()) }
    var recentSessions by remember(profileStore) { mutableStateOf(profileStore.recentSessions()) }
    var activePlan by remember { mutableStateOf(pausedWorkoutAtLaunch?.plan) }
    var pendingFeedbackPlan by remember { mutableStateOf<WorkoutPlan?>(null) }
    var sessionPaused by remember { mutableStateOf(pausedWorkoutAtLaunch != null) }
    var restSecondsRemaining by remember { mutableStateOf(0) }
    var completionMessage by remember { mutableStateOf<String?>(null) }
    val completedExerciseIds = remember {
        mutableStateListOf<String>().apply {
            pausedWorkoutAtLaunch?.completedExerciseIds?.forEach { add(it) }
        }
    }
    val latestFeedback = remember(profileStore, profile?.goal, recentSessions) {
        profile?.goal?.let(profileStore::latestCompletedFeedback)
    }
    fun storeSession(plan: WorkoutPlan, feedback: SessionFeedback?) {
        profileStore.recordSession(
            title = plan.title,
            completedExercises = completedExerciseIds.size,
            totalExercises = plan.exercises.size,
            goal = plan.goal,
            feedback = feedback
        )
        profileStore.clearPausedWorkout()
        completedSessions = profileStore.completedSessionCount()
        sessionsLastSevenDays = profileStore.sessionsInLastSevenDays()
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
                    recentSessions = emptyList()
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
                        recentSessions = recentSessions,
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
                        onEditProfile = { page = AppPage.SETUP },
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
                        }
                    )
                }
            }
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
                    onSubmit = { feedback ->
                        storeSession(plan, feedback)
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

        Button(
            onClick = {
                val selectedGoal = goal ?: return@Button
                val selectedExperience = experience ?: return@Button
                onSave(TrainingProfile(selectedGoal, selectedExperience, hasChair, hasBackpack))
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
    recentSessions: List<SessionHistoryEntry>,
    pausedPlan: WorkoutPlan?,
    pausedCompletedCount: Int,
    completionMessage: String?,
    onEnergySelected: (EnergyLevel) -> Unit,
    onTimeSelected: (Int) -> Unit,
    onEditProfile: () -> Unit,
    onResumeWorkout: () -> Unit,
    onStartWorkout: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(42.dp), shape = RoundedCornerShape(14.dp), color = Color(0xFF087A4A)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("H", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text("TREINA COM", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5D6B62))
                Text("Hortêncio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEditProfile) { Text("Perfil") }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Bom dia.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Um passo de cada vez. Hoje conta.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF5D6B62))
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

        Text("O teu percurso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(sessionsLastSevenDays.toString(), "últimos 7 dias", Modifier.weight(1f))
            SummaryTile(completedSessions.toString(), "sessões concluídas", Modifier.weight(1f))
        }
        if (recentSessions.isNotEmpty()) {
            Text("Sessões recentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            recentSessions.forEach { session -> RecentSessionItem(session) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SessionFeedbackScreen(
    modifier: Modifier = Modifier,
    plan: WorkoutPlan,
    onSubmit: (SessionFeedback?) -> Unit
) {
    var selectedFeedback by remember(plan) { mutableStateOf<SessionFeedback?>(null) }
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
        Button(
            onClick = { onSubmit(selectedFeedback) },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedFeedback != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Guardar e terminar")
        }
        TextButton(onClick = { onSubmit(null) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
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
                        session.feedback?.label()
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

private fun SessionFeedback.label() = when (this) {
    SessionFeedback.COULD_DO_MORE -> "Leve"
    SessionFeedback.JUST_RIGHT -> "Adequado"
    SessionFeedback.TOO_DEMANDING -> "Exigente"
}

package com.hortencio.treinacomhortencio.ui

enum class TrainingGoal {
    STRENGTH,
    MOBILITY,
    CONSISTENCY
}

enum class ExperienceLevel {
    NEW,
    RETURNING,
    REGULAR
}

enum class EnergyLevel {
    LOW,
    STEADY,
    HIGH
}

enum class SessionFeedback {
    COULD_DO_MORE,
    JUST_RIGHT,
    TOO_DEMANDING
}

data class TrainingProfile(
    val goal: TrainingGoal,
    val experience: ExperienceLevel,
    val hasChair: Boolean,
    val hasBackpack: Boolean
)

data class PlannedExercise(
    val id: String,
    val name: String,
    val prescription: String,
    val instruction: String,
    val alternative: ExerciseVariation? = null
)

data class ExerciseVariation(val name: String, val instruction: String)

fun PlannedExercise.toggleAlternative(): PlannedExercise? {
    val next = alternative ?: return null
    return copy(
        name = next.name,
        instruction = next.instruction,
        alternative = ExerciseVariation(name, instruction)
    )
}

data class WorkoutPlan(
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val goal: TrainingGoal,
    val reason: String,
    val exercises: List<PlannedExercise>
)

private data class ExerciseTemplate(
    val id: String,
    val name: String,
    val instruction: String,
    val alternative: ExerciseVariation
)

fun createWorkoutPlan(
    profile: TrainingProfile,
    availableMinutes: Int,
    energy: EnergyLevel,
    previousFeedback: SessionFeedback? = null
): WorkoutPlan {
    require(availableMinutes in 5..60) { "Available time must be between 5 and 60 minutes" }

    val gentleDay = energy == EnergyLevel.LOW
    val templates = if (gentleDay) {
        gentleExercises
    } else {
        when (profile.goal) {
            TrainingGoal.STRENGTH -> strengthExercises(profile)
            TrainingGoal.MOBILITY -> mobilityExercises
            TrainingGoal.CONSISTENCY -> consistencyExercises(profile)
        }
    }

    val duration = if (gentleDay) minOf(availableMinutes, 8) else availableMinutes
    val exerciseCount = when (duration) {
        in 5..10 -> 2
        in 11..17 -> 3
        else -> 5
    }
    val baseRounds = when {
        gentleDay -> 1
        profile.experience == ExperienceLevel.REGULAR && availableMinutes >= 20 && energy == EnergyLevel.HIGH -> 3
        else -> 2
    }
    val rounds = when (previousFeedback) {
        SessionFeedback.COULD_DO_MORE -> (baseRounds + 1).coerceAtMost(3)
        SessionFeedback.TOO_DEMANDING -> (baseRounds - 1).coerceAtLeast(1)
        else -> baseRounds
    }
    val baseRepetitions = when {
        profile.experience != ExperienceLevel.REGULAR -> 8
        energy == EnergyLevel.HIGH -> 12
        else -> 10
    }
    val repetitions = when (previousFeedback) {
        SessionFeedback.COULD_DO_MORE -> (baseRepetitions + 1).coerceAtMost(12)
        SessionFeedback.TOO_DEMANDING -> (baseRepetitions - 2).coerceAtLeast(6)
        else -> baseRepetitions
    }
    val isMobility = gentleDay || profile.goal == TrainingGoal.MOBILITY
    val mobilityDurationSeconds = when {
        gentleDay -> 30
        previousFeedback == SessionFeedback.TOO_DEMANDING -> 30
        previousFeedback == SessionFeedback.COULD_DO_MORE -> 45
        else -> 40
    }

    return WorkoutPlan(
        title = if (gentleDay) "Movimento leve" else profile.goal.title(),
        description = if (gentleDay) {
            "Hoje, o objetivo é terminar a sentir-te bem. Podes parar ou adaptar a qualquer momento."
        } else {
            "Um plano ajustado ao teu tempo, experiência e ao que tens em casa."
        },
        durationMinutes = duration,
        goal = profile.goal,
        reason = recommendationReason(profile, duration, energy, previousFeedback),
        exercises = templates.take(exerciseCount).map { template ->
            PlannedExercise(
                id = template.id,
                name = template.name,
                prescription = when {
                    isMobility -> "$mobilityDurationSeconds segundos · ritmo confortável"
                    else -> "$rounds séries · $repetitions repetições"
                },
                instruction = template.instruction,
                alternative = template.alternative
            )
        }
    )
}

private fun recommendationReason(
    profile: TrainingProfile,
    duration: Int,
    energy: EnergyLevel,
    previousFeedback: SessionFeedback?
) = when {
    energy == EnergyLevel.LOW ->
        "Escolheste pouca energia; hoje reduzimos a sessão a movimentos leves e curtos."
    previousFeedback == SessionFeedback.TOO_DEMANDING ->
        "A última sessão de ${profile.goal.title().lowercase()} foi exigente; baixámos a dose um passo."
    previousFeedback == SessionFeedback.COULD_DO_MORE ->
        "A última sessão de ${profile.goal.title().lowercase()} pareceu leve; aumentámos a dose gradualmente."
    duration <= 10 ->
        "Escolheste pouco tempo; reduzimos a lista para caber numa sessão de $duration minutos."
    profile.goal == TrainingGoal.MOBILITY ->
        "Escolheste mobilidade; o plano privilegia ritmo livre e amplitude confortável."
    else ->
        "Combinámos o teu objetivo, experiência e equipamento disponível para montar este treino."
}

private fun TrainingGoal.title() = when (this) {
    TrainingGoal.STRENGTH -> "Força no teu ritmo"
    TrainingGoal.MOBILITY -> "Mobilidade tranquila"
    TrainingGoal.CONSISTENCY -> "Um pouco já conta"
}

private fun strengthExercises(profile: TrainingProfile) = listOf(
    ExerciseTemplate(
        id = "squat",
        name = if (profile.hasChair) "Agachamento até à cadeira" else "Agachamento livre",
        instruction = "Mantém o movimento confortável e usa a cadeira como apoio, se escolheste essa opção.",
        alternative = if (profile.hasChair) {
            ExerciseVariation("Agachamento curto", "Faz uma amplitude menor, sem cadeira, e mantém o controlo.")
        } else {
            ExerciseVariation("Agachamento curto com apoio", "Apoia uma mão na parede e usa uma amplitude confortável.")
        }
    ),
    ExerciseTemplate(
        id = "push",
        name = "Flexão na parede",
        instruction = "Ajusta a inclinação para conseguires manter o controlo durante todo o movimento.",
        alternative = ExerciseVariation("Pressão na parede", "Empurra a parede suavemente durante alguns segundos e relaxa.")
    ),
    ExerciseTemplate(
        id = "pull",
        name = if (profile.hasBackpack) "Remada com mochila leve" else "Elevação em W no chão",
        instruction = if (profile.hasBackpack) {
            "Usa uma mochila leve e bem fechada; puxa os cotovelos para trás sem balanço."
        } else {
            "Deita-te de barriga para baixo e aproxima suavemente as omoplatas."
        },
        alternative = if (profile.hasBackpack) {
            ExerciseVariation("Elevação em W no chão", "Deita-te de barriga para baixo e aproxima suavemente as omoplatas.")
        } else {
            ExerciseVariation("Retração escapular", "Sentado ou de pé, aproxima as omoplatas sem elevar os ombros.")
        }
    ),
    ExerciseTemplate(
        "bridge",
        "Ponte de glúteos",
        "Sobe a bacia sem arquear a zona lombar.",
        ExerciseVariation("Ponte curta", "Sobe a bacia apenas alguns centímetros e mantém a respiração natural.")
    ),
    ExerciseTemplate(
        "dead-bug",
        "Dead bug",
        "Move um braço e a perna oposta sem levantar a lombar do chão.",
        ExerciseVariation("Bird dog", "Em quatro apoios, estende braço e perna opostos sem perder o equilíbrio.")
    )
)

private fun consistencyExercises(profile: TrainingProfile) = listOf(
    ExerciseTemplate("march", "Marcha no lugar", "Escolhe um ritmo em que consigas falar confortavelmente.", ExerciseVariation("Marcha sentada", "Sentado, alterna os pés num ritmo confortável.")),
    ExerciseTemplate(
        id = "sit-stand",
        name = if (profile.hasChair) "Sentar e levantar" else "Agachamento curto",
        instruction = "Usa apenas uma amplitude confortável e mantém o equilíbrio.",
        alternative = if (profile.hasChair) {
            ExerciseVariation("Agachamento curto", "Faz uma amplitude menor e mantém os pés bem apoiados.")
        } else {
            ExerciseVariation("Sentar e levantar", "Usa uma cadeira estável e levanta-te com controlo.")
        }
    ),
    ExerciseTemplate("wall-push", "Empurrar a parede", "Coloca as mãos na parede e aproxima o peito com controlo.", ExerciseVariation("Pressão na parede", "Empurra a parede suavemente e mantém a posição por alguns segundos.")),
    ExerciseTemplate("calf-raise", "Elevação de calcanhares", "Se precisares, apoia uma mão na parede para equilibrar.", ExerciseVariation("Mobilidade dos tornozelos", "Sentado ou de pé, move os tornozelos devagar.")),
    ExerciseTemplate("reach", "Alcance alternado", "Estende um braço de cada vez, sem prender a respiração.", ExerciseVariation("Círculos de braços", "Faz círculos pequenos e lentos, sem forçar a amplitude."))
)

private val mobilityExercises = listOf(
    ExerciseTemplate("cat-cow", "Gato e vaca", "Em quatro apoios ou sentado, mobiliza a coluna sem forçar.", ExerciseVariation("Inclinação pélvica", "Sentado, inclina ligeiramente a bacia para a frente e para trás.")),
    ExerciseTemplate("hip-flow", "Mobilidade da anca", "Alterna o movimento de cada lado apenas dentro de uma amplitude confortável.", ExerciseVariation("Marcha sentada", "Sentado, eleva um joelho de cada vez sem forçar.")),
    ExerciseTemplate("rotation", "Rotação torácica", "Roda o tronco suavemente e acompanha o movimento com a respiração.", ExerciseVariation("Abertura de braços", "Abre os braços para os lados e regressa devagar.")),
    ExerciseTemplate("ankles", "Mobilidade dos tornozelos", "Leva o joelho para a frente sem levantar o calcanhar.", ExerciseVariation("Círculos de tornozelos", "Desenha círculos pequenos com cada pé.")),
    ExerciseTemplate("breathing", "Respiração tranquila", "Inspira e expira sem tentar controlar ou prolongar à força.", ExerciseVariation("Pausa consciente", "Fica numa posição confortável e observa a respiração natural."))
)

private val gentleExercises = listOf(
    ExerciseTemplate("breathing", "Respiração tranquila", "Encontra uma posição confortável e respira naturalmente.", ExerciseVariation("Pausa consciente", "Fica numa posição confortável e observa a respiração natural.")),
    ExerciseTemplate("shoulder-roll", "Círculos de ombros", "Faz círculos pequenos e lentos, sem forçar a amplitude.", ExerciseVariation("Elevar e baixar os ombros", "Eleva os ombros suavemente e deixa-os descer ao expirar.")),
    ExerciseTemplate("ankles", "Mobilidade dos tornozelos", "Move cada tornozelo devagar, sentado ou de pé com apoio.", ExerciseVariation("Flexão dos pés", "Sentado, aponta os pés e traz as pontas em direção a ti.")),
    ExerciseTemplate("easy-march", "Marcha suave", "Só se te souber bem: marcha devagar ou continua a respirar sentado.", ExerciseVariation("Marcha sentada", "Sentado, alterna os pés sem pressa.")),
    ExerciseTemplate("reach", "Alcance confortável", "Estende os braços apenas até onde for confortável.", ExerciseVariation("Abertura de braços", "Abre e fecha os braços devagar, sem forçar.") )
)
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

enum class SessionMood {
    EXHAUSTED,
    OKAY,
    STRONG
}

data class TrainingProfile(
    val goal: TrainingGoal,
    val experience: ExperienceLevel,
    val hasChair: Boolean,
    val hasBackpack: Boolean,
    val prefersNoFloorExercises: Boolean = false
)

data class PlannedExercise(
    val id: String,
    val name: String,
    val prescription: String,
    val instruction: String,
    val alternative: ExerciseVariation? = null
)

data class ExerciseVariation(val name: String, val instruction: String)

data class ExerciseGuide(
    val focus: String,
    val steps: List<String>,
    val watchOut: String
)

fun exerciseGuideFor(exerciseId: String, exerciseName: String? = null): ExerciseGuide = when {
    exerciseId == "pull" && exerciseName == "Remada com mochila leve" -> ExerciseGuide(
        focus = "Costas e postura",
        steps = listOf("Segura a mochila leve com as costas direitas.", "Leva os cotovelos para trás, perto do tronco.", "Baixa a mochila devagar sem balançar o corpo."),
        watchOut = "Confirma que a mochila está fechada e mantém o peso leve."
    )
    exerciseId == "pull" && (
        exerciseName?.contains("sentada", ignoreCase = true) == true ||
            exerciseName?.contains("omoplatas", ignoreCase = true) == true
        ) -> ExerciseGuide(
            focus = "Costas e postura sem ir ao chão",
            steps = listOf("Senta-te ou fica de pé com o tronco confortável.", "Aproxima suavemente as omoplatas sem subir os ombros.", "Relaxa e repete sem prender a respiração."),
            watchOut = "Mantém o movimento pequeno e confortável."
        )
    exerciseId == "pull" && exerciseName?.contains("apoio", ignoreCase = true) == true -> ExerciseGuide(
        focus = "Costas com apoio",
        steps = listOf("Apoia uma mão numa superfície estável.", "Leva o cotovelo livre para trás, sem rodar o tronco.", "Regressa devagar e troca de lado."),
        watchOut = "Não uses uma cadeira instável como apoio."
    )
    exerciseId == "bridge" && (
        exerciseName?.contains("em pé", ignoreCase = true) == true ||
            exerciseName?.contains("apoiada", ignoreCase = true) == true
        ) -> ExerciseGuide(
            focus = "Glúteos e anca em pé",
            steps = listOf("Fica de pé junto a uma parede ou cadeira estável.", "Leva uma perna ligeiramente para trás sem arquear as costas.", "Regressa ao centro com controlo e alterna."),
            watchOut = "Usa apoio se precisares e evita inclinar o tronco."
        )
    exerciseId == "dead-bug" && (
        exerciseName?.contains("em pé", ignoreCase = true) == true ||
            exerciseName?.contains("sentado", ignoreCase = true) == true
        ) -> ExerciseGuide(
            focus = "Coordenação e controlo em pé",
            steps = listOf("Fica de pé com apoio próximo, se necessário.", "Leva um braço à frente enquanto elevas ligeiramente a perna oposta.", "Regressa ao centro e alterna sem perder o equilíbrio."),
            watchOut = "Faz o movimento sentado se preferires; não precisas de elevar muito a perna."
        )
    exerciseId == "cat-cow" && (
        exerciseName?.contains("em pé", ignoreCase = true) == true ||
            exerciseName?.contains("sentada", ignoreCase = true) == true
        ) -> ExerciseGuide(
            focus = "Mobilidade da coluna em pé",
            steps = listOf("Fica de pé ou sentado com os pés bem apoiados.", "Arredonda suavemente as costas e regressa a uma posição neutra.", "Coordena o movimento com a respiração natural."),
            watchOut = "Mantém o pescoço confortável e usa uma amplitude pequena."
        )
    exerciseId == "squat" || exerciseId == "sit-stand" -> ExerciseGuide(
        focus = "Pernas e sentar/levantar",
        steps = listOf("Pés apoiados e afastados à largura confortável.", "Leva a anca para trás e dobra os joelhos.", "Empurra o chão para regressar, sem pressa."),
        watchOut = "Usa a cadeira ou a parede como apoio; reduz a amplitude se perderes o equilíbrio."
    )
    (exerciseId == "push" || exerciseId == "wall-push") &&
        exerciseName == "Pressão na parede" -> ExerciseGuide(
        focus = "Peito e braços",
        steps = listOf("Fica de frente para a parede com as mãos à altura do peito.", "Empurra a parede suavemente e mantém alguns segundos.", "Relaxa sem prender a respiração."),
        watchOut = "Mantém os ombros relaxados e usa apenas uma força confortável."
    )
    exerciseId == "push" || exerciseId == "wall-push" -> ExerciseGuide(
        focus = "Peito, braços e ombros",
        steps = listOf("Coloca as mãos na parede à altura do peito.", "Mantém o corpo alinhado e aproxima o peito.", "Empurra a parede para voltar, expirando com naturalidade."),
        watchOut = "Afasta os pés para tornar o movimento mais fácil e evita bloquear os cotovelos."
    )
    exerciseId == "pull" -> ExerciseGuide(
        focus = "Costas e postura",
        steps = listOf("Mantém o tronco estável e os ombros relaxados.", "Leva os cotovelos para trás.", "Regressa devagar à posição inicial."),
        watchOut = "Se usares mochila, confirma que está fechada e leve; segue a variação indicada para o teu treino."
    )
    exerciseId == "bridge" -> ExerciseGuide(
        focus = "Glúteos e anca",
        steps = listOf("Deita-te com os pés apoiados e joelhos dobrados.", "Eleva a bacia até onde for confortável.", "Desce devagar, mantendo a respiração natural."),
        watchOut = "Evita arquear a zona lombar; uma elevação pequena também conta."
    )
    exerciseId == "dead-bug" -> ExerciseGuide(
        focus = "Controlo do tronco",
        steps = listOf("Deita-te de costas numa posição confortável.", "Move lentamente o braço e a perna opostos.", "Regressa ao centro e alterna o lado."),
        watchOut = "Reduz o alcance ou escolhe a alternativa se não conseguires manter o controlo."
    )
    exerciseId == "march" -> ExerciseGuide(
        focus = "Movimento leve e equilíbrio",
        steps = listOf("Fica de pé ou sentado com apoio próximo.", "Eleva um pé de cada vez num ritmo confortável.", "Mantém a respiração livre e abranda quando precisares."),
        watchOut = "Podes fazer sentado; não precisas de elevar muito os joelhos."
    )
    exerciseId == "easy-march" -> ExerciseGuide(
        focus = "Ativação suave e equilíbrio",
        steps = listOf("Escolhe ficar sentado ou de pé com apoio por perto.", "Alterna os pés devagar, sem precisares de elevar muito os joelhos.", "Mantém um ritmo em que consigas respirar e falar confortavelmente."),
        watchOut = "Podes parar ou continuar sentado se te sentires mais confortável."
    )
    exerciseId == "calf-raise" || exerciseId == "ankles" -> ExerciseGuide(
        focus = "Tornozelos e pernas",
        steps = listOf("Apoia-te numa parede ou cadeira, se necessário.", "Eleva os calcanhares ou move os tornozelos devagar.", "Regressa ao início com controlo."),
        watchOut = "Mantém o apoio estável e evita forçar a amplitude."
    )
    exerciseId == "cat-cow" -> ExerciseGuide(
        focus = "Mobilidade suave da coluna",
        steps = listOf("Fica em quatro apoios ou sentado, com o tronco confortável.", "Arredonda e depois alonga suavemente as costas, sem forçar o pescoço.", "Coordena o movimento com a respiração natural."),
        watchOut = "Reduz a amplitude ou faz sentado; interrompe se surgir dor."
    )
    exerciseId == "reach" || exerciseId == "rotation" || exerciseId == "hip-flow" ||
        exerciseId == "shoulder-roll" || exerciseId == "breathing" -> ExerciseGuide(
        focus = "Mobilidade e respiração",
        steps = listOf("Encontra uma posição confortável.", "Move devagar, acompanhando o teu alcance natural.", "Faz uma pausa e respira sem forçar."),
        watchOut = "Interrompe se sentires dor aguda, tontura ou desconforto invulgar."
    )
    else -> ExerciseGuide(
        focus = "Movimento controlado",
        steps = listOf("Começa numa posição estável.", "Move devagar e dentro de uma amplitude confortável.", "Regressa ao início sem prender a respiração."),
        watchOut = "Adapta ou interrompe se surgir dor ou desconforto invulgar."
    )
}

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

data class ShortChallenge(val title: String, val prompt: String, val encouragement: String)

fun hydrationProgress(intakeMl: Int, goalMl: Int): Float {
    require(goalMl > 0) { "Hydration goal must be positive" }
    return (intakeMl.coerceAtLeast(0).toFloat() / goalMl).coerceIn(0f, 1f)
}

fun shortChallengeFor(dayOfWeek: Int, goal: TrainingGoal): ShortChallenge {
    require(dayOfWeek in 1..7) { "Day of week must use Calendar values (1..7)" }
    val movement = when (dayOfWeek) {
        1 -> "Recomeço gentil"
        2 -> "Pausa de ombros"
        3 -> "Pernas despertas"
        4 -> "Respira e alonga"
        5 -> "Força tranquila"
        6 -> "Movimento livre"
        else -> "Domingo sem pressa"
    }
    val focus = when (goal) {
        TrainingGoal.STRENGTH -> "força sem pressa"
        TrainingGoal.MOBILITY -> "mobilidade confortável"
        TrainingGoal.CONSISTENCY -> "um pequeno compromisso contigo"
    }
    return ShortChallenge(
        title = movement,
        prompt = "Reserva 8 minutos para $focus. Faz o que conseguires, com conforto.",
        encouragement = "Não é um teste. É só uma forma simples de aparecer por ti hoje."
    )
}

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
            TrainingGoal.MOBILITY -> mobilityExercises(profile)
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
) : String {
    val baseReason = when {
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
    return if (profile.prefersNoFloorExercises) {
        "$baseReason Priorizámos opções de pé ou sentado, sem exercícios no chão."
    } else {
        baseReason
    }
}

private fun TrainingGoal.title() = when (this) {
    TrainingGoal.STRENGTH -> "Força no teu ritmo"
    TrainingGoal.MOBILITY -> "Mobilidade tranquila"
    TrainingGoal.CONSISTENCY -> "Um pouco já conta"
}

fun coachMessage(
    profile: TrainingProfile,
    energy: EnergyLevel,
    availableMinutes: Int,
    streakDays: Int,
    recentMood: SessionMood? = null
): String {
    val focus = when (profile.goal) {
        TrainingGoal.STRENGTH -> "força"
        TrainingGoal.MOBILITY -> "mobilidade"
        TrainingGoal.CONSISTENCY -> "consistência"
    }
    val energyNote = when (energy) {
        EnergyLevel.LOW -> "Hoje parece ser um dia de respeito pelo corpo: um treino curto e agradável vale mais do que " +
            "uma sessão de esforço máximo."
        EnergyLevel.STEADY -> "Hoje tens energia suficiente para manter um ritmo estável e sem pressão."
        EnergyLevel.HIGH -> "Hoje tens energia para avançar com um pouco mais de intensidade, sem perder o controlo."
    }
    val timeNote = when {
        availableMinutes <= 10 -> "Como tens pouco tempo, a melhor aposta é começar e terminar bem, sem forçar."
        availableMinutes <= 20 -> "Com este tempo, a chave é consistência e boa forma, não volume."
        else -> "Com um bloco maior, aproveita para manter o ritmo e não perder a qualidade."
    }
    val streakNote = when {
        streakDays >= 5 -> "Estás a construir um ritmo muito bonito. Mantém a linha e deixa este momentum falar por ti."
        streakDays >= 2 -> "Estás a ganhar consistência. Hoje não precisas de ser perfeito; precisas de continuar."
        else -> "Hoje é uma boa oportunidade para criar uma base sólida para a próxima semana."
    }
    val moodNote = when (recentMood) {
        SessionMood.EXHAUSTED -> "Da última vez sentiste cansaço; hoje, escolhe um ritmo especialmente gentil."
        SessionMood.OKAY -> "Da última vez sentiste-te estável; vamos manter essa sensação sem pressa."
        SessionMood.STRONG -> "Da última vez sentiste-te com força; aproveita essa confiança sem ignorar os teus limites."
        null -> ""
    }
    return listOf(streakNote, energyNote, timeNote, moodNote, "Foca-te em $focus e vai com suavidade.")
        .filter(String::isNotBlank)
        .joinToString(" ")
}

fun habitScoreFor(
    completedSessions: Int,
    sessionsLastSevenDays: Int,
    streakDays: Int,
    recentMood: SessionMood? = null
): Int {
    val baseScore = ((completedSessions.coerceAtLeast(0) * 18) +
        (sessionsLastSevenDays.coerceAtMost(7) * 10) +
        (streakDays * 8))
    val moodBonus = when (recentMood) {
        SessionMood.STRONG -> 8
        SessionMood.OKAY -> 4
        SessionMood.EXHAUSTED -> 2
        null -> 0
    }
    return (baseScore + moodBonus).coerceAtMost(100)
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
        name = when {
            profile.hasBackpack -> "Remada com mochila leve"
            profile.prefersNoFloorExercises -> "Retração escapular sentada ou de pé"
            else -> "Elevação em W no chão"
        },
        instruction = if (profile.hasBackpack) {
            "Usa uma mochila leve e bem fechada; puxa os cotovelos para trás sem balanço."
        } else if (profile.prefersNoFloorExercises) {
            "Sentado ou de pé, aproxima suavemente as omoplatas sem elevar os ombros."
        } else {
            "Deita-te de barriga para baixo e aproxima suavemente as omoplatas."
        },
        alternative = if (profile.hasBackpack) {
            if (profile.prefersNoFloorExercises) {
                ExerciseVariation("Remada em pé com apoio", "Apoia uma mão numa superfície estável e leva o cotovelo livre para trás.")
            } else {
                ExerciseVariation("Elevação em W no chão", "Deita-te de barriga para baixo e aproxima suavemente as omoplatas.")
            }
        } else if (profile.prefersNoFloorExercises) {
            ExerciseVariation("Apertar as omoplatas", "Fica sentado ou de pé e aproxima as omoplatas suavemente.")
        } else {
            ExerciseVariation("Retração escapular", "Sentado ou de pé, aproxima as omoplatas sem elevar os ombros.")
        }
    ),
    ExerciseTemplate(
        id = "bridge",
        name = if (profile.prefersNoFloorExercises) "Extensão da anca em pé" else "Ponte de glúteos",
        instruction = if (profile.prefersNoFloorExercises) {
            "Junto a uma parede ou cadeira estável, leva uma perna ligeiramente para trás sem arquear as costas."
        } else {
            "Sobe a bacia sem arquear a zona lombar."
        },
        alternative = if (profile.prefersNoFloorExercises) {
            ExerciseVariation("Extensão curta em pé com apoio", "Com apoio estável, move uma perna poucos centímetros para trás.")
        } else {
            ExerciseVariation("Ponte curta", "Sobe a bacia apenas alguns centímetros e mantém a respiração natural.")
        }
    ),
    ExerciseTemplate(
        id = "dead-bug",
        name = if (profile.prefersNoFloorExercises) "Alcance diagonal em pé" else "Dead bug",
        instruction = if (profile.prefersNoFloorExercises) {
            "De pé ou sentado com apoio próximo, move o braço e a perna opostos dentro de um alcance confortável."
        } else {
            "Move um braço e a perna oposta sem levantar a lombar do chão."
        },
        alternative = if (profile.prefersNoFloorExercises) {
            ExerciseVariation("Alcance diagonal sentado", "Sentado numa cadeira estável, alterna braço e perna opostos sem pressa.")
        } else {
            ExerciseVariation("Bird dog", "Em quatro apoios, estende braço e perna opostos sem perder o equilíbrio.")
        }
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

private fun mobilityExercises(profile: TrainingProfile) = listOf(
    if (profile.prefersNoFloorExercises) {
        ExerciseTemplate("cat-cow", "Mobilidade da coluna em pé", "De pé ou sentado, arredonda suavemente as costas e regressa ao neutro.", ExerciseVariation("Inclinação pélvica sentada", "Sentado numa cadeira estável, inclina ligeiramente a bacia para a frente e para trás."))
    } else {
        ExerciseTemplate("cat-cow", "Gato e vaca", "Em quatro apoios ou sentado, mobiliza a coluna sem forçar.", ExerciseVariation("Inclinação pélvica", "Sentado, inclina ligeiramente a bacia para a frente e para trás."))
    },
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
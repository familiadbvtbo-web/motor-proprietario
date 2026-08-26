package com.motorproprietario.app

data class AlertPolicy(
    val minScoreChange: Double = 5.0,
    val minProbabilityChange: Double = 5.0,
    val minDeterministicChange: Double = 5.0,
    val maxFalseSignalRisk: Double = 65.0,
    val cooldownMinutes: Int = 15,
    val requireGoodData: Boolean = true,
    val alertOnDirectionChange: Boolean = true,
    val alertOnEntryValidityChange: Boolean = true
)

data class AlertState(
    val direction: String = "NEUTRO",
    val score: Double = 50.0,
    val probability: Double = 33.33,
    val deterministicConfidence: Double = 50.0,
    val falseSignalRisk: Double = 0.0,
    val entryValid: Boolean = false,
    val timestamp: Long = 0L
)

data class AlertEvaluation(
    val shouldAlert: Boolean,
    val reason: String
)

object AlertEngine {

    fun evaluate(
        previous: AlertState,
        current: AlertState,
        quality: String,
        now: Long,
        policy: AlertPolicy = AlertPolicy()
    ): AlertEvaluation {

        if (
            policy.requireGoodData &&
            quality != "GOOD"
        ) {
            return AlertEvaluation(
                shouldAlert = false,
                reason = "DADOS_NAO_CONFIAVEIS"
            )
        }

        if (
            current.falseSignalRisk >=
                policy.maxFalseSignalRisk
        ) {
            return AlertEvaluation(
                shouldAlert = false,
                reason = "RISCO_DE_FALSO_SINAL_ALTO"
            )
        }

        val cooldown =
            policy.cooldownMinutes *
                60_000L

        if (
            previous.timestamp > 0L &&
            now - previous.timestamp <
                cooldown
        ) {
            return AlertEvaluation(
                shouldAlert = false,
                reason = "COOLDOWN_ATIVO"
            )
        }

        if (
            policy.alertOnDirectionChange &&
            previous.direction !=
                current.direction
        ) {

            return AlertEvaluation(
                shouldAlert = true,
                reason = "MUDANCA_DE_DIRECAO"
            )
        }

        if (
            policy.alertOnEntryValidityChange &&
            !previous.entryValid &&
            current.entryValid
        ) {

            return AlertEvaluation(
                shouldAlert = true,
                reason = "ENTRADA_TORNOU_SE_VALIDA"
            )
        }

        val scoreChange =
            kotlin.math.abs(
                current.score -
                    previous.score
            )

        if (
            scoreChange >=
                policy.minScoreChange
        ) {

            return AlertEvaluation(
                shouldAlert = true,
                reason = "MUDANCA_RELEVANTE_DE_SCORE"
            )
        }

        val probabilityChange =
            kotlin.math.abs(
                current.probability -
                    previous.probability
            )

        if (
            probabilityChange >=
                policy.minProbabilityChange
        ) {

            return AlertEvaluation(
                shouldAlert = true,
                reason = "MUDANCA_RELEVANTE_DE_PROBABILIDADE"
            )
        }

        val deterministicChange =
            kotlin.math.abs(
                current.deterministicConfidence -
                    previous.deterministicConfidence
            )

        if (
            deterministicChange >=
                policy.minDeterministicChange
        ) {

            return AlertEvaluation(
                shouldAlert = true,
                reason = "MUDANCA_RELEVANTE_DE_DETERMINISMO"
            )
        }

        return AlertEvaluation(
            shouldAlert = false,
            reason = "SEM_MUDANCA_RELEVANTE"
        )
    }
}

fun shouldAlert(
    previousScore: Double,
    currentScore: Double,
    quality: String,
    policy: AlertPolicy
): Boolean {

    if (
        policy.requireGoodData &&
        quality != "GOOD"
    ) {
        return false
    }

    return kotlin.math.abs(
        currentScore -
            previousScore
    ) >=
        policy.minScoreChange
}

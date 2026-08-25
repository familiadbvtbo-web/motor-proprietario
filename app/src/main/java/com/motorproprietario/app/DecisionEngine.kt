package com.motorproprietario.app

data class DecisionInput(
    val score: Double,
    val fsi: FsiResult,
    val sequenceConfirmed: Boolean
)

data class DecisionResult(
    val decision: String,
    val reason: String,
    val executableInPaper: Boolean
)

object DecisionEngine {

    fun evaluate(input: DecisionInput): DecisionResult {

        val score = input.score.coerceIn(0.0, 100.0)

        if (input.fsi.blocked) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "FSI_CRITICO",
                executableInPaper = false
            )
        }

        if (input.fsi.extraConfirmationRequired &&
            !input.sequenceConfirmed
        ) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "CONFIRMACAO_ADICIONAL",
                executableInPaper = false
            )
        }

        if (!input.sequenceConfirmed) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "SEQUENCIA_NAO_CONFIRMADA",
                executableInPaper = false
            )
        }

        return when {
            score >= 75.0 -> DecisionResult(
                decision = "COMPRA",
                reason = "SCORE_FORTE",
                executableInPaper = true
            )

            score <= 25.0 -> DecisionResult(
                decision = "VENDA",
                reason = "SCORE_FRACO",
                executableInPaper = true
            )

            else -> DecisionResult(
                decision = "AGUARDAR",
                reason = "SCORE_NEUTRO",
                executableInPaper = false
            )
        }
    }
}

package com.motorproprietario.app

data class DecisionInput(
    val score: Double,
    val fsi: FsiResult,
    val sequenceConfirmed: Boolean,

    val probability: ProbabilityResult? = null
)

data class DecisionResult(
    val decision: String,
    val reason: String,
    val executableInPaper: Boolean
)

object DecisionEngine {

    fun evaluate(
        input: DecisionInput
    ): DecisionResult {

        val score =
            input.score.coerceIn(
                0.0,
                100.0
            )

        if (
            input.fsi.blocked
        ) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "FSI_CRITICO",
                executableInPaper = false
            )
        }

        if (
            input.fsi.extraConfirmationRequired &&
            !input.sequenceConfirmed
        ) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "CONFIRMACAO_ADICIONAL",
                executableInPaper = false
            )
        }

        if (
            !input.sequenceConfirmed
        ) {
            return DecisionResult(
                decision = "AGUARDAR",
                reason = "SEQUENCIA_NAO_CONFIRMADA",
                executableInPaper = false
            )
        }

        val probability =
            input.probability

        if (
            probability != null
        ) {

            val buy =
                probability.buyProbability

            val sell =
                probability.sellProbability

            val neutral =
                probability.neutralProbability

            /*
             * Compra forte.
             */
            if (
                probability.directionalBias ==
                    "COMPRA" &&
                buy >= 70.0 &&
                buy > sell + 8.0 &&
                buy > neutral &&
                score >= 60.0
            ) {
                return DecisionResult(
                    decision = "COMPRA",
                    reason = "PROBABILIDADE_COMPRA_CONFIRMADA",
                    executableInPaper = true
                )
            }

            /*
             * Venda forte.
             */
            if (
                probability.directionalBias ==
                    "VENDA" &&
                sell >= 70.0 &&
                sell > buy + 8.0 &&
                sell > neutral &&
                score <= 40.0
            ) {
                return DecisionResult(
                    decision = "VENDA",
                    reason = "PROBABILIDADE_VENDA_CONFIRMADA",
                    executableInPaper = true
                )
            }

            /*
             * Probabilidade moderada:
             * não libera execução.
             */
            if (
                buy >= 60.0 &&
                buy > sell + 5.0 &&
                buy > neutral
            ) {
                return DecisionResult(
                    decision = "AGUARDAR",
                    reason = "COMPRA_MODERADA_SEM_DOMINANCIA",
                    executableInPaper = false
                )
            }

            if (
                sell >= 60.0 &&
                sell > buy + 5.0 &&
                sell > neutral
            ) {
                return DecisionResult(
                    decision = "AGUARDAR",
                    reason = "VENDA_MODERADA_SEM_DOMINANCIA",
                    executableInPaper = false
                )
            }

            return DecisionResult(
                decision = "AGUARDAR",
                reason = "PROBABILIDADE_INSUFICIENTE",
                executableInPaper = false
            )
        }

        /*
         * Compatibilidade com chamadas antigas.
         */
        return when {

            score >= 75.0 ->
                DecisionResult(
                    decision = "COMPRA",
                    reason = "SCORE_FORTE",
                    executableInPaper = true
                )

            score <= 25.0 ->
                DecisionResult(
                    decision = "VENDA",
                    reason = "SCORE_FRACO",
                    executableInPaper = true
                )

            else ->
                DecisionResult(
                    decision = "AGUARDAR",
                    reason = "SCORE_NEUTRO",
                    executableInPaper = false
                )
        }
    }
}

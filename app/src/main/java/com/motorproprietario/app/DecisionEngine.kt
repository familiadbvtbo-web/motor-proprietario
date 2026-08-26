package com.motorproprietario.app

data class DecisionInput(
    val score: Double,
    val fsi: FsiResult,
    val sequenceConfirmed: Boolean,

    /*
     * Motor probabilístico.
     */
    val probability: ProbabilityResult? = null,

    /*
     * Motor determinístico.
     *
     * Mantido opcional nesta etapa para preservar
     * compatibilidade com o restante do projeto.
     */
    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0,

    /*
     * Melhor controle de falso sinal.
     */
    val falseSignalRisk: Double = 0.0,

    /*
     * Confluência dos timeframes.
     */
    val mtfConfluence: Double = 50.0
)

data class DecisionResult(
    val decision: String,
    val reason: String,
    val executableInPaper: Boolean,

    /*
     * Percentuais finais.
     *
     * São o resultado da combinação da lógica
     * probabilística com a lógica determinística.
     */
    val buyProbability: Double = 0.0,
    val sellProbability: Double = 0.0,
    val neutralProbability: Double = 100.0,

    /*
     * Força da lógica determinística.
     */
    val deterministicConfidence: Double = 50.0,

    /*
     * Risco de falso sinal utilizado
     * na decisão.
     */
    val falseSignalRisk: Double = 0.0,

    /*
     * Confluência MTF.
     */
    val mtfConfluence: Double = 0.0
)

object DecisionEngine {

    private fun clamp(
        value: Double,
        min: Double = 0.0,
        max: Double = 100.0
    ): Double {

        return value.coerceIn(
            min,
            max
        )
    }

    /*
     * Normaliza três probabilidades para que
     * COMPRA + VENDA + NEUTRO = 100%.
     */
    private fun normalizeProbabilities(
        buy: Double,
        sell: Double,
        neutral: Double
    ): Triple<Double, Double, Double> {

        val safeBuy =
            buy.coerceAtLeast(0.0)

        val safeSell =
            sell.coerceAtLeast(0.0)

        val safeNeutral =
            neutral.coerceAtLeast(0.0)

        val total =
            safeBuy +
                safeSell +
                safeNeutral

        if (
            total <= 0.0
        ) {

            return Triple(
                33.33,
                33.33,
                33.34
            )
        }

        return Triple(
            safeBuy / total * 100.0,
            safeSell / total * 100.0,
            safeNeutral / total * 100.0
        )
    }

    /*
     * Combina:
     *
     * PROBABILIDADE
     * +
     * DETERMINISMO
     *
     * O determinismo funciona como uma segunda
     * camada de validação do sinal.
     */
    private fun combineProbabilityAndDeterminism(
        input: DecisionInput
    ): Triple<Double, Double, Double> {

        val probability =
            input.probability

        /*
         * Se o motor probabilístico ainda não
         * estiver conectado, utilizamos o score
         * como compatibilidade.
         */
        val probabilityBuy =
            probability?.buyProbability
                ?: input.score

        val probabilitySell =
            probability?.sellProbability
                ?: (
                    100.0 -
                        input.score
                    )

        val probabilityNeutral =
            probability?.neutralProbability
                ?: 0.0

        val pBuy =
            clamp(
                probabilityBuy
            )

        val pSell =
            clamp(
                probabilitySell
            )

        val pNeutral =
            clamp(
                probabilityNeutral
            )

        val dBuy =
            clamp(
                input.deterministicBuy
            )

        val dSell =
            clamp(
                input.deterministicSell
            )

        val dNeutral =
            clamp(
                input.deterministicNeutral
            )

        /*
         * Peso inicial:
         *
         * 60% Probabilidade
         * 40% Determinismo
         *
         * Essa proporção poderá ser calibrada
         * posteriormente através de backtest.
         */
        val rawBuy =
            pBuy * 0.60 +
                dBuy * 0.40

        val rawSell =
            pSell * 0.60 +
                dSell * 0.40

        val rawNeutral =
            pNeutral * 0.60 +
                dNeutral * 0.40

        /*
         * Falso sinal reduz a confiança direcional
         * e aumenta a neutralidade.
         */
        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val riskFactor =
            1.0 -
                falseRisk / 100.0

        val adjustedBuy =
            rawBuy *
                (
                    0.65 +
                        riskFactor * 0.35
                )

        val adjustedSell =
            rawSell *
                (
                    0.65 +
                        riskFactor * 0.35
                )

        val adjustedNeutral =
            rawNeutral +
                falseRisk * 0.35

        /*
         * MTF aumenta a confiabilidade somente
         * quando existe confluência.
         */
        val mtf =
            clamp(
                input.mtfConfluence
            )

        val mtfFactor =
            0.85 +
                (
                    mtf / 100.0
                ) * 0.15

        val finalBuy =
            adjustedBuy *
                mtfFactor

        val finalSell =
            adjustedSell *
                mtfFactor

        val finalNeutral =
            adjustedNeutral +
                (
                    100.0 -
                        mtf
                ) * 0.15

        return normalizeProbabilities(
            finalBuy,
            finalSell,
            finalNeutral
        )
    }

    fun evaluate(
        input: DecisionInput
    ): DecisionResult {

        val score =
            clamp(
                input.score
            )

        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val mtf =
            clamp(
                input.mtfConfluence
            )

        val deterministicConfidence =
            clamp(
                input.deterministicConfidence
            )

        /*
         * ==================================
         * 1. BLOQUEIO DE FSI CRÍTICO
         * ==================================
         */

        if (
            input.fsi.blocked
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "FSI_CRITICO",

                executableInPaper =
                    false,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf,

                deterministicConfidence =
                    deterministicConfidence
            )
        }

        /*
         * ==================================
         * 2. SEQUÊNCIA
         * ==================================
         */

        if (
            input.fsi.extraConfirmationRequired &&
            !input.sequenceConfirmed
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "CONFIRMACAO_ADICIONAL",

                executableInPaper =
                    false,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf,

                deterministicConfidence =
                    deterministicConfidence
            )
        }

        if (
            !input.sequenceConfirmed
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "SEQUENCIA_NAO_CONFIRMADA",

                executableInPaper =
                    false,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf,

                deterministicConfidence =
                    deterministicConfidence
            )
        }

        /*
         * ==================================
         * 3. FUSÃO PROBABILIDADE +
         *    DETERMINISMO
         * ==================================
         */

        val combined =
            combineProbabilityAndDeterminism(
                input
            )

        val buyProbability =
            combined.first

        val sellProbability =
            combined.second

        val neutralProbability =
            combined.third

        /*
         * ==================================
         * 4. DOMINÂNCIA DIRECIONAL
         * ==================================
         */

        val strongest =
            maxOf(
                buyProbability,
                sellProbability,
                neutralProbability
            )

        val secondStrongest =
            listOf(
                buyProbability,
                sellProbability,
                neutralProbability
            )
                .sortedDescending()
                .getOrElse(1) {
                    0.0
                }

        val dominance =
            strongest -
                secondStrongest

        /*
         * ==================================
         * 5. PROTEÇÃO DETERMINÍSTICA
         * ==================================
         *
         * Se a probabilidade aponta uma direção,
         * mas o determinismo aponta fortemente
         * contra ela, o Motor não entra.
         */

        val deterministicConflictBuy =
            input.deterministicSell >
                input.deterministicBuy + 20.0

        val deterministicConflictSell =
            input.deterministicBuy >
                input.deterministicSell + 20.0

        /*
         * ==================================
         * 6. COMPRA
         * ==================================
         */

        if (
            buyProbability >= 70.0 &&
            buyProbability >
                sellProbability + 8.0 &&
            buyProbability >
                neutralProbability &&
            dominance >= 8.0 &&
            score >= 55.0 &&
            !deterministicConflictBuy &&
            deterministicConfidence >= 45.0 &&
            falseRisk < 65.0
        ) {

            return DecisionResult(
                decision =
                    "COMPRA",

                reason =
                    "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",

                executableInPaper =
                    true,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 7. VENDA
         * ==================================
         */

        if (
            sellProbability >= 70.0 &&
            sellProbability >
                buyProbability + 8.0 &&
            sellProbability >
                neutralProbability &&
            dominance >= 8.0 &&
            score <= 45.0 &&
            !deterministicConflictSell &&
            deterministicConfidence >= 45.0 &&
            falseRisk < 65.0
        ) {

            return DecisionResult(
                decision =
                    "VENDA",

                reason =
                    "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",

                executableInPaper =
                    true,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 8. CONFLITO
         * ==================================
         */

        if (
            buyProbability >= 60.0 &&
            sellProbability >= 60.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "CONFLITO_ENTRE_DIRECOES",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 9. DETERMINISMO FRACO
         * ==================================
         */

        if (
            deterministicConfidence < 40.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "DETERMINISMO_INSUFICIENTE",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 10. FALSO SINAL ELEVADO
         * ==================================
         */

        if (
            falseRisk >= 60.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "RISCO_DE_FALSO_SINAL_ELEVADO",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 11. MTF FRACO
         * ==================================
         */

        if (
            mtf < 50.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "CONFLUENCIA_MTF_INSUFICIENTE",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 12. PROBABILIDADE MODERADA
         * ==================================
         */

        if (
            buyProbability >= 60.0 &&
            buyProbability >
                sellProbability + 5.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "COMPRA_MODERADA_AGUARDAR_TIMING",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        if (
            sellProbability >= 60.0 &&
            sellProbability >
                buyProbability + 5.0
        ) {

            return DecisionResult(
                decision =
                    "AGUARDAR",

                reason =
                    "VENDA_MODERADA_AGUARDAR_TIMING",

                executableInPaper =
                    false,

                buyProbability =
                    buyProbability,

                sellProbability =
                    sellProbability,

                neutralProbability =
                    neutralProbability,

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
            )
        }

        /*
         * ==================================
         * 13. NEUTRO
         * ==================================
         */

        return DecisionResult(
            decision =
                "AGUARDAR",

            reason =
                "PROBABILIDADE_E_DETERMINISMO_SEM_DOMINANCIA",

            executableInPaper =
                false,

            buyProbability =
                buyProbability,

            sellProbability =
                sellProbability,

            neutralProbability =
                neutralProbability,

            deterministicConfidence =
                deterministicConfidence,

            falseSignalRisk =
                falseRisk,

            mtfConfluence =
                mtf
        )
    }
}

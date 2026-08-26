package com.motorproprietario.app

data class DecisionInput(
    val score: Double,
    val fsi: FsiResult,
    val sequenceConfirmed: Boolean,

    val probability: ProbabilityResult? = null,

    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0,

    val falseSignalRisk: Double = 0.0,
    val mtfConfluence: Double = 50.0,

    /*
     * Parâmetros de calibração.
     *
     * Estes valores NÃO representam taxa de acerto.
     * São apenas os pesos utilizados na fusão.
     *
     * O backtest poderá substituí-los posteriormente.
     */
    val probabilityWeight: Double = 0.50,
    val deterministicWeight: Double = 0.50
)

data class DecisionResult(
    val decision: String,
    val reason: String,
    val executableInPaper: Boolean,

    val buyProbability: Double = 0.0,
    val sellProbability: Double = 0.0,
    val neutralProbability: Double = 100.0,

    val deterministicConfidence: Double = 50.0,
    val falseSignalRisk: Double = 0.0,
    val mtfConfluence: Double = 0.0
)

object DecisionEngine {

    private fun clamp(
        value: Double
    ): Double {

        return value.coerceIn(
            0.0,
            100.0
        )
    }

    private fun normalize(
        buy: Double,
        sell: Double,
        neutral: Double
    ): Triple<Double, Double, Double> {

        val b =
            buy.coerceAtLeast(0.0)

        val s =
            sell.coerceAtLeast(0.0)

        val n =
            neutral.coerceAtLeast(0.0)

        val total =
            b + s + n

        if (
            total <= 0.0 ||
            !total.isFinite()
        ) {

            return Triple(
                33.33,
                33.33,
                33.34
            )
        }

        return Triple(
            b / total * 100.0,
            s / total * 100.0,
            n / total * 100.0
        )
    }

    /*
     * Garante que os pesos sejam válidos.
     *
     * Se ambos forem zero ou inválidos,
     * retorna 50/50.
     */
    private fun calibratedWeights(
        probabilityWeight: Double,
        deterministicWeight: Double
    ): Pair<Double, Double> {

        val p =
            probabilityWeight
                .takeIf {
                    it.isFinite() &&
                    it >= 0.0
                }
                ?: 0.50

        val d =
            deterministicWeight
                .takeIf {
                    it.isFinite() &&
                    it >= 0.0
                }
                ?: 0.50

        val total =
            p + d

        if (
            total <= 0.0
        ) {

            return Pair(
                0.50,
                0.50
            )
        }

        return Pair(
            p / total,
            d / total
        )
    }

    /*
     * ==================================
     * FUSÃO CALIBRÁVEL
     * ==================================
     *
     * Não existe mais um 60/40 fixo.
     *
     * O peso é fornecido pelo DecisionInput
     * e poderá ser alterado pelo calibrador.
     */
    private fun combine(
        input: DecisionInput
    ): Triple<Double, Double, Double> {

        val probability =
            input.probability

        val probabilityBuy =
            probability
                ?.buyProbability
                ?: input.score

        val probabilitySell =
            probability
                ?.sellProbability
                ?: (
                    100.0 -
                        input.score
                    )

        val probabilityNeutral =
            probability
                ?.neutralProbability
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

        val weights =
            calibratedWeights(
                input.probabilityWeight,
                input.deterministicWeight
            )

        val probabilityWeight =
            weights.first

        val deterministicWeight =
            weights.second

        var buy =
            pBuy *
                probabilityWeight +
            dBuy *
                deterministicWeight

        var sell =
            pSell *
                probabilityWeight +
            dSell *
                deterministicWeight

        var neutral =
            pNeutral *
                probabilityWeight +
            dNeutral *
                deterministicWeight

        /*
         * ==================================
         * FSI
         * ==================================
         */

        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val riskFactor =
            (
                1.0 -
                    falseRisk / 100.0
            )
                .coerceIn(
                    0.0,
                    1.0
                )

        val directionalFactor =
            0.60 +
                riskFactor * 0.40

        buy *=
            directionalFactor

        sell *=
            directionalFactor

        neutral +=
            falseRisk * 0.35

        /*
         * ==================================
         * MTF
         * ==================================
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

        buy *=
            mtfFactor

        sell *=
            mtfFactor

        neutral +=
            (
                100.0 -
                    mtf
            ) * 0.20

        /*
         * ==================================
         * DETERMINISMO FRACO
         * ==================================
         */

        val deterministicConfidence =
            clamp(
                input.deterministicConfidence
            )

        if (
            deterministicConfidence < 40.0
        ) {

            val penalty =
                (
                    40.0 -
                        deterministicConfidence
                ) * 0.50

            buy *=
                1.0 -
                    penalty / 100.0

            sell *=
                1.0 -
                    penalty / 100.0

            neutral +=
                penalty
        }

        return normalize(
            buy,
            sell,
            neutral
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
         * 1. FSI
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
         * 2. CONFIRMAÇÃO
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

                deterministicConfidence =
                    deterministicConfidence,

                falseSignalRisk =
                    falseRisk,

                mtfConfluence =
                    mtf
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
         * 3. FUSÃO
         * ==================================
         */

        val final =
            combine(
                input
            )

        val buy =
            final.first

        val sell =
            final.second

        val neutral =
            final.third

        /*
         * ==================================
         * 4. DOMINÂNCIA
         * ==================================
         */

        val strongest =
            maxOf(
                buy,
                sell,
                neutral
            )

        val second =
            listOf(
                buy,
                sell,
                neutral
            )
                .sortedDescending()
                .getOrElse(1) {
                    0.0
                }

        val dominance =
            strongest -
                second

        /*
         * ==================================
         * 5. CONFLITO DETERMINÍSTICO
         * ==================================
         */

        val buyConflict =
            input.deterministicSell >
                input.deterministicBuy +
                20.0

        val sellConflict =
            input.deterministicBuy >
                input.deterministicSell +
                20.0

        /*
         * ==================================
         * 6. FALSO SINAL
         * ==================================
         */

        if (
            falseRisk >= 65.0
        ) {

            return result(
                "RISCO_DE_FALSO_SINAL_ELEVADO",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        /*
         * ==================================
         * 7. MTF
         * ==================================
         */

        if (
            mtf < 50.0
        ) {

            return result(
                "CONFLUENCIA_MTF_INSUFICIENTE",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        /*
         * ==================================
         * 8. DETERMINISMO
         * ==================================
         */

        if (
            deterministicConfidence < 40.0
        ) {

            return result(
                "DETERMINISMO_INSUFICIENTE",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        /*
         * ==================================
         * 9. COMPRA
         * ==================================
         */

        if (
            buy >= 70.0 &&
            buy >
                sell + 8.0 &&
            buy >
                neutral &&
            dominance >= 8.0 &&
            score >= 55.0 &&
            !buyConflict &&
            deterministicConfidence >= 45.0
        ) {

            return DecisionResult(
                decision =
                    "COMPRA",

                reason =
                    "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",

                executableInPaper =
                    true,

                buyProbability =
                    buy,

                sellProbability =
                    sell,

                neutralProbability =
                    neutral,

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
         * 10. VENDA
         * ==================================
         */

        if (
            sell >= 70.0 &&
            sell >
                buy + 8.0 &&
            sell >
                neutral &&
            dominance >= 8.0 &&
            score <= 45.0 &&
            !sellConflict &&
            deterministicConfidence >= 45.0
        ) {

            return DecisionResult(
                decision =
                    "VENDA",

                reason =
                    "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",

                executableInPaper =
                    true,

                buyProbability =
                    buy,

                sellProbability =
                    sell,

                neutralProbability =
                    neutral,

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
         * 11. CONFLITO
         * ==================================
         */

        if (
            buy >= 60.0 &&
            sell >= 60.0
        ) {

            return result(
                "CONFLITO_ENTRE_DIRECOES",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        /*
         * ==================================
         * 12. DIREÇÃO MODERADA
         * ==================================
         */

        if (
            buy >= 60.0 &&
            buy >
                sell + 5.0
        ) {

            return result(
                "COMPRA_MODERADA_AGUARDAR_TIMING",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        if (
            sell >= 60.0 &&
            sell >
                buy + 5.0
        ) {

            return result(
                "VENDA_MODERADA_AGUARDAR_TIMING",
                buy,
                sell,
                neutral,
                deterministicConfidence,
                falseRisk,
                mtf
            )
        }

        /*
         * ==================================
         * 13. NEUTRO
         * ==================================
         */

        return result(
            "SEM_DOMINANCIA_SUFICIENTE",
            buy,
            sell,
            neutral,
            deterministicConfidence,
            falseRisk,
            mtf
        )
    }

    private fun result(
        reason: String,
        buy: Double,
        sell: Double,
        neutral: Double,
        deterministicConfidence: Double,
        falseRisk: Double,
        mtf: Double
    ): DecisionResult {

        return DecisionResult(

            decision =
                "AGUARDAR",

            reason =
                reason,

            executableInPaper =
                false,

            buyProbability =
                buy,

            sellProbability =
                sell,

            neutralProbability =
                neutral,

            deterministicConfidence =
                deterministicConfidence,

            falseSignalRisk =
                falseRisk,

            mtfConfluence =
                mtf
        )
    }
}

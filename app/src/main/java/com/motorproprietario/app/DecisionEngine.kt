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
     * Pesos de fusão.
     * Não são taxa de acerto.
     * Só devem ser calibrados depois de backtest.
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

    private const val FSI_BLOCK_THRESHOLD = 65.0
    private const val MTF_MINIMUM = 50.0
    private const val DETERMINISTIC_MINIMUM = 40.0

    private const val STRONG_PROBABILITY = 70.0
    private const val MODERATE_PROBABILITY = 60.0

    private const val STRONG_DOMINANCE = 8.0
    private const val MODERATE_DOMINANCE = 5.0

    private fun clamp(value: Double): Double {
        return if (value.isFinite()) {
            value.coerceIn(0.0, 100.0)
        } else {
            0.0
        }
    }

    private fun normalize(
        buy: Double,
        sell: Double,
        neutral: Double
    ): Triple<Double, Double, Double> {

        val b = if (buy.isFinite()) buy.coerceAtLeast(0.0) else 0.0
        val s = if (sell.isFinite()) sell.coerceAtLeast(0.0) else 0.0
        val n = if (neutral.isFinite()) neutral.coerceAtLeast(0.0) else 0.0

        val total = b + s + n

        if (!total.isFinite() || total <= 0.0) {
            return Triple(33.33, 33.33, 33.34)
        }

        return Triple(
            b / total * 100.0,
            s / total * 100.0,
            n / total * 100.0
        )
    }

    private fun calibratedWeights(
        probabilityWeight: Double,
        deterministicWeight: Double
    ): Pair<Double, Double> {

        val p = probabilityWeight.takeIf {
            it.isFinite() && it >= 0.0
        } ?: 0.50

        val d = deterministicWeight.takeIf {
            it.isFinite() && it >= 0.0
        } ?: 0.50

        val total = p + d

        if (!total.isFinite() || total <= 0.0) {
            return 0.50 to 0.50
        }

        return p / total to d / total
    }

    /*
     * Fusão das evidências.
     *
     * Importante:
     * - probabilidade, determinismo, FSI e MTF não são independentes;
     * - FSI/MTF reduzem direção e aumentam neutralidade;
     * - não há transformação artificial de 50 em certeza.
     */
    private fun combine(
        input: DecisionInput
    ): Triple<Double, Double, Double> {

        val probability = input.probability

        val pBuy = clamp(
            probability?.buyProbability ?: input.score
        )

        val pSell = clamp(
            probability?.sellProbability ?: (100.0 - clamp(input.score))
        )

        val pNeutral = clamp(
            probability?.neutralProbability ?: 0.0
        )

        val dBuy = clamp(input.deterministicBuy)
        val dSell = clamp(input.deterministicSell)
        val dNeutral = clamp(input.deterministicNeutral)

        val weights = calibratedWeights(
            input.probabilityWeight,
            input.deterministicWeight
        )

        var buy =
            pBuy * weights.first +
                dBuy * weights.second

        var sell =
            pSell * weights.first +
                dSell * weights.second

        var neutral =
            pNeutral * weights.first +
                dNeutral * weights.second

        val falseRisk = clamp(input.falseSignalRisk)
        val mtf = clamp(input.mtfConfluence)
        val deterministicConfidence =
            clamp(input.deterministicConfidence)

        /*
         * Risco elevado reduz exposição direcional.
         * O piso evita zerar artificialmente uma evidência real.
         */
        val riskFactor =
            (1.0 - falseRisk / 100.0)
                .coerceIn(0.0, 1.0)

        val directionalFactor =
            (0.60 + riskFactor * 0.40)
                .coerceIn(0.60, 1.0)

        buy *= directionalFactor
        sell *= directionalFactor

        neutral += falseRisk * 0.35

        /*
         * MTF fraco não deve aumentar COMPRA/VENDA.
         * MTF forte também não cria direção sozinho.
         */
        val mtfFactor =
            (0.85 + mtf / 100.0 * 0.15)
                .coerceIn(0.85, 1.0)

        buy *= mtfFactor
        sell *= mtfFactor

        neutral += (100.0 - mtf) * 0.20

        /*
         * Determinismo insuficiente aumenta neutralidade.
         */
        if (deterministicConfidence < DETERMINISTIC_MINIMUM) {

            val weakness =
                ((DETERMINISTIC_MINIMUM - deterministicConfidence) * 0.50)
                    .coerceIn(0.0, 20.0)

            buy *= 1.0 - weakness / 100.0
            sell *= 1.0 - weakness / 100.0
            neutral += weakness
        }

        return normalize(buy, sell, neutral)
    }

    fun evaluate(
        input: DecisionInput
    ): DecisionResult {

        val score = clamp(input.score)
        val falseRisk = clamp(input.falseSignalRisk)
        val mtf = clamp(input.mtfConfluence)
        val deterministicConfidence =
            clamp(input.deterministicConfidence)

        /*
         * GATE 1 — FSI
         *
         * Bloqueio vem antes da direção.
         */
        if (input.fsi.blocked || falseRisk >= FSI_BLOCK_THRESHOLD) {
            return blockedResult(
                reason = "FSI_CRITICO",
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        /*
         * GATE 2 — sequência.
         *
         * Sem sequência confirmada, não existe entrada liberada.
         */
        if (
            input.fsi.extraConfirmationRequired &&
            !input.sequenceConfirmed
        ) {
            return blockedResult(
                reason = "CONFIRMACAO_ADICIONAL",
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        if (!input.sequenceConfirmed) {
            return blockedResult(
                reason = "SEQUENCIA_NAO_CONFIRMADA",
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        val final = combine(input)

        val buy = final.first
        val sell = final.second
        val neutral = final.third

        val sorted = listOf(buy, sell, neutral).sortedDescending()
        val strongest = sorted[0]
        val second = sorted[1]
        val dominance = strongest - second

        /*
         * Conflito estrutural entre as duas direções.
         */
        val buyConflict =
            clamp(input.deterministicSell) >
                clamp(input.deterministicBuy) + 20.0

        val sellConflict =
            clamp(input.deterministicBuy) >
                clamp(input.deterministicSell) + 20.0

        /*
         * GATE 3 — MTF.
         */
        if (mtf < MTF_MINIMUM) {
            return result(
                reason = "CONFLUENCIA_MTF_INSUFICIENTE",
                buy = buy,
                sell = sell,
                neutral = neutral,
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        /*
         * GATE 4 — determinismo.
         */
        if (deterministicConfidence < DETERMINISTIC_MINIMUM) {
            return result(
                reason = "DETERMINISMO_INSUFICIENTE",
                buy = buy,
                sell = sell,
                neutral = neutral,
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        /*
         * COMPRA FORTE.
         *
         * O score global continua sendo uma evidência adicional,
         * não substituto da fusão.
         */
        if (
            buy >= STRONG_PROBABILITY &&
            buy > sell + 8.0 &&
            buy > neutral &&
            dominance >= STRONG_DOMINANCE &&
            score >= 55.0 &&
            !buyConflict &&
            deterministicConfidence >= 45.0
        ) {
            return DecisionResult(
                decision = "COMPRA",
                reason = "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",
                executableInPaper = true,
                buyProbability = buy,
                sellProbability = sell,
                neutralProbability = neutral,
                deterministicConfidence = deterministicConfidence,
                falseSignalRisk = falseRisk,
                mtfConfluence = mtf
            )
        }

        /*
         * VENDA FORTE.
         */
        if (
            sell >= STRONG_PROBABILITY &&
            sell > buy + 8.0 &&
            sell > neutral &&
            dominance >= STRONG_DOMINANCE &&
            score <= 45.0 &&
            !sellConflict &&
            deterministicConfidence >= 45.0
        ) {
            return DecisionResult(
                decision = "VENDA",
                reason = "PROBABILIDADE_E_DETERMINISMO_CONFIRMADOS",
                executableInPaper = true,
                buyProbability = buy,
                sellProbability = sell,
                neutralProbability = neutral,
                deterministicConfidence = deterministicConfidence,
                falseSignalRisk = falseRisk,
                mtfConfluence = mtf
            )
        }

        /*
         * Conflito explícito.
         */
        if (buy >= MODERATE_PROBABILITY && sell >= MODERATE_PROBABILITY) {
            return result(
                reason = "CONFLITO_ENTRE_DIRECOES",
                buy = buy,
                sell = sell,
                neutral = neutral,
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        /*
         * Direção moderada:
         * ainda não libera execução; aguarda timing.
         */
        if (
            buy >= MODERATE_PROBABILITY &&
            buy > sell + MODERATE_DOMINANCE
        ) {
            return result(
                reason = "COMPRA_MODERADA_AGUARDAR_TIMING",
                buy = buy,
                sell = sell,
                neutral = neutral,
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        if (
            sell >= MODERATE_PROBABILITY &&
            sell > buy + MODERATE_DOMINANCE
        ) {
            return result(
                reason = "VENDA_MODERADA_AGUARDAR_TIMING",
                buy = buy,
                sell = sell,
                neutral = neutral,
                deterministicConfidence = deterministicConfidence,
                falseRisk = falseRisk,
                mtf = mtf
            )
        }

        return result(
            reason = "SEM_DOMINANCIA_SUFICIENTE",
            buy = buy,
            sell = sell,
            neutral = neutral,
            deterministicConfidence = deterministicConfidence,
            falseRisk = falseRisk,
            mtf = mtf
        )
    }

    private fun blockedResult(
        reason: String,
        deterministicConfidence: Double,
        falseRisk: Double,
        mtf: Double
    ): DecisionResult {

        return DecisionResult(
            decision = "AGUARDAR",
            reason = reason,
            executableInPaper = false,
            buyProbability = 0.0,
            sellProbability = 0.0,
            neutralProbability = 100.0,
            deterministicConfidence = deterministicConfidence,
            falseSignalRisk = falseRisk,
            mtfConfluence = mtf
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
            decision = "AGUARDAR",
            reason = reason,
            executableInPaper = false,
            buyProbability = clamp(buy),
            sellProbability = clamp(sell),
            neutralProbability = clamp(neutral),
            deterministicConfidence = deterministicConfidence,
            falseSignalRisk = falseRisk,
            mtfConfluence = mtf
        )
    }
}

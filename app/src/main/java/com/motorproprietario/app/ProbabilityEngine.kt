package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ProbabilityInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val falseSignalRisk: Double,
    val fibonacciBullish: Double = 50.0,
    val fibonacciBearish: Double = 50.0,
    val institutionalBullish: Double = 50.0,
    val institutionalBearish: Double = 50.0
)

data class ProbabilityResult(
    val buyProbability: Double,
    val sellProbability: Double,
    val neutralProbability: Double,
    val directionalBias: String,
    val confidence: Double,

    // Probabilidade antes do provisionamento
    val rawBuyProbability: Double = 0.0,
    val rawSellProbability: Double = 0.0,

    // Desconto provocado pelo risco
    val provisionPenalty: Double = 0.0,

    // Qualidade da evidência
    val evidenceStrength: Double = 0.0
)

object ProbabilityEngine {

    private fun clamp(
        value: Double,
        minValue: Double = 0.0,
        maxValue: Double = 100.0
    ): Double {
        return value.coerceIn(
            minValue,
            maxValue
        )
    }

    private fun normalize(
        buy: Double,
        sell: Double,
        neutral: Double
    ): Triple<Double, Double, Double> {

        val b = max(0.0, buy)
        val s = max(0.0, sell)
        val n = max(0.0, neutral)

        val total = b + s + n

        if (total <= 0.0) {
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

    private fun directionalEvidence(
        value: Double
    ): Double {
        return clamp(value) - 50.0
    }

    fun calculate(
        input: ProbabilityInput
    ): ProbabilityResult {

        val m = input.metrics

        val trend =
            clamp(m.trend)

        val momentum =
            clamp(m.momentum)

        val structure =
            clamp(m.structure)

        val volume =
            clamp(m.volume)

        val candle =
            clamp(m.candlePattern)

        val breakout =
            clamp(m.breakout)

        val divergence =
            clamp(m.divergence)

        val mtf =
            clamp(input.mtfConfluence)

        val falseRisk =
            clamp(input.falseSignalRisk)

        val macdEvidence =
            when {
                m.macd > m.macdSignal -> 65.0
                m.macd < m.macdSignal -> 35.0
                else -> 50.0
            }

        val emaEvidence =
            when {
                m.ema9 > m.ema21 -> 65.0
                m.ema9 < m.ema21 -> 35.0
                else -> 50.0
            }

        val emaLongEvidence =
            when {
                m.ema21 > m.ema50 -> 65.0
                m.ema21 < m.ema50 -> 35.0
                else -> 50.0
            }

        val rsiEvidence =
            when {
                m.rsi > 55.0 && m.rsi < 70.0 -> 65.0
                m.rsi >= 70.0 -> 55.0
                m.rsi < 45.0 && m.rsi > 30.0 -> 35.0
                m.rsi <= 30.0 -> 45.0
                else -> 50.0
            }

        val fibonacciBuy =
            clamp(
                input.fibonacciBullish
            )

        val fibonacciSell =
            clamp(
                input.fibonacciBearish
            )

        val institutionalBuy =
            clamp(
                input.institutionalBullish
            )

        val institutionalSell =
            clamp(
                input.institutionalBearish
            )

        /*
         * Grupos de evidência.
         *
         * Evitamos contar EMA/MACD como evidências
         * completamente independentes.
         */

        val trendGroup =
            (
                trend * 0.50 +
                emaEvidence * 0.25 +
                emaLongEvidence * 0.25
            )

        val momentumGroup =
            (
                momentum * 0.45 +
                rsiEvidence * 0.30 +
                macdEvidence * 0.25
            )

        val priceActionGroup =
            (
                structure * 0.35 +
                candle * 0.25 +
                breakout * 0.25 +
                divergence * 0.15
            )

        val buyRaw =
            trendGroup * 0.22 +
            momentumGroup * 0.16 +
            priceActionGroup * 0.20 +
            volume * 0.08 +
            mtf * 0.12 +
            fibonacciBuy * 0.07 +
            institutionalBuy * 0.15

        val sellRaw =
            (100.0 - trendGroup) * 0.22 +
            (100.0 - momentumGroup) * 0.16 +
            (100.0 - priceActionGroup) * 0.20 +
            (100.0 - volume) * 0.08 +
            (100.0 - mtf) * 0.12 +
            fibonacciSell * 0.07 +
            institutionalSell * 0.15

        /*
         * Divergência forte contra a direção
         * recebe penalização adicional.
         */
        val buyDivergencePenalty =
            if (divergence < 40.0) {
                (50.0 - divergence) * 0.35
            } else {
                0.0
            }

        val sellDivergencePenalty =
            if (divergence > 60.0) {
                (divergence - 50.0) * 0.35
            } else {
                0.0
            }

        var buy =
            max(
                0.0,
                buyRaw -
                    buyDivergencePenalty
            )

        var sell =
            max(
                0.0,
                sellRaw -
                    sellDivergencePenalty
            )

        /*
         * Confluência MTF baixa não destrói o sinal,
         * mas reduz sua força.
         */
        val mtfFactor =
            0.70 +
                mtf / 100.0 * 0.30

        buy *= mtfFactor
        sell *= mtfFactor

        /*
         * Provisionamento progressivo.
         *
         * 0 risco = nenhum desconto.
         * 100 risco = forte redução.
         */
        val provisionFactor =
            1.0 -
                (
                    falseRisk / 100.0
                ) * 0.65

        val rawBuy =
            buy

        val rawSell =
            sell

        buy *= provisionFactor
        sell *= provisionFactor

        /*
         * Conflito direcional.
         */
        val directionalDifference =
            abs(
                buy - sell
            )

        /*
         * Quanto mais equilibrados os lados,
         * maior a neutralidade.
         */
        var neutral =
            12.0

        neutral +=
            when {
                directionalDifference < 4.0 ->
                    35.0

                directionalDifference < 8.0 ->
                    22.0

                directionalDifference < 14.0 ->
                    12.0

                else ->
                    0.0
            }

        /*
         * Falso sinal gera provisionamento.
         */
        neutral +=
            falseRisk * 0.42

        /*
         * Volatilidade extrema sem confirmação
         * aumenta cautela.
         */
        if (
            m.volatility >= 80.0 &&
            mtf < 60.0
        ) {
            neutral += 12.0
        }

        /*
         * ADX muito baixo significa pouca força
         * de tendência.
         */
        if (m.adx < 20.0) {
            neutral += 10.0
        }

        neutral =
            clamp(
                neutral,
                5.0,
                92.0
            )

        val normalized =
            normalize(
                buy,
                sell,
                neutral
            )

        val buyProbability =
            normalized.first

        val sellProbability =
            normalized.second

        val neutralProbability =
            normalized.third

        val directionalBias =
            when {
                buyProbability >=
                    sellProbability &&
                    buyProbability >=
                    neutralProbability ->
                    "COMPRA"

                sellProbability >=
                    buyProbability &&
                    sellProbability >=
                    neutralProbability ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        val strongest =
            max(
                buyProbability,
                sellProbability
            )

        val evidenceStrength =
            clamp(
                (
                    abs(
                        buyRaw -
                            sellRaw
                    ) * 1.15 +
                    mtf * 0.25 +
                    m.adx * 0.15 -
                    falseRisk * 0.35
                )
            )

        val confidence =
            clamp(
                (
                    strongest -
                        neutralProbability
                ) * 1.20 +
                    evidenceStrength * 0.25 -
                    falseRisk * 0.20
            )

        val provisionPenalty =
            clamp(
                falseRisk * 0.65
            )

        return ProbabilityResult(
            buyProbability =
                buyProbability,

            sellProbability =
                sellProbability,

            neutralProbability =
                neutralProbability,

            directionalBias =
                directionalBias,

            confidence =
                confidence,

            rawBuyProbability =
                clamp(rawBuy),

            rawSellProbability =
                clamp(rawSell),

            provisionPenalty =
                provisionPenalty,

            evidenceStrength =
                evidenceStrength
        )
    }
}

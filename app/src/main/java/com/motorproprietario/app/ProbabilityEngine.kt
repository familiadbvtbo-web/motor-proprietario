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
    val confidence: Double
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

        val safeBuy =
            max(0.0, buy)

        val safeSell =
            max(0.0, sell)

        val safeNeutral =
            max(0.0, neutral)

        val total =
            safeBuy +
                safeSell +
                safeNeutral

        if (total <= 0.0) {
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

    fun calculate(
        input: ProbabilityInput
    ): ProbabilityResult {

        val m =
            input.metrics

        /*
         * Cada componente produz evidência
         * entre 0 e 100.
         *
         * 50 = neutro
         * >50 = comprador
         * <50 = vendedor
         */

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

        /*
         * MACD é convertido para uma escala
         * direcional centrada em 50.
         */
        val macdEvidence =
            when {

                m.macd > m.macdSignal ->
                    65.0

                m.macd < m.macdSignal ->
                    35.0

                else ->
                    50.0
            }

        /*
         * EMA 9 x EMA 21.
         */
        val emaEvidence =
            when {

                m.ema9 > m.ema21 ->
                    65.0

                m.ema9 < m.ema21 ->
                    35.0

                else ->
                    50.0
            }

        /*
         * ADX mede força da tendência.
         * Ele não determina sozinho a direção.
         */
        val trendStrength =
            clamp(
                m.adx
            )

        /*
         * Fibonacci e fluxo institucional
         * entram como evidências adicionais.
         */
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
         * Peso dos componentes.
         *
         * Indicadores correlacionados não recebem
         * peso excessivo individualmente.
         */
        val buyEvidence =
            (
                trend * 0.18 +
                momentum * 0.10 +
                structure * 0.12 +
                volume * 0.08 +
                candle * 0.08 +
                breakout * 0.10 +
                macdEvidence * 0.10 +
                emaEvidence * 0.08 +
                fibonacciBuy * 0.06 +
                institutionalBuy * 0.10
            )

        val sellEvidence =
            (
                (100.0 - trend) * 0.18 +
                (100.0 - momentum) * 0.10 +
                (100.0 - structure) * 0.12 +
                (100.0 - volume) * 0.08 +
                (100.0 - candle) * 0.08 +
                (100.0 - breakout) * 0.10 +
                (100.0 - macdEvidence) * 0.10 +
                (100.0 - emaEvidence) * 0.08 +
                fibonacciSell * 0.06 +
                institutionalSell * 0.10
            )

        /*
         * Divergência reduz a convicção
         * quando está apontando contra a direção.
         */
        val divergencePenalty =
            abs(
                divergence - 50.0
            ) * 0.35

        /*
         * Confluência MTF aumenta a confiança
         * somente quando existe concordância.
         */
        val mtfFactor =
            clamp(
                input.mtfConfluence
            ) / 100.0

        /*
         * ADX funciona como confirmação da força,
         * não como direção.
         */
        val strengthFactor =
            0.75 +
                (
                    trendStrength / 100.0
                ) * 0.25

        var buy =
            buyEvidence *
                strengthFactor *
                (
                    0.75 +
                        mtfFactor * 0.25
                )

        var sell =
            sellEvidence *
                strengthFactor *
                (
                    0.75 +
                        mtfFactor * 0.25
                )

        /*
         * Provisionamento:
         * quanto maior o risco de falso sinal,
         * menor a confiança direcional.
         */
        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val riskFactor =
            1.0 -
                falseRisk / 100.0

        buy *=
            0.60 +
                riskFactor * 0.40

        sell *=
            0.60 +
                riskFactor * 0.40

        /*
         * Se os dois lados estão muito próximos,
         * aumenta a probabilidade de NEUTRO.
         */
        val directionalDifference =
            abs(
                buy - sell
            )

        var neutral =
            20.0

        if (
            directionalDifference < 5.0
        ) {
            neutral += 30.0
        } else if (
            directionalDifference < 10.0
        ) {
            neutral += 15.0
        }

        /*
         * FSI elevado aumenta neutralidade.
         */
        neutral +=
            falseRisk * 0.30

        /*
         * Divergência forte também aumenta cautela.
         */
        neutral +=
            divergencePenalty * 0.30

        /*
         * Evita neutralidade exagerada.
         */
        neutral =
            clamp(
                neutral,
                5.0,
                90.0
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

        /*
         * Confiança não é taxa histórica de acerto.
         *
         * É a força da evidência atual.
         */
        val strongestProbability =
            max(
                buyProbability,
                sellProbability
            )

        val confidence =
            clamp(
                (
                    strongestProbability -
                        neutralProbability
                ) *
                    1.15
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
                confidence
        )
    }
}

package com.motorproprietario.app

import kotlin.math.abs

data class MultiTimeframeResult(
    val confluence: Double,
    val bullish: Double,
    val bearish: Double,
    val neutral: Double,
    val direction: String,
    val strongestTimeframe: String,
    val analyzedTimeframes: Int
)

object MultiTimeframeEngine {

    private val timeframeOrder =
        listOf(
            "M1",
            "M5",
            "M15",
            "M30",
            "H1",
            "H4",
            "D1",
            "W1",
            "MN1",
            "Y1"
        )

    /*
     * Pesos hierárquicos.
     *
     * Timeframes maiores possuem maior peso
     * para o contexto estrutural.
     *
     * Timeframes menores continuam importantes
     * para o timing.
     */
    private val timeframeWeights =
        mapOf(
            "M1" to 0.40,
            "M5" to 0.55,
            "M15" to 0.70,
            "M30" to 0.85,
            "H1" to 1.00,
            "H4" to 1.15,
            "D1" to 1.30,
            "W1" to 1.45,
            "MN1" to 1.60,
            "Y1" to 1.75
        )

    private fun clamp(
        value: Double
    ): Double {

        if (!value.isFinite()) {
            return 0.0
        }

        return value.coerceIn(
            0.0,
            100.0
        )
    }

    private fun directionScore(
        metrics: QuantMetrics
    ): Double {

        /*
         * 0   = forte vendedor
         * 50  = neutro
         * 100 = forte comprador
         */
        val trend =
            metrics.trend

        val structure =
            metrics.structure

        val momentum =
            metrics.momentum

        val breakout =
            metrics.breakout

        val force =
            metrics.forceIndex

        val emaBias =
            when {

                metrics.ema9 >
                    metrics.ema21 &&
                metrics.ema21 >
                    metrics.ema50 ->
                    100.0

                metrics.ema9 <
                    metrics.ema21 &&
                metrics.ema21 <
                    metrics.ema50 ->
                    0.0

                metrics.ema9 >
                    metrics.ema21 ->
                    65.0

                metrics.ema9 <
                    metrics.ema21 ->
                    35.0

                else ->
                    50.0
            }

        return clamp(
            trend * 0.22 +
                structure * 0.18 +
                momentum * 0.18 +
                breakout * 0.12 +
                force * 0.15 +
                emaBias * 0.15
        )
    }

    private fun strength(
        score: Double
    ): Double {

        return clamp(
            abs(
                score -
                    50.0
            ) * 2.0
        )
    }

    fun calculate(
        metricsByTimeframe:
            Map<String, QuantMetrics>
    ): MultiTimeframeResult {

        if (
            metricsByTimeframe.isEmpty()
        ) {

            return MultiTimeframeResult(
                confluence =
                    0.0,

                bullish =
                    0.0,

                bearish =
                    0.0,

                neutral =
                    100.0,

                direction =
                    "NEUTRO",

                strongestTimeframe =
                    "--",

                analyzedTimeframes =
                    0
            )
        }

        var bullishWeighted =
            0.0

        var bearishWeighted =
            0.0

        var neutralWeighted =
            0.0

        var totalWeight =
            0.0

        var strongestTimeframe =
            "--"

        var strongestStrength =
            -1.0

        for (
            timeframe in timeframeOrder
        ) {

            val metrics =
                metricsByTimeframe[
                    timeframe
                ]
                    ?: continue

            val weight =
                timeframeWeights[
                    timeframe
                ]
                    ?: 1.0

            val score =
                directionScore(
                    metrics
                )

            val strength =
                strength(
                    score
                )

            /*
             * Força direcional:
             *
             * > 50 = comprador
             * < 50 = vendedor
             * próximo de 50 = neutro
             */
            when {

                score >= 55.0 -> {

                    bullishWeighted +=
                        (
                            score -
                                50.0
                        ) *
                            weight
                }

                score <= 45.0 -> {

                    bearishWeighted +=
                        (
                            50.0 -
                                score
                        ) *
                            weight
                }

                else -> {

                    neutralWeighted +=
                        (
                            50.0 -
                                abs(
                                    score -
                                        50.0
                                )
                        ) *
                            weight
                }
            }

            totalWeight +=
                weight

            if (
                strength >
                    strongestStrength
            ) {

                strongestStrength =
                    strength

                strongestTimeframe =
                    timeframe
            }
        }

        if (
            totalWeight <= 0.0
        ) {

            return MultiTimeframeResult(
                confluence =
                    0.0,

                bullish =
                    0.0,

                bearish =
                    0.0,

                neutral =
                    100.0,

                direction =
                    "NEUTRO",

                strongestTimeframe =
                    "--",

                analyzedTimeframes =
                    0
            )
        }

        val bullish =
            clamp(
                bullishWeighted /
                    totalWeight *
                    2.0
            )

        val bearish =
            clamp(
                bearishWeighted /
                    totalWeight *
                    2.0
            )

        val neutral =
            clamp(
                100.0 -
                    bullish -
                    bearish
            )

        /*
         * Confluência mede o quanto os timeframes
         * apontam para a mesma direção.
         *
         * Não significa probabilidade de acerto.
         */
        val directional =
            maxOf(
                bullish,
                bearish
            )

        val disagreement =
            minOf(
                bullish,
                bearish
            )

        val confluence =
            clamp(
                directional -
                    disagreement * 0.75
            )

        val direction =
            when {

                bullish >=
                    bearish &&
                bullish >=
                    neutral &&
                bullish >= 55.0 ->
                    "COMPRA"

                bearish >=
                    bullish &&
                bearish >=
                    neutral &&
                bearish >= 55.0 ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        return MultiTimeframeResult(
            confluence =
                confluence,

            bullish =
                bullish,

            bearish =
                bearish,

            neutral =
                neutral,

            direction =
                direction,

            strongestTimeframe =
                strongestTimeframe,

            analyzedTimeframes =
                metricsByTimeframe
                    .count {
                        it.key in
                            timeframeOrder
                    }
        )
    }

    fun timeframeRank(
        timeframe: String
    ): Int {

        return timeframeOrder
            .indexOf(
                timeframe.uppercase()
            )
            .coerceAtLeast(0)
    }

    fun isHigherTimeframe(
        reference: String,
        candidate: String
    ): Boolean {

        return timeframeRank(
            candidate
        ) >
            timeframeRank(
                reference
            )
    }

    fun higherTimeframes(
        reference: String,
        metrics:
            Map<String, QuantMetrics>
    ): Map<String, QuantMetrics> {

        val referenceRank =
            timeframeRank(
                reference
            )

        return metrics.filterKeys {

            timeframeRank(it) >
                referenceRank
        }
    }
}

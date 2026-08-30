package com.motorproprietario.app

import kotlin.math.abs

data class TimeframeDetail(
    val timeframe: String,
    val score: Double,
    val strength: Double,
    val direction: String,
    val weight: Double
)

data class MultiTimeframeResult(
    val confluence: Double,
    val bullish: Double,
    val bearish: Double,
    val neutral: Double,
    val direction: String,
    val strongestTimeframe: String,
    val analyzedTimeframes: Int,
    val bullishTimeframes: Int,
    val bearishTimeframes: Int,
    val neutralTimeframes: Int,
    val coverage: Double = 0.0,
    val timeframeDetails: List<TimeframeDetail> = emptyList()
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
     * Todos os 10 timeframes participam quando
     * possuem métricas válidas.
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

        val trend =
            metrics.trend

        val structure =
            metrics.structure

        val momentum =
            metrics.momentum

        val breakout =
            metrics.breakout

        val force =
            metrics.forceIndexScore

        val emaBias =
            when {

                metrics.ema9 > metrics.ema21 &&
                    metrics.ema21 > metrics.ema50 ->
                    100.0

                metrics.ema9 < metrics.ema21 &&
                    metrics.ema21 < metrics.ema50 ->
                    0.0

                metrics.ema9 > metrics.ema21 ->
                    65.0

                metrics.ema9 < metrics.ema21 ->
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
                score - 50.0
            ) * 2.0
        )
    }

    private fun direction(
        score: Double
    ): String {

        return when {

            score >= 55.0 ->
                "COMPRA"

            score <= 45.0 ->
                "VENDA"

            else ->
                "NEUTRO"
        }
    }

    fun calculate(
        metricsByTimeframe:
            Map<String, QuantMetrics>
    ): MultiTimeframeResult {

        if (
            metricsByTimeframe.isEmpty()
        ) {

            return MultiTimeframeResult(
                confluence = 0.0,
                bullish = 0.0,
                bearish = 0.0,
                neutral = 100.0,
                direction = "NEUTRO",
                strongestTimeframe = "--",
                analyzedTimeframes = 0,
                bullishTimeframes = 0,
                bearishTimeframes = 0,
                neutralTimeframes = 0,
                coverage = 0.0,
                timeframeDetails = emptyList()
            )
        }

        var bullishWeighted =
            0.0

        var bearishWeighted =
            0.0

        var neutralWeighted =
            0.0

        var bullishTimeframes =
            0

        var bearishTimeframes =
            0

        var neutralTimeframes =
            0

        var totalWeight =
            0.0

        var strongestTimeframe =
            "--"

        var strongestStrength =
            -1.0

        val details =
            ArrayList<TimeframeDetail>()

        /*
         * Os 10 timeframes são percorridos explicitamente.
         */
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

            val localDirection =
                direction(
                    score
                )

            details.add(
                TimeframeDetail(
                    timeframe =
                        timeframe,
                    score =
                        score,
                    strength =
                        strength,
                    direction =
                        localDirection,
                    weight =
                        weight
                )
            )

            when (
                localDirection
            ) {

                "COMPRA" -> {

                    bullishTimeframes++

                    bullishWeighted +=
                        (
                            score -
                                50.0
                        ) *
                        weight
                }

                "VENDA" -> {

                    bearishTimeframes++

                    bearishWeighted +=
                        (
                            50.0 -
                                score
                        ) *
                        weight
                }

                else -> {

                    neutralTimeframes++

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

        val analyzedTimeframes =
            details.size

        val coverage =
            (
                analyzedTimeframes.toDouble() /
                    timeframeOrder.size.toDouble()
            ) *
                100.0

        if (
            totalWeight <= 0.0
        ) {

            return MultiTimeframeResult(
                confluence = 0.0,
                bullish = 0.0,
                bearish = 0.0,
                neutral = 100.0,
                direction = "NEUTRO",
                strongestTimeframe = "--",
                analyzedTimeframes = 0,
                bullishTimeframes = 0,
                bearishTimeframes = 0,
                neutralTimeframes = 0,
                coverage =
                    coverage.coerceIn(
                        0.0,
                        100.0
                    ),
                timeframeDetails =
                    details
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

        val rawConfluence =
            clamp(
                directional -
                    disagreement *
                    0.75
            )

        /*
         * Se algum dos 10 timeframes ainda não estiver disponível,
         * a confluência não pode fingir que analisou os 10.
         */
        val coverageFactor =
            coverage.coerceIn(
                0.0,
                100.0
            ) / 100.0

        val confluence =
            clamp(
                rawConfluence *
                    coverageFactor
            )

        val finalDirection =
            when {

                bullish >= bearish &&
                    bullish >= neutral &&
                    bullish >= 55.0 ->
                    "COMPRA"

                bearish >= bullish &&
                    bearish >= neutral &&
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
                finalDirection,

            strongestTimeframe =
                strongestTimeframe,

            analyzedTimeframes =
                analyzedTimeframes,

            bullishTimeframes =
                bullishTimeframes,

            bearishTimeframes =
                bearishTimeframes,

            neutralTimeframes =
                neutralTimeframes,

            coverage =
                coverage.coerceIn(
                    0.0,
                    100.0
                ),

            timeframeDetails =
                details
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

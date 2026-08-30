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
     * PESOS MTF
     *
     * Timeframes maiores têm maior peso,
     * mas nenhum timeframe pode dominar sozinho.
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

    /*
     * ============================================================
     * SCORE DIRECIONAL
     * ============================================================
     *
     * 50 = equilíbrio
     * >50 = pressão compradora
     * <50 = pressão vendedora
     */
    private fun directionScore(
        metrics: QuantMetrics
    ): Double {

        val emaBias =
            when {

                metrics.ema9 > metrics.ema21 &&
                    metrics.ema21 > metrics.ema50 -> 100.0

                metrics.ema9 < metrics.ema21 &&
                    metrics.ema21 < metrics.ema50 -> 0.0

                metrics.ema9 > metrics.ema21 -> 65.0

                metrics.ema9 < metrics.ema21 -> 35.0

                else -> 50.0
            }

        return clamp(

            metrics.trend * 0.22 +

            metrics.structure * 0.18 +

            metrics.momentum * 0.18 +

            metrics.breakout * 0.12 +

            metrics.forceIndexScore * 0.15 +

            emaBias * 0.15
        )
    }

    /*
     * ============================================================
     * FORÇA
     * ============================================================
     */
    private fun strength(
        score: Double
    ): Double {

        return clamp(
            abs(
                score - 50.0
            ) * 2.0
        )
    }

    /*
     * ============================================================
     * DIREÇÃO
     * ============================================================
     *
     * A faixa neutra foi ampliada para evitar
     * classificar pequenos desvios como direção real.
     */
    private fun direction(
        score: Double
    ): String {

        return when {

            score >= 60.0 ->
                "COMPRA"

            score <= 40.0 ->
                "VENDA"

            else ->
                "NEUTRO"
        }
    }

    /*
     * ============================================================
     * CÁLCULO PRINCIPAL
     * ============================================================
     */
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

                timeframeDetails =
                    emptyList()
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

        var bullishTimeframes =
            0

        var bearishTimeframes =
            0

        var neutralTimeframes =
            0

        var strongestTimeframe =
            "--"

        var strongestStrength =
            -1.0

        val details =
            ArrayList<TimeframeDetail>()

        /*
         * Somente timeframes existentes e válidos
         * entram no cálculo.
         */
        for (
            timeframe in timeframeOrder
        ) {

            val metrics =
                metricsByTimeframe[
                    timeframe
                ]
                    ?: continue

            val values =
                listOf(
                    metrics.trend,
                    metrics.structure,
                    metrics.momentum,
                    metrics.breakout,
                    metrics.forceIndexScore,
                    metrics.ema9,
                    metrics.ema21,
                    metrics.ema50
                )

            /*
             * Não deixa métrica inválida contaminar
             * o cálculo MTF.
             */
            if (
                values.any {
                    !it.isFinite()
                }
            ) {
                continue
            }

            val weight =
                timeframeWeights[
                    timeframe
                ]
                    ?: continue

            val score =
                directionScore(
                    metrics
                )

            val localStrength =
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
                        localStrength,

                    direction =
                        localDirection,

                    weight =
                        weight
                )
            )

            /*
             * ==================================================
             * CLASSIFICAÇÃO
             * ==================================================
             */
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

                    /*
                     * Neutralidade é mantida separada.
                     * Ela não é convertida artificialmente
                     * em compra ou venda.
                     */
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

            /*
             * Melhor timeframe =
             * maior força direcional,
             * não simplesmente o maior peso.
             */
            if (
                localStrength >
                    strongestStrength
            ) {

                strongestStrength =
                    localStrength

                strongestTimeframe =
                    timeframe
            }
        }

        val analyzedTimeframes =
            details.size

        /*
         * ============================================================
         * COBERTURA REAL
         * ============================================================
         */
        val coverage =
            (
                analyzedTimeframes.toDouble() /
                    timeframeOrder.size.toDouble()
            ) *
                100.0

        if (
            analyzedTimeframes == 0 ||
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

        /*
         * ============================================================
         * FORÇA DIRECIONAL NORMALIZADA
         * ============================================================
         */
        val bullishRaw =
            (
                bullishWeighted /
                    totalWeight
            )

        val bearishRaw =
            (
                bearishWeighted /
                    totalWeight
            )

        /*
         * O valor máximo teórico de cada lado
         * é aproximadamente 50.
         *
         * Multiplicamos por 2 para obter 0–100.
         */
        val bullish =
            clamp(
                bullishRaw * 2.0
            )

        val bearish =
            clamp(
                bearishRaw * 2.0
            )

        /*
         * ============================================================
         * NEUTRO
         * ============================================================
         *
         * O neutro não deve ser simplesmente
         * 100 - compra - venda.
         *
         * Isso poderia produzir um neutro artificialmente
         * alto quando há pouca cobertura.
         */
        val directionalTotal =
            (
                bullish +
                    bearish
            ).coerceAtMost(
                100.0
            )

        val neutralBase =
            100.0 -
                directionalTotal

        val neutral =
            clamp(
                neutralBase
            )

        /*
         * ============================================================
         * CONFLUÊNCIA
         * ============================================================
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

        /*
         * Quanto maior o conflito entre compra
         * e venda, menor a confluência.
         */
        val conflictPenalty =
            disagreement *
                0.75

        val rawConfluence =
            clamp(
                directional -
                    conflictPenalty
            )

        /*
         * ============================================================
         * PENALIDADE DE COBERTURA
         * ============================================================
         *
         * Se apenas M15 foi analisado,
         * o sistema não pode declarar 100% de
         * confluência MTF.
         */
        val coverageFactor =
            coverage.coerceIn(
                0.0,
                100.0
            ) /
                100.0

        val confluence =
            clamp(

                rawConfluence *
                    coverageFactor
            )

        /*
         * ============================================================
         * DIREÇÃO FINAL
         * ============================================================
         *
         * Exige vantagem mínima de 10 pontos.
         */
        val finalDirection =
            when {

                bullish >= 60.0 &&
                    bullish >
                    bearish + 10.0 ->

                    "COMPRA"

                bearish >= 60.0 &&
                    bearish >
                    bullish + 10.0 ->

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

    /*
     * ============================================================
     * RANKING
     * ============================================================
     */
    fun timeframeRank(
        timeframe: String
    ): Int {

        return timeframeOrder
            .indexOf(
                timeframe.uppercase()
            )
            .coerceAtLeast(0)
    }

    /*
     * ============================================================
     * TIMEFRAME SUPERIOR
     * ============================================================
     */
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

    /*
     * ============================================================
     * FILTRA TIMEFRAMES SUPERIORES
     * ============================================================
     */
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

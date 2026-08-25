package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class QuantMetrics(
    val ema9: Double,
    val ema21: Double,
    val ema50: Double,
    val rsi: Double,
    val macd: Double,
    val macdSignal: Double,
    val bollingerUpper: Double,
    val bollingerMiddle: Double,
    val bollingerLower: Double,
    val atr: Double,
    val adx: Double,
    val trend: Double,
    val momentum: Double,
    val structure: Double,
    val volume: Double,
    val volatility: Double,
    val support: Double,
    val resistance: Double,
    val breakout: Double,
    val candlePattern: Double,
    val divergence: Double
)

data class RealtimeAnalysis(
    val market: MarketData,
    val metrics: Map<String, QuantMetrics>,
    val score: Double,
    val fsi: Double,
    val falseSignal: Double,
    val mtfConfluence: Double,
    val regime: String,
    val direction: String,
    val decision: String,
    val confidence: Double
)

object RealtimeMarketAnalyzer {

    private fun clamp(
        value: Double,
        minValue: Double = 0.0,
        maxValue: Double = 100.0
    ): Double {

        if (!value.isFinite()) {
            return minValue
        }

        return value.coerceIn(
            minValue,
            maxValue
        )
    }

    private fun safe(
        value: Double,
        fallback: Double = 0.0
    ): Double {

        return if (
            value.isFinite()
        ) {
            value
        } else {
            fallback
        }
    }

    private fun normalizeTimestamp(
        timestamp: Long
    ): Long {

        if (timestamp <= 0L) {
            return 0L
        }

        return if (
            timestamp < 10_000_000_000L
        ) {
            timestamp * 1000L
        } else {
            timestamp
        }
    }

    private fun mean(
        values: List<Double>
    ): Double {

        val valid =
            values.filter {
                it.isFinite()
            }

        return if (
            valid.isEmpty()
        ) {
            0.0
        } else {
            valid.average()
        }
    }

    private fun std(
        values: List<Double>
    ): Double {

        val valid =
            values.filter {
                it.isFinite()
            }

        if (valid.isEmpty()) {
            return 0.0
        }

        val avg =
            valid.average()

        return sqrt(
            valid.map {
                (it - avg).pow(2)
            }.average()
        )
    }

    private fun ema(
        values: List<Double>,
        period: Int
    ): Double {

        val valid =
            values.filter {
                it.isFinite()
            }

        if (valid.isEmpty()) {
            return 0.0
        }

        if (valid.size < period) {
            return valid.last()
        }

        val multiplier =
            2.0 /
                (period + 1)

        var result =
            valid
                .take(period)
                .average()

        for (
            i in period until valid.size
        ) {

            result =
                (
                    valid[i] -
                        result
                ) *
                    multiplier +
                    result
        }

        return safe(result)
    }

    private fun rsi(
        values: List<Double>,
        period: Int = 14
    ): Double {

        if (
            values.size <= period
        ) {
            return 50.0
        }

        var gains = 0.0
        var losses = 0.0

        for (
            i in 1..period
        ) {

            val change =
                values[i] -
                    values[i - 1]

            if (change >= 0.0) {
                gains += change
            } else {
                losses += abs(change)
            }
        }

        var avgGain =
            gains / period

        var avgLoss =
            losses / period

        for (
            i in period + 1 until values.size
        ) {

            val change =
                values[i] -
                    values[i - 1]

            val gain =
                max(
                    0.0,
                    change
                )

            val loss =
                max(
                    0.0,
                    -change
                )

            avgGain =
                (
                    avgGain *
                        (period - 1) +
                        gain
                ) /
                    period

            avgLoss =
                (
                    avgLoss *
                        (period - 1) +
                        loss
                ) /
                    period
        }

        if (
            avgLoss <= 0.0
        ) {

            return if (
                avgGain > 0.0
            ) {
                100.0
            } else {
                50.0
            }
        }

        val rs =
            avgGain /
                avgLoss

        return clamp(
            100.0 -
                (
                    100.0 /
                        (1.0 + rs)
                )
        )
    }

    private fun trueRanges(
        candles: List<MarketCandle>
    ): List<Double> {

        if (
            candles.size < 2
        ) {
            return emptyList()
        }

        val result =
            mutableListOf<Double>()

        for (
            i in 1 until candles.size
        ) {

            val current =
                candles[i]

            val previous =
                candles[i - 1]

            val tr =
                max(
                    current.high -
                        current.low,

                    max(
                        abs(
                            current.high -
                                previous.close
                        ),
                        abs(
                            current.low -
                                previous.close
                        )
                    )
                )

            if (
                tr.isFinite() &&
                tr >= 0.0
            ) {
                result.add(tr)
            }
        }

        return result
    }

    private fun atr(
        candles: List<MarketCandle>,
        period: Int = 14
    ): Double {

        val tr =
            trueRanges(candles)

        if (
            tr.isEmpty()
        ) {
            return 0.0
        }

        return mean(
            tr.takeLast(
                min(
                    period,
                    tr.size
                )
            )
        )
    }

    private fun macd(
        closes: List<Double>
    ): Pair<Double, Double> {

        if (
            closes.size < 35
        ) {
            return 0.0 to 0.0
        }

        val macdValues =
            mutableListOf<Double>()

        for (
            i in 25 until closes.size
        ) {

            val subset =
                closes.subList(
                    0,
                    i + 1
                )

            val fast =
                ema(
                    subset,
                    12
                )

            val slow =
                ema(
                    subset,
                    26
                )

            macdValues.add(
                fast - slow
            )
        }

        if (
            macdValues.isEmpty()
        ) {
            return 0.0 to 0.0
        }

        val current =
            macdValues.last()

        val signal =
            ema(
                macdValues,
                9
            )

        return safe(current) to
            safe(signal)
    }

    private fun bollinger(
        closes: List<Double>,
        period: Int = 20
    ): Triple<Double, Double, Double> {

        if (
            closes.size < period
        ) {

            val last =
                closes.lastOrNull()
                    ?: 0.0

            return Triple(
                last,
                last,
                last
            )
        }

        val window =
            closes.takeLast(
                period
            )

        val middle =
            mean(window)

        val deviation =
            std(window)

        return Triple(
            middle +
                deviation * 2.0,

            middle,

            middle -
                deviation * 2.0
        )
    }

    private fun adx(
        candles: List<MarketCandle>,
        period: Int = 14
    ): Double {

        if (
            candles.size <
            period * 2
        ) {
            return 0.0
        }

        val trs =
            mutableListOf<Double>()

        val plusDm =
            mutableListOf<Double>()

        val minusDm =
            mutableListOf<Double>()

        for (
            i in 1 until candles.size
        ) {

            val current =
                candles[i]

            val previous =
                candles[i - 1]

            val up =
                current.high -
                    previous.high

            val down =
                previous.low -
                    current.low

            plusDm.add(
                if (
                    up > down &&
                    up > 0.0
                ) {
                    up
                } else {
                    0.0
                }
            )

            minusDm.add(
                if (
                    down > up &&
                    down > 0.0
                ) {
                    down
                } else {
                    0.0
                }
            )

            val tr =
                max(
                    current.high -
                        current.low,

                    max(
                        abs(
                            current.high -
                                previous.close
                        ),
                        abs(
                            current.low -
                                previous.close
                        )
                    )
                )

            if (
                tr.isFinite()
            ) {
                trs.add(tr)
            }
        }

        if (
            trs.isEmpty()
        ) {
            return 0.0
        }

        val tr =
            mean(
                trs.takeLast(period)
            )

        if (
            tr <= 0.0
        ) {
            return 0.0
        }

        val plus =
            mean(
                plusDm.takeLast(period)
            ) /
                tr *
                100.0

        val minus =
            mean(
                minusDm.takeLast(period)
            ) /
                tr *
                100.0

        if (
            plus + minus <= 0.0
        ) {
            return 0.0
        }

        return clamp(
            abs(
                plus - minus
            ) /
                (
                    plus + minus
                ) *
                100.0
        )
    }

    private fun structure(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 20
        ) {
            return 50.0
        }

        val recent =
            candles.takeLast(20)

        val high =
            recent.maxOf {
                it.high
            }

        val low =
            recent.minOf {
                it.low
            }

        val price =
            recent.last().close

        val range =
            high - low

        if (
            range <= 0.0
        ) {
            return 50.0
        }

        return clamp(
            (
                price - low
            ) /
                range *
                100.0
        )
    }

    private fun volumeScore(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 20
        ) {
            return 50.0
        }

        val recent =
            mean(
                candles
                    .takeLast(5)
                    .map {
                        it.volume
                    }
            )

        val baseline =
            mean(
                candles
                    .drop(
                        candles.size - 20
                    )
                    .take(10)
                    .map {
                        it.volume
                    }
            )

        if (
            baseline <= 0.0
        ) {
            return 50.0
        }

        return clamp(
            50.0 +
                (
                    recent /
                        baseline -
                        1.0
                ) *
                    50.0
        )
    }

    private fun volatilityScore(
        candles: List<MarketCandle>
    ): Double {

        val closes =
            candles.map {
                it.close
            }

        if (
            closes.size < 20
        ) {
            return 50.0
        }

        val returns =
            mutableListOf<Double>()

        for (
            i in 1 until closes.size
        ) {

            val previous =
                closes[i - 1]

            if (
                previous == 0.0
            ) {
                continue
            }

            val change =
                abs(
                    (
                        closes[i] -
                            previous
                    ) /
                        previous
                ) *
                    100.0

            if (
                change.isFinite()
            ) {
                returns.add(change)
            }
        }

        if (
            returns.isEmpty()
        ) {
            return 50.0
        }

        val current =
            mean(
                returns.takeLast(5)
            )

        val baseline =
            mean(
                returns.takeLast(20)
            )

        if (
            baseline <= 0.0
        ) {
            return 50.0
        }

        return clamp(
            50.0 +
                (
                    current /
                        baseline -
                        1.0
                ) *
                    50.0
        )
    }

    private fun candlePattern(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 3
        ) {
            return 50.0
        }

        val b =
            candles[
                candles.size - 2
            ]

        val c =
            candles.last()

        val bullishEngulfing =
            b.close < b.open &&
                c.close > c.open &&
                c.close >= b.open &&
                c.open <= b.close

        val bearishEngulfing =
            b.close > b.open &&
                c.close < c.open &&
                c.open >= b.close &&
                c.close <= b.open

        val body =
            abs(
                c.close -
                    c.open
            )

        val range =
            c.high -
                c.low

        if (
            range <= 0.0
        ) {
            return 50.0
        }

        val upperWick =
            c.high -
                max(
                    c.open,
                    c.close
                )

        val lowerWick =
            min(
                c.open,
                c.close
            ) -
                c.low

        if (
            bullishEngulfing
        ) {
            return 85.0
        }

        if (
            bearishEngulfing
        ) {
            return 15.0
        }

        if (
            body <= range * 0.10
        ) {

            return 50.0
        }

        if (
            lowerWick >
                body * 2.0
        ) {
            return 70.0
        }

        if (
            upperWick >
                body * 2.0
        ) {
            return 30.0
        }

        if (
            c.close >
                c.open
        ) {
            return 60.0
        }

        if (
            c.close <
                c.open
        ) {
            return 40.0
        }

        return 50.0
    }

    private fun breakout(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 25
        ) {
            return 50.0
        }

        val previous =
            candles
                .dropLast(1)
                .takeLast(20)

        if (
            previous.isEmpty()
        ) {
            return 50.0
        }

        val current =
            candles.last()

        val resistance =
            previous.maxOf {
                it.high
            }

        val support =
            previous.minOf {
                it.low
            }

        return when {

            current.close >
                resistance ->
                90.0

            current.close <
                support ->
                10.0

            else ->
                50.0
        }
    }

    private fun divergence(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 30
        ) {
            return 50.0
        }

        val closes =
            candles.map {
                it.close
            }

        val rsiNow =
            rsi(closes)

        val earlier =
            closes.dropLast(10)

        if (
            earlier.size <= 14
        ) {
            return 50.0
        }

        val rsiEarlier =
            rsi(earlier)

        val priceChange =
            closes.last() -
                earlier.last()

        val rsiChange =
            rsiNow -
                rsiEarlier

        if (
            priceChange > 0.0 &&
            rsiChange < 0.0
        ) {
            return 25.0
        }

        if (
            priceChange < 0.0 &&
            rsiChange > 0.0
        ) {
            return 75.0
        }

        return 50.0
    }

    /*
     * Fibonacci interno.
     *
     * Resultado:
     *
     * 0   = resistência de baixa
     * 100 = suporte de alta
     *
     * A posição do preço dentro da faixa de Fibonacci
     * é usada como uma confirmação adicional.
     */
    private fun fibonacciScore(
        candles: List<MarketCandle>
    ): Double {

        if (
            candles.size < 30
        ) {
            return 50.0
        }

        val window =
            candles.takeLast(50)

        val high =
            window.maxOf {
                it.high
            }

        val low =
            window.minOf {
                it.low
            }

        val range =
            high - low

        if (
            range <= 0.0
        ) {
            return 50.0
        }

        val price =
            candles.last().close

        val level =
            (
                price - low
            ) /
                range *
                100.0

        /*
         * Faixas aproximadas de Fibonacci.
         *
         * Abaixo de 38.2:
         * maior assimetria compradora.
         *
         * Acima de 61.8:
         * maior assimetria vendedora.
         */
        return when {

            level <= 23.6 ->
                85.0

            level <= 38.2 ->
                75.0

            level <= 50.0 ->
                60.0

            level <= 61.8 ->
                40.0

            level <= 76.4 ->
                25.0

            else ->
                15.0
        }
    }

    private fun metrics(
        candles: List<MarketCandle>
    ): QuantMetrics {

        val closes =
            candles.map {
                it.close
            }

        if (
            closes.isEmpty()
        ) {

            return QuantMetrics(
                ema9 = 0.0,
                ema21 = 0.0,
                ema50 = 0.0,
                rsi = 50.0,
                macd = 0.0,
                macdSignal = 0.0,
                bollingerUpper = 0.0,
                bollingerMiddle = 0.0,
                bollingerLower = 0.0,
                atr = 0.0,
                adx = 0.0,
                trend = 50.0,
                momentum = 50.0,
                structure = 50.0,
                volume = 50.0,
                volatility = 50.0,
                support = 0.0,
                resistance = 0.0,
                breakout = 50.0,
                candlePattern = 50.0,
                divergence = 50.0
            )
        }

        val ema9 =
            ema(
                closes,
                9
            )

        val ema21 =
            ema(
                closes,
                21
            )

        val ema50 =
            ema(
                closes,
                50
            )

        val rsiValue =
            rsi(
                closes
            )

        val macdResult =
            macd(
                closes
            )

        val bands =
            bollinger(
                closes
            )

        val atrValue =
            atr(
                candles
            )

        val adxValue =
            adx(
                candles
            )

        val trendValue =
            when {

                ema9 >
                    ema21 &&
                    ema21 >
                    ema50 ->
                    85.0

                ema9 <
                    ema21 &&
                    ema21 <
                    ema50 ->
                    15.0

                ema9 >
                    ema21 ->
                    65.0

                ema9 <
                    ema21 ->
                    35.0

                else ->
                    50.0
            }

        val momentumValue =
            clamp(
                50.0 +
                    (
                        rsiValue -
                            50.0
                    ) *
                        1.4
            )

        val support =
            candles
                .takeLast(
                    min(
                        30,
                        candles.size
                    )
                )
                .minOf {
                    it.low
                }

        val resistance =
            candles
                .takeLast(
                    min(
                        30,
                        candles.size
                    )
                )
                .maxOf {
                    it.high
                }

        return QuantMetrics(
            ema9 = ema9,
            ema21 = ema21,
            ema50 = ema50,
            rsi = rsiValue,
            macd = macdResult.first,
            macdSignal = macdResult.second,
            bollingerUpper = bands.first,
            bollingerMiddle = bands.second,
            bollingerLower = bands.third,
            atr = atrValue,
            adx = adxValue,
            trend = trendValue,
            momentum = momentumValue,
            structure =
                structure(
                    candles
                ),
            volume =
                volumeScore(
                    candles
                ),
            volatility =
                volatilityScore(
                    candles
                ),
            support = support,
            resistance = resistance,
            breakout =
                breakout(
                    candles
                ),
            candlePattern =
                candlePattern(
                    candles
                ),
            divergence =
                divergence(
                    candles
                )
        )
    }

    fun analyze(
        symbol: String,
        candlesByTimeframe:
            Map<String, List<MarketCandle>>,
        price: Double,
        bid: Double,
        ask: Double,
        timestamp: Long,
        now: Long
    ): RealtimeAnalysis {

        val normalizedTimestamp =
            normalizeTimestamp(
                timestamp
            )

        val metrics =
            candlesByTimeframe
                .filter {
                    it.value.isNotEmpty()
                }
                .mapValues {
                    metrics(
                        it.value
                    )
                }

        if (
            metrics.isEmpty()
        ) {

            throw IllegalStateException(
                "SEM_CANDLES_VALIDOS"
            )
        }

        /*
         * Peso estrutural dos timeframes.
         *
         * H1/H4/D1 têm maior peso para direção.
         * M1/M5 servem principalmente para timing.
         */
        val weights =
            mapOf(
                "M1" to 0.05,
                "M5" to 0.10,
                "M15" to 0.15,
                "M30" to 0.15,
                "H1" to 0.20,
                "H4" to 0.20,
                "D1" to 0.15
            )

        var bullish =
            0.0

        var bearish =
            0.0

        var totalWeight =
            0.0

        for (
            (timeframe, m) in metrics
        ) {

            val weight =
                weights[timeframe]
                    ?: 0.05

            totalWeight +=
                weight

            /*
             * Fibonacci entra como confirmação
             * estrutural adicional.
             */
            val fibonacci =
                fibonacciScore(
                    candlesByTimeframe[
                        timeframe
                    ].orEmpty()
                )

            val components =
                listOf(
                    m.trend,
                    m.momentum,
                    m.structure,
                    m.volume,
                    m.candlePattern,
                    m.breakout,
                    fibonacci
                )

            val local =
                mean(
                    components
                )

            if (
                local >= 55.0
            ) {

                bullish +=
                    weight *
                        (
                            local -
                                50.0
                        )
            }

            if (
                local <= 45.0
            ) {

                bearish +=
                    weight *
                        (
                            50.0 -
                                local
                        )
            }
        }

        if (
            totalWeight <= 0.0
        ) {
            totalWeight = 1.0
        }

        val directionalEdge =
            (
                bullish -
                    bearish
            ) /
                totalWeight

        /*
         * Timeframe principal.
         */
        val primary =
            metrics["M15"]
                ?: metrics.values.first()

        val h1 =
            metrics["H1"]
                ?: primary

        val h4 =
            metrics["H4"]
                ?: primary

        val d1 =
            metrics["D1"]
                ?: primary

        /*
         * Confluência estrutural.
         */
        val mtfDirections =
            listOf(
                primary.trend,
                h1.trend,
                h4.trend,
                d1.trend
            )

        val mtfBull =
            mtfDirections.count {
                it >= 60.0
            }

        val mtfBear =
            mtfDirections.count {
                it <= 40.0
            }

        val mtfNeutral =
            mtfDirections.count {
                it > 40.0 &&
                    it < 60.0
            }

        val mtfConfluence =
            max(
                mtfBull,
                mtfBear
            ).toDouble() /
                mtfDirections.size *
                100.0

        /*
         * Conflito entre timeframes.
         */
        val timeframeConflict =
            if (
                mtfBull > 0 &&
                mtfBear > 0
            ) {

                min(
                    100.0,
                    (
                        min(
                            mtfBull,
                            mtfBear
                        ).toDouble() /
                            mtfDirections.size
                    ) *
                        100.0
                )

            } else {
                0.0
            }

        /*
         * Confirmação dos indicadores.
         */
        val indicatorBull =
            listOf(
                primary.trend >= 60.0,
                primary.rsi in 50.0..70.0,
                primary.macd >
                    primary.macdSignal,
                primary.ema9 >
                    primary.ema21,
                primary.breakout >= 70.0,
                primary.candlePattern >= 60.0,
                primary.structure >= 55.0
            ).count {
                it
            }

        val indicatorBear =
            listOf(
                primary.trend <= 40.0,
                primary.rsi in 30.0..50.0,
                primary.macd <
                    primary.macdSignal,
                primary.ema9 <
                    primary.ema21,
                primary.breakout <= 30.0,
                primary.candlePattern <= 40.0,
                primary.structure <= 45.0
            ).count {
                it
            }

        /*
         * Divergência é tratada como risco,
         * e não simplesmente como direção.
         */
        val divergenceRisk =
            when {

                primary.divergence <= 30.0 &&
                    indicatorBull >= indicatorBear ->
                    65.0

                primary.divergence >= 70.0 &&
                    indicatorBear >= indicatorBull ->
                    65.0

                else ->
                    15.0
            }

        /*
         * FALSO SINAL
         *
         * Quanto maior:
         * maior o provisionamento necessário.
         */
        val confirmationBalance =
            abs(
                indicatorBull -
                    indicatorBear
            ).toDouble() /
                7.0 *
                100.0

        val falseSignal =
            clamp(
                100.0 -
                    (
                        mtfConfluence *
                            0.30 +

                        confirmationBalance *
                            0.20 +

                        primary.adx *
                            0.15 +

                        primary.volume *
                            0.10 +

                        primary.structure *
                            0.10 +

                        (
                            100.0 -
                                timeframeConflict
                        ) *
                            0.10 +

                        (
                            100.0 -
                                divergenceRisk
                        ) *
                            0.05
                    )
            )

        /*
         * FSI = índice de risco do sinal.
         *
         * FSI alto = sinal mais vulnerável.
         */
        val fsi =
            clamp(
                falseSignal *
                    0.35 +

                    abs(
                        primary.rsi -
                            50.0
                    ) *
                        0.10 +

                    primary.volatility *
                        0.15 +

                    (
                        100.0 -
                            mtfConfluence
                    ) *
                        0.20 +

                    timeframeConflict *
                        0.10 +

                    divergenceRisk *
                        0.10
            )

        /*
         * Score direcional.
         */
        val directionalScore =
            clamp(
                50.0 +

                    directionalEdge *
                        0.75 +

                    (
                        indicatorBull -
                            indicatorBear
                    ) *
                        4.5 +

                    (
                        mtfBull -
                            mtfBear
                    ) *
                        6.0 +

                    (
                        primary.trend -
                            50.0
                    ) *
                        0.15
            )

        /*
         * SCORE FINAL
         *
         * O falso sinal reduz o score.
         * A confluência e força de tendência
         * aumentam o score.
         */
        val score =
            clamp(
                directionalScore -

                    fsi *
                        0.30 +

                    mtfConfluence *
                        0.20 +

                    primary.adx *
                        0.10 +

                    primary.volume *
                        0.05
            )

        /*
         * Direção dominante.
         */
        val direction =
            when {

                bullish >
                    bearish * 1.18 &&
                    indicatorBull >
                    indicatorBear &&
                    mtfBull >= mtfBear ->
                    "COMPRA"

                bearish >
                    bullish * 1.18 &&
                    indicatorBear >
                    indicatorBull &&
                    mtfBear >= mtfBull ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        /*
         * Regime de mercado.
         */
        val regime =
            when {

                primary.adx >= 60.0 ->
                    "TENDÊNCIA FORTE"

                primary.adx >= 35.0 ->
                    "TENDÊNCIA"

                primary.volatility >= 70.0 ->
                    "VOLATILIDADE ALTA"

                mtfNeutral >= 2 ->
                    "LATERAL"

                else ->
                    "LATERAL / INDEFINIDO"
            }

        /*
         * PROVISIONAMENTO
         *
         * O motor somente libera uma direção
         * quando a probabilidade supera o risco.
         */
        val decision =
            when {

                direction == "COMPRA" &&
                    score >= 70.0 &&
                    fsi < 35.0 &&
                    falseSignal < 45.0 &&
                    mtfConfluence >= 60.0 ->
                    "COMPRA"

                direction == "VENDA" &&
                    score <= 30.0 &&
                    fsi < 35.0 &&
                    falseSignal < 45.0 &&
                    mtfConfluence >= 60.0 ->
                    "VENDA"

                else ->
                    "AGUARDAR"
            }

        /*
         * Confiança final.
         */
        val confidence =
            clamp(
                abs(
                    score -
                        50.0
                ) *
                    1.25 +

                    mtfConfluence *
                        0.30 +

                    primary.adx *
                        0.10 -

                    fsi *
                        0.45 -

                    timeframeConflict *
                        0.15
            )

        val marketDataQuality =
            if (
                price > 0.0 &&
                    normalizedTimestamp > 0L &&
                    now >= normalizedTimestamp &&
                    now -
                        normalizedTimestamp <=
                    120_000L
            ) {
                "GOOD"
            } else {
                "BAD"
            }

        val market =
            MarketData(
                asset =
                    symbol,

                timestamp =
                    normalizedTimestamp,

                price =
                    price,

                structure =
                    primary.structure,

                trend =
                    primary.trend,

                momentum =
                    primary.momentum,

                volume =
                    primary.volume,

                volatility =
                    primary.volatility,

                fsi =
                    fsi,

                multiTimeframe =
                    mtfConfluence,

                dataQuality =
                    marketDataQuality,

                bid =
                    bid,

                ask =
                    ask,

                spread =
                    max(
                        0.0,
                        ask - bid
                    ),

                source =
                    "TWELVE_DATA"
            )

        return RealtimeAnalysis(
            market =
                market,

            metrics =
                metrics,

            score =
                score,

            fsi =
                fsi,

            falseSignal =
                falseSignal,

            mtfConfluence =
                mtfConfluence,

            regime =
                regime,

            direction =
                direction,

            decision =
                decision,

            confidence =
                confidence
        )
    }
}

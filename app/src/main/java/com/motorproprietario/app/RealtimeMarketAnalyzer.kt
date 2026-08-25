package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class RealtimeAnalysis(
    val market: MarketData,
    val timeframes: Map<String, MarketCandle>,
    val score: ScoreResult,
    val fsi: FsiResult,
    val falseSignal: FalseSignalResult,
    val sequence: SequenceResult,
    val decision: DecisionResult
)

object RealtimeMarketAnalyzer {

    private fun clamp(
        value: Double
    ): Double {
        return value.coerceIn(0.0, 100.0)
    }

    private fun average(
        values: List<Double>
    ): Double {
        if (values.isEmpty()) return 0.0
        return values.average()
    }

    private fun trend(
        candles: List<MarketCandle>
    ): Double {

        if (candles.size < 10) {
            return 50.0
        }

        val start =
            candles[candles.size - 10].close

        val end =
            candles.last().close

        if (start <= 0.0) {
            return 50.0
        }

        val change =
            ((end - start) / start) * 100.0

        return clamp(
            50.0 + change * 10.0
        )
    }

    private fun momentum(
        candles: List<MarketCandle>
    ): Double {

        if (candles.size < 6) {
            return 50.0
        }

        val previous =
            candles[candles.size - 6].close

        val current =
            candles.last().close

        if (previous <= 0.0) {
            return 50.0
        }

        val change =
            ((current - previous) / previous) * 100.0

        return clamp(
            50.0 + change * 15.0
        )
    }

    private fun volatility(
        candles: List<MarketCandle>
    ): Double {

        if (candles.size < 10) {
            return 0.0
        }

        val returns =
            mutableListOf<Double>()

        for (i in 1 until candles.size) {

            val previous =
                candles[i - 1].close

            val current =
                candles[i].close

            if (previous <= 0.0) {
                continue
            }

            returns.add(
                abs(
                    (current - previous) /
                        previous
                ) * 100.0
            )
        }

        if (returns.isEmpty()) {
            return 0.0
        }

        return clamp(
            average(returns) * 20.0
        )
    }

    private fun volume(
        candles: List<MarketCandle>
    ): Double {

        if (candles.size < 20) {
            return 50.0
        }

        val recent =
            candles
                .takeLast(5)
                .map { it.volume }

        val previous =
            candles
                .drop(candles.size - 10)
                .take(5)
                .map { it.volume }

        val recentAverage =
            average(recent)

        val previousAverage =
            average(previous)

        if (previousAverage <= 0.0) {
            return 50.0
        }

        return clamp(
            50.0 +
                (
                    (recentAverage /
                        previousAverage) - 1.0
                ) * 50.0
        )
    }

    private fun structure(
        candles: List<MarketCandle>
    ): Double {

        if (candles.size < 10) {
            return 50.0
        }

        val recent =
            candles.takeLast(10)

        val highs =
            recent.map { it.high }

        val lows =
            recent.map { it.low }

        val highest =
            highs.maxOrNull() ?: return 50.0

        val lowest =
            lows.minOrNull() ?: return 50.0

        val current =
            recent.last().close

        val range =
            highest - lowest

        if (range <= 0.0) {
            return 50.0
        }

        return clamp(
            ((current - lowest) / range) * 100.0
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
    ): MarketData {

        val valid =
            candlesByTimeframe.filter {
                it.value.isNotEmpty()
            }

        if (valid.isEmpty()) {

            return MarketData(
                asset = symbol,
                timestamp = timestamp,
                price = price,
                structure = 0.0,
                trend = 0.0,
                momentum = 0.0,
                volume = 0.0,
                volatility = 0.0,
                fsi = 100.0,
                multiTimeframe = 0.0,
                dataQuality = "BAD",
                bid = bid,
                ask = ask,
                spread = max(
                    0.0,
                    ask - bid
                ),
                source = "TWELVE_DATA"
            )
        }

        val analyses =
            valid.map { entry ->

                val candles =
                    entry.value

                TimeframeMetrics(
                    timeframe = entry.key,
                    trend = trend(candles),
                    momentum = momentum(candles),
                    volume = volume(candles),
                    volatility =
                        volatility(candles),
                    structure =
                        structure(candles)
                )
            }

        val trendValue =
            average(
                analyses.map { it.trend }
            )

        val momentumValue =
            average(
                analyses.map { it.momentum }
            )

        val volumeValue =
            average(
                analyses.map { it.volume }
            )

        val volatilityValue =
            average(
                analyses.map {
                    it.volatility
                }
            )

        val structureValue =
            average(
                analyses.map {
                    it.structure
                }
            )

        val mtf =
            calculateMultiTimeframe(
                analyses
            )

        val quality =
            if (
                price > 0.0 &&
                bid > 0.0 &&
                ask >= bid &&
                timestamp > 0L &&
                now >= timestamp &&
                now - timestamp <= 120_000L
            ) {
                "GOOD"
            } else {
                "BAD"
            }

        return MarketData(
            asset = symbol,
            timestamp = timestamp,
            price = price,
            structure = structureValue,
            trend = trendValue,
            momentum = momentumValue,
            volume = volumeValue,
            volatility = volatilityValue,
            fsi = 0.0,
            multiTimeframe = mtf,
            dataQuality = quality,
            bid = bid,
            ask = ask,
            spread = max(
                0.0,
                ask - bid
            ),
            source = "TWELVE_DATA"
        )
    }

    private fun calculateMultiTimeframe(
        metrics:
            List<TimeframeMetrics>
    ): Double {

        if (metrics.isEmpty()) {
            return 0.0
        }

        val directions =
            metrics.map {

                when {
                    it.trend >= 60.0 &&
                        it.momentum >= 55.0 ->
                        1

                    it.trend <= 40.0 &&
                        it.momentum <= 45.0 ->
                        -1

                    else ->
                        0
                }
            }

        val positive =
            directions.count {
                it == 1
            }

        val negative =
            directions.count {
                it == -1
            }

        val total =
            directions.size

        if (total == 0) {
            return 0.0
        }

        val agreement =
            max(
                positive,
                negative
            ).toDouble() / total

        return clamp(
            agreement * 100.0
        )
    }

    private data class TimeframeMetrics(
        val timeframe: String,
        val trend: Double,
        val momentum: Double,
        val volume: Double,
        val volatility: Double,
        val structure: Double
    )
}

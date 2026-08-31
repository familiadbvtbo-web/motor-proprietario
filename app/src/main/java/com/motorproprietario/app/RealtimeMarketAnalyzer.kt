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
    val divergence: Double,
    val forceIndex: Double = 0.0,
    val forceIndexScore: Double = 50.0
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

    private fun clamp(value: Double, minValue: Double = 0.0, maxValue: Double = 100.0): Double {
        return if (value.isFinite()) value.coerceIn(minValue, maxValue) else minValue
    }

    private fun safe(value: Double, fallback: Double = 0.0): Double =
        if (value.isFinite()) value else fallback

    private fun normalizeTimestamp(timestamp: Long): Long {
        if (timestamp <= 0L) return 0L
        return if (timestamp < 10_000_000_000L) timestamp * 1000L else timestamp
    }

    private fun mean(values: List<Double>): Double {
        val valid = values.filter { it.isFinite() }
        return if (valid.isEmpty()) 0.0 else valid.average()
    }

    private fun std(values: List<Double>): Double {
        val valid = values.filter { it.isFinite() }
        if (valid.isEmpty()) return 0.0
        val avg = valid.average()
        return sqrt(valid.map { (it - avg).pow(2) }.average())
    }

    private fun ema(values: List<Double>, period: Int): Double {
        val valid = values.filter { it.isFinite() }
        if (valid.isEmpty()) return 0.0
        if (valid.size < period) return valid.last()

        val multiplier = 2.0 / (period + 1.0)
        var result = valid.take(period).average()

        for (i in period until valid.size) {
            result = (valid[i] - result) * multiplier + result
        }

        return safe(result)
    }

    private fun rsi(values: List<Double>, period: Int = 14): Double {
        if (values.size <= period) return 50.0

        var gains = 0.0
        var losses = 0.0

        for (i in 1..period) {
            val change = values[i] - values[i - 1]
            if (change >= 0.0) gains += change else losses += abs(change)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in period + 1 until values.size) {
            val change = values[i] - values[i - 1]
            val gain = max(0.0, change)
            val loss = max(0.0, -change)

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss <= 0.0) {
            return if (avgGain > 0.0) 100.0 else 50.0
        }

        val rs = avgGain / avgLoss
        return clamp(100.0 - 100.0 / (1.0 + rs))
    }

    private fun trueRanges(candles: List<MarketCandle>): List<Double> {
        if (candles.size < 2) return emptyList()

        val result = mutableListOf<Double>()

        for (i in 1 until candles.size) {
            val current = candles[i]
            val previous = candles[i - 1]

            val tr = max(
                current.high - current.low,
                max(
                    abs(current.high - previous.close),
                    abs(current.low - previous.close)
                )
            )

            if (tr.isFinite() && tr >= 0.0) result.add(tr)
        }

        return result
    }

    /*
     * ATR de Wilder.
     */
    private fun atr(candles: List<MarketCandle>, period: Int = 14): Double {
        val tr = trueRanges(candles)
        if (tr.isEmpty()) return 0.0
        if (tr.size < period) return mean(tr)

        var value = tr.take(period).average()

        for (i in period until tr.size) {
            value = ((value * (period - 1)) + tr[i]) / period
        }

        return safe(value)
    }

    /*
     * ADX de Wilder.
     *
     * Diferente do código anterior, aqui o resultado é realmente
     * o ADX (média suavizada do DX), e não somente o DX atual.
     */
    private fun adx(candles: List<MarketCandle>, period: Int = 14): Double {
        if (candles.size < period * 2 + 1) return 0.0

        val tr = mutableListOf<Double>()
        val plusDm = mutableListOf<Double>()
        val minusDm = mutableListOf<Double>()

        for (i in 1 until candles.size) {
            val current = candles[i]
            val previous = candles[i - 1]

            val upMove = current.high - previous.high
            val downMove = previous.low - current.low

            plusDm.add(
                if (upMove > downMove && upMove > 0.0) upMove else 0.0
            )

            minusDm.add(
                if (downMove > upMove && downMove > 0.0) downMove else 0.0
            )

            tr.add(
                max(
                    current.high - current.low,
                    max(
                        abs(current.high - previous.close),
                        abs(current.low - previous.close)
                    )
                )
            )
        }

        if (tr.size < period * 2) return 0.0

        var smoothedTr = tr.take(period).sum()
        var smoothedPlus = plusDm.take(period).sum()
        var smoothedMinus = minusDm.take(period).sum()

        val dxValues = mutableListOf<Double>()

        fun addDx() {
            if (smoothedTr <= 0.0) {
                dxValues.add(0.0)
                return
            }

            val plusDi = 100.0 * smoothedPlus / smoothedTr
            val minusDi = 100.0 * smoothedMinus / smoothedTr
            val denominator = plusDi + minusDi

            val dx = if (denominator <= 0.0) {
                0.0
            } else {
                abs(plusDi - minusDi) / denominator * 100.0
            }

            dxValues.add(dx)
        }

        addDx()

        for (i in period until tr.size) {
            smoothedTr = smoothedTr - smoothedTr / period + tr[i]
            smoothedPlus = smoothedPlus - smoothedPlus / period + plusDm[i]
            smoothedMinus = smoothedMinus - smoothedMinus / period + minusDm[i]
            addDx()
        }

        if (dxValues.size < period) return 0.0

        var adxValue = dxValues.take(period).average()

        for (i in period until dxValues.size) {
            adxValue = ((adxValue * (period - 1)) + dxValues[i]) / period
        }

        return clamp(adxValue)
    }

    private fun macd(closes: List<Double>): Pair<Double, Double> {
        if (closes.size < 35) return 0.0 to 0.0

        val macdValues = mutableListOf<Double>()

        for (i in 25 until closes.size) {
            val subset = closes.subList(0, i + 1)
            macdValues.add(
                ema(subset, 12) - ema(subset, 26)
            )
        }

        if (macdValues.isEmpty()) return 0.0 to 0.0

        val current = macdValues.last()
        val signal = if (macdValues.size >= 9) {
            ema(macdValues, 9)
        } else {
            macdValues.average()
        }

        return safe(current) to safe(signal)
    }

    private fun bollinger(closes: List<Double>, period: Int = 20): Triple<Double, Double, Double> {
        if (closes.size < period) {
            val last = closes.lastOrNull() ?: 0.0
            return Triple(last, last, last)
        }

        val window = closes.takeLast(period)
        val middle = mean(window)
        val deviation = std(window)

        return Triple(
            middle + deviation * 2.0,
            middle,
            middle - deviation * 2.0
        )
    }

    private fun structure(candles: List<MarketCandle>): Double {
        if (candles.size < 20) return 50.0

        val recent = candles.takeLast(20)
        val high = recent.maxOf { it.high }
        val low = recent.minOf { it.low }
        val price = recent.last().close
        val range = high - low

        if (range <= 0.0) return 50.0

        return clamp((price - low) / range * 100.0)
    }

    private fun volumeScore(candles: List<MarketCandle>): Double {
        if (candles.size < 20) return 50.0

        val recent = mean(candles.takeLast(5).map { it.volume })
        val baseline = mean(
            candles
                .drop(candles.size - 20)
                .take(10)
                .map { it.volume }
        )

        if (baseline <= 0.0) return 50.0

        return clamp(50.0 + (recent / baseline - 1.0) * 50.0)
    }

    private fun forceIndex(candles: List<MarketCandle>): Double {
        if (candles.size < 2) return 0.0

        val current = candles.last()
        val previous = candles[candles.size - 2]

        val volume = if (current.volume.isFinite() && current.volume > 0.0) {
            current.volume
        } else {
            0.0
        }

        return safe((current.close - previous.close) * volume)
    }

    private fun forceIndexScore(candles: List<MarketCandle>): Double {
        if (candles.size < 21) return 50.0

        val values = mutableListOf<Double>()

        for (i in 1 until candles.size) {
            val current = candles[i]
            val previous = candles[i - 1]

            val volume = if (current.volume.isFinite() && current.volume > 0.0) {
                current.volume
            } else {
                0.0
            }

            val fi = (current.close - previous.close) * volume
            if (fi.isFinite()) values.add(fi)
        }

        if (values.isEmpty()) return 50.0

        val recent = mean(values.takeLast(5))
        val scale = values.takeLast(20).map { abs(it) }.average()

        if (!scale.isFinite() || scale <= 0.0) return 50.0

        return clamp(50.0 + recent / scale * 25.0)
    }

    private fun volatilityScore(candles: List<MarketCandle>): Double {
        val closes = candles.map { it.close }
        if (closes.size < 20) return 50.0

        val returns = mutableListOf<Double>()

        for (i in 1 until closes.size) {
            val previous = closes[i - 1]
            if (previous == 0.0) continue

            val change = abs((closes[i] - previous) / previous) * 100.0
            if (change.isFinite()) returns.add(change)
        }

        if (returns.isEmpty()) return 50.0

        val current = mean(returns.takeLast(5))
        val baseline = mean(returns.takeLast(20))

        if (baseline <= 0.0) return 50.0

        return clamp(50.0 + (current / baseline - 1.0) * 50.0)
    }

    private fun candlePattern(candles: List<MarketCandle>): Double {
        if (candles.size < 3) return 50.0

        val previous = candles[candles.size - 2]
        val current = candles.last()

        val bullishEngulfing =
            previous.close < previous.open &&
                current.close > current.open &&
                current.close >= previous.open &&
                current.open <= previous.close

        val bearishEngulfing =
            previous.close > previous.open &&
                current.close < current.open &&
                current.open >= previous.close &&
                current.close <= previous.open

        val body = abs(current.close - current.open)
        val range = current.high - current.low

        if (range <= 0.0) return 50.0

        val upperWick = current.high - max(current.open, current.close)
        val lowerWick = min(current.open, current.close) - current.low

        if (bullishEngulfing) return 85.0
        if (bearishEngulfing) return 15.0
        if (body <= range * 0.10) return 50.0
        if (lowerWick > body * 2.0) return 70.0
        if (upperWick > body * 2.0) return 30.0

        return when {
            current.close > current.open -> 60.0
            current.close < current.open -> 40.0
            else -> 50.0
        }
    }

    private fun breakout(candles: List<MarketCandle>): Double {
        if (candles.size < 25) return 50.0

        val previous = candles.dropLast(1).takeLast(20)
        val current = candles.last()

        val resistance = previous.maxOf { it.high }
        val support = previous.minOf { it.low }

        return when {
            current.close > resistance -> 90.0
            current.close < support -> 10.0
            else -> 50.0
        }
    }

    private fun divergence(candles: List<MarketCandle>): Double {
        if (candles.size < 30) return 50.0

        val closes = candles.map { it.close }
        val rsiNow = rsi(closes)
        val earlier = closes.dropLast(10)

        if (earlier.size <= 14) return 50.0

        val rsiEarlier = rsi(earlier)
        val priceChange = closes.last() - earlier.last()
        val rsiChange = rsiNow - rsiEarlier

        return when {
            priceChange > 0.0 && rsiChange < 0.0 -> 25.0
            priceChange < 0.0 && rsiChange > 0.0 -> 75.0
            else -> 50.0
        }
    }

    private fun fibonacciScore(candles: List<MarketCandle>): Double {
        if (candles.size < 30) return 50.0

        val window = candles.takeLast(50)
        val high = window.maxOf { it.high }
        val low = window.minOf { it.low }
        val range = high - low

        if (range <= 0.0) return 50.0

        val price = candles.last().close
        val level = (price - low) / range * 100.0

        return when {
            level <= 23.6 -> 85.0
            level <= 38.2 -> 75.0
            level <= 50.0 -> 60.0
            level <= 61.8 -> 40.0
            level <= 76.4 -> 25.0
            else -> 15.0
        }
    }

    private fun metrics(candles: List<MarketCandle>): QuantMetrics {
        val closes = candles.map { it.close }

        if (closes.isEmpty()) {
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
                divergence = 50.0,
                forceIndex = 0.0
            )
        }

        val ema9 = ema(closes, 9)
        val ema21 = ema(closes, 21)
        val ema50 = ema(closes, 50)
        val rsiValue = rsi(closes)
        val macdResult = macd(closes)
        val bands = bollinger(closes)
        val atrValue = atr(candles)
        val adxValue = adx(candles)
        val forceIndexValue = forceIndex(candles)

        val trendValue = when {
            ema9 > ema21 && ema21 > ema50 -> 85.0
            ema9 < ema21 && ema21 < ema50 -> 15.0
            ema9 > ema21 -> 65.0
            ema9 < ema21 -> 35.0
            else -> 50.0
        }

        val momentumValue = clamp(50.0 + (rsiValue - 50.0) * 1.4)

        val recent = candles.takeLast(min(30, candles.size))
        val support = recent.minOf { it.low }
        val resistance = recent.maxOf { it.high }

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
            structure = structure(candles),
            volume = volumeScore(candles),
            volatility = volatilityScore(candles),
            support = support,
            resistance = resistance,
            breakout = breakout(candles),
            candlePattern = candlePattern(candles),
            divergence = divergence(candles),
            forceIndex = forceIndexValue,
            forceIndexScore = forceIndexScore(candles)
        )
    }

    private fun timeframePriority(timeframe: String): Int =
        when (timeframe.uppercase()) {
            "M1" -> 1
            "M5" -> 2
            "M15" -> 3
            "M30" -> 4
            "H1" -> 5
            "H4" -> 6
            "D1" -> 7
            "W1" -> 8
            "MN1" -> 9
            "Y1" -> 10
            else -> 0
        }

    fun analyze(
        symbol: String,
        candlesByTimeframe: Map<String, List<MarketCandle>>,
        price: Double,
        bid: Double,
        ask: Double,
        timestamp: Long,
        now: Long
    ): RealtimeAnalysis {

        val normalizedTimestamp = normalizeTimestamp(timestamp)

        val metrics = candlesByTimeframe
            .filter { it.value.isNotEmpty() }
            .mapValues { metrics(it.value) }

        if (metrics.isEmpty()) {
            throw IllegalStateException("SEM_CANDLES_VALIDOS")
        }

        val weights = mapOf(
            "M1" to 0.05,
            "M5" to 0.10,
            "M15" to 0.15,
            "M30" to 0.15,
            "H1" to 0.20,
            "H4" to 0.20,
            "D1" to 0.15
        )

        var bullish = 0.0
        var bearish = 0.0
        var totalWeight = 0.0

        for ((timeframe, m) in metrics) {
            val weight = weights[timeframe] ?: 0.05
            totalWeight += weight

            val candles = candlesByTimeframe[timeframe].orEmpty()
            val fibonacci = fibonacciScore(candles)
            val fiScore = forceIndexScore(candles)

            val local = mean(
                listOf(
                    m.trend,
                    m.momentum,
                    m.structure,
                    m.candlePattern,
                    m.breakout,
                    fibonacci,
                    fiScore,
                    50.0
                )
            )

            if (local > 50.0) {
                bullish += weight * (local - 50.0)
            } else if (local < 50.0) {
                bearish += weight * (50.0 - local)
            }
        }

        if (totalWeight <= 0.0) totalWeight = 1.0

        val directionalEdge = (bullish - bearish) / totalWeight

        val primary = metrics["M15"]
            ?: metrics.entries
                .sortedByDescending { entry ->
                    timeframePriority(entry.key)
                }
                .first()
                .value

        val mtfResult = MultiTimeframeEngine.calculate(metrics)
        val mtfConfluence = mtfResult.confluence
        val mtfBull = mtfResult.bullishTimeframes
        val mtfBear = mtfResult.bearishTimeframes
        val mtfNeutral = mtfResult.neutralTimeframes
        val mtfAnalyzed = mtfResult.analyzedTimeframes

        val timeframeConflict =
            if (mtfBull > 0 && mtfBear > 0 && mtfAnalyzed > 0) {
                min(
                    100.0,
                    min(mtfBull, mtfBear).toDouble() / mtfAnalyzed.toDouble() * 100.0
                )
            } else {
                0.0
            }

        val primaryCandles = candlesByTimeframe["M15"].orEmpty()
        val primaryFiScore = forceIndexScore(primaryCandles)

        val indicatorBull = listOf(
            primary.trend >= 60.0,
            primary.rsi in 50.0..70.0,
            primary.macd > primary.macdSignal,
            primary.ema9 > primary.ema21,
            primary.breakout >= 70.0,
            primary.candlePattern >= 60.0,
            primary.structure >= 55.0,
            primaryFiScore >= 55.0
        ).count { it }

        val indicatorBear = listOf(
            primary.trend <= 40.0,
            primary.rsi in 30.0..50.0,
            primary.macd < primary.macdSignal,
            primary.ema9 < primary.ema21,
            primary.breakout <= 30.0,
            primary.candlePattern <= 40.0,
            primary.structure <= 45.0,
            primaryFiScore <= 45.0
        ).count { it }

        val divergenceRisk = when {
            primary.divergence <= 30.0 && indicatorBull >= indicatorBear -> 65.0
            primary.divergence >= 70.0 && indicatorBear >= indicatorBull -> 65.0
            else -> 15.0
        }

        val forceIndexConflict = when {
            indicatorBull > indicatorBear && primaryFiScore < 45.0 -> 75.0
            indicatorBear > indicatorBull && primaryFiScore > 55.0 -> 75.0
            else -> 0.0
        }

        val confirmationBalance =
            abs(indicatorBull - indicatorBear).toDouble() / 8.0 * 100.0

        /*
         * FSI:
         * risco maior quando há conflito, divergência, volatilidade
         * excessiva ou pouca confluência.
         */
        val falseSignalBase = clamp(
            100.0 - (
                mtfConfluence * 0.28 +
                    confirmationBalance * 0.18 +
                    primary.adx * 0.14 +
                    primary.volume * 0.09 +
                    primary.structure * 0.09 +
                    primaryFiScore * 0.10 +
                    (100.0 - timeframeConflict) * 0.07 +
                    (100.0 - divergenceRisk) * 0.03
            )
        )

        val falseSignal = clamp(
            falseSignalBase +
                forceIndexConflict * 0.15
        )

        val fsi = clamp(
            falseSignal * 0.35 +
                abs(primary.rsi - 50.0) * 0.10 +
                primary.volatility * 0.15 +
                (100.0 - mtfConfluence) * 0.20 +
                timeframeConflict * 0.10 +
                divergenceRisk * 0.05 +
                (100.0 - primaryFiScore) * 0.05
        )

        val directionalScore = clamp(
            50.0 +
                directionalEdge * 0.75 +
                (indicatorBull - indicatorBear) * 4.0 +
                (mtfBull - mtfBear) * 6.0 +
                (primary.trend - 50.0) * 0.15 +
                (primaryFiScore - 50.0) * 0.20
        )

        val score = clamp(
            directionalScore -
                fsi * 0.30 +
                mtfConfluence * 0.20 +
                primary.adx * 0.10 +
                primary.volume * 0.05
        )

        /*
         * Não transforma uma diferença pequena em COMPRA/VENDA.
         * Isso reduz o NEUTRO artificial e evita sinais sem vantagem real.
         */
        val directionalAdvantage = abs(bullish - bearish)
        val minimumDirectionalEdge = 0.025

        val direction = when {
            totalWeight > 0.0 &&
                bullish > bearish &&
                bullish > 0.0 &&
                directionalAdvantage / totalWeight >= minimumDirectionalEdge &&
                indicatorBull >= indicatorBear + 1 &&
                mtfBull >= mtfBear -> "COMPRA"

            totalWeight > 0.0 &&
                bearish > bullish &&
                bearish > 0.0 &&
                directionalAdvantage / totalWeight >= minimumDirectionalEdge &&
                indicatorBear >= indicatorBull + 1 &&
                mtfBear >= mtfBull -> "VENDA"

            else -> "NEUTRO"
        }

        val regime = when {
            primary.adx >= 60.0 -> "TENDÊNCIA FORTE"
            primary.adx >= 35.0 -> "TENDÊNCIA"
            primary.volatility >= 70.0 -> "VOLATILIDADE ALTA"
            mtfNeutral >= 2 -> "LATERAL"
            else -> "LATERAL / INDEFINIDO"
        }

        val decision = when {
            direction == "COMPRA" &&
                score >= 70.0 &&
                fsi < 35.0 &&
                falseSignal < 45.0 &&
                mtfConfluence >= 60.0 -> "COMPRA"

            direction == "VENDA" &&
                score <= 30.0 &&
                fsi < 35.0 &&
                falseSignal < 45.0 &&
                mtfConfluence >= 60.0 -> "VENDA"

            else -> "AGUARDAR"
        }

        val confidence = clamp(
            abs(score - 50.0) * 1.25 +
                mtfConfluence * 0.30 +
                primary.adx * 0.10 -
                fsi * 0.45 -
                timeframeConflict * 0.15
        )

        val marketDataQuality =
            if (
                price > 0.0 &&
                normalizedTimestamp > 0L &&
                now >= normalizedTimestamp &&
                now - normalizedTimestamp <= 120_000L
            ) {
                "GOOD"
            } else {
                "BAD"
            }

        val market = MarketData(
            asset = symbol,
            timestamp = normalizedTimestamp,
            price = price,
            structure = primary.structure,
            trend = primary.trend,
            momentum = primary.momentum,
            volume = primary.volume,
            volatility = primary.volatility,
            fsi = fsi,
            multiTimeframe = mtfConfluence,
            dataQuality = marketDataQuality,
            bid = bid,
            ask = ask,
            spread = max(0.0, ask - bid),
            source = "TWELVE_DATA"
        )

        return RealtimeAnalysis(
            market = market,
            metrics = metrics,
            score = score,
            fsi = fsi,
            falseSignal = falseSignal,
            mtfConfluence = mtfConfluence,
            regime = regime,
            direction = direction,
            decision = decision,
            confidence = confidence
        )
    }
}

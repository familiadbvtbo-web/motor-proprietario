package com.motorproprietario.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * MainActivity - integração do Motor Proprietário.
 *
 * VERSÃO MTF + 5.000 CANDLES:
 * - alimenta o MTF com os 10 timeframes;
 * - solicita até 5.000 candles por timeframe;
 * - mantém cache persistente separado por ativo/timeframe;
 * - respeita o intervalo de atualização de cada timeframe;
 * - limita consultas por ciclo para reduzir risco de 429;
 * - mantém o motor funcionando com o cache quando a API limita requisições.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var dopmDashboardController: DopmDashboardController

    private val handler = Handler(Looper.getMainLooper())
    private val quoteClient = TwelveDataClient()
    private val candleClient = TwelveDataCandleClient()

    private val marketCandleCache by lazy { MarketCandleCache(applicationContext) }
    private val calibrationRuntime by lazy { CalibrationRuntime(applicationContext) }
    private val deterministicHistoryEngine by lazy { DeterministicHistoryEngine(applicationContext) }

    private var selectedAsset = "EUR/USD"
    private var selectedTimeframe = "M15"
    private var selectedHorizon = "GERAL"
    private var lastQuote: RealTimeQuote? = null
    private var analyzing = false
    private var sequenceStage = SequenceStage.S0
    private var latestDetailedAnalysis = "Aguardando dados reais..."

    private val candleCache = LinkedHashMap<String, List<MarketCandle>>()
    private val lastCandleUpdate = HashMap<String, Long>()

    private val candleIntervals = mapOf(
        "M1" to 60_000L,
        "M5" to 300_000L,
        "M15" to 900_000L,
        "M30" to 1_800_000L,
        "H1" to 3_600_000L,
        "H4" to 14_400_000L,
        "D1" to 86_400_000L,
        "W1" to 604_800_000L,
        "MN1" to 2_592_000_000L,
        "Y1" to 31_536_000_000L
    )

    private val mtfTimeframes = listOf(
        "M1", "M5", "M15", "M30", "H1",
        "H4", "D1", "W1", "MN1", "Y1"
    )

    private val candleRequestsPerCycle = 3
    private var candleRoundRobin = 0
    private var candleApiBackoffUntil = 0L
    private val candleApiBackoffMs = 65_000L

    private val refreshTask = object : Runnable {
        override fun run() {
            analyzeMarket()
            handler.postDelayed(this, 5_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dopmDashboardController = DopmDashboardController(this)
        dopmDashboardController.install()

        dopmDashboardController.setSelectionListeners(
            marketChanged = { market ->
                when (market) {
                    "FOREX" -> selectAsset("EUR/USD")
                    "CRIPTO" -> selectAsset("BTC/USD")
                    "B3" -> selectAsset("IBOV")
                }
            },
            assetChanged = { asset ->
                if (asset.isNotBlank() && asset != selectedAsset) {
                    selectAsset(asset)
                }
            },
            timeframeChanged = { timeframe ->
                if (timeframe.isNotBlank() && timeframe != selectedTimeframe) {
                    selectedTimeframe = timeframe.uppercase()
                    analyzeMarket()
                }
            }
        )

        connectRealtime()
        handler.post(refreshTask)
    }

    private fun selectAsset(asset: String) {
        selectedAsset = asset
        selectedTimeframe = "M15"
        resetForNewAsset()
        connectRealtime()
    }

    private fun resetForNewAsset() {
        quoteClient.disconnect()
        lastQuote = null

        synchronized(candleCache) { candleCache.clear() }
        lastCandleUpdate.clear()
        candleRoundRobin = 0
        candleApiBackoffUntil = 0L
        sequenceStage = SequenceStage.S0
        latestDetailedAnalysis = "Aguardando dados reais..."

        dopmDashboardController.view()?.apply {
            setOnline(false)
            setApi("TWELVE DATA")
            setPrice("--")
            setDecision("AGUARDAR", 0.0)
            setProbabilities(0.0, 0.0, 100.0)
            setProbability(0.0)
            setDeterminism(0.0)
            setMtf(0.0)
            setBestTimeframe("--")
            setTradePlan("--", "--", "--", "--", "--")
            setTiming("AGUARDAR", "--")
        }
    }

    private fun connectRealtime() {
        quoteClient.connect(
            symbols = listOf(selectedAsset),
            onQuote = { quote ->
                if (quote.symbol.isNotBlank() && quote.price.isFinite() && quote.price > 0.0) {
                    lastQuote = quote
                    runOnUiThread {
                        dopmDashboardController.view()?.apply {
                            setOnline(true)
                            setApi("TWELVE DATA")
                            setPrice(String.format("%.5f", quote.price))
                        }
                    }
                }
            },
            onError = {
                runOnUiThread {
                    dopmDashboardController.view()?.setOnline(false)
                }
            }
        )
    }

    private fun analyzeMarket() {
        if (analyzing) return

        val quote = lastQuote ?: return
        if (!quote.price.isFinite() || quote.price <= 0.0) return

        analyzing = true

        thread {
            try {
                val now = System.currentTimeMillis()
                updateCandles(now)

                val candles = synchronized(candleCache) {
                    LinkedHashMap<String, List<MarketCandle>>(candleCache)
                }

                if (candles.isEmpty()) {
                    showWaiting("AGUARDANDO CANDLES REAIS")
                    return@thread
                }

                val realtime = RealtimeMarketAnalyzer.analyze(
                    symbol = selectedAsset,
                    candlesByTimeframe = candles,
                    price = quote.price,
                    bid = quote.price,
                    ask = quote.price,
                    timestamp = quote.timestamp,
                    now = now
                )

                val mtfResult = MultiTimeframeEngine.calculate(realtime.metrics)
                val effectiveMtf = mtfResult.confluence

                val selectedMetrics = realtime.metrics[selectedTimeframe]
                    ?: realtime.metrics["M15"]
                    ?: realtime.metrics.values.maxByOrNull { it.adx }!!

                val selectedHigher = higherMetrics(selectedTimeframe, realtime.metrics)

                val deterministic = DeterministicEngine.calculate(
                    DeterministicInput(
                        metrics = selectedMetrics,
                        mtfConfluence = effectiveMtf,
                        falseSignalRisk = realtime.fsi,
                        currentPrice = quote.price,
                        higherTimeframes = selectedHigher
                    )
                )

                val probability = ProbabilityEngine.calculate(
                    ProbabilityInput(
                        metrics = selectedMetrics,
                        mtfConfluence = effectiveMtf,
                        falseSignalRisk = realtime.fsi,
                        fibonacciBullish = fibonacciEvidence(candles[selectedTimeframe], true),
                        fibonacciBearish = fibonacciEvidence(candles[selectedTimeframe], false),
                        institutionalBullish = deterministic.buyScore,
                        institutionalBearish = deterministic.sellScore
                    )
                )

                val selectedFinal = combineFinalProbabilities(
                    probability, deterministic, realtime.fsi, effectiveMtf
                )

                val bestTimeframe = findBestTimeframe(realtime, candles, effectiveMtf)
                val bestMetrics = realtime.metrics[bestTimeframe] ?: selectedMetrics

                var bestDeterministic = DeterministicEngine.calculate(
                    DeterministicInput(
                        metrics = bestMetrics,
                        mtfConfluence = effectiveMtf,
                        falseSignalRisk = realtime.fsi,
                        currentPrice = quote.price,
                        higherTimeframes = higherMetrics(bestTimeframe, realtime.metrics)
                    )
                )

                bestDeterministic = deterministicHistoryEngine.apply(
                    symbol = selectedAsset,
                    timeframe = bestTimeframe,
                    stage = sequenceStage.name,
                    metrics = bestMetrics,
                    deterministic = bestDeterministic,
                    currentPrice = quote.price,
                    now = now
                )

                val historicalStats = deterministicHistoryEngine.statistics(
                    symbol = selectedAsset,
                    timeframe = bestTimeframe,
                    stage = sequenceStage.name,
                    metrics = bestMetrics,
                    direction = bestDeterministic.directionalBias
                )

                val effectiveFsi = effectiveFsi(
                    realtime.fsi,
                    historicalStats.samples,
                    historicalStats.capturePressure,
                    historicalStats.realizationPressure
                )

                val bestProbability = ProbabilityEngine.calculate(
                    ProbabilityInput(
                        metrics = bestMetrics,
                        mtfConfluence = effectiveMtf,
                        falseSignalRisk = effectiveFsi,
                        fibonacciBullish = fibonacciEvidence(candles[bestTimeframe], true),
                        fibonacciBearish = fibonacciEvidence(candles[bestTimeframe], false),
                        institutionalBullish = bestDeterministic.buyScore,
                        institutionalBearish = bestDeterministic.sellScore
                    )
                )

                val bestFinal = combineFinalProbabilities(
                    bestProbability, bestDeterministic, effectiveFsi, effectiveMtf
                )

                val bestDirection = finalDirection(
                    probabilities = bestFinal,
                    mtfCoverage = mtfResult.coverage,
                    dataQuality = realtime.market.dataQuality,
                    falseSignalRisk = effectiveFsi,
                    deterministic = bestDeterministic
                )

                val entryPlan = EntryPlanEngine.calculate(
                    EntryPlanInput(
                        direction = bestDirection,
                        currentPrice = quote.price,
                        metrics = bestMetrics,
                        timeframe = bestTimeframe,
                        probability = max(bestFinal.first, bestFinal.second),
                        deterministicConfidence = bestDeterministic.confidence,
                        falseSignalRisk = effectiveFsi,
                        now = now
                    )
                )

                val sequence = SequenceEngine.advance(
                    sequenceStage,
                    SequenceInput(
                        signalDetected = bestDirection != "NEUTRO",
                        confirmation = effectiveMtf >= 60.0 && mtfResult.coverage >= 60.0,
                        continuation = when (bestDirection) {
                            "COMPRA" -> bestMetrics.ema9 > bestMetrics.ema21 &&
                                bestMetrics.macd > bestMetrics.macdSignal
                            "VENDA" -> bestMetrics.ema9 < bestMetrics.ema21 &&
                                bestMetrics.macd < bestMetrics.macdSignal
                            else -> false
                        },
                        invalidated = effectiveFsi >= 80.0 ||
                            realtime.market.dataQuality != "GOOD"
                    )
                )

                sequenceStage = sequence.stage

                val detailed = buildDetailedAnalysis(
                    realtime = realtime,
                    effectiveFsi = effectiveFsi,
                    effectiveMtfConfluence = effectiveMtf,
                    mtfResult = mtfResult,
                    deterministic = bestDeterministic,
                    probability = bestProbability,
                    finalProbabilities = bestFinal,
                    direction = bestDirection,
                    bestTimeframe = bestTimeframe,
                    metrics = bestMetrics,
                    entryPlan = entryPlan,
                    sequence = sequence,
                    candles = candles,
                    historicalSamples = historicalStats.samples
                )

                runOnUiThread {
                    latestDetailedAnalysis = detailed
                    updateMainScreen(
                        realtime = realtime,
                        effectiveMtfConfluence = effectiveMtf,
                        finalProbabilities = bestFinal,
                        direction = bestDirection,
                        bestTimeframe = bestTimeframe,
                        entryPlan = entryPlan,
                        deterministic = bestDeterministic
                    )
                }
            } catch (error: Exception) {
                showWaiting("ERRO NA ANÁLISE: ${error.message?.take(160) ?: "ERRO_DESCONHECIDO"}")
            } finally {
                analyzing = false
            }
        }
    }

    private fun effectiveFsi(
        realtimeFsi: Double,
        samples: Int,
        capturePressure: Double,
        realizationPressure: Double
    ): Double {
        if (samples < 10) return realtimeFsi.coerceIn(0.0, 100.0)

        return (
            realtimeFsi * 0.75 +
                capturePressure.coerceIn(0.0, 100.0) * 0.15 +
                realizationPressure.coerceIn(0.0, 100.0) * 0.10
            ).coerceIn(0.0, 100.0)
    }

    private fun higherMetrics(
        reference: String,
        metrics: Map<String, QuantMetrics>
    ): List<QuantMetrics> {
        val rank = timeframeRank(reference)
        return metrics.filterKeys { timeframeRank(it) > rank }.values.toList()
    }

    private fun showWaiting(message: String) {
        runOnUiThread {
            dopmDashboardController.view()?.apply {
                setOnline(false)
                setApi("TWELVE DATA")
                setDecision("AGUARDAR", 0.0)
                setProbabilities(0.0, 0.0, 100.0)
                setProbability(0.0)
                setDeterminism(0.0)
                setMtf(0.0)
                setBestTimeframe("--")
                setTradePlan("--", "--", "--", "--", "--")
                setTiming("AGUARDAR", message)
            }
        }
    }

    /**
     * Carrega os 10 timeframes.
     *
     * Cada timeframe é independente no cache persistente do ativo.
     * Até 5.000 candles são solicitados quando uma atualização é necessária.
     *
     * Apenas 3 requisições são feitas por ciclo de análise para reduzir
     * risco de 429. Como a Activity roda a cada 5 segundos, os 10
     * timeframes são preenchidos progressivamente e depois atualizados
     * conforme seus próprios intervalos.
     */
    private fun updateCandles(now: Long) {
        if (now < candleApiBackoffUntil) return

        val candidates = ArrayList<String>()
        val ordered = ArrayList<String>()

        ordered.add(selectedTimeframe.uppercase())
        ordered.addAll(mtfTimeframes)

        for (tf in ordered.distinct()) {
            if (tf in mtfTimeframes && tf !in candidates) {
                candidates.add(tf)
            }
        }

        val due = candidates.filter { timeframe ->
            val interval = candleIntervals[timeframe] ?: return@filter false
            val last = lastCandleUpdate[timeframe] ?: 0L
            val cached = synchronized(candleCache) { candleCache[timeframe] }

            cached.isNullOrEmpty() || now - last >= interval
        }

        if (due.isEmpty()) return

        val rotation = candleRoundRobin % due.size
        val rotated = due.drop(rotation) + due.take(rotation)

        var requested = 0

        for (timeframe in rotated) {
            if (requested >= candleRequestsPerCycle) break

            requested++
            candleRoundRobin++

            try {
                /*
                 * Primeiro tenta recuperar o cache persistente deste
                 * ativo + timeframe. O cache não é compartilhado entre
                 * combinações diferentes.
                 */
                val persistent = marketCandleCache.get(
                    symbol = selectedAsset,
                    timeframe = timeframe
                )

                if (persistent.isNotEmpty()) {
                    synchronized(candleCache) {
                        candleCache[timeframe] = persistent
                    }
                }

                val cached = synchronized(candleCache) {
                    candleCache[timeframe]
                }

                val interval = candleIntervals[timeframe] ?: 60_000L
                val last = lastCandleUpdate[timeframe] ?: 0L

                if (!cached.isNullOrEmpty() && now - last < interval) {
                    continue
                }

                val fresh = candleClient.getCandles(
                    symbol = selectedAsset,
                    timeframe = timeframe,
                    outputSize = 5_000
                )

                if (fresh.isNotEmpty()) {
                    /*
                     * merge() preserva o histórico já armazenado e
                     * incorpora os candles novos sem duplicação,
                     * conforme a implementação do cache.
                     */
                    val merged = marketCandleCache.merge(
                        symbol = selectedAsset,
                        timeframe = timeframe,
                        freshCandles = fresh
                    )

                    synchronized(candleCache) {
                        candleCache[timeframe] = merged
                    }

                    lastCandleUpdate[timeframe] = now
                }
            } catch (error: Exception) {
                val message =
                    error.message?.trim()?.take(180)
                        ?: "ERRO_DESCONHECIDO"

                if (message.contains("429", ignoreCase = true)) {
                    candleApiBackoffUntil =
                        System.currentTimeMillis() + candleApiBackoffMs
                    break
                }

                /* Um timeframe com erro não derruba os demais. */
            }
        }
    }

    private fun combineFinalProbabilities(
        probability: ProbabilityResult,
        deterministic: DeterministicResult,
        fsi: Double,
        mtf: Double
    ): Triple<Double, Double, Double> {
        val weights = calibrationRuntime.weights()
        val pw = weights.first.coerceIn(0.0, 1.0)
        val dw = weights.second.coerceIn(0.0, 1.0)
        val weightTotal = (pw + dw).takeIf { it > 0.0 } ?: 1.0

        var buy = (probability.buyProbability * pw + deterministic.buyScore * dw) / weightTotal
        var sell = (probability.sellProbability * pw + deterministic.sellScore * dw) / weightTotal
        var neutral = (probability.neutralProbability * pw + deterministic.neutralScore * dw) / weightTotal

        val risk = fsi.coerceIn(0.0, 100.0) / 100.0
        val mtfFactor = 0.90 + mtf.coerceIn(0.0, 100.0) / 100.0 * 0.10

        val directionalFactor =
            (1.0 - risk * 0.45).coerceIn(0.55, 1.0) * mtfFactor

        buy *= directionalFactor
        sell *= directionalFactor

        neutral =
            neutral * (0.80 + risk * 0.20) +
                risk * 8.0 +
                (1.0 - mtf / 100.0) * 4.0

        val total =
            max(0.0, buy) +
                max(0.0, sell) +
                max(0.0, neutral)

        if (total <= 0.0 || !total.isFinite()) {
            return Triple(33.33, 33.33, 33.34)
        }

        return Triple(
            max(0.0, buy) / total * 100.0,
            max(0.0, sell) / total * 100.0,
            max(0.0, neutral) / total * 100.0
        )
    }

    private fun finalDirection(
        probabilities: Triple<Double, Double, Double>,
        mtfCoverage: Double,
        dataQuality: String,
        falseSignalRisk: Double,
        deterministic: DeterministicResult
    ): String {
        val buy = probabilities.first
        val sell = probabilities.second
        val neutral = probabilities.third

        val coverageOk = mtfCoverage >= 60.0
        val qualityOk = dataQuality == "GOOD"
        val riskOk = falseSignalRisk < 55.0
        val confirmationOk = deterministic.confirmation >= 50.0

        if (!qualityOk || !coverageOk || !riskOk || !confirmationOk) {
            return "NEUTRO"
        }

        return when {
            buy >= 60.0 && buy > sell + 5.0 && buy > neutral -> "COMPRA"
            sell >= 60.0 && sell > buy + 5.0 && sell > neutral -> "VENDA"
            else -> "NEUTRO"
        }
    }

    private fun findBestTimeframe(
        realtime: RealtimeAnalysis,
        candles: Map<String, List<MarketCandle>>,
        mtfConfluence: Double
    ): String {
        val candidates = listOf("M5", "M15", "M30", "H1")
        var best =
            if (realtime.metrics.containsKey(selectedTimeframe)) {
                selectedTimeframe
            } else {
                "M15"
            }

        var bestValue = Double.NEGATIVE_INFINITY

        for (timeframe in candidates) {
            val metrics = realtime.metrics[timeframe] ?: continue
            val higher = higherMetrics(timeframe, realtime.metrics)

            val det = DeterministicEngine.calculate(
                DeterministicInput(
                    metrics = metrics,
                    mtfConfluence = mtfConfluence,
                    falseSignalRisk = realtime.fsi,
                    currentPrice = lastQuote?.price ?: 0.0,
                    higherTimeframes = higher
                )
            )

            val prob = ProbabilityEngine.calculate(
                ProbabilityInput(
                    metrics = metrics,
                    mtfConfluence = mtfConfluence,
                    falseSignalRisk = realtime.fsi,
                    fibonacciBullish = fibonacciEvidence(candles[timeframe], true),
                    fibonacciBearish = fibonacciEvidence(candles[timeframe], false),
                    institutionalBullish = det.buyScore,
                    institutionalBearish = det.sellScore
                )
            )

            val final =
                combineFinalProbabilities(
                    prob,
                    det,
                    realtime.fsi,
                    mtfConfluence
                )

            val directional = max(final.first, final.second)

            val score =
                directional +
                    det.confidence * 0.20 +
                    det.confirmation * 0.15 -
                    final.third * 0.25

            val timeframeBonus = when (timeframe) {
                "M15" -> 5.0
                "M30" -> 4.0
                "H1" -> 2.0
                else -> 0.0
            }

            val value = score + timeframeBonus

            if (value > bestValue) {
                bestValue = value
                best = timeframe
            }
        }

        return best
    }

    private fun timeframeRank(timeframe: String): Int =
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

    private fun fibonacciEvidence(
        candles: List<MarketCandle>?,
        bullish: Boolean
    ): Double {
        if (candles == null || candles.size < 20) return 50.0

        val window = candles.takeLast(min(100, candles.size))
        val high = window.maxOf { it.high }
        val low = window.minOf { it.low }
        val last = window.last().close
        val range = high - low

        if (!range.isFinite() || range <= 0.0) return 50.0

        val levels = listOf(
            high - range * 0.382,
            high - range * 0.500,
            high - range * 0.618
        )

        val distance = levels.minOf { abs(last - it) }

        if (distance / range < 0.025) return 65.0

        return if (bullish) {
            if (last > levels[1]) 58.0 else 42.0
        } else {
            if (last < levels[1]) 58.0 else 42.0
        }
    }

    private fun updateMainScreen(
        realtime: RealtimeAnalysis,
        effectiveMtfConfluence: Double,
        finalProbabilities: Triple<Double, Double, Double>,
        direction: String,
        bestTimeframe: String,
        entryPlan: EntryPlanResult,
        deterministic: DeterministicResult
    ) {
        val buy = finalProbabilities.first
        val sell = finalProbabilities.second
        val neutral = finalProbabilities.third

        dopmDashboardController.updateConnection(true, "TWELVE DATA")
        dopmDashboardController.updateMarket(realtime.market.price, selectedAsset)
        dopmDashboardController.updateTimeframe(selectedTimeframe)
        dopmDashboardController.updateDecision(direction, buy, sell, neutral)
        dopmDashboardController.view()?.setProbability(max(buy, sell))
        dopmDashboardController.view()?.setDeterminism(deterministic.confidence)

        dopmDashboardController.view()?.setDeterministicAnalysis(
            trapRisk = deterministic.trapRisk,
            expansion = deterministic.expansion,
            accumulation = deterministic.accumulation,
            distribution = deterministic.distribution,
            exhaustion = deterministic.exhaustion,
            realizationRisk = deterministic.realizationRisk
        )

        dopmDashboardController.view()?.setMtf(effectiveMtfConfluence)

        dopmDashboardController.updateMathematics(
            max(buy, sell),
            deterministic.confidence,
            effectiveMtfConfluence
        )

        dopmDashboardController.updateIndicators(
            realtime.metrics[bestTimeframe]
                ?: realtime.metrics[selectedTimeframe]
                ?: realtime.metrics["M15"]
                ?: return,
            realtime.fsi,
            effectiveMtfConfluence
        )

        dopmDashboardController.updateBestTimeframe(bestTimeframe)

        if (direction == "NEUTRO" || !entryPlan.valid) {
            dopmDashboardController.view()?.setTradePlan(
                "--", "--", "--", "--", "--"
            )
            dopmDashboardController.updateTiming(
                "AGUARDAR",
                "--"
            )
        } else {
            dopmDashboardController.updateTradePlan(
                entryPlan.entry,
                entryPlan.stop,
                entryPlan.tp1,
                entryPlan.tp2,
                entryPlan.tp3
            )
            dopmDashboardController.updateTiming(
                entryPlan.timing,
                "${entryPlan.validityMinutes} min"
            )
        }
    }

    private fun buildDetailedAnalysis(
        realtime: RealtimeAnalysis,
        effectiveFsi: Double,
        effectiveMtfConfluence: Double,
        mtfResult: MultiTimeframeResult,
        deterministic: DeterministicResult,
        probability: ProbabilityResult,
        finalProbabilities: Triple<Double, Double, Double>,
        direction: String,
        bestTimeframe: String,
        metrics: QuantMetrics,
        entryPlan: EntryPlanResult,
        sequence: SequenceResult,
        candles: Map<String, List<MarketCandle>>,
        historicalSamples: Int
    ): String {
        val buy = finalProbabilities.first
        val sell = finalProbabilities.second
        val neutral = finalProbabilities.third

        val total = when (direction) {
            "COMPRA" -> buy
            "VENDA" -> sell
            else -> neutral
        }

        return buildString {
            append("MOTOR PROPRIETÁRIO\n")
            append("ATIVO: $selectedAsset\n")
            append("TIMEFRAME ESCOLHIDO: $selectedTimeframe\n")
            append("VISÃO: $selectedHorizon\n")
            append("MELHOR TIMEFRAME: $bestTimeframe\n\n")
            append("RESULTADO FINAL\n")
            append("DIREÇÃO: $direction\n")
            append("TOTAL: ${"%.1f".format(total)}%\n")
            append("COMPRA: ${"%.1f".format(buy)}%\n")
            append("VENDA: ${"%.1f".format(sell)}%\n")
            append("NEUTRO: ${"%.1f".format(neutral)}%\n")
            append("FSI EFETIVO: ${"%.1f".format(effectiveFsi)}%\n")
            append("FALSO SINAL: ${"%.1f".format(realtime.falseSignal)}%\n\n")
            append("CONFLUÊNCIA MTF\n")
            append("MTF: ${"%.1f".format(effectiveMtfConfluence)}%\n")
            append("COBERTURA: ${"%.1f".format(mtfResult.coverage)}%\n")
            append("ANALISADOS: ${mtfResult.analyzedTimeframes}\n")
            append("COMPRA: ${mtfResult.bullishTimeframes}\n")
            append("VENDA: ${mtfResult.bearishTimeframes}\n")
            append("NEUTRO: ${mtfResult.neutralTimeframes}\n\n")
            append("PROBABILÍSTICO\n")
            append("COMPRA: ${"%.1f".format(probability.buyProbability)}%\n")
            append("VENDA: ${"%.1f".format(probability.sellProbability)}%\n")
            append("NEUTRO: ${"%.1f".format(probability.neutralProbability)}%\n")
            append("VIÉS: ${probability.directionalBias}\n\n")
            append("DETERMINÍSTICO\n")
            append("CONFIANÇA: ${"%.1f".format(deterministic.confidence)}%\n")
            append("ARMADILHA: ${"%.1f".format(deterministic.trapRisk)}%\n")
            append("EXPANSÃO: ${"%.1f".format(deterministic.expansion)}%\n")
            append("ACUMULAÇÃO: ${"%.1f".format(deterministic.accumulation)}%\n")
            append("DISTRIBUIÇÃO: ${"%.1f".format(deterministic.distribution)}%\n")
            append("EXAUSTÃO: ${"%.1f".format(deterministic.exhaustion)}%\n")
            append("LIQUIDEZ: ${"%.1f".format(deterministic.liquidityPressure)}%\n")
            append("REALIZAÇÃO: ${"%.1f".format(deterministic.realizationRisk)}%\n")
            append("CONFLITO MTF: ${"%.1f".format(deterministic.timeframeConflict)}%\n")
            append("CONFIRMAÇÃO: ${"%.1f".format(deterministic.confirmation)}%\n\n")
            append("HISTÓRICO: $historicalSamples amostras\n\n")
            append("MELHOR TIMEFRAME\n")
            append("TENDÊNCIA: ${metricDirection(metrics)}\n")
            append("EMA 9: ${"%.5f".format(metrics.ema9)}\n")
            append("EMA 21: ${"%.5f".format(metrics.ema21)}\n")
            append("EMA 50: ${"%.5f".format(metrics.ema50)}\n")
            append("RSI: ${"%.1f".format(metrics.rsi)}\n")
            append("MACD: ${"%.5f".format(metrics.macd)}\n")
            append("MACD SIGNAL: ${"%.5f".format(metrics.macdSignal)}\n")
            append("ADX: ${"%.1f".format(metrics.adx)}\n")
            append("ATR: ${"%.5f".format(metrics.atr)}\n")
            append("SUPORTE: ${"%.5f".format(metrics.support)}\n")
            append("RESISTÊNCIA: ${"%.5f".format(metrics.resistance)}\n\n")
            append("PLANO OPERACIONAL\n")
            if (direction == "NEUTRO" || !entryPlan.valid) {
                append("STATUS: AGUARDAR\n")
                append("MOTIVO: ${entryPlan.reason}\n")
            } else {
                append("STATUS: ENTRADA VÁLIDA\n")
                append("TIMING: ${entryPlan.timing}\n")
                append("ENTRADA: ${"%.5f".format(entryPlan.entry)}\n")
                append("ZONA: ${"%.5f".format(entryPlan.zoneLow)} – ${"%.5f".format(entryPlan.zoneHigh)}\n")
                append("STOP: ${"%.5f".format(entryPlan.stop)}\n")
                append("TP1: ${"%.5f".format(entryPlan.tp1)}  R:R 1:${entryPlan.rr1}\n")
                append("TP2: ${"%.5f".format(entryPlan.tp2)}  R:R 1:${entryPlan.rr2}\n")
                append("TP3: ${"%.5f".format(entryPlan.tp3)}  R:R 1:${entryPlan.rr3}\n")
                append("VALIDADE: ${entryPlan.validityMinutes} minutos\n")
                append("EXPIRA EM: ${formatExpiry(entryPlan.expiresAt)}\n")
                append("MOTIVO: ${entryPlan.reason}\n")
            }
            append("\nSEQUÊNCIA\n")
            append("ESTÁGIO: ${sequence.stage}\n")
            append("CONFIRMADA: ${if (sequence.confirmed) "SIM" else "NÃO"}\n\n")
            append("DADOS DE MERCADO\n")
            append("FONTE: TWELVE DATA\n")
            append("QUALIDADE: ${realtime.market.dataQuality}\n")
            append("PREÇO: ${"%.5f".format(realtime.market.price)}\n")
            append("CANDLES: ${candles.values.sumOf { it.size }}\n")
            append("SPREAD: NÃO DISPONÍVEL NA FONTE DE QUOTE\n")
            append("EXECUÇÃO DE ORDENS: DESATIVADA\n")
            append("ATUALIZAÇÃO: CONTÍNUA")
        }
    }

    private fun metricDirection(metrics: QuantMetrics): String {
        val bullish = listOf(
            metrics.trend >= 60.0,
            metrics.momentum >= 55.0,
            metrics.ema9 > metrics.ema21,
            metrics.macd > metrics.macdSignal,
            metrics.structure >= 55.0,
            metrics.candlePattern >= 55.0,
            metrics.breakout >= 55.0
        ).count { it }

        val bearish = listOf(
            metrics.trend <= 40.0,
            metrics.momentum <= 45.0,
            metrics.ema9 < metrics.ema21,
            metrics.macd < metrics.macdSignal,
            metrics.structure <= 45.0,
            metrics.candlePattern <= 45.0,
            metrics.breakout <= 45.0
        ).count { it }

        return when {
            bullish >= bearish + 2 -> "MAIOR COMPRA"
            bearish >= bullish + 2 -> "MAIOR VENDA"
            else -> "NEUTRO / EQUILIBRADO"
        }
    }

    private fun formatExpiry(timestamp: Long): String {
        if (timestamp <= 0L) return "--"

        val formatter = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        )

        return formatter.format(java.util.Date(timestamp))
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshTask)
        quoteClient.disconnect()
        super.onDestroy()
    }
}

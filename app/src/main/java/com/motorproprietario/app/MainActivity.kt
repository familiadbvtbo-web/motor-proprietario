package com.motorproprietario.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var dopmDashboardController:
        DopmDashboardController

    private val handler =
        Handler(Looper.getMainLooper())

    private val quoteClient =
        TwelveDataClient()

    private val candleClient =
        TwelveDataCandleClient()

        private val marketCandleCache by lazy {
    MarketCandleCache(
        applicationContext
    )
}

    private val calibrationRuntime by lazy {
        CalibrationRuntime(
            applicationContext
        )
    }

    private val deterministicHistoryEngine by lazy {
    DeterministicHistoryEngine(
        applicationContext
    )
    }

    private var selectedAsset =
        "EUR/USD"

    private var selectedTimeframe =
        "M15"

    private var selectedHorizon =
        "GERAL"

    private var lastQuote:
        RealTimeQuote? = null

    private var analyzing =
        false

    private var sequenceStage =
        SequenceStage.S0

    private var latestDetailedAnalysis =
        "Aguardando dados reais..."

    private val candleCache =
        LinkedHashMap<
            String,
            List<MarketCandle>
        >()

    private val lastCandleUpdate =
        HashMap<String, Long>()

    private val candleIntervals =
    mapOf(
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

    private var candleApiBackoffUntil =
        0L

    private val candleApiBackoffMs =
        65_000L

    private val refreshTask =
        object : Runnable {

            override fun run() {

                analyzeMarket()

                handler.postDelayed(
                    this,
                    5_000L
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        /*
         * NOVA INTERFACE DOPM
         *
         * A interface antiga não é mais
         * construída dentro da Activity.
         */
        dopmDashboardController =
            DopmDashboardController(this)

        dopmDashboardController.install()

        dopmDashboardController.setSelectionListeners(

            marketChanged = { market ->

                when (market) {

                    "FOREX" -> {

                        selectedAsset =
                            "EUR/USD"

                        selectedTimeframe =
                            "M15"

                        resetForNewAsset()

                        connectRealtime()
                    }

                    "CRIPTO" -> {

                        selectedAsset =
                            "BTC/USD"

                        selectedTimeframe =
                            "M15"

                        resetForNewAsset()

                        connectRealtime()
                    }

                    "B3" -> {

                        selectedAsset =
                            "IBOV"

                        selectedTimeframe =
                            "M15"

                        resetForNewAsset()

                        connectRealtime()
                    }
                }
            },

            assetChanged = { asset ->

                if (
                    asset.isNotBlank() &&
                    asset != selectedAsset
                ) {

                    selectedAsset =
                        asset

                    resetForNewAsset()

                    connectRealtime()
                }
            },

            timeframeChanged = { timeframe ->

                if (
                    timeframe.isNotBlank() &&
                    timeframe != selectedTimeframe
                ) {

                    selectedTimeframe =
                        timeframe

                    analyzeMarket()
                }
            }
        )

        connectRealtime()

        handler.post(
            refreshTask
        )
    }

    private fun resetForNewAsset() {

        quoteClient.disconnect()

        lastQuote =
            null

        synchronized(
    candleCache
) {
    candleCache.clear()
}

lastCandleUpdate.clear()

        candleApiBackoffUntil =
            0L

        sequenceStage =
            SequenceStage.S0

        latestDetailedAnalysis =
            "Aguardando dados reais..."

        dopmDashboardController
            .view()
            ?.apply {

                setOnline(
                    false
                )

                setApi(
                    "TWELVE DATA"
                )

                setPrice(
                    "--"
                )

                setDecision(
                    "AGUARDAR",
                    0.0
                )

                setProbabilities(
                    0.0,
                    0.0,
                    100.0
                )

                setProbability(
                    0.0
                )

                setDeterminism(
                    0.0
                )

                setMtf(
                    0.0
                )

                setBestTimeframe(
                    "--"
                )

                setTradePlan(
                    "--",
                    "--",
                    "--",
                    "--",
                    "--"
                )

                setTiming(
                    "AGUARDAR",
                    "--"
                )
            }
    }

    private fun connectRealtime() {

        quoteClient.connect(

            symbols =
                listOf(
                    selectedAsset
                ),

            onQuote = { quote ->

                lastQuote =
                    quote

                runOnUiThread {

                    dopmDashboardController
                        .view()
                        ?.apply {

                            setOnline(
                                true
                            )

                            setApi(
                                "TWELVE DATA"
                            )

                            setPrice(
                                String.format(
                                    "%.5f",
                                    quote.price
                                )
                            )
                        }
                }
            },

            onError = { _ ->

                runOnUiThread {

                    dopmDashboardController
                        .view()
                        ?.setOnline(
                            false
                        )
                }
            }
        )
    }

    /*
     * ============================================================
     * ANÁLISE PRINCIPAL
     * ============================================================
     *
     * Fluxo real:
     *
     * TWELVE DATA
     *      ↓
     * Candles
     *      ↓
     * RealtimeMarketAnalyzer
     *      ↓
     * ┌──────────────────────┐
     * │ ProbabilityEngine    │
     * │ DeterministicEngine  │
     * └──────────────────────┘
     *      ↓
     * Combinação final
     *      ↓
     * Melhor timeframe
     *      ↓
     * EntryPlanEngine
     *      ↓
     * SequenceEngine
     *      ↓
     * Nova interface DOPM
     */
    private fun analyzeMarket() {

        if (
            analyzing
        ) {
            return
        }

        val quote =
            lastQuote
                ?: return

        analyzing =
            true

        thread {

            try {

                val now =
                    System.currentTimeMillis()

                updateCandles(
                    now
                )

                val candles =
                    LinkedHashMap<
                        String,
                        List<MarketCandle>
                    >()

                synchronized(
                    candleCache
                ) {

                    candles.putAll(
                        candleCache
                    )
                }

                if (
                    candles.isEmpty()
                ) {

                    runOnUiThread {

                        dopmDashboardController
                            .view()
                            ?.apply {

                                setOnline(
                                    false
                                )

                                setApi(
                                    "TWELVE DATA"
                                )

                                setPrice(
                                    "--"
                                )

                                setDecision(
                                    "AGUARDAR",
                                    0.0
                                )

                                setProbabilities(
                                    0.0,
                                    0.0,
                                    100.0
                                )

                                setProbability(
                                    0.0
                                )

                                setDeterminism(
                                    0.0
                                )

                                setMtf(
                                    0.0
                                )

                                setBestTimeframe(
                                    "--"
                                )

                                setTradePlan(
                                    "--",
                                    "--",
                                    "--",
                                    "--",
                                    "--"
                                )

                                setTiming(
                                    "AGUARDAR",
                                    "AGUARDANDO CANDLES REAIS"
                                )
                            }
                    }

                    return@thread
                }

                /*
                 * ==================================================
                 * ANÁLISE DE MERCADO
                 * ==================================================
                 */
                val realtime =
                    RealtimeMarketAnalyzer.analyze(

                        symbol =
                            selectedAsset,

                        candlesByTimeframe =
                            candles,

                        price =
                            quote.price,

                        bid =
                            quote.price,

                        ask =
                            quote.price,

                        timestamp =
                            quote.timestamp,

                        now =
                            now
                    )

                /*
                 * ==================================================
                 * TIMEFRAME SELECIONADO
                 * ==================================================
                 */
                val selectedMetrics =
                    realtime.metrics[
                        selectedTimeframe
                    ]
                        ?: realtime.metrics[
                            "M15"
                        ]
                        ?: realtime.metrics.values.first()

                        val mtfResult =
    MultiTimeframeEngine.calculate(
        realtime.metrics
    )

val effectiveMtfConfluence =
    mtfResult.confluence

                val higherMetrics =
                    realtime.metrics
                        .filterKeys {

                            timeframeRank(it) >
                                timeframeRank(
                                    selectedTimeframe
                                )
                        }
                        .values
                        .toList()

                /*
                 * ==================================================
                 * MOTOR DETERMINÍSTICO
                 * ==================================================
                 */
                var deterministic =
                    DeterministicEngine.calculate(

                        DeterministicInput(

                            metrics =
                                selectedMetrics,

                            mtfConfluence =
    effectiveMtfConfluence,

                            falseSignalRisk =
                                realtime.fsi,

                            currentPrice =
                                quote.price,

                            higherTimeframes =
                                higherMetrics
                        )
                    )

                /*
                 * ==================================================
                 * MOTOR PROBABILÍSTICO
                 * ==================================================
                 */
                val probability =
                    ProbabilityEngine.calculate(

                        ProbabilityInput(

                            metrics =
                                selectedMetrics,

                            mtfConfluence =
    effectiveMtfConfluence,

                            falseSignalRisk =
                                realtime.fsi,

                            fibonacciBullish =
                                fibonacciEvidence(
                                    candles[
                                        selectedTimeframe
                                    ],
                                    true
                                ),

                            fibonacciBearish =
                                fibonacciEvidence(
                                    candles[
                                        selectedTimeframe
                                    ],
                                    false
                                ),

                            institutionalBullish =
                                deterministic.buyScore,

                            institutionalBearish =
                                deterministic.sellScore
                        )
                    )

                /*
                 * ==================================================
                 * DECISÃO FINAL
                 * ==================================================
                 */
                val finalProbabilities =
                    combineFinalProbabilities(

                        probability,

                        deterministic,

                        realtime.fsi,

                        effectiveMtfConfluence
                    )

                /*
                 * ==================================================
                 * MELHOR TIMEFRAME
                 * ==================================================
                 */
                val bestTimeframe =
                    findBestTimeframe(
                        realtime,
                        candles,
                        effectiveMtfConfluence
                    )

                val bestMetrics =
                    realtime.metrics[
                        bestTimeframe
                    ]
                        ?: selectedMetrics

                /*
                 * ==================================================
                 * DETERMINISMO DO MELHOR TIMEFRAME
                 * ==================================================
                 */
                var bestDeterministic =
                    DeterministicEngine.calculate(

                        DeterministicInput(

                            metrics =
                                bestMetrics,

                            mtfConfluence =
                               effectiveMtfConfluence,

                            falseSignalRisk =
                                realtime.fsi,

                            currentPrice =
                                quote.price,

                            higherTimeframes =
                                realtime.metrics
                                    .filterKeys {

                                        timeframeRank(it) >
                                            timeframeRank(
                                                bestTimeframe
                                            )
                                    }
                                    .values
                                    .toList()
                        )
                    )

                    bestDeterministic =
    deterministicHistoryEngine.apply(

        symbol =
            selectedAsset,

        timeframe =
            bestTimeframe,

        stage =
            sequenceStage.name,

        metrics =
            bestMetrics,

        deterministic =
            bestDeterministic,

        currentPrice =
            quote.price,

        now =
            now
    )

    /*
 * ============================================================
 * FSI EFETIVO DO MOTOR PROPRIETÁRIO
 * ============================================================
 *
 * Combina:
 *
 * 1. FSI atual do mercado
 * 2. Pressão histórica de captura
 * 3. Pressão histórica de realização
 *
 * O histórico só interfere depois de existir
 * quantidade mínima de amostras.
 */
val historicalStats =
    deterministicHistoryEngine.statistics(

        symbol =
            selectedAsset,

        timeframe =
            bestTimeframe,

        stage =
            sequenceStage.name,

        metrics =
            bestMetrics,

        direction =
            bestDeterministic.directionalBias
    )

val effectiveFsi =
    if (
        historicalStats.samples < 3
    ) {

        realtime.fsi

    } else {

        (
            realtime.fsi * 0.65 +

            historicalStats.capturePressure *
                0.20 +

            historicalStats.realizationPressure *
                0.15

        ).coerceIn(
            0.0,
            100.0
        )
    }

                /*
                 * ==================================================
                 * PROBABILIDADE DO MELHOR TIMEFRAME
                 * ==================================================
                 */
                val bestProbability =
                    ProbabilityEngine.calculate(

                        ProbabilityInput(

                            metrics =
                                bestMetrics,

                            mtfConfluence =
    effectiveMtfConfluence,

                            falseSignalRisk =
                                  effectiveFsi,


                            fibonacciBullish =
                                fibonacciEvidence(
                                    candles[
                                        bestTimeframe
                                    ],
                                    true
                                ),

                            fibonacciBearish =
                                fibonacciEvidence(
                                    candles[
                                        bestTimeframe
                                    ],
                                    false
                                ),

                            institutionalBullish =
                                bestDeterministic.buyScore,

                            institutionalBearish =
                                bestDeterministic.sellScore
                        )
                    )

                val bestFinal =
    combineFinalProbabilities(

        bestProbability,

        bestDeterministic,

        effectiveFsi,

        effectiveMtfConfluence
    )

                /*
                 * ==================================================
                 * DIREÇÃO FINAL
                 * ==================================================
                 */
                val bestDirection =
                    finalDirection(
                        bestFinal
                    )

                /*
                 * ==================================================
                 * PLANO OPERACIONAL
                 * ==================================================
                 */
                val entryPlan =
                    EntryPlanEngine.calculate(

                        EntryPlanInput(

                            direction =
                                bestDirection,

                            currentPrice =
                                quote.price,

                            metrics =
                                bestMetrics,

                            timeframe =
                                bestTimeframe,

                            probability =
                                max(
                                    bestFinal.first,
                                    bestFinal.second
                                ),

                            deterministicConfidence =
                                bestDeterministic.confidence,

                            falseSignalRisk =
                                  effectiveFsi,

                            now =
                                now
                        )
                    )

                /*
                 * ==================================================
                 * SEQUÊNCIA
                 * ==================================================
                 */
                val sequence =
                    SequenceEngine.advance(

                        sequenceStage,

                        SequenceInput(

                            signalDetected =
                                bestDirection !=
                                    "NEUTRO",

                            confirmation =
    effectiveMtfConfluence >=
        60.0,

                            continuation =
                                when (
                                    bestDirection
                                ) {

                                    "COMPRA" ->

                                        bestMetrics.ema9 >
                                            bestMetrics.ema21 &&
                                        bestMetrics.macd >
                                            bestMetrics.macdSignal

                                    "VENDA" ->

                                        bestMetrics.ema9 <
                                            bestMetrics.ema21 &&
                                        bestMetrics.macd <
                                            bestMetrics.macdSignal

                                    else ->
                                        false
                                },

                            invalidated =
    effectiveFsi >=
        80.0 ||
    realtime.market.dataQuality !=
        "GOOD"
                        )
                    )

                sequenceStage =
                    sequence.stage

                /*
                 * ==================================================
                 * ANÁLISE DETALHADA
                 * ==================================================
                 */
                val detailed =
                    buildDetailedAnalysis(

                        realtime =
                            realtime,
                        
                        effectiveFsi =
                            effectiveFsi,
                        
                        effectiveMtfConfluence =
    effectiveMtfConfluence,

                        deterministic =
                            bestDeterministic,

                        probability =
                            bestProbability,

                        finalProbabilities =
                            bestFinal,

                        direction =
                            bestDirection,

                        bestTimeframe =
                            bestTimeframe,

                        metrics =
                            bestMetrics,

                        entryPlan =
                            entryPlan,

                        sequence =
                            sequence,

                        candles =
                            candles
                    )

                runOnUiThread {

                    latestDetailedAnalysis =
                        detailed

                    updateMainScreen(

                        realtime =
                            realtime,

                        effectiveMtfConfluence =
    effectiveMtfConfluence,

                        finalProbabilities =
                            bestFinal,

                        direction =
                            bestDirection,

                        bestTimeframe =
                            bestTimeframe,

                        entryPlan =
                            entryPlan,

                        deterministic =
                            bestDeterministic
                    )
                }

            } catch (
                error: Exception
            ) {

                /*
                 * Um erro de análise não deve
                 * acessar os TextViews antigos.
                 */
                runOnUiThread {

                    dopmDashboardController
                        .view()
                        ?.apply {

                            setOnline(
                                false
                            )

                            setApi(
                                "TWELVE DATA"
                            )

                            setDecision(
                                "AGUARDAR",
                                0.0
                            )

                            setProbabilities(
                                0.0,
                                0.0,
                                100.0
                            )

                            setProbability(
                                0.0
                            )

                            setDeterminism(
                                0.0
                            )

                            setMtf(
                                0.0
                            )

                            setBestTimeframe(
                                "--"
                            )

                            setTradePlan(
                                "--",
                                "--",
                                "--",
                                "--",
                                "--"
                            )

                            setTiming(
                                "ERRO NA ANÁLISE",
                                "AGUARDAR"
                            )
                        }
                }

            } finally {

                analyzing =
                    false
            }
        }
    }

    /*
     * ============================================================
     * ATUALIZAÇÃO DA NOVA INTERFACE
     * ============================================================
     */
    private fun updateMainScreen(

        realtime:
            RealtimeAnalysis,

        effectiveMtfConfluence:
    Double,

        finalProbabilities:
            Triple<Double, Double, Double>,

        direction:
            String,

        bestTimeframe:
            String,

        entryPlan:
            EntryPlanResult,

        deterministic:
            DeterministicResult
    ) {

        val buy =
            finalProbabilities.first

        val sell =
            finalProbabilities.second

        val neutral =
            finalProbabilities.third

        /*
         * TOTAL é a probabilidade correspondente
         * à direção final.
         */
        val total =
            when (
                direction
            ) {

                "COMPRA" ->
                    buy

                "VENDA" ->
                    sell

                else ->
                    neutral
            }

        /*
         * ==================================================
         * CONEXÃO
         * ==================================================
         */
        dopmDashboardController.updateConnection(

            online =
                true,

            api =
                "TWELVE DATA"
        )

        /*
         * ==================================================
         * ATIVO / PREÇO
         * ==================================================
         */
        dopmDashboardController.updateMarket(

            price =
                realtime.market.price,

            asset =
                selectedAsset
        )

        /*
         * ==================================================
         * DECISÃO
         * ==================================================
         */
        dopmDashboardController.updateDecision(

            direction =
                direction,

            buy =
                buy,

            sell =
                sell,

            neutral =
                neutral
        )

        /*
         * ==================================================
         * PROBABILIDADE REAL
         * ==================================================
         */
        dopmDashboardController
            .view()
            ?.setProbability(
                max(
                    buy,
                    sell
                )
            )

        /*
         * ==================================================
         * DETERMINISMO REAL
         * ==================================================
         */
        dopmDashboardController
            .view()
            ?.setDeterminism(
                deterministic.confidence
            )

            dopmDashboardController
    .view()
    ?.setDeterministicAnalysis(

        trapRisk =
            deterministic.trapRisk,

        expansion =
            deterministic.expansion,

        accumulation =
            deterministic.accumulation,

        distribution =
            deterministic.distribution,

        exhaustion =
            deterministic.exhaustion,

        realizationRisk =
            deterministic.realizationRisk
    )

        /*
         * ==================================================
         * CONFLUÊNCIA MTF REAL
         * ==================================================
         */
        dopmDashboardController
            .view()
            ?.setMtf(
                effectiveMtfConfluence
            )

        /*
         * Mantém também o fluxo do Controller.
         */
        dopmDashboardController.updateMathematics(

            probability =
                max(
                    buy,
                    sell
                ),

            deterministic =
                deterministic.confidence,

            mtf =
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

        /*
         * ==================================================
         * MELHOR TIMEFRAME
         * ==================================================
         */
        dopmDashboardController.updateBestTimeframe(

            bestTimeframe
        )

        /*
         * ==================================================
         * PLANO OPERACIONAL
         * ==================================================
         */
        if (
            direction ==
                "NEUTRO" ||
            !entryPlan.valid
        ) {

            /*
             * Não envia números fictícios.
             *
             * A interface recebe "--".
             */
            dopmDashboardController
                .view()
                ?.setTradePlan(

                    entry =
                        "--",

                    stop =
                        "--",

                    tp1 =
                        "--",

                    tp2 =
                        "--",

                    tp3 =
                        "--"
                )

            dopmDashboardController
                .updateTiming(

                    timing =
                        "AGUARDAR",

                    validity =
                        "--"
                )

        } else {

            dopmDashboardController
                .updateTradePlan(

                    entry =
                        entryPlan.entry,

                    stop =
                        entryPlan.stop,

                    tp1 =
                        entryPlan.tp1,

                    tp2 =
                        entryPlan.tp2,

                    tp3 =
                        entryPlan.tp3
                )

            dopmDashboardController
                .updateTiming(

                    timing =
                        entryPlan.timing,

                    validity =
                        "${entryPlan.validityMinutes} min"
                )
        }

        /*
         * Evita variável não utilizada.
         *
         * O valor continua sendo calculado pelo motor.
         */
        @Suppress("UNUSED_VARIABLE")
        val finalTotal =
            total
    }

    /*
     * ============================================================
     * CANDLES REAIS
     * ============================================================
     */
    private fun updateCandles(
        now: Long
    ) {

        if (
            now <
                candleApiBackoffUntil
        ) {
            return
        }

        val timeframes =
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

        for (
            timeframe in timeframes
        ) {

            val interval =
                candleIntervals[
                    timeframe
                ]
                    ?: 60_000L

            val last =
                lastCandleUpdate[
                    timeframe
                ]
                    ?: 0L

            val cached =
                candleCache[
                    timeframe
                ]
            val persistent =
    marketCandleCache.get(
        symbol =
            selectedAsset,

        timeframe =
            timeframe
    )

if (
    persistent.isNotEmpty()
) {

    synchronized(
        candleCache
    ) {

        candleCache[
            timeframe
        ] =
            persistent
    }
}

            if (
                cached != null &&
                now - last <
                    interval
            ) {
                continue
            }

            try {

                val fresh =
                    candleClient.getCandles(

                        symbol =
                            selectedAsset,

                        timeframe =
                            timeframe,

                        outputSize =
                            1000
                    )

             if (
    fresh.isNotEmpty()
) {

    val merged =
        marketCandleCache.merge(
            symbol =
                selectedAsset,

            timeframe =
                timeframe,

            freshCandles =
                fresh
        )

    synchronized(
        candleCache
    ) {

        candleCache[
            timeframe
        ] =
            merged
    }

    lastCandleUpdate[
        timeframe
    ] =
        now
}   

                Thread.sleep(
                    700L
                )

            } catch (
                error: Exception
            ) {

                val message =
                    error.message
                        ?: ""

                if (
                    message.contains(
                        "429",
                        ignoreCase = true
                    )
                ) {

                    candleApiBackoffUntil =
                        System.currentTimeMillis() +
                            candleApiBackoffMs

                    runOnUiThread {

                        dopmDashboardController
                            .view()
                            ?.setApi(
                                "TWELVE DATA • CACHE"
                            )
                    }

                    break
                }
            }
        }
    }

    /*
     * ============================================================
     * COMBINAÇÃO PROBABILÍSTICA + DETERMINÍSTICA
     * ============================================================
     */
    private fun combineFinalProbabilities(

        probability:
            ProbabilityResult,

        deterministic:
            DeterministicResult,

        fsi:
            Double,

        mtf:
            Double
    ): Triple<Double, Double, Double> {

        val weights =
            calibrationRuntime.weights()

        val probabilityWeight =
            weights.first

        val deterministicWeight =
            weights.second

        var buy =
            probability.buyProbability *
                probabilityWeight +
            deterministic.buyScore *
                deterministicWeight

        var sell =
            probability.sellProbability *
                probabilityWeight +
            deterministic.sellScore *
                deterministicWeight

        var neutral =
            probability.neutralProbability *
                probabilityWeight +
            deterministic.neutralScore *
                deterministicWeight

        val riskFactor =
            1.0 -
                fsi.coerceIn(
                    0.0,
                    100.0
                ) /
                100.0

        buy *=
            0.65 +
                riskFactor * 0.35

        sell *=
            0.65 +
                riskFactor * 0.35

        neutral +=
            fsi * 0.30

        val mtfFactor =
            0.85 +
                mtf.coerceIn(
                    0.0,
                    100.0
                ) /
                100.0 *
                0.15

        buy *=
            mtfFactor

        sell *=
            mtfFactor

        neutral +=
            (
                100.0 -
                    mtf
            ) *
                0.20

        val total =
            buy.coerceAtLeast(
                0.0
            ) +
            sell.coerceAtLeast(
                0.0
            ) +
            neutral.coerceAtLeast(
                0.0
            )

        if (
            total <= 0.0
        ) {

            return Triple(
                33.33,
                33.33,
                33.34
            )
        }

        return Triple(

            buy.coerceAtLeast(
                0.0
            ) /
                total *
                100.0,

            sell.coerceAtLeast(
                0.0
            ) /
                total *
                100.0,

            neutral.coerceAtLeast(
                0.0
            ) /
                total *
                100.0
        )
    }

    /*
     * ============================================================
     * DIREÇÃO FINAL
     * ============================================================
     */
    private fun finalDirection(
        probabilities:
            Triple<Double, Double, Double>
    ): String {

        val buy =
            probabilities.first

        val sell =
            probabilities.second

        val neutral =
            probabilities.third

        return when {

            buy >= 60.0 &&
            buy >
                sell + 5.0 &&
            buy >
                neutral ->

                "COMPRA"

            sell >= 60.0 &&
            sell >
                buy + 5.0 &&
            sell >
                neutral ->

                "VENDA"

            else ->
                "NEUTRO"
        }
    }

    /*
     * ============================================================
     * MELHOR TIMEFRAME
     * ============================================================
     */
    private fun findBestTimeframe(

        realtime:
            RealtimeAnalysis,

        candles:
            Map<String, List<MarketCandle>>,
mtfConfluence:
    Double
        
    ): String {

        val candidates =
            listOf(
                "M5",
                "M15",
                "M30",
                "H1"
            )

        var best =
            "M15"

        var bestValue =
            Double.NEGATIVE_INFINITY

        for (
            timeframe in candidates
        ) {

            val metrics =
                realtime.metrics[
                    timeframe
                ]
                    ?: continue

            val higher =
                realtime.metrics
                    .filterKeys {

                        timeframeRank(it) >
                            timeframeRank(
                                timeframe
                            )
                    }
                    .values
                    .toList()

            val deterministic =
                DeterministicEngine.calculate(

                    DeterministicInput(

                        metrics =
                            metrics,

                        mtfConfluence =
                            mtfConfluence,

                        falseSignalRisk =
                            realtime.fsi,

                        currentPrice =
                            lastQuote?.price
                                ?: 0.0,

                        higherTimeframes =
                            higher
                    )
                )

            val probability =
                ProbabilityEngine.calculate(

                    ProbabilityInput(

                        metrics =
                            metrics,

                        mtfConfluence =
                            mtfConfluence,

                        falseSignalRisk =
                            realtime.fsi,

                        fibonacciBullish =
                            fibonacciEvidence(
                                candles[
                                    timeframe
                                ],
                                true
                            ),

                        fibonacciBearish =
                            fibonacciEvidence(
                                candles[
                                    timeframe
                                ],
                                false
                            ),

                        institutionalBullish =
                            deterministic.buyScore,

                        institutionalBearish =
                            deterministic.sellScore
                    )
                )

            val final =
                combineFinalProbabilities(

                    probability,

                    deterministic,

                    realtime.fsi,

                    mtfConfluence
                )

            val directional =
                max(
                    final.first,
                    final.second
                )

            val score =
                directional +
                    deterministic.confidence *
                    0.20 +
                    deterministic.confirmation *
                    0.15 -
                    final.third *
                    0.35

            val timeframeBonus =
                when (
                    timeframe
                ) {

                    "M15" ->
                        5.0

                    "M30" ->
                        4.0

                    "H1" ->
                        2.0

                    else ->
                        0.0
                }

            val value =
                score +
                    timeframeBonus

            if (
                value >
                    bestValue
            ) {

                bestValue =
                    value

                best =
                    timeframe
            }
        }

        return best
    }

    /*
     * ============================================================
     * RANKING DOS TIMEFRAMES
     * ============================================================
     */
    
private fun timeframeRank(
    timeframe: String
): Int =
    when (
        timeframe
    ) {

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
    
    /*
     * ============================================================
     * EVIDÊNCIA DE FIBONACCI
     * ============================================================
     */
    private fun fibonacciEvidence(

        candles:
            List<MarketCandle>?,

        bullish:
            Boolean
    ): Double {

        if (
            candles == null ||
            candles.size < 20
        ) {

            return 50.0
        }

        val window =
            candles.takeLast(
                min(
                    100,
                    candles.size
                )
            )

        val high =
            window.maxOf {
                it.high
            }

        val low =
            window.minOf {
                it.low
            }

        val last =
            window.last().close

        val range =
            high -
                low

        if (
            range <= 0.0
        ) {

            return 50.0
        }

        val level382 =
            high -
                range *
                0.382

        val level500 =
            high -
                range *
                0.500

        val level618 =
            high -
                range *
                0.618

        val distance =
            min(
                abs(
                    last -
                        level382
                ),
                min(
                    abs(
                        last -
                            level500
                    ),
                    abs(
                        last -
                            level618
                    )
                )
            )

        if (
            distance /
                range <
                0.025
        ) {

            return 65.0
        }

        return if (
            bullish
        ) {

            if (
                last >
                    level500
            ) {

                58.0

            } else {

                42.0
            }

        } else {

            if (
                last <
                    level500
            ) {

                58.0

            } else {

                42.0
            }
        }
    }

    /*
     * ============================================================
     * ANÁLISE DETALHADA
     * ============================================================
     */
    private fun buildDetailedAnalysis(

        realtime:
            RealtimeAnalysis,
        
            effectiveFsi:
        Double,

        effectiveMtfConfluence:
    Double,

        deterministic:
            DeterministicResult,

        probability:
            ProbabilityResult,

        finalProbabilities:
            Triple<Double, Double, Double>,

        direction:
            String,

        bestTimeframe:
            String,

        metrics:
            QuantMetrics,

        entryPlan:
            EntryPlanResult,

        sequence:
            SequenceResult,

        candles:
            Map<String, List<MarketCandle>>
    ): String {

        val buy =
            finalProbabilities.first

        val sell =
            finalProbabilities.second

        val neutral =
            finalProbabilities.third

        val finalTotal =
            when (
                direction
            ) {

                "COMPRA" ->
                    buy

                "VENDA" ->
                    sell

                else ->
                    neutral
            }

        val calibrationWeights =
            calibrationRuntime.weights()

        val probabilityWeight =
            calibrationWeights.first *
                100.0

        val deterministicWeight =
            calibrationWeights.second *
                100.0

        val calibrationStatus =
            if (
                calibrationRuntime.isCalibrated()
            ) {

                "HISTÓRICO ATIVO"

            } else {

                "PADRÃO 50/50"
            }

        val confidence =
            (
                max(
                    buy,
                    sell
                ) -
                    neutral
            ).coerceIn(
                0.0,
                100.0
            )

        val deterministicLevel =
            when {

                deterministic.confidence >=
                    80.0 ->
                    "MUITO FORTE"

                deterministic.confidence >=
                    65.0 ->
                    "FORTE"

                deterministic.confidence >=
                    50.0 ->
                    "MODERADO"

                else ->
                    "FRACO"
            }

        val trapLevel =
            when {

                deterministic.trapRisk >=
                    70.0 ->
                    "ALTA"

                deterministic.trapRisk >=
                    50.0 ->
                    "MODERADA"

                else ->
                    "BAIXA"
            }

        val expansionLevel =
            when {

                deterministic.expansion >=
                    70.0 ->
                    "CONFIRMADA"

                deterministic.expansion >=
                    50.0 ->
                    "EM FORMAÇÃO"

                else ->
                    "NÃO CONFIRMADA"
            }

        val accumulationLevel =
            when {

                deterministic.accumulation >=
                    70.0 ->
                    "DETECTADA"

                deterministic.accumulation >=
                    50.0 ->
                    "POSSÍVEL"

                else ->
                    "NÃO DETECTADA"
            }

        val distributionLevel =
            when {

                deterministic.distribution >=
                    70.0 ->
                    "DETECTADA"

                deterministic.distribution >=
                    50.0 ->
                    "POSSÍVEL"

                else ->
                    "NÃO DETECTADA"
            }

        val realizationLevel =
            when {

                deterministic.realizationRisk >=
                    70.0 ->
                    "ALTO"

                deterministic.realizationRisk >=
                    50.0 ->
                    "MODERADO"

                else ->
                    "BAIXO"
            }

            val historicalStats =
    deterministicHistoryEngine.statistics(

        symbol =
            selectedAsset,

        timeframe =
            bestTimeframe,

        stage =
            sequence.stage.name,

        metrics =
            metrics,

        direction =
            direction
    )

        return buildString {

            append(
                "MOTOR PROPRIETÁRIO\n"
            )

            append(
                "ATIVO: $selectedAsset\n"
            )

            append(
                "TIMEFRAME ESCOLHIDO: $selectedTimeframe\n"
            )

            append(
                "VISÃO: $selectedHorizon\n"
            )

            append(
                "MELHOR TIMEFRAME: $bestTimeframe\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "RESULTADO FINAL\n\n"
            )

            append(
                "DIREÇÃO: ${
                    when (
                        direction
                    ) {

                        "COMPRA" ->
                            "COMPRA"

                        "VENDA" ->
                            "VENDA"

                        else ->
                            "NEUTRO / AGUARDAR"
                    }
                }\n"
            )

            append(
                "TOTAL: ${
                    "%.1f".format(
                        finalTotal
                    )
                }%\n"
            )

            append(
                "COMPRA: ${
                    "%.1f".format(
                        buy
                    )
                }%\n"
            )

            append(
                "VENDA: ${
                    "%.1f".format(
                        sell
                    )
                }%\n"
            )

            append(
                "NEUTRO: ${
                    "%.1f".format(
                        neutral
                    )
                }%\n"
            )

            append(
                "CONFIANÇA RELATIVA: ${
                    "%.1f".format(
                        confidence
                    )
                }%\n\n"
            )

            append(
    "CONFLUÊNCIA MULTI-TIMEFRAME\n\n"
)

append(
    "MTF: ${
        "%.1f".format(
            effectiveMtfConfluence
        )
    }%\n"
)

append(
    "TIMEFRAMES ANALISADOS: ${
        mtfAnalyzed
    }\n"
)

append(
    "COMPRA: ${
        mtfBull
    }\n"
)

append(
    "VENDA: ${
        mtfBear
    }\n"
)

append(
    "NEUTRO: ${
        mtfNeutral
    }\n\n"
)
            
            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "CALIBRAÇÃO\n\n"
            )

            append(
                "STATUS: $calibrationStatus\n"
            )

            append(
                "PROBABILIDADE: ${
                    "%.1f".format(
                        probabilityWeight
                    )
                }%\n"
            )

            append(
                "DETERMINISMO: ${
                    "%.1f".format(
                        deterministicWeight
                    )
                }%\n\n"
            )

            append(
                "Os pesos acima são os pesos atualmente\n"
            )

            append(
                "retornados pelo CalibrationRuntime.\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "COMPOSIÇÃO PROBABILÍSTICA\n\n"
            )

            append(
                "COMPRA: ${
                    "%.1f".format(
                        probability.buyProbability
                    )
                }%\n"
            )

            append(
                "VENDA: ${
                    "%.1f".format(
                        probability.sellProbability
                    )
                }%\n"
            )

            append(
                "NEUTRO: ${
                    "%.1f".format(
                        probability.neutralProbability
                    )
                }%\n"
            )

            append(
                "VIÉS: ${
                    probability.directionalBias
                }\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "DETERMINISMO\n\n"
            )

            append(
                "CONFIANÇA: ${
                    "%.1f".format(
                        deterministic.confidence
                    )
                }%\n"
            )

            append(
                "NÍVEL: $deterministicLevel\n"
            )

            append(
                "ARMADILHA: $trapLevel\n"
            )

            append(
                "EXPANSÃO: $expansionLevel\n"
            )

            append(
                "ACUMULAÇÃO: $accumulationLevel\n"
            )

            append(
                "DISTRIBUIÇÃO: $distributionLevel\n"
            )

            append(
                "EXAUSTÃO: ${
                    "%.1f".format(
                        deterministic.exhaustion
                    )
                }%\n"
            )

            append(
                "LIQUIDEZ: ${
                    "%.1f".format(
                        deterministic.liquidityPressure
                    )
                }%\n"
            )

            append(
                "REALIZAÇÃO: $realizationLevel\n"
            )

            append(
                "CONFLITO MTF: ${
                    "%.1f".format(
                        deterministic.timeframeConflict
                    )
                }%\n"
            )

            append(
                "CONFIRMAÇÃO: ${
                    "%.1f".format(
                        deterministic.confirmation
                    )
                }%\n\n"
            )

            append(
    "HISTÓRICO DE FALSOS SINAIS\n\n"
)

append(
    "AMOSTRAS: ${
        historicalStats.samples
    }\n"
)

append(
    "ACERTOS: ${
        historicalStats.wins
    }\n"
)

append(
    "FALSOS SINAIS: ${
        historicalStats.falseSignals
    }\n"
)

append(
    "FALSOS CONSECUTIVOS: ${
        historicalStats.consecutiveFalseSignals
    }\n"
)

append(
    "PRESSÃO DE CAPTURA: ${
        "%.1f".format(
            historicalStats.capturePressure
        )
    }%\n"
)

append(
    "PRESSÃO DE REALIZAÇÃO: ${
        "%.1f".format(
            historicalStats.realizationPressure
        )
    }%\n\n"
)

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "CONFLUÊNCIA MTF / RISCO\n\n"
            )

            append(
                "MTF: ${
                    "%.1f".format(
                        realtime.mtfConfluence
                    )
                }%\n"
            )

            append(
                "REGIME: ${
                    realtime.regime
                }\n"
            )

            append(
    "FSI EFETIVO: ${
        "%.1f".format(
            effectiveFsi
        )
    }%\n"
)

            append(
                "FALSO SINAL: ${
                    "%.1f".format(
                        realtime.falseSignal
                    )
                }%\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "MELHOR TIMEFRAME\n\n"
            )

            append(
                "$bestTimeframe\n\n"
            )

            append(
                "TENDÊNCIA: ${
                    metricDirection(
                        metrics
                    )
                }\n"
            )

            append(
                "EMA 9: ${
                    "%.5f".format(
                        metrics.ema9
                    )
                }\n"
            )

            append(
                "EMA 21: ${
                    "%.5f".format(
                        metrics.ema21
                    )
                }\n"
            )

            append(
                "EMA 50: ${
                    "%.5f".format(
                        metrics.ema50
                    )
                }\n"
            )

            append(
                "RSI: ${
                    "%.1f".format(
                        metrics.rsi
                    )
                }\n"
            )

            append(
                "MACD: ${
                    "%.5f".format(
                        metrics.macd
                    )
                }\n"
            )

            append(
                "MACD SIGNAL: ${
                    "%.5f".format(
                        metrics.macdSignal
                    )
                }\n"
            )

            append(
                "ADX: ${
                    "%.1f".format(
                        metrics.adx
                    )
                }\n"
            )

            append(
                "ATR: ${
                    "%.5f".format(
                        metrics.atr
                    )
                }\n"
            )

            append(
                "SUPORTE: ${
                    "%.5f".format(
                        metrics.support
                    )
                }\n"
            )

            append(
                "RESISTÊNCIA: ${
                    "%.5f".format(
                        metrics.resistance
                    )
                }\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "PLANO OPERACIONAL\n\n"
            )

            if (
                direction ==
                    "NEUTRO" ||
                !entryPlan.valid
            ) {

                append(
                    "STATUS: AGUARDAR\n"
                )

                append(
                    "Motivo: ${
                        entryPlan.reason
                    }\n\n"
                )

            } else {

                append(
                    "STATUS: ENTRADA VÁLIDA\n"
                )

                append(
                    "TIMING: ${
                        entryPlan.timing
                    }\n"
                )

                append(
                    "ENTRADA: ${
                        "%.5f".format(
                            entryPlan.entry
                        )
                    }\n"
                )

                append(
                    "ZONA: ${
                        "%.5f".format(
                            entryPlan.zoneLow
                        )
                    } – ${
                        "%.5f".format(
                            entryPlan.zoneHigh
                        )
                    }\n"
                )

                append(
                    "STOP: ${
                        "%.5f".format(
                            entryPlan.stop
                        )
                    }\n"
                )

                append(
                    "TP1: ${
                        "%.5f".format(
                            entryPlan.tp1
                        )
                    }  R:R 1:${entryPlan.rr1}\n"
                )

                append(
                    "TP2: ${
                        "%.5f".format(
                            entryPlan.tp2
                        )
                    }  R:R 1:${entryPlan.rr2}\n"
                )

                append(
                    "TP3: ${
                        "%.5f".format(
                            entryPlan.tp3
                        )
                    }  R:R 1:${entryPlan.rr3}\n"
                )

                append(
                    "VALIDADE: ${
                        entryPlan.validityMinutes
                    } minutos\n"
                )

                append(
                    "EXPIRA EM: ${
                        formatExpiry(
                            entryPlan.expiresAt
                        )
                    }\n"
                )

                append(
                    "MOTIVO: ${
                        entryPlan.reason
                    }\n\n"
                )
            }

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "SEQUÊNCIA\n\n"
            )

            append(
                "ESTÁGIO: ${
                    sequence.stage
                }\n"
            )

            append(
                "CONFIRMADA: ${
                    if (
                        sequence.confirmed
                    ) {
                        "SIM"
                    } else {
                        "NÃO"
                    }
                }\n\n"
            )

            append(
                "━━━━━━━━━━━━━━━━━━━━\n"
            )

            append(
                "DADOS DE MERCADO\n\n"
            )

            append(
                "FONTE: TWELVE DATA\n"
            )

            append(
                "QUALIDADE: ${
                    realtime.market.dataQuality
                }\n"
            )

            append(
                "PREÇO: ${
                    "%.5f".format(
                        realtime.market.price
                    )
                }\n"
            )

            append(
                "CANDLES: ${
                    candles.values.sumOf {
                        it.size
                    }
                }\n"
            )

            append(
                "WEBSOCKET: TEMPO REAL\n"
            )

            append(
                "EXECUÇÃO DE ORDENS: DESATIVADA\n"
            )

            append(
                "\nATUALIZAÇÃO: CONTÍNUA"
            )
        }
    }

    /*
     * ============================================================
     * DIREÇÃO DOS INDICADORES
     * ============================================================
     */
    private fun metricDirection(
        metrics:
            QuantMetrics
    ): String {

        val bullish =
            listOf(

                metrics.trend >=
                    60.0,

                metrics.momentum >=
                    55.0,

                metrics.ema9 >
                    metrics.ema21,

                metrics.macd >
                    metrics.macdSignal,

                metrics.structure >=
                    55.0,

                metrics.candlePattern >=
                    55.0,

                metrics.breakout >=
                    55.0

            ).count {
                it
            }

        val bearish =
            listOf(

                metrics.trend <=
                    40.0,

                metrics.momentum <=
                    45.0,

                metrics.ema9 <
                    metrics.ema21,

                metrics.macd <
                    metrics.macdSignal,

                metrics.structure <=
                    45.0,

                metrics.candlePattern <=
                    45.0,

                metrics.breakout <=
                    45.0

            ).count {
                it
            }

        return when {

            bullish >=
                bearish + 2 ->

                "MAIOR COMPRA"

            bearish >=
                bullish + 2 ->

                "MAIOR VENDA"

            else ->

                "NEUTRO / EQUILIBRADO"
        }
    }

    /*
     * ============================================================
     * EXPIRAÇÃO
     * ============================================================
     */
    private fun formatExpiry(
        timestamp: Long
    ): String {

        if (
            timestamp <= 0L
        ) {
            return "--"
        }

        val formatter =
            java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            )

        return formatter.format(
            java.util.Date(
                timestamp
            )
        )
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            refreshTask
        )

        quoteClient.disconnect()

        super.onDestroy()
    }
}

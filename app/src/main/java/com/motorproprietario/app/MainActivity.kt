package com.motorproprietario.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var assetView: TextView
    private lateinit var priceView: TextView
    private lateinit var resultView: TextView
    private lateinit var probabilityView: TextView
    private lateinit var bestTimeframeView: TextView
    private lateinit var operationView: TextView
    private lateinit var entryView: TextView
    private lateinit var stopView: TextView
    private lateinit var targetsView: TextView
    private lateinit var timingView: TextView
    private lateinit var analysisButton: Button
    
private lateinit var dopmDashboardController:
    DopmDashboardController
    
    private lateinit var assetSpinner: Spinner
    private lateinit var timeframeSpinner: Spinner
    private lateinit var horizonSpinner: Spinner

    private val handler =
        Handler(Looper.getMainLooper())

    private val quoteClient =
        TwelveDataClient()

    private val candleClient =
        TwelveDataCandleClient()

    private val calibrationRuntime by lazy {
        CalibrationRuntime(
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
            "D1" to 86_400_000L
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

    private fun dp(
        value: Int
    ): Int =
        (
            value *
                resources.displayMetrics.density
        ).toInt()

    private fun text(
        value: String,
        size: Float = 16f,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {

            text = value
            textSize = size
            setTextColor(Color.WHITE)

            if (bold) {
                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
            )
        }
    }

    private fun sectionTitle(
        value: String
    ): TextView {

        return text(
            value,
            14f,
            true
        ).apply {

            setTextColor(
                Color.rgb(
                    190,
                    190,
                    190
                )
            )

            setPadding(
                dp(8),
                dp(12),
                dp(8),
                dp(4)
            )
        }
    }

    private fun divider(): View {

        return View(this).apply {

            setBackgroundColor(
                Color.rgb(
                    70,
                    70,
                    70
                )
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {

                    topMargin =
                        dp(6)

                    bottomMargin =
                        dp(6)
                }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        dopmDashboardController =
    DopmDashboardController(this)

dopmDashboardController.install()

connectRealtime()

handler.post(
    refreshTask
)
    }

    private fun buildInterface() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(18),
                    dp(16),
                    dp(24)
                )

                setBackgroundColor(
                    Color.rgb(
                        18,
                        15,
                        22
                    )
                )
            }

        val title =
            text(
                "MOTOR PROPRIETÁRIO",
                25f,
                true
            )

        title.gravity =
            Gravity.CENTER_HORIZONTAL

        root.addView(title)

        val subtitle =
            text(
                "ANÁLISE DE MERCADO EM TEMPO REAL",
                13f,
                false
            )

        subtitle.gravity =
            Gravity.CENTER_HORIZONTAL

        root.addView(
            subtitle
        )

        root.addView(
            divider()
        )

        root.addView(
            sectionTitle(
                "ATIVO"
            )
        )

        assetSpinner =
            Spinner(this)

        val assets =
            listOf(
                "EUR/USD",
                "GBP/USD",
                "USD/JPY",
                "USD/CHF",
                "AUD/USD",
                "USD/CAD",
                "NZD/USD",
                "EUR/GBP",
                "EUR/JPY",
                "GBP/JPY"
            )

        assetSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                assets
            )

        assetSpinner.setSelection(
            assets.indexOf(
                selectedAsset
            ).coerceAtLeast(0)
        )

        assetSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    val newAsset =
                        assets[position]

                    if (
                        newAsset ==
                            selectedAsset
                    ) {
                        return
                    }

                    selectedAsset =
                        newAsset

                    resetForNewAsset()

                    connectRealtime()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        root.addView(
            assetSpinner
        )

        root.addView(
            sectionTitle(
                "TIMEFRAME ESCOLHIDO"
            )
        )

        timeframeSpinner =
            Spinner(this)

        val timeframes =
            listOf(
                "M1",
                "M5",
                "M15",
                "M30",
                "H1",
                "H4",
                "D1"
            )

        timeframeSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                timeframes
            )

        timeframeSpinner.setSelection(
            timeframes.indexOf(
                selectedTimeframe
            ).coerceAtLeast(0)
        )

        timeframeSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    selectedTimeframe =
                        timeframes[position]

                    analyzeMarket()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        root.addView(
            timeframeSpinner
        )

        root.addView(
            sectionTitle(
                "VISÃO TEMPORAL"
            )
        )

        horizonSpinner =
            Spinner(this)

        val horizons =
            listOf(
                "GERAL",
                "DIA",
                "SEMANA",
                "MÊS"
            )

        horizonSpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                horizons
            )

        horizonSpinner.setSelection(0)

        horizonSpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {

                    selectedHorizon =
                        horizons[position]

                    analyzeMarket()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        root.addView(
            horizonSpinner
        )

        root.addView(
            divider()
        )

        root.addView(
            sectionTitle(
                "MERCADO"
            )
        )

        assetView =
            text(
                selectedAsset,
                22f,
                true
            )

        root.addView(
            assetView
        )

        priceView =
            text(
                "Preço: aguardando..."
            )

        root.addView(
            priceView
        )

        statusView =
            text(
                "CONECTANDO..."
            )

        root.addView(
            statusView
        )

        root.addView(
            divider()
        )

        root.addView(
            sectionTitle(
                "MELHOR TIMEFRAME DO MOTOR"
            )
        )

        bestTimeframeView =
            text(
                "Aguardando análise...",
                20f,
                true
            )

        root.addView(
            bestTimeframeView
        )

        root.addView(
            divider()
        )

        root.addView(
            sectionTitle(
                "RESULTADO FINAL"
            )
        )

        resultView =
            text(
                "⚪ AGUARDAR",
                28f,
                true
            ).apply {

                gravity =
                    Gravity.CENTER

                setPadding(
                    dp(8),
                    dp(18),
                    dp(8),
                    dp(18)
                )
            }

        root.addView(
            resultView
        )

        probabilityView =
            text(
                "COMPRA ---%   VENDA ---%   NEUTRO ---%",
                15f,
                true
            ).apply {

                gravity =
                    Gravity.CENTER
            }

        root.addView(
            probabilityView
        )

        operationView =
            text(
                "AGUARDANDO DADOS...",
                18f,
                true
            )

        root.addView(
            operationView
        )

        root.addView(
            divider()
        )

        root.addView(
            sectionTitle(
                "PLANO OPERACIONAL"
            )
        )

        entryView =
            text(
                "Entrada: --"
            )

        root.addView(
            entryView
        )

        stopView =
            text(
                "Stop: --"
            )

        root.addView(
            stopView
        )

        targetsView =
            text(
                "TP1: --\nTP2: --\nTP3: --\nR:R: --"
            )

        root.addView(
            targetsView
        )

        timingView =
            text(
                "Timing: --\nValidade: --"
            )

        root.addView(
            timingView
        )

        root.addView(
            divider()
        )

        analysisButton =
            Button(this).apply {

                text =
                    "🔎 ANÁLISE DETALHADA"

                setOnClickListener {
                    showDetailedAnalysis()
                }
            }

        root.addView(
            analysisButton
        )

        val scroll =
            ScrollView(this).apply {

                addView(root)
            }

        setContentView(
            scroll
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

        assetView.text =
            selectedAsset

        priceView.text =
            "Preço: aguardando..."

        bestTimeframeView.text =
            "Aguardando análise..."

        resultView.text =
            "⚪ AGUARDAR"

        probabilityView.text =
            "COMPRA ---%   VENDA ---%   NEUTRO ---%"

        operationView.text =
            "CARREGANDO $selectedAsset..."

        entryView.text =
            "Entrada: --"

        stopView.text =
            "Stop: --"

        targetsView.text =
            "TP1: --\nTP2: --\nTP3: --\nR:R: --"

        timingView.text =
            "Timing: --\nValidade: --"

        statusView.text =
            "CONECTANDO..."
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

                    assetView.text =
                        quote.symbol

                    priceView.text =
                        "Preço: ${
                            String.format(
                                "%.5f",
                                quote.price
                            )
                        }"

                    statusView.text =
                        if (
                            System.currentTimeMillis() <
                                candleApiBackoffUntil
                        ) {

                            "● ONLINE • TWELVE DATA • CANDLES EM CACHE"

                        } else {

                            "● ONLINE • TWELVE DATA • TEMPO REAL"
                        }
                }
            },

            onError = { error ->

                runOnUiThread {

                    statusView.text =
                        "ERRO DE CONEXÃO\n" +
                        (
                            error.message
                                ?: "Erro desconhecido"
                        )
                }
            }
        )
    }

    private fun analyzeMarket() {

        if (analyzing) {
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

                        operationView.text =
                            "AGUARDANDO CANDLES REAIS..."
                    }

                    return@thread
                }

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

                val selectedMetrics =
                    realtime.metrics[
                        selectedTimeframe
                    ]
                        ?: realtime.metrics[
                            "M15"
                        ]
                        ?: realtime.metrics.values.first()

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

                val deterministic =
                    DeterministicEngine.calculate(

                        DeterministicInput(

                            metrics =
                                selectedMetrics,

                            mtfConfluence =
                                realtime.mtfConfluence,

                            falseSignalRisk =
                                realtime.fsi,

                            currentPrice =
                                quote.price,

                            higherTimeframes =
                                higherMetrics
                        )
                    )

                val probability =
                    ProbabilityEngine.calculate(

                        ProbabilityInput(

                            metrics =
                                selectedMetrics,

                            mtfConfluence =
                                realtime.mtfConfluence,

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

                val finalProbabilities =
                    combineFinalProbabilities(

                        probability,

                        deterministic,

                        realtime.fsi,

                        realtime.mtfConfluence
                    )

                val bestTimeframe =
                    findBestTimeframe(
                        realtime,
                        candles
                    )

                val bestMetrics =
                    realtime.metrics[
                        bestTimeframe
                    ]
                        ?: selectedMetrics

                val bestDeterministic =
                    DeterministicEngine.calculate(

                        DeterministicInput(

                            metrics =
                                bestMetrics,

                            mtfConfluence =
                                realtime.mtfConfluence,

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

                val bestProbability =
                    ProbabilityEngine.calculate(

                        ProbabilityInput(

                            metrics =
                                bestMetrics,

                            mtfConfluence =
                                realtime.mtfConfluence,

                            falseSignalRisk =
                                realtime.fsi,

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

                        realtime.fsi,

                        realtime.mtfConfluence
                    )

                val bestDirection =
                    finalDirection(
                        bestFinal
                    )

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
                                realtime.fsi,

                            now =
                                now
                        )
                    )

                val sequence =
                    SequenceEngine.advance(

                        sequenceStage,

                        SequenceInput(

                            signalDetected =
                                bestDirection !=
                                    "NEUTRO",

                            confirmation =
                                realtime.mtfConfluence >=
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
                                realtime.fsi >=
                                    80.0 ||
                                realtime.market.dataQuality !=
                                    "GOOD"
                        )
                    )

                sequenceStage =
                    sequence.stage

                val detailed =
                    buildDetailedAnalysis(

                        realtime =
                            realtime,

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

                        finalProbabilities =
                            bestFinal,

                        direction =
                            bestDirection,

                        bestTimeframe =
                            bestTimeframe,

                        entryPlan =
                            entryPlan
                    )
                }

            } catch (
                error: Exception
            ) {

                runOnUiThread {

                    operationView.text =
                        "ERRO NA ANÁLISE\n\n" +
                        (
                            error.message
                                ?: "Erro desconhecido"
                        )
                }

            } finally {

                analyzing =
                    false
            }
        }
    }

    private fun updateMainScreen(
        realtime:
            RealtimeAnalysis,

        finalProbabilities:
            Triple<Double, Double, Double>,

        direction:
            String,

        bestTimeframe:
            String,

        entryPlan:
            EntryPlanResult
    ) {

        val buy =
            finalProbabilities.first

        val sell =
            finalProbabilities.second

        val neutral =
            finalProbabilities.third

        val total =
            when (direction) {

                "COMPRA" ->
                    buy

                "VENDA" ->
                    sell

                else ->
                    neutral
            }

                    dopmDashboardController.updateConnection(
            online = true,
            api = "TWELVE DATA"
        )

        dopmDashboardController.updateMarket(
            price = realtime.market.price,
            asset = selectedAsset
        )

        dopmDashboardController.updateDecision(
            direction = direction,
            buy = buy,
            sell = sell,
            neutral = neutral
        )

        dopmDashboardController.updateMathematics(
            probability =
                maxOf(buy, sell),
            deterministic =
                realtime.metrics[
                    bestTimeframe
                ]?.let {
                    DeterministicEngine.calculate(
                        DeterministicInput(
                            metrics = it,
                            mtfConfluence =
                                realtime.mtfConfluence,
                            falseSignalRisk =
                                realtime.fsi,
                            currentPrice =
                                realtime.market.price,
                            higherTimeframes =
                                emptyList()
                        )
                    ).confidence
                } ?: 0.0,
            mtf =
                realtime.mtfConfluence
        )

        dopmDashboardController.updateBestTimeframe(
            bestTimeframe
        )

        dopmDashboardController.updateTradePlan(
            entry = entryPlan.entry,
            stop = entryPlan.stop,
            tp1 = entryPlan.tp1,
            tp2 = entryPlan.tp2,
            tp3 = entryPlan.tp3
        )

        dopmDashboardController.updateTiming(
            timing = entryPlan.timing,
            validity =
                "${entryPlan.validityMinutes} min"
        )

        bestTimeframeView.text =
            "⭐ $bestTimeframe"

        probabilityView.text =
            "COMPRA ${
                "%.1f".format(
                    buy
                )
            }%   •   VENDA ${
                "%.1f".format(
                    sell
                )
            }%   •   NEUTRO ${
                "%.1f".format(
                    neutral
                )
            }%"

        resultView.text =
            when (direction) {

                "COMPRA" ->
                    "🟢 COMPRA\n${
                        "%.1f".format(
                            total
                        )
                    }%"

                "VENDA" ->
                    "🔴 VENDA\n${
                        "%.1f".format(
                            total
                        )
                    }%"

                else ->
                    "⚪ AGUARDAR\n${
                        "%.1f".format(
                            total
                        )
                    }%"
            }

        if (
            direction ==
                "NEUTRO" ||
            !entryPlan.valid
        ) {

            operationView.text =
                "AGUARDAR — SEM DIREÇÃO OPERACIONAL"

            entryView.text =
                "Entrada: não disponível"

            stopView.text =
                "Stop: não disponível"

            targetsView.text =
                "TP1: não disponível\n" +
                "TP2: não disponível\n" +
                "TP3: não disponível\n" +
                "R:R: não disponível"

            timingView.text =
                "Timing: aguardar confirmação\n" +
                "Validade: --"

        } else {

            operationView.text =
                if (
                    direction ==
                        "COMPRA"
                ) {
                    "🟢 ENTRADA DE COMPRA"
                } else {
                    "🔴 ENTRADA DE VENDA"
                }

            entryView.text =
                "Entrada: ${
                    "%.5f".format(
                        entryPlan.entry
                    )
                }\n" +
                "Zona: ${
                    "%.5f".format(
                        entryPlan.zoneLow
                    )
                } – ${
                    "%.5f".format(
                        entryPlan.zoneHigh
                    )
                }"

            stopView.text =
                "Stop: ${
                    "%.5f".format(
                        entryPlan.stop
                    )
                }"

            targetsView.text =
                "TP1: ${
                    "%.5f".format(
                        entryPlan.tp1
                    )
                }   R:R 1:${entryPlan.rr1}\n" +
                "TP2: ${
                    "%.5f".format(
                        entryPlan.tp2
                    )
                }   R:R 1:${entryPlan.rr2}\n" +
                "TP3: ${
                    "%.5f".format(
                        entryPlan.tp3
                    )
                }   R:R 1:${entryPlan.rr3}"

            timingView.text =
                "Timing: ${
                    entryPlan.timing
                }\n" +
                "Validade: ${
                    entryPlan.validityMinutes
                } minutos\n" +
                "Expira: ${
                    formatExpiry(
                        entryPlan.expiresAt
                    )
                }"
        }

        statusView.text =
            if (
                System.currentTimeMillis() <
                    candleApiBackoffUntil
            ) {

                "● ONLINE • CANDLES EM CACHE"

            } else {

                "● ONLINE • DADOS REAIS • WEBSOCKET ATIVO"
            }
    }

    private fun showDetailedAnalysis() {

        val content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(8),
                    dp(16),
                    dp(8)
                )

                setBackgroundColor(
                    Color.rgb(
                        18,
                        15,
                        22
                    )
                )
            }

        val detailedText =
            TextView(this).apply {

                text =
                    latestDetailedAnalysis

                textSize =
                    14f

                setTextColor(
                    Color.WHITE
                )

                setPadding(
                    dp(4),
                    dp(4),
                    dp(4),
                    dp(20)
                )
            }

        content.addView(
            detailedText
        )

        val scroll =
            ScrollView(this).apply {

                addView(
                    content
                )
            }

        val dialog =
            AlertDialog.Builder(
                this
            )
                .setTitle(
                    "ANÁLISE DETALHADA"
                )
                .setView(
                    scroll
                )
                .setPositiveButton(
                    "FECHAR",
                    null
                )
                .create()

        dialog.show()
    }

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
                "D1"
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
                            200
                    )

                if (
                    fresh.isNotEmpty()
                ) {

                    synchronized(
                        candleCache
                    ) {

                        candleCache[
                            timeframe
                        ] =
                            fresh
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

                        statusView.text =
                            "● ONLINE • API EM LIMITE TEMPORÁRIO • USANDO CACHE"
                    }

                    break
                }
            }
        }
    }

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

        /*
         * Os pesos vêm do CalibrationRuntime.
         *
         * Sem calibração aceita:
         * 50% probabilidade
         * 50% determinismo
         *
         * Com calibração aceita:
         * utiliza os pesos persistidos.
         */
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

    private fun findBestTimeframe(
        realtime:
            RealtimeAnalysis,

        candles:
            Map<String, List<MarketCandle>>
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
                            realtime.mtfConfluence,

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
                            realtime.mtfConfluence,

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

                    realtime.mtfConfluence
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

    private fun timeframeRank(
        timeframe: String
    ): Int =
        when (
            timeframe
        ) {

            "M1" ->
                1

            "M5" ->
                2

            "M15" ->
                3

            "M30" ->
                4

            "H1" ->
                5

            "H4" ->
                6

            "D1" ->
                7

            else ->
                0
        }

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

    private fun buildDetailedAnalysis(
        realtime:
            RealtimeAnalysis,

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
                "FSI: ${
                    "%.1f".format(
                        realtime.fsi
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

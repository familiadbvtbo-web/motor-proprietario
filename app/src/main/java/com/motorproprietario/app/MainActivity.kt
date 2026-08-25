package com.motorproprietario.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
    private lateinit var analysisView: TextView

    private lateinit var assetSpinner: Spinner
    private lateinit var timeframeSpinner: Spinner
    private lateinit var horizonSpinner: Spinner

    private val handler =
        Handler(Looper.getMainLooper())

    private val quoteClient =
        TwelveDataClient()

    private val candleClient =
        TwelveDataCandleClient()

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

    private val candleCache =
        LinkedHashMap<String, List<MarketCandle>>()

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

    private var sequenceStage =
        SequenceStage.S0

    private val refreshTask =
        object : Runnable {

            override fun run() {

                analyzeMarket()

                handler.postDelayed(
                    this,
                    5000L
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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        buildInterface()

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
                    dp(20),
                    dp(25),
                    dp(20),
                    dp(20)
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

        val subtitle =
            text(
                "PROBABILIDADE • PROVISIONAMENTO • SINAIS FALSOS",
                14f,
                true
            )

        subtitle.gravity =
            Gravity.CENTER_HORIZONTAL

        statusView =
            text(
                "CONECTANDO..."
            )

        /*
         * -------------------------
         * ATIVO
         * -------------------------
         */

        root.addView(
            text(
                "ATIVO",
                16f,
                true
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

        /*
         * -------------------------
         * TIMEFRAME
         * -------------------------
         */

        root.addView(
            text(
                "TIMEFRAME INDIVIDUAL",
                16f,
                true
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

        /*
         * -------------------------
         * HORIZONTE
         * -------------------------
         */

        root.addView(
            text(
                "VISÃO DO MOTOR",
                16f,
                true
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

        horizonSpinner.setSelection(
            0
        )

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

        /*
         * -------------------------
         * STATUS
         * -------------------------
         */

        root.addView(
            statusView
        )

        /*
         * -------------------------
         * ATIVO / PREÇO
         * -------------------------
         */

        root.addView(
            text(
                "ATIVO ATUAL",
                17f,
                true
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

        root.addView(
            text(
                "PREÇO REAL",
                17f,
                true
            )
        )

        priceView =
            text(
                "Aguardando..."
            )

        root.addView(
            priceView
        )

        /*
         * -------------------------
         * ANÁLISE
         * -------------------------
         */

        root.addView(
            text(
                "ANÁLISE DO MOTOR",
                20f,
                true
            )
        )

        analysisView =
            text(
                "Aguardando dados..."
            )

        root.addView(
            analysisView
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

        analysisView.text =
            "CARREGANDO $selectedAsset..."

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

                    statusView.text =
                        if (
                            System.currentTimeMillis() <
                                candleApiBackoffUntil
                        ) {

                            "● ONLINE\n" +
                            "FONTE: TWELVE DATA\n" +
                            "WEBSOCKET: ATIVO\n" +
                            "CANDLES: CACHE\n" +
                            "EXECUÇÃO: DESATIVADA"

                        } else {

                            "● ONLINE\n" +
                            "FONTE: TWELVE DATA\n" +
                            "DADOS: REAIS\n" +
                            "WEBSOCKET: ATIVO\n" +
                            "EXECUÇÃO: DESATIVADA"
                        }

                    assetView.text =
                        quote.symbol

                    priceView.text =
                        String.format(
                            "%.5f",
                            quote.price
                        )
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

                val apiBlocked =
                    now <
                        candleApiBackoffUntil

                if (!apiBlocked) {

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

                        val lastUpdate =
                            lastCandleUpdate[
                                timeframe
                            ]
                                ?: 0L

                        val hasCache =
                            candleCache[
                                timeframe
                            ] != null

                        val shouldUpdate =
                            !hasCache ||
                            now -
                                lastUpdate >=
                            interval

                        if (
                            !shouldUpdate
                        ) {
                            continue
                        }

                        try {

                            val freshCandles =
                                candleClient.getCandles(
                                    symbol =
                                        selectedAsset,

                                    timeframe =
                                        timeframe,

                                    outputSize =
                                        200
                                )

                            if (
                                freshCandles.isNotEmpty()
                            ) {

                                candleCache[
                                    timeframe
                                ] =
                                    freshCandles

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
                                        "● ONLINE\n" +
                                        "FONTE: TWELVE DATA\n" +
                                        "WEBSOCKET: ATIVO\n" +
                                        "CANDLES: CACHE\n" +
                                        "API: LIMITE TEMPORÁRIO"
                                }

                                break
                            }
                        }
                    }
                }

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

                        analysisView.text =
                            "AGUARDANDO CANDLES REAIS..."
                    }

                    return@thread
                }

                /*
                 * MOTOR GERAL
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
                 * MÉTRICA DO TIMEFRAME
                 * ESCOLHIDO PELO USUÁRIO.
                 */
                val selectedMetrics =
                    realtime.metrics[
                        selectedTimeframe
                    ]
                        ?: realtime.metrics[
                            "M15"
                        ]
                        ?: realtime.metrics.values.first()

                /*
                 * Sequência do Motor.
                 */
                val signalDetected =
                    realtime.direction !=
                        "NEUTRO"

                val confirmation =
                    realtime.mtfConfluence >=
                        60.0

                val continuation =
                    when (
                        realtime.direction
                    ) {

                        "COMPRA" ->
                            selectedMetrics.ema9 >
                                selectedMetrics.ema21 &&
                            selectedMetrics.macd >
                                selectedMetrics.macdSignal

                        "VENDA" ->
                            selectedMetrics.ema9 <
                                selectedMetrics.ema21 &&
                            selectedMetrics.macd <
                                selectedMetrics.macdSignal

                        else ->
                            false
                    }

                val invalidated =
                    realtime.fsi >=
                        70.0 ||
                        realtime.market.dataQuality !=
                        "GOOD"

                val sequenceInput =
                    SequenceInput(
                        signalDetected =
                            signalDetected,

                        confirmation =
                            confirmation,

                        continuation =
                            continuation,

                        invalidated =
                            invalidated
                    )

                val sequence =
                    SequenceEngine.advance(
                        sequenceStage,
                        sequenceInput
                    )

                sequenceStage =
                    sequence.stage

                /*
                 * Falso sinal individual.
                 */
                val falseSignalInput =
                    FalseSignalInput(
                        structureContradiction =
                            abs(
                                selectedMetrics.structure -
                                    realtime.mtfConfluence
                            ),

                        momentumDivergence =
                            abs(
                                selectedMetrics.momentum -
                                    selectedMetrics.rsi
                            ),

                        volumeMismatch =
                            abs(
                                selectedMetrics.volume -
                                    selectedMetrics.momentum
                            ),

                        confirmationFailure =
                            100.0 -
                                realtime.mtfConfluence,

                        timeframeConflict =
                            100.0 -
                                realtime.mtfConfluence
                    )

                val finalInput =
                    FinalMotorInput(
                        market =
                            realtime.market,

                        now =
                            now,

                        sequence =
                            sequenceInput,

                        sequenceStage =
                            sequence.stage,

                        falseSignal =
                            falseSignalInput
                    )

                val finalResult =
                    FinalMotorEngine.evaluate(
                        finalInput
                    )

                runOnUiThread {

                    analysisView.text =
                        buildAnalysisText(
                            realtime =
                                realtime,

                            result =
                                finalResult,

                            candles =
                                candles,

                            selectedMetrics =
                                selectedMetrics
                        )
                }

            } catch (
                error: Exception
            ) {

                runOnUiThread {

                    analysisView.text =
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

    private fun buildAnalysisText(
        realtime:
            RealtimeAnalysis,

        result:
            FinalMotorResult,

        candles:
            Map<String, List<MarketCandle>>,

        selectedMetrics:
            QuantMetrics
    ): String {

        val output =
            StringBuilder()

        /*
         * ==================================
         * CABEÇALHO
         * ==================================
         */

        output.append(
            "━━━━━━━━━━━━━━━━━━━━\n"
        )

        output.append(
            "MOTOR PROPRIETÁRIO\n"
        )

        output.append(
            "ATIVO: $selectedAsset\n"
        )

        output.append(
            "TIMEFRAME: $selectedTimeframe\n"
        )

        output.append(
            "VISÃO: $selectedHorizon\n"
        )

        output.append(
            "━━━━━━━━━━━━━━━━━━━━\n\n"
        )

        /*
         * ==================================
         * RESULTADO PRINCIPAL
         * ==================================
         */

        output.append(
            "RESULTADO DO MOTOR\n\n"
        )

        output.append(
            "DIREÇÃO GERAL: ${
                realtime.direction
            }\n"
        )

        output.append(
            "DECISÃO: ${
                result.decision.decision
            }\n"
        )

        output.append(
            "MOTIVO: ${
                result.decision.reason
            }\n\n"
        )

        /*
         * ==================================
         * PROBABILIDADE
         * ==================================
         */

        val probability =
            when {

                realtime.direction ==
                    "COMPRA" ->
                    realtime.score

                realtime.direction ==
                    "VENDA" ->
                    100.0 -
                        realtime.score

                else ->
                    50.0
            }

        val neutralProbability =
            100.0 -
                probability

        output.append(
            "PROBABILIDADE DIRECIONAL\n\n"
        )

        output.append(
            "COMPRA: ${
                probability
                    .coerceIn(
                        0.0,
                        100.0
                    )
                    .let {
                        if (
                            realtime.direction ==
                                "COMPRA"
                        ) {
                            "%.1f".format(it)
                        } else {
                            "—"
                        }
                    }
            }%\n"
        )

        output.append(
            "VENDA: ${
                if (
                    realtime.direction ==
                        "VENDA"
                ) {
                    "%.1f".format(
                        probability
                    )
                } else {
                    "—"
                }
            }%\n"
        )

        output.append(
            "NEUTRO/INCERTEZA: ${
                "%.1f".format(
                    neutralProbability
                        .coerceIn(
                            0.0,
                            100.0
                        )
                )
            }%\n\n"
        )

        /*
         * ==================================
         * PROVISIONAMENTO
         * ==================================
         */

        val provisioning =
            when {

                realtime.fsi >=
                    70.0 ->
                    "MUITO ALTO — EVITAR SINAL"

                realtime.fsi >=
                    50.0 ->
                    "ALTO — AGUARDAR CONFIRMAÇÃO"

                realtime.fsi >=
                    35.0 ->
                    "MODERADO — PROTEGER"

                else ->
                    "BAIXO — SINAL MAIS CONSISTENTE"
            }

        output.append(
            "PROVISIONAMENTO\n\n"
        )

        output.append(
            "FSI: ${
                "%.1f".format(
                    realtime.fsi
                )
            }\n"
        )

        output.append(
            "FALSO SINAL: ${
                "%.1f".format(
                    realtime.falseSignal
                )
            }\n"
        )

        output.append(
            "RISCO: $provisioning\n"
        )

        output.append(
            "CONFLUÊNCIA MTF: ${
                "%.1f".format(
                    realtime.mtfConfluence
                )
            }%\n"
        )

        output.append(
            "CONFIANÇA: ${
                "%.1f".format(
                    realtime.confidence
                )
            }%\n\n"
        )

        /*
         * ==================================
         * TIMEFRAME ESCOLHIDO
         * ==================================
         */

        output.append(
            "ANÁLISE INDIVIDUAL — $selectedTimeframe\n\n"
        )

        output.append(
            "TENDÊNCIA: ${
                metricDirection(
                    selectedMetrics
                )
            }\n"
        )

        output.append(
            "EMA 9: ${
                "%.5f".format(
                    selectedMetrics.ema9
                )
            }\n"
        )

        output.append(
            "EMA 21: ${
                "%.5f".format(
                    selectedMetrics.ema21
                )
            }\n"
        )

        output.append(
            "EMA 50: ${
                "%.5f".format(
                    selectedMetrics.ema50
                )
            }\n"
        )

        output.append(
            "RSI: ${
                "%.1f".format(
                    selectedMetrics.rsi
                )
            }\n"
        )

        output.append(
            "MACD: ${
                "%.5f".format(
                    selectedMetrics.macd
                )
            }\n"
        )

        output.append(
            "MACD SIGNAL: ${
                "%.5f".format(
                    selectedMetrics.macdSignal
                )
            }\n"
        )

        output.append(
            "ADX: ${
                "%.1f".format(
                    selectedMetrics.adx
                )
            }\n"
        )

        output.append(
            "ATR: ${
                "%.5f".format(
                    selectedMetrics.atr
                )
            }\n"
        )

        output.append(
            "ESTRUTURA: ${
                "%.1f".format(
                    selectedMetrics.structure
                )
            }\n"
        )

        output.append(
            "MOMENTUM: ${
                "%.1f".format(
                    selectedMetrics.momentum
                )
            }\n"
        )

        output.append(
            "VOLUME: ${
                "%.1f".format(
                    selectedMetrics.volume
                )
            }\n"
        )

        output.append(
            "VOLATILIDADE: ${
                "%.1f".format(
                    selectedMetrics.volatility
                )
            }\n"
        )

        output.append(
            "BREAKOUT: ${
                "%.1f".format(
                    selectedMetrics.breakout
                )
            }\n"
        )

        output.append(
            "PADRÃO DE CANDLE: ${
                "%.1f".format(
                    selectedMetrics.candlePattern
                )
            }\n"
        )

        output.append(
            "DIVERGÊNCIA: ${
                "%.1f".format(
                    selectedMetrics.divergence
                )
            }\n"
        )

        output.append(
            "SUPORTE: ${
                "%.5f".format(
                    selectedMetrics.support
                )
            }\n"
        )

        output.append(
            "RESISTÊNCIA: ${
                "%.5f".format(
                    selectedMetrics.resistance
                )
            }\n\n"
        )

        /*
         * ==================================
         * ANÁLISE GERAL
         * ==================================
         */

        output.append(
            "ANÁLISE GERAL MULTI-TIMEFRAME\n\n"
        )

        output.append(
            "REGIME: ${
                realtime.regime
            }\n"
        )

        output.append(
            "SCORE: ${
                "%.1f".format(
                    realtime.score
                )
            }\n"
        )

        output.append(
            "MTF: ${
                "%.1f".format(
                    realtime.mtfConfluence
                )
            }%\n\n"
        )

        /*
         * ==================================
         * DIA / SEMANA / MÊS
         * ==================================
         */

        output.append(
            "VISÃO TEMPORAL\n\n"
        )

        output.append(
            buildHorizonSummary(
                candles,
                "DIA"
            )
        )

        output.append(
            buildHorizonSummary(
                candles,
                "SEMANA"
            )
        )

        output.append(
            buildHorizonSummary(
                candles,
                "MÊS"
            )
        )

        /*
         * ==================================
         * SEQUÊNCIA
         * ==================================
         */

        output.append(
            "\nSEQUÊNCIA DO MOTOR\n\n"
        )

        output.append(
            "ESTÁGIO: ${
                result.sequence.stage
            }\n"
        )

        output.append(
            "CONFIRMADA: ${
                if (
                    result.sequence.confirmed
                ) {
                    "SIM"
                } else {
                    "NÃO"
                }
            }\n\n"
        )

        /*
         * ==================================
         * DADOS
         * ==================================
         */

        output.append(
            "DADOS UTILIZADOS\n\n"
        )

        output.append(
            "FONTE: TWELVE DATA\n"
        )

        output.append(
            "QUALIDADE: ${
                realtime.market.dataQuality
            }\n"
        )

        output.append(
            "PREÇO: ${
                "%.5f".format(
                    realtime.market.price
                )
            }\n"
        )

        output.append(
            "WEBSOCKET: TEMPO REAL\n"
        )

        output.append(
            "EXECUÇÃO DE ORDENS: DESATIVADA\n"
        )

        output.append(
            "\nATUALIZAÇÃO: CONTÍNUA"
        )

        return output.toString()
    }

    private fun metricDirection(
        metrics:
            QuantMetrics
    ): String {

        val bullish =
            listOf(
                metrics.trend >= 60.0,
                metrics.momentum >= 55.0,
                metrics.ema9 >
                    metrics.ema21,
                metrics.macd >
                    metrics.macdSignal,
                metrics.structure >= 55.0,
                metrics.candlePattern >= 55.0,
                metrics.breakout >= 55.0
            ).count {
                it
            }

        val bearish =
            listOf(
                metrics.trend <= 40.0,
                metrics.momentum <= 45.0,
                metrics.ema9 <
                    metrics.ema21,
                metrics.macd <
                    metrics.macdSignal,
                metrics.structure <= 45.0,
                metrics.candlePattern <= 45.0,
                metrics.breakout <= 45.0
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

    private fun buildHorizonSummary(
        candles:
            Map<String, List<MarketCandle>>,

        horizon:
            String
    ): String {

        val daily =
            candles["D1"]
                ?: emptyList()

        if (
            daily.isEmpty()
        ) {

            return "$horizon: SEM DADOS D1\n"
        }

        val amount =
            when (horizon) {

                "DIA" ->
                    1

                "SEMANA" ->
                    5

                "MÊS" ->
                    20

                else ->
                    20
            }

        val window =
            daily.takeLast(
                min(
                    amount,
                    daily.size
                )
            )

        if (
            window.isEmpty()
        ) {

            return "$horizon: SEM DADOS\n"
        }

        val first =
            window.first().open

        val last =
            window.last().close

        val high =
            window.maxOf {
                it.high
            }

        val low =
            window.minOf {
                it.low
            }

        val change =
            if (
                first != 0.0
            ) {

                (
                    last -
                        first
                ) /
                    first *
                    100.0

            } else {
                0.0
            }

        val direction =
            when {

                change > 0.20 ->
                    "MAIOR COMPRA"

                change < -0.20 ->
                    "MAIOR VENDA"

                else ->
                    "NEUTRO"
            }

        return buildString {

            append(
                "$horizon: $direction\n"
            )

            append(
                "Variação: ${
                    "%.2f".format(
                        change
                    )
                }%\n"
            )

            append(
                "Máxima: ${
                    "%.5f".format(
                        high
                    )
                }\n"
            )

            append(
                "Mínima: ${
                    "%.5f".format(
                        low
                    )
                }\n"
            )
        }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            refreshTask
        )

        quoteClient.disconnect()

        super.onDestroy()
    }
}

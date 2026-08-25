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

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var assetView: TextView
    private lateinit var priceView: TextView
    private lateinit var analysisView: TextView

    private val handler =
        Handler(Looper.getMainLooper())

    private val quoteClient =
        TwelveDataClient()

    private val candleClient =
        TwelveDataCandleClient()

    private var selectedAsset =
        "EUR/USD"

    private var lastQuote:
        RealTimeQuote? = null

    private var analyzing = false
   
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
                    dp(30),
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
                "DADOS REAIS • ANÁLISE EM TEMPO REAL",
                16f,
                true
            )

        subtitle.gravity =
            Gravity.CENTER_HORIZONTAL

        statusView =
            text(
                "CONECTANDO..."
            )

        val assetTitle =
            text(
                "ATIVO",
                18f,
                true
            )

        assetView =
            text(
                selectedAsset,
                22f,
                true
            )

        val priceTitle =
            text(
                "PREÇO REAL",
                18f,
                true
            )

        priceView =
            text(
                "Aguardando..."
            )

        val analysisTitle =
            text(
                "ANÁLISE DO MOTOR",
                19f,
                true
            )

        analysisView =
            text(
                "Aguardando dados..."
            )

        root.addView(title)
        root.addView(subtitle)
        root.addView(statusView)

        root.addView(assetTitle)
        root.addView(assetView)

        root.addView(priceTitle)
        root.addView(priceView)

        root.addView(analysisTitle)
        root.addView(analysisView)

        val scroll =
            ScrollView(this).apply {
                addView(root)
            }

        setContentView(scroll)
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
                        "● ONLINE\n" +
                        "FONTE: TWELVE DATA\n" +
                        "DADOS: REAIS\n" +
                        "EXECUÇÃO: DESATIVADA"

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

        analyzing = true

        thread {

            try {

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

                val candles =
                    LinkedHashMap<
                        String,
                        List<MarketCandle>
                    >()

                for (
                    timeframe in timeframes
                ) {

                    candles[timeframe] =
                        candleClient.getCandles(
                            symbol =
                                selectedAsset,
                            timeframe =
                                timeframe,
                            outputSize =
                                200
                        )
                }

                val now =
                    System.currentTimeMillis()

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

                val primary =
                    realtime.metrics["M15"]
                        ?: realtime.metrics.values.first()

                /*
                 * O sinal precisa ser confirmado
                 * pelos indicadores quantitativos.
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
                            primary.ema9 >
                                primary.ema21 &&
                                primary.macd >
                                primary.macdSignal

                        "VENDA" ->
                            primary.ema9 <
                                primary.ema21 &&
                                primary.macd <
                                primary.macdSignal

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

                val falseSignalInput =
                    FalseSignalInput(
                        structureContradiction =
                            abs(
                                primary.structure -
                                    realtime.mtfConfluence
                            ),

                        momentumDivergence =
                            abs(
                                primary.momentum -
                                    primary.rsi
                            ),

                        volumeMismatch =
                            abs(
                                primary.volume -
                                    primary.momentum
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
                            realtime,
                            finalResult,
                            candles
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

                analyzing = false
            }
        }
    }

    private fun buildAnalysisText(
        realtime:
            RealtimeAnalysis,

        result:
            FinalMotorResult,

        candles:
            Map<String, List<MarketCandle>>
    ): String {

        val output =
            StringBuilder()

        output.append(
            "FONTE: TWELVE DATA\n"
        )

        output.append(
            "QUALIDADE: ${
                realtime.market.dataQuality
            }\n\n"
        )

        output.append(
            "REGIME: ${
                realtime.regime
            }\n"
        )

        output.append(
            "DIREÇÃO: ${
                realtime.direction
            }\n\n"
        )

        output.append(
            "SCORE QUANTITATIVO: ${
                "%.1f".format(
                    realtime.score
                )
            }\n"
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
            "CONFLUÊNCIA MTF: ${
                "%.1f".format(
                    realtime.mtfConfluence
                )
            }\n"
        )

        output.append(
            "CONFIANÇA DO MODELO: ${
                "%.1f".format(
                    realtime.confidence
                )
            }\n\n"
        )

        output.append(
            "SEQUÊNCIA: ${
                result.sequence.stage
            }\n"
        )

        output.append(
            "SEQUÊNCIA CONFIRMADA: ${
                if (
                    result.sequence.confirmed
                ) {
                    "SIM"
                } else {
                    "NÃO"
                }
            }\n"
        )

        output.append(
            "DECISÃO FINAL: ${
                result.decision.decision
            }\n"
        )

        output.append(
            "MOTIVO: ${
                result.decision.reason
            }\n\n"
        )

        val m15 =
            realtime.metrics["M15"]

        if (m15 != null) {

            output.append(
                "M15\n"
            )

            output.append(
                "EMA 9: ${
                    "%.5f".format(
                        m15.ema9
                    )
                }\n"
            )

            output.append(
                "EMA 21: ${
                    "%.5f".format(
                        m15.ema21
                    )
                }\n"
            )

            output.append(
                "EMA 50: ${
                    "%.5f".format(
                        m15.ema50
                    )
                }\n"
            )

            output.append(
                "RSI: ${
                    "%.1f".format(
                        m15.rsi
                    )
                }\n"
            )

            output.append(
                "MACD: ${
                    "%.5f".format(
                        m15.macd
                    )
                }\n"
            )

            output.append(
                "MACD SIGNAL: ${
                    "%.5f".format(
                        m15.macdSignal
                    )
                }\n"
            )

            output.append(
                "ADX: ${
                    "%.1f".format(
                        m15.adx
                    )
                }\n"
            )

            output.append(
                "ATR: ${
                    "%.5f".format(
                        m15.atr
                    )
                }\n"
            )

            output.append(
                "SUPORTE: ${
                    "%.5f".format(
                        m15.support
                    )
                }\n"
            )

            output.append(
                "RESISTÊNCIA: ${
                    "%.5f".format(
                        m15.resistance
                    )
                }\n"
            )
        }

        output.append(
            "\nTIMEFRAMES\n"
        )

        for (
            timeframe in candles.keys
        ) {

            output.append(
                "$timeframe: ${
                    candles[timeframe]?.size
                        ?: 0
                } candles\n"
            )
        }

        output.append(
            "\nATUALIZAÇÃO: CONTÍNUA"
        )

        output.append(
            "\nEXECUÇÃO DE ORDENS: DESATIVADA"
        )

        return output.toString()
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            refreshTask
        )

        quoteClient.disconnect()

        super.onDestroy()
    }
}

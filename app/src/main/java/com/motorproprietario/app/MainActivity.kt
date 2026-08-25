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

    private var analyzing =
        false

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

                val market =
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

                val finalInput =
                    FinalMotorInput(
                        market =
                            market,
                        now =
                            now,

                        /*
                         * A sequência completa será alimentada
                         * pelo histórico do motor conforme
                         * avançarmos o estado persistente.
                         */
                        sequence =
                            SequenceInput(
                                signal = 0,
                                confirmation = 0,
                                invalidation = 0
                            ),

                        sequenceStage =
                            SequenceStage.S0,

                        falseSignal =
                            FalseSignalInput(
                                structureContradiction = 0.0,
                                momentumDivergence = 0.0,
                                volumeMismatch = 0.0,
                                confirmationFailure = 0.0,
                                timeframeConflict = 0.0
                            )
                    )

                val result =
                    FinalMotorEngine.evaluate(
                        finalInput
                    )

                runOnUiThread {

                    analysisView.text =
                        buildAnalysisText(
                            market,
                            result,
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
        market: MarketData,
        result: FinalMotorResult,
        candles:
            Map<String, List<MarketCandle>>
    ): String {

        val output =
            StringBuilder()

        output.append(
            "FONTE: ${market.source}\n"
        )

        output.append(
            "QUALIDADE: ${market.dataQuality}\n\n"
        )

        output.append(
            "ESTRUTURA: ${
                "%.1f".format(
                    market.structure
                )
            }\n"
        )

        output.append(
            "TENDÊNCIA: ${
                "%.1f".format(
                    market.trend
                )
            }\n"
        )

        output.append(
            "MOMENTUM: ${
                "%.1f".format(
                    market.momentum
                )
            }\n"
        )

        output.append(
            "VOLUME: ${
                "%.1f".format(
                    market.volume
                )
            }\n"
        )

        output.append(
            "VOLATILIDADE: ${
                "%.1f".format(
                    market.volatility
                )
            }\n"
        )

        output.append(
            "MTF: ${
                "%.1f".format(
                    market.multiTimeframe
                )
            }\n\n"
        )

        output.append(
            "SCORE: ${
                "%.1f".format(
                    result.score.score
                )
            }\n"
        )

        output.append(
            "FSI: ${
                "%.1f".format(
                    result.fsi.value
                )
            } — ${
                result.fsi.level
            }\n"
        )

        output.append(
            "FALSO SINAL: ${
                if (
                    result.falseSignal.blocked
                ) {
                    "BLOQUEADO"
                } else {
                    "OK"
                }
            }\n"
        )

        output.append(
            "SEQUÊNCIA: ${
                result.sequence.stage
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

        output.append(
            "TIMEFRAMES\n"
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
            "\nATUALIZAÇÃO CONTÍNUA: ATIVA"
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

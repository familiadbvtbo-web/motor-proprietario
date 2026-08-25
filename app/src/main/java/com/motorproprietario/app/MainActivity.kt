package com.motorproprietario.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.round

class MainActivity : AppCompatActivity() {

    private var previousScore = 0.0

    private val policy = AlertPolicy(
        minScoreChange = 10.0,
        cooldownMinutes = 15,
        requireGoodData = true
    )

    private var state = BetaState(
        connected = true,
        dataQuality = "GOOD",
        paperMode = true,
        lastUpdate = System.currentTimeMillis()
    )

    private lateinit var statusView: TextView
    private lateinit var indicatorsView: TextView
    private lateinit var decisionView: TextView
    private lateinit var updateView: TextView

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

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
                setTypeface(null, Typeface.BOLD)
            }

            setPadding(
                dp(4),
                dp(8),
                dp(4),
                dp(8)
            )
        }
    }

    private fun fmt(value: Double): String {
        return "%.1f".format(value)
    }

    private fun buildMarketData(now: Long): MarketData {

        /*
         * Nesta etapa os dados abaixo são SIMULADOS.
         *
         * Nenhum dado de corretora ou mercado real está sendo
         * fingido como se fosse real.
         *
         * Quando o adaptador de mercado for integrado,
         * esta função será substituída pela fonte real.
         */
        return MarketData(
            asset = "SIMULADO",
            timestamp = now,
            price = 100.0,
            structure = 0.0,
            trend = 0.0,
            momentum = 0.0,
            volume = 0.0,
            volatility = 0.0,
            fsi = 0.0,
            multiTimeframe = 0.0,
            dataQuality = state.dataQuality
        )
    }

    private fun evaluateMotor(): FinalMotorResult {

        val now = System.currentTimeMillis()

        val market = buildMarketData(now)

        return FinalMotorEngine.evaluate(
            FinalMotorInput(
                market = market,
                now = now,

                sequence = SequenceInput(
                    signalDetected = false,
                    confirmation = false,
                    continuation = false,
                    invalidated = false
                ),

                sequenceStage = SequenceStage.S0,

                falseSignal = FalseSignalInput(
                    structureContradiction = 0.0,
                    momentumDivergence = 0.0,
                    volumeMismatch = 0.0,
                    confirmationFailure = 0.0,
                    timeframeConflict = 0.0
                )
            )
        )
    }

    private fun refreshState() {

        val now = System.currentTimeMillis()

        state = state.copy(
            lastUpdate = now
        )

        val result = evaluateMotor()

        val connectedText =
            if (state.connected) {
                "ONLINE"
            } else {
                "OFFLINE"
            }

        statusView.text =
            "CONEXÃO       $connectedText\n" +
            "DADOS         ${state.dataQuality}\n" +
            "PAPER         ${if (state.paperMode) "ATIVO" else "INATIVO"}"

        indicatorsView.text =
            "SCORE              ${fmt(result.score.score)}\n\n" +
            "FSI                  ${fmt(result.fsi.value)}\n\n" +
            "FALSO SINAL     ${fmt(result.falseSignal.risk)}\n\n" +
            "SEQUÊNCIA       ${if (result.sequence.confirmed) "CONFIRMADA" else "NÃO CONFIRMADA"}"

        val currentScore = result.score.score

        val alert = shouldAlert(
            previousScore,
            currentScore,
            state.dataQuality,
            policy
        )

        previousScore = currentScore

        decisionView.text =
            "${result.decision.decision}\n\n" +
            "Motivo: ${result.decision.reason}\n" +
            "Score: ${fmt(currentScore)}\n" +
            "Alerta: ${if (alert) "SIM" else "NÃO"}"

        updateView.text =
            "Última atualização: $now\n\n" +
            "Ativo: ${result.sequence.confirmed}\n" +
            "Mercado utilizável: ${if (result.marketUsable) "SIM" else "NÃO"}\n" +
            "Modo: PAPER TRADING\n" +
            "Execução real: DESATIVADA\n\n" +
            "FONTE ATUAL: DADOS SIMULADOS\n" +
            "Nenhuma ordem real pode ser enviada nesta etapa."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(36),
                dp(24),
                dp(24)
            )

            setBackgroundColor(
                Color.rgb(18, 15, 22)
            )
        }

        val title = text(
            "MOTOR PROPRIETÁRIO",
            26f,
            true
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val version = text(
            "V174.0",
            18f
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.LTGRAY)
        }

        val mode = text(
            "SMARTPHONE BETA\n" +
            "PAPER TRADING: ATIVO\n" +
            "EXECUÇÃO REAL: DESATIVADA",
            17f
        ).apply {
            setPadding(
                dp(4),
                dp(24),
                dp(4),
                dp(24)
            )
        }

        val statusTitle = text(
            "STATUS DO MOTOR",
            19f,
            true
        )

        statusView = text(
            "",
            17f
        )

        val indicatorsTitle = text(
            "INDICADORES DO MOTOR",
            19f,
            true
        ).apply {
            setPadding(
                dp(4),
                dp(20),
                dp(4),
                dp(8)
            )
        }

        indicatorsView = text(
            "",
            17f
        )

        val decisionTitle = text(
            "DECISÃO",
            19f,
            true
        ).apply {
            setPadding(
                dp(4),
                dp(20),
                dp(4),
                dp(8)
            )
        }

        decisionView = text(
            "",
            18f
        )

        val updateTitle = text(
            "ESTADO",
            19f,
            true
        ).apply {
            setPadding(
                dp(4),
                dp(20),
                dp(4),
                dp(8)
            )
        }

        updateView = text(
            "",
            14f
        ).apply {
            setTextColor(Color.LTGRAY)
        }

        val refreshButton = Button(this).apply {
            text = "EXECUTAR CICLO DO MOTOR"

            setOnClickListener {
                refreshState()
            }
        }

        root.addView(title)
        root.addView(version)
        root.addView(mode)

        root.addView(statusTitle)
        root.addView(statusView)

        root.addView(indicatorsTitle)
        root.addView(indicatorsView)

        root.addView(decisionTitle)
        root.addView(decisionView)

        root.addView(updateTitle)
        root.addView(updateView)

        root.addView(refreshButton)

        val scroll = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scroll)

        refreshState()
    }
}

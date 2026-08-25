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
import kotlin.math.abs

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

    private fun metric(name: String, value: Double): TextView {
        return text(
            "$name    ${"%.1f".format(value)}",
            17f,
            true
        ).apply {
            setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(12)
            )
        }
    }

    private fun calculateDisplayScore(): Double {
        /*
         * Nesta etapa NÃO existe fórmula proprietária de Score.
         * Portanto não fabricamos um Score a partir dos indicadores.
         */
        return 0.0
    }

    private fun refreshState() {

        state = state.copy(
            lastUpdate = System.currentTimeMillis()
        )

        val connectedText =
            if (state.connected) "ONLINE" else "OFFLINE"

        val qualityText = state.dataQuality

        statusView.text =
            "CONEXÃO       $connectedText\n" +
            "DADOS         $qualityText\n" +
            "PAPER         ${if (state.paperMode) "ATIVO" else "INATIVO"}"

        val currentScore = calculateDisplayScore()

        val alert = shouldAlert(
            previousScore,
            currentScore,
            state.dataQuality,
            policy
        )

        previousScore = currentScore

        decisionView.text =
            "AGUARDAR\n\n" +
            "Regime: —\n" +
            "Score: ${"%.1f".format(currentScore)}\n" +
            "Alerta: ${if (alert) "SIM" else "NÃO"}"

        updateView.text =
            "Última atualização: ${state.lastUpdate}\n\n" +
            "Os indicadores permanecem neutros enquanto " +
            "a fonte de dados de mercado não estiver conectada."
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
            setBackgroundColor(Color.rgb(18, 15, 22))
        }

        val title = text(
            "MOTOR PROPRIETÁRIO",
            26f,
            true
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val version = text(
            "V173.0",
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

        statusView = text("", 17f)

        val indicatorsTitle = text(
            "INDICADORES",
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

        val fsi = metric("FSI", 0.0)
        val pfs = metric("PFS", 0.0)
        val mis = metric("MIS", 0.0)
        val antiTrap = metric("ANTI-TRAP", 0.0)

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

        decisionView = text("", 18f)

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

        updateView = text("", 14f).apply {
            setTextColor(Color.LTGRAY)
        }

        val refreshButton = Button(this).apply {
            text = "ATUALIZAR ESTADO"
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
        root.addView(fsi)
        root.addView(pfs)
        root.addView(mis)
        root.addView(antiTrap)
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

package com.motorproprietario.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun label(text: String, size: Float = 16f): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            setPadding(dp(4), dp(8), dp(4), dp(8))
        }
    }

    private fun metric(name: String, value: String): TextView {
        return TextView(this).apply {
            text = "$name    $value"
            textSize = 17f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(36), dp(24), dp(24))
            setBackgroundColor(Color.rgb(18, 15, 22))
        }

        val title = TextView(this).apply {
            text = "MOTOR PROPRIETÁRIO"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, dp(8))
        }

        val version = label("V172.0", 18f).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.LTGRAY)
        }

        val mode = label(
            "SMARTPHONE BETA\n" +
            "PAPER TRADING: ATIVO\n" +
            "EXECUÇÃO REAL: DESATIVADA",
            17f
        ).apply {
            setPadding(dp(4), dp(24), dp(4), dp(24))
        }

        val connection = label("STATUS DO MOTOR", 19f).apply {
            setTypeface(null, Typeface.BOLD)
        }

        val status = label(
            "CONEXÃO       ONLINE\n" +
            "DADOS         GOOD",
            17f
        )

        val scores = label("INDICADORES", 19f).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(4), dp(20), dp(4), dp(8))
        }

        val fsi = metric("FSI", "0.0")
        val pfs = metric("PFS", "0.0")
        val mis = metric("MIS", "0.0")
        val antiTrap = metric("ANTI-TRAP", "0.0")

        val decisionTitle = label("DECISÃO", 19f).apply {
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(4), dp(20), dp(4), dp(8))
        }

        val decision = label(
            "AGUARDAR\n\n" +
            "Regime: —\n" +
            "Dados: GOOD",
            18f
        )

        val note = label(
            "Valores 0.0 são neutros nesta etapa.\n" +
            "Nenhuma ordem real pode ser executada.",
            14f
        ).apply {
            setTextColor(Color.LTGRAY)
            setPadding(dp(4), dp(24), dp(4), dp(8))
        }

        root.addView(title)
        root.addView(version)
        root.addView(mode)
        root.addView(connection)
        root.addView(status)
        root.addView(scores)
        root.addView(fsi)
        root.addView(pfs)
        root.addView(mis)
        root.addView(antiTrap)
        root.addView(decisionTitle)
        root.addView(decision)
        root.addView(note)

        val scroll = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scroll)
    }
}

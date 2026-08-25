package com.motorproprietario.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var assetView: TextView
    private lateinit var priceView: TextView
    private lateinit var timeView: TextView
    private lateinit var analysisView: TextView

    private val handler =
        Handler(Looper.getMainLooper())

    private var selectedAsset =
        "EUR/USD"

    private var latestQuote:
        RealTimeQuote? = null

    private val client =
        TwelveDataClient()

    private fun dp(
        value: Int
    ): Int {

        return (
            value *
                resources.displayMetrics.density
        ).toInt()
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
                26f,
                true
            )

        title.gravity =
            Gravity.CENTER_HORIZONTAL

        val version =
            text(
                "ANÁLISE REAL • TEMPO REAL",
                17f
            )

        version.gravity =
            Gravity.CENTER_HORIZONTAL

        statusView =
            text(
                "CONECTANDO..."
            )

        val assetTitle =
            text(
                "ATIVO",
                19f,
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
                19f,
                true
            )

        priceView =
            text(
                "Aguardando cotação...",
                24f,
                true
            )

        val timeTitle =
            text(
                "ATUALIZAÇÃO",
                17f,
                true
            )

        timeView =
            text(
                "—"
            )

        val analysisTitle =
            text(
                "MOTOR DE ANÁLISE",
                19f,
                true
            )

        analysisView =
            text(
                "Aguardando dados reais..."
            )

        val changeButton =
            Button(this).apply {

                text =
                    "ANALISAR EUR/USD"

                setOnClickListener {

                    selectedAsset =
                        "EUR/USD"

                    assetView.text =
                        selectedAsset

                    reconnect()
                }
            }

        root.addView(title)
        root.addView(version)

        root.addView(statusView)

        root.addView(assetTitle)
        root.addView(assetView)

        root.addView(priceTitle)
        root.addView(priceView)

        root.addView(timeTitle)
        root.addView(timeView)

        root.addView(analysisTitle)
        root.addView(analysisView)

        root.addView(changeButton)

        val scroll =
            ScrollView(this).apply {

                addView(root)
            }

        setContentView(scroll)

        connectRealtime()
    }

    private fun connectRealtime() {

        statusView.text =
            "CONECTANDO AO MERCADO REAL..."

        client.connect(
            symbols =
                listOf(
                    selectedAsset
                ),

            onQuote = {
                quote ->

                latestQuote =
                    quote

                runOnUiThread {

                    updateQuote(
                        quote
                    )
                }
            },

            onError = {
                error ->

                runOnUiThread {

                    statusView.text =
                        "ERRO DE DADOS\n" +
                        (
                            error.message
                                ?: "Conexão indisponível"
                        )
                }
            }
        )
    }

    private fun updateQuote(
        quote: RealTimeQuote
    ) {

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

        timeView.text =
            java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(
                    quote.timestamp
                )
            )

        analysisView.text =
            """
            COTAÇÃO RECEBIDA

            Ativo: ${quote.symbol}
            Preço: ${quote.price}

            O preço acima está chegando
            diretamente pelo fluxo em tempo real.

            PRÓXIMA CAMADA:
            candles → timeframes →
            FSI → falso sinal →
            sequência → score →
            confluência → análise final
            """.trimIndent()
    }

    private fun reconnect() {

        client.disconnect()

        latestQuote = null

        priceView.text =
            "Reconectando..."

        analysisView.text =
            "Solicitando dados reais..."

        connectRealtime()
    }

    override fun onDestroy() {

        client.disconnect()

        handler.removeCallbacksAndMessages(
            null
        )

        super.onDestroy()
    }
}

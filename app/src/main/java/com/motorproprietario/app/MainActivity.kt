package com.motorproprietario.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusView: TextView
    private lateinit var priceView: TextView
    private lateinit var analysisView: TextView
    private lateinit var assetsSpinner: Spinner

    private val handler = Handler(Looper.getMainLooper())

    /*
     * IMPORTANTE:
     *
     * Para teste no mesmo Wi-Fi:
     *
     * http://IP_DO_PC:8080
     *
     * Não use localhost.
     *
     * localhost no Android significa o próprio celular.
     */
    private val gatewayUrl =
        "http://192.168.1.100:8080"

    private var selectedAsset = "EURUSD"

    private val updateTask = object : Runnable {
        override fun run() {

            loadAnalysis()

            handler.postDelayed(
                this,
                2000L
            )
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

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

        val title = text(
            "MOTOR PROPRIETÁRIO",
            25f,
            true
        )

        title.gravity =
            Gravity.CENTER_HORIZONTAL

        val statusTitle = text(
            "FOREX — MERCADO REAL",
            19f,
            true
        )

        statusView = text(
            "Conectando ao gateway..."
        )

        val assetTitle = text(
            "ATIVO",
            18f,
            true
        )

        assetsSpinner =
            Spinner(this)

        val refreshButton =
            Button(this).apply {

                text = "ATUALIZAR ATIVOS"

                setOnClickListener {

                    loadAssets()
                }
            }

        val priceTitle = text(
            "PREÇO EM TEMPO REAL",
            18f,
            true
        )

        priceView = text(
            "Aguardando dados..."
        )

        val analysisTitle = text(
            "ANÁLISE DO MOTOR",
            18f,
            true
        )

        analysisView = text(
            "Aguardando análise..."
        )

        root.addView(title)
        root.addView(statusTitle)
        root.addView(statusView)

        root.addView(assetTitle)
        root.addView(assetsSpinner)
        root.addView(refreshButton)

        root.addView(priceTitle)
        root.addView(priceView)

        root.addView(analysisTitle)
        root.addView(analysisView)

        val scroll =
            ScrollView(this).apply {

                addView(root)
            }

        setContentView(scroll)

        loadAssets()

        handler.post(updateTask)
    }

    private fun loadAssets() {

        thread {

            try {

                val json =
                    getJson(
                        "$gatewayUrl/assets"
                    )

                val array =
                    json.getJSONArray(
                        "assets"
                    )

                val assets =
                    mutableListOf<String>()

                for (
                    i in 0 until array.length()
                ) {

                    assets.add(
                        array.getString(i)
                    )
                }

                assets.sort()

                runOnUiThread {

                    if (assets.isEmpty()) {

                        statusView.text =
                            "MT5 conectado, mas nenhum ativo foi encontrado."

                        return@runOnUiThread
                    }

                    val adapter =
                        ArrayAdapter(
                            this,
                            android.R.layout.simple_spinner_dropdown_item,
                            assets
                        )

                    assetsSpinner.adapter =
                        adapter

                    val index =
                        assets.indexOf(
                            selectedAsset
                        )

                    if (index >= 0) {

                        assetsSpinner
                            .setSelection(index)
                    }

                    assetsSpinner
                        .onItemSelectedListener =
                        object :
                            android.widget.AdapterView.OnItemSelectedListener {

                            override fun onNothingSelected(
                                parent: android.widget.AdapterView<*>?
                            ) {
                            }

                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: android.view.View?,
                                position: Int,
                                id: Long
                            ) {

                                selectedAsset =
                                    assets[position]
                            }
                        }

                    statusView.text =
                        "ONLINE\n" +
                        "ATIVOS: ${assets.size}\n" +
                        "FONTE: MT5"
                }

            } catch (error: Exception) {

                runOnUiThread {

                    statusView.text =
                        "ERRO DE CONEXÃO\n" +
                        error.message
                }
            }
        }
    }

    private fun loadAnalysis() {

        val asset =
            selectedAsset

        thread {

            try {

                val url =
                    "$gatewayUrl/analysis" +
                    "?symbol=$asset" +
                    "&timeframes=" +
                    "M1,M5,M15,M30,H1,H4,D1"

                val json =
                    getJson(url)

                val analysis =
                    json.getJSONObject(
                        "analysis"
                    )

                val price =
                    analysis.getDouble(
                        "price"
                    )

                val bid =
                    analysis.getDouble(
                        "bid"
                    )

                val ask =
                    analysis.getDouble(
                        "ask"
                    )

                val spread =
                    analysis.getDouble(
                        "spread"
                    )

                val trend =
                    analysis.getDouble(
                        "trend"
                    )

                val momentum =
                    analysis.getDouble(
                        "momentum"
                    )

                val volatility =
                    analysis.getDouble(
                        "volatility"
                    )

                val volume =
                    analysis.getDouble(
                        "volume"
                    )

                val structure =
                    analysis.getDouble(
                        "structure"
                    )

                val mtf =
                    analysis.getDouble(
                        "multi_timeframe"
                    )

                val quality =
                    analysis.getString(
                        "data_quality"
                    )

                val timeframes =
                    analysis.getJSONObject(
                        "timeframes"
                    )

                val result =
                    StringBuilder()

                val names =
                    listOf(
                        "M1",
                        "M5",
                        "M15",
                        "M30",
                        "H1",
                        "H4",
                        "D1"
                    )

                for (name in names) {

                    if (
                        timeframes.has(name)
                    ) {

                        val tf =
                            timeframes.getJSONObject(
                                name
                            )

                        result.append(
                            "\n$name\n"
                        )

                        result.append(
                            "Preço: ${
                                tf.getDouble(
                                    "price"
                                )
                            }\n"
                        )

                        result.append(
                            "Tendência: ${
                                tf.getDouble(
                                    "trend"
                                )
                            }\n"
                        )

                        result.append(
                            "Momentum: ${
                                tf.getDouble(
                                    "momentum"
                                )
                            }\n"
                        )

                        result.append(
                            "Volatilidade: ${
                                tf.getDouble(
                                    "volatility"
                                )
                            }\n"
                        )
                    }
                }

                runOnUiThread {

                    priceView.text =
                        "$asset\n\n" +
                        "PREÇO: $price\n" +
                        "BID: $bid\n" +
                        "ASK: $ask\n" +
                        "SPREAD: $spread\n" +
                        "QUALIDADE: $quality"

                    analysisView.text =
                        "ESTRUTURA: $structure\n" +
                        "TENDÊNCIA: $trend\n" +
                        "MOMENTUM: $momentum\n" +
                        "VOLUME: $volume\n" +
                        "VOLATILIDADE: $volatility\n" +
                        "CONFLUÊNCIA MTF: $mtf\n" +
                        result.toString()
                }

            } catch (error: Exception) {

                runOnUiThread {

                    priceView.text =
                        "Sem dados reais"

                    analysisView.text =
                        "Gateway indisponível:\n" +
                        error.message
                }
            }
        }
    }

    private fun getJson(
        address: String
    ): JSONObject {

        val connection =
            URL(address)
                .openConnection()
                    as HttpURLConnection

        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                5000

            connection.readTimeout =
                5000

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val code =
                connection.responseCode

            if (code !in 200..299) {

                throw RuntimeException(
                    "HTTP $code"
                )
            }

            val body =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            return JSONObject(body)

        } finally {

            connection.disconnect()
        }
    }

    override fun onDestroy() {

        handler.removeCallbacks(
            updateTask
        )

        super.onDestroy()
    }
}

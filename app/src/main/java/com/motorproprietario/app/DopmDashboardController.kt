package com.motorproprietario.app

import android.app.Activity
import android.widget.Toast
import java.util.Locale

/**
 * Ponte única entre os motores e a interface DOPM.
 *
 * Não calcula sinais.
 * Não altera resultados dos motores.
 * Apenas transporta resultados para o Dashboard.
 */
class DopmDashboardController(
    private val activity: Activity
) {

    private var dashboard: DopmDashboardView? = null

    fun install() {
        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            return
        }

        dashboard =
            DopmDashboardView(activity)

        activity.setContentView(dashboard)
    }

    fun view(): DopmDashboardView? =
        dashboard

    fun setSelectionListeners(
        marketChanged: (String) -> Unit,
        assetChanged: (String) -> Unit,
        timeframeChanged: (String) -> Unit
    ) {
        dashboard?.setSelectionListeners(
            marketChanged,
            assetChanged,
            timeframeChanged
        )
    }

    fun updateConnection(
        online: Boolean,
        api: String = "TWELVE DATA"
    ) {
        activity.runOnUiThread {
            dashboard?.apply {
                setOnline(online)
                setApi(api)
            }
        }
    }

    fun updateMarket(
    price: Double,
    asset: String
) {
    activity.runOnUiThread {
        dashboard?.apply {

            setAsset(asset)

            setPrice(
                String.format(
                    Locale.US,
                    "%.5f",
                    price
                )
            )
        }
    }
}

    fun updateTimeframe(
    timeframe: String
) {
    activity.runOnUiThread {
        dashboard?.setTimeframe(timeframe)
    }
}

    fun updateDecision(
        direction: String,
        buy: Double,
        sell: Double,
        neutral: Double
    ) {
        val total =
            when (
                direction.uppercase(Locale.US)
            ) {
                "COMPRA",
                "BUY" -> buy

                "VENDA",
                "SELL" -> sell

                else -> neutral
            }

        activity.runOnUiThread {
            dashboard?.apply {
                setDecision(
                    direction,
                    total
                )

                setProbabilities(
                    buy,
                    sell,
                    neutral
                )
            }
        }
    }

    fun updateMathematics(
        probability: Double,
        deterministic: Double,
        mtf: Double
    ) {
        activity.runOnUiThread {
            dashboard?.apply {
                setProbability(probability)
                setDeterminism(deterministic)
                setMtf(mtf)
            }
        }
    }

    fun updateBestTimeframe(
        timeframe: String
    ) {
        activity.runOnUiThread {
            dashboard?.setBestTimeframe(timeframe)
        }
    }

    fun updateTradePlan(
        entry: Double,
        stop: Double,
        tp1: Double,
        tp2: Double,
        tp3: Double
    ) {
        activity.runOnUiThread {
            dashboard?.setTradePlan(
                formatPrice(entry),
                formatPrice(stop),
                formatPrice(tp1),
                formatPrice(tp2),
                formatPrice(tp3)
            )
        }
    }

    fun updateTiming(
        timing: String,
        validity: String
    ) {
        activity.runOnUiThread {
            dashboard?.setTiming(
                timing,
                validity
            )
        }
    }

    /**
     * Indicadores reais calculados pelo RealtimeMarketAnalyzer.
     */
    fun updateIndicators(
        metrics: QuantMetrics
    ) {
        activity.runOnUiThread {
            dashboard?.apply {

                /*
                 * RSI já está em 0..100.
                 */
                setIndicator(
                    "RSI",
                    metrics.rsi
                )

                /*
                 * ADX já está em 0..100.
                 */
                setIndicator(
                    "ADX",
                    metrics.adx
                )

                /*
                 * EMA:
                 * transforma a relação EMA9/EMA21
                 * em força direcional 0..100.
                 */
                val emaScore =
                    when {
                        metrics.ema9 > metrics.ema21 ->
                            75.0

                        metrics.ema9 < metrics.ema21 ->
                            25.0

                        else ->
                            50.0
                    }

                setIndicator(
                    "EMA",
                    emaScore
                )

                /*
                 * MACD:
                 * somente normalização visual.
                 * Não altera o MACD original.
                 */
                val macdDelta =
                    metrics.macd -
                        metrics.macdSignal

                val macdScore =
                    (
                        50.0 +
                            macdDelta
                                .coerceIn(
                                    -1.0,
                                    1.0
                                ) *
                            50.0
                    ).coerceIn(
                        0.0,
                        100.0
                    )

                setIndicator(
                    "MACD",
                    macdScore
                )

                /*
                 * FSI:
                 * risco alto = barra alta.
                 */
                setIndicator(
                    "FSI",
                    50.0
                )

                /*
                 * FI:
                 * utiliza volume como proxy visual
                 * da força de fluxo disponível.
                 */
                setIndicator(
                    "FI",
                    metrics.volume
                )
            }
        }
    }

    /**
     * Versão completa dos indicadores com FSI e MTF.
     */
    fun updateIndicators(
        metrics: QuantMetrics,
        fsi: Double,
        mtf: Double
    ) {
        activity.runOnUiThread {
            dashboard?.apply {

                val emaScore =
                    when {
                        metrics.ema9 > metrics.ema21 &&
                            metrics.ema21 >= metrics.ema50 ->
                            90.0

                        metrics.ema9 > metrics.ema21 ->
                            70.0

                        metrics.ema9 < metrics.ema21 &&
                            metrics.ema21 <= metrics.ema50 ->
                            10.0

                        metrics.ema9 < metrics.ema21 ->
                            30.0

                        else ->
                            50.0
                    }

                val macdDelta =
                    metrics.macd -
                        metrics.macdSignal

                val macdScore =
                    (
                        50.0 +
                            macdDelta
                                .coerceIn(
                                    -1.0,
                                    1.0
                                ) *
                            50.0
                    ).coerceIn(
                        0.0,
                        100.0
                    )

                setIndicator(
                    "FI",
                    metrics.volume
                )

                setIndicator(
                    "FSI",
                    (100.0 - fsi)
                        .coerceIn(
                            0.0,
                            100.0
                        )
                )

                setIndicator(
                    "RSI",
                    metrics.rsi
                )

                setIndicator(
                    "MACD",
                    macdScore
                )

                setIndicator(
                    "EMA",
                    emaScore
                )

                setIndicator(
                    "ADX",
                    metrics.adx
                )
            }
        }
    }

    /**
     * Estado completo da análise.
     */
    fun updateAnalysis(
        metrics: QuantMetrics,
        buy: Double,
        sell: Double,
        neutral: Double,
        probability: Double,
        deterministic: Double,
        mtf: Double,
        fsi: Double,
        direction: String,
        bestTimeframe: String
    ) {
        activity.runOnUiThread {
            dashboard?.apply {

                setDecision(
                    direction,
                    when (direction.uppercase(Locale.US)) {
                        "COMPRA" -> buy
                        "VENDA" -> sell
                        else -> neutral
                    }
                )

                setProbabilities(
                    buy,
                    sell,
                    neutral
                )

                setProbability(
                    probability
                )

                setDeterminism(
                    deterministic
                )

                setMtf(
                    mtf
                )

                setBestTimeframe(
                    bestTimeframe
                )

                updateIndicators(
                    metrics,
                    fsi,
                    mtf
                )
            }
        }
    }

    fun marketNotConnected(
        market: String
    ) {
        activity.runOnUiThread {
            Toast.makeText(
                activity,
                "$market ainda não possui conector de dados ativo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun showOffline(
        message: String = "Conexão indisponível"
    ) {
        activity.runOnUiThread {
            dashboard?.setOnline(false)

            Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun clear() {
        dashboard = null
    }

    private fun formatPrice(
        value: Double
    ): String {
        if (!value.isFinite()) {
            return "--"
        }

        return String.format(
            Locale.US,
            "%.5f",
            value
        )
    }
}

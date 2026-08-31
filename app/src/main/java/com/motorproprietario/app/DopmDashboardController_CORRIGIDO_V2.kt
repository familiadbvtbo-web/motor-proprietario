package com.motorproprietario.app

import android.content.Context
import java.util.Locale
import kotlin.math.max

/**
 * Controller compatível com a DopmDashboardView fornecida pelo projeto.
 * Não calcula sinais; apenas encaminha os resultados para a View.
 */
class DopmDashboardController(
    context: Context
) {
    private val dashboardView = DopmDashboardView(context)

    fun view(): DopmDashboardView = dashboardView

    fun setSelectionListeners(
        marketChanged: (String) -> Unit,
        assetChanged: (String) -> Unit,
        timeframeChanged: (String) -> Unit
    ) {
        dashboardView.setSelectionListeners(
            marketChanged,
            assetChanged,
            timeframeChanged
        )
    }

    fun updateConnection(
        connected: Boolean,
        source: String
    ) {
        dashboardView.setOnline(connected)
        dashboardView.setApi(source)
    }

    fun updateMarket(
        price: Double,
        asset: String
    ) {
        dashboardView.setAsset(asset)
        dashboardView.setPrice(formatPrice(price))
    }

    fun updateTimeframe(
        timeframe: String
    ) {
        dashboardView.setTimeframe(timeframe)
    }

    fun updateDecision(
        direction: String,
        buy: Double,
        sell: Double,
        neutral: Double
    ) {
        dashboardView.setDecision(
            direction,
            max(buy, sell)
        )

        dashboardView.setProbabilities(
            buy,
            sell,
            neutral
        )
    }

    fun updateMathematics(
        probability: Double,
        determinism: Double,
        mtf: Double
    ) {
        dashboardView.setProbability(probability)
        dashboardView.setDeterminism(determinism)
        dashboardView.setMtf(mtf)
    }

    fun updateIndicators(
        metrics: QuantMetrics,
        fsi: Double,
        mtf: Double
    ) {
        // A View fornecida expõe setIndicator(name, value).
        dashboardView.setIndicator("FSI", fsi)
        dashboardView.setMtf(mtf)
    }

    fun updateBestTimeframe(
        timeframe: String
    ) {
        dashboardView.setBestTimeframe(timeframe)
    }

    fun updateTiming(
        timing: String,
        validity: String
    ) {
        dashboardView.setTiming(
            timing,
            validity
        )
    }

    fun updateTradePlan(
        entry: Double,
        stop: Double,
        tp1: Double,
        tp2: Double,
        tp3: Double
    ) {
        dashboardView.setTradePlan(
            formatPrice(entry),
            formatPrice(stop),
            formatPrice(tp1),
            formatPrice(tp2),
            formatPrice(tp3)
        )
    }

    private fun formatPrice(value: Double): String {
        if (!value.isFinite()) return "--"

        return String.format(
            Locale.US,
            "%.8f",
            value
        ).trimEnd('0').trimEnd('.')
    }
}

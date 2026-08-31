package com.motorproprietario.app

import android.content.Context
import android.graphics.Color

/**
 * Controller da Dashboard DOPM.
 *
 * Compatível com as chamadas existentes da MainActivity:
 * updateConnection, updateMarket, updateTimeframe, updateDecision,
 * updateMathematics, updateIndicators, updateBestTimeframe,
 * updateTiming, updateTradePlan e view().
 *
 * O Controller somente encaminha resultados para a View.
 * Nenhum cálculo do Motor é realizado aqui.
 */
class DopmDashboardController(
    private val context: Context
) {

    private var dashboardView: DopmDashboardView? = null

    fun install() {
        dashboardView =
            DopmDashboardView(context)

        /*
         * A Activity continua podendo instalar a View através
         * do Controller. setContentView é feito aqui para manter
         * compatibilidade com a arquitetura atual.
         */
        (context as? android.app.Activity)
            ?.setContentView(dashboardView)
    }

    fun view(): DopmDashboardView? =
        dashboardView

    fun setSelectionListeners(
        marketChanged: ((String) -> Unit)? = null,
        assetChanged: ((String) -> Unit)? = null,
        timeframeChanged: ((String) -> Unit)? = null
    ) {
        dashboardView?.setSelectionListeners(
            marketChanged,
            assetChanged,
            timeframeChanged
        )
    }

    fun updateConnection(
        connected: Boolean,
        source: String
    ) {
        dashboardView?.setConnection(
            connected,
            source
        )
    }

    fun updateMarket(
        price: Double,
        asset: String
    ) {
        dashboardView?.setMarket(
            price,
            asset
        )
    }

    fun updateTimeframe(
        timeframe: String
    ) {
        dashboardView?.setTimeframe(
            timeframe
        )
    }

    fun updateDecision(
        direction: String,
        buy: Double,
        sell: Double,
        neutral: Double
    ) {
        dashboardView?.setDecision(
            direction,
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
        dashboardView?.setMathematics(
            probability,
            determinism,
            mtf
        )
    }

    fun updateIndicators(
        metrics: QuantMetrics,
        fsi: Double,
        mtf: Double
    ) {
        dashboardView?.setIndicators(
            metrics,
            fsi,
            mtf
        )
    }

    fun updateBestTimeframe(
        timeframe: String
    ) {
        dashboardView?.setBestTimeframe(
            timeframe
        )
    }

    fun updateTiming(
        timing: String,
        validity: String
    ) {
        dashboardView?.setTiming(
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
        dashboardView?.setTradePlan(
            entry,
            stop,
            tp1,
            tp2,
            tp3
        )
    }
}

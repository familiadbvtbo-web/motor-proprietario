package com.motorproprietario.app

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import java.util.Locale
import kotlin.math.max

/**
 * Controller DOPM.
 *
 * Mantém a API usada pela MainActivity e instala um seletor expandido
 * sem alterar a lógica matemática do Motor.
 *
 * Os nomes amigáveis de índices/commodities são convertidos em consultas
 * para o SymbolResolver/Twelve Data, que é responsável por encontrar o
 * símbolo real disponível.
 */
class DopmDashboardController(context: Context) {

    private val dashboardView = DopmDashboardView(context)

    private val assetLabels = listOf(
        "EUR/USD",
        "GBP/USD",
        "USD/JPY",
        "USD/CHF",
        "AUD/USD",
        "USD/CAD",
        "NZD/USD",
        "EUR/GBP",
        "EUR/JPY",
        "GBP/JPY",
        "BTC/USD",
        "ETH/USD",
        "SOL/USD",
        "XRP/USD",
        "US100 • Nasdaq 100",
        "US500 • S&P 500",
        "US30 • Dow Jones",
        "DE40 • DAX",
        "UK100 • FTSE 100",
        "JP225 • Nikkei 225",
        "XAU/USD • Ouro",
        "XAG/USD • Prata",
        "WTI • Petróleo",
        "Brent • Petróleo",
        "IBOV • Bovespa",
    )

    private val assetQueries = linkedMapOf(
        "EUR/USD" to "EUR/USD",
        "GBP/USD" to "GBP/USD",
        "USD/JPY" to "USD/JPY",
        "USD/CHF" to "USD/CHF",
        "AUD/USD" to "AUD/USD",
        "USD/CAD" to "USD/CAD",
        "NZD/USD" to "NZD/USD",
        "EUR/GBP" to "EUR/GBP",
        "EUR/JPY" to "EUR/JPY",
        "GBP/JPY" to "GBP/JPY",
        "BTC/USD" to "BTC/USD",
        "ETH/USD" to "ETH/USD",
        "SOL/USD" to "SOL/USD",
        "XRP/USD" to "XRP/USD",
        "US100 • Nasdaq 100" to "Nasdaq 100",
        "US500 • S&P 500" to "S&P 500",
        "US30 • Dow Jones" to "Dow Jones Industrial Average",
        "DE40 • DAX" to "DAX",
        "UK100 • FTSE 100" to "FTSE 100",
        "JP225 • Nikkei 225" to "Nikkei 225",
        "XAU/USD • Ouro" to "XAU/USD",
        "XAG/USD • Prata" to "XAG/USD",
        "WTI • Petróleo" to "Crude Oil WTI",
        "Brent • Petróleo" to "Brent Crude Oil",
        "IBOV • Bovespa" to "IBOV",
    )

    fun install() {
        (dashboardView.context as? Activity)
            ?.setContentView(dashboardView)
    }

    fun view(): DopmDashboardView = dashboardView

    fun setSelectionListeners(
        marketChanged: (String) -> Unit,
        assetChanged: (String) -> Unit,
        timeframeChanged: (String) -> Unit
    ) {
        // Mercado e timeframe continuam usando a implementação oficial da View.
        dashboardView.setSelectionListeners(
            marketChanged,
            {},
            timeframeChanged
        )

        installExpandedAssetSelector(assetChanged)
    }

    private fun installExpandedAssetSelector(
        assetChanged: (String) -> Unit
    ) {
        try {
            val field =
                DopmDashboardView::class.java
                    .getDeclaredField("assetSpinner")

            field.isAccessible = true

            val spinner =
                field.get(dashboardView) as Spinner

            spinner.adapter =
                ArrayAdapter(
                    dashboardView.context,
                    android.R.layout.simple_spinner_dropdown_item,
                    assetLabels
                )

            spinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        if (position in assetLabels.indices) {
                            val query =
                                assetQueries[assetLabels[position]]
                                    ?: assetLabels[position]

                            assetChanged(query)
                        }
                    }

                    override fun onNothingSelected(
                        parent: AdapterView<*>?
                    ) {}
                }

            // Mantém EUR/USD como seleção inicial.
            spinner.setSelection(0)
        } catch (_: Exception) {
            // Se a implementação interna da View mudar, o restante do Dashboard
            // continua funcionando com o seletor original.
        }
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
        dashboardView.setPrice(formatPrice(price))
        selectExpandedAsset(asset)
    }

    private fun selectExpandedAsset(asset: String) {
        val label =
            assetQueries.entries
                .firstOrNull { it.value.equals(asset, ignoreCase = true) }
                ?.key
                ?: asset

        val index =
            assetLabels.indexOfFirst {
                it.equals(label, ignoreCase = true)
            }

        if (index < 0) return

        try {
            val field =
                DopmDashboardView::class.java
                    .getDeclaredField("assetSpinner")

            field.isAccessible = true

            val spinner =
                field.get(dashboardView) as Spinner

            if (spinner.selectedItemPosition != index) {
                spinner.setSelection(index)
            }
        } catch (_: Exception) {
        }
    }

    fun updateTimeframe(timeframe: String) {
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
        dashboardView.setIndicator("FSI", fsi)
        dashboardView.setMtf(mtf)
    }

    fun updateBestTimeframe(timeframe: String) {
        dashboardView.setBestTimeframe(timeframe)
    }

    fun updateTiming(
        timing: String,
        validity: String
    ) {
        dashboardView.setTiming(timing, validity)
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

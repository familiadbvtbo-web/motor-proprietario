package com.motorproprietario.app

import android.app.Activity
import android.widget.Toast
import java.util.Locale

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

        activity.setContentView(
            dashboard
        )
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

            dashboard?.setPrice(

                String.format(
                    Locale.US,
                    "%.5f",
                    price
                )
            )
        }
    }

    fun updateDecision(
        direction: String,
        buy: Double,
        sell: Double,
        neutral: Double
    ) {

        val total =
            maxOf(
                buy,
                sell
            )

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

                setProbability(
                    probability
                )

                setDeterminism(
                    deterministic
                )

                setMtf(
                    mtf
                )
            }
        }
    }

    fun updateBestTimeframe(
        timeframe: String
    ) {

        activity.runOnUiThread {

            dashboard?.setBestTimeframe(
                timeframe
            )
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

                String.format(
                    Locale.US,
                    "%.5f",
                    entry
                ),

                String.format(
                    Locale.US,
                    "%.5f",
                    stop
                ),

                String.format(
                    Locale.US,
                    "%.5f",
                    tp1
                ),

                String.format(
                    Locale.US,
                    "%.5f",
                    tp2
                ),

                String.format(
                    Locale.US,
                    "%.5f",
                    tp3
                )
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
        message: String =
            "Conexão indisponível"
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
}

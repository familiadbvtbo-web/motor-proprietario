package com.motorproprietario.app

import android.app.Activity
import android.widget.Toast

/**
 * Controlador da interface principal DOPM.
 *
 * Responsabilidade:
 *
 * - instalar o dashboard;
 * - receber eventos dos seletores;
 * - encaminhar os resultados reais do motor;
 * - não executar cálculos matemáticos.
 *
 * A lógica matemática permanece nos Engines existentes.
 */
class DopmDashboardController(
    private val activity: Activity
) {

    private var dashboard:
        DopmDashboardView? = null

    /**
     * Instala a nova interface.
     */
    fun install() {

        if (
            activity.isFinishing ||
            activity.isDestroyed
        ) {
            return
        }

        val view =
            DopmDashboardView(
                activity
            )

        dashboard =
            view

        activity.setContentView(
            view
        )
    }

    /**
     * Retorna a interface instalada.
     */
    fun view():
        DopmDashboardView? {

        return dashboard
    }

    /**
     * Conecta os seletores do dashboard
     * à Activity principal.
     *
     * Fluxo:
     *
     * Dashboard
     *      ↓
     * Controller
     *      ↓
     * MainActivity
     */
    fun setSelectionListeners(
        marketChanged: (String) -> Unit,
        assetChanged: (String) -> Unit,
        timeframeChanged: (String) -> Unit
    ) {

        dashboard?.setSelectionListeners(

            marketChanged =
                marketChanged,

            assetChanged =
                assetChanged,

            timeframeChanged =
                timeframeChanged
        )
    }

    /**
     * Atualiza o estado da conexão.
     */
    fun updateConnection(
        online: Boolean,
        api: String = "TWELVE DATA"
    ) {

        activity.runOnUiThread {

            dashboard?.apply {

                setOnline(
                    online
                )

                setApi(
                    api
                )
            }
        }
    }

    /**
     * Atualiza ativo e preço.
     */
    fun updateMarket(
        price: Double,
        asset: String
    ) {

        activity.runOnUiThread {

            dashboard?.apply {

                setPrice(
                    String.format(
                        "%.5f",
                        price
                    )
                )
            }
        }
    }

    /**
     * Atualiza o resultado principal.
     *
     * TOTAL representa a maior probabilidade
     * direcional entre compra e venda.
     */
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

    /**
     * Atualiza os componentes matemáticos.
     */
    fun updateMathematics(
        probability: Double,
        deterministic: Double,
        mtf: Double
    ) {

        activity.runOnUiThread {

            dashboard?.apply {

                setDeterminism(
                    deterministic
                )

                setMtf(
                    mtf
                )
            }
        }
    }

    /**
     * Atualiza o melhor timeframe.
     */
    fun updateBestTimeframe(
        timeframe: String
    ) {

        activity.runOnUiThread {

            dashboard?.setBestTimeframe(
                timeframe
            )
        }
    }

    /**
     * Atualiza o plano operacional.
     */
    fun updateTradePlan(
        entry: Double,
        stop: Double,
        tp1: Double,
        tp2: Double,
        tp3: Double
    ) {

        activity.runOnUiThread {

            dashboard?.setTradePlan(

                entry =
                    String.format(
                        "%.5f",
                        entry
                    ),

                stop =
                    String.format(
                        "%.5f",
                        stop
                    ),

                tp1 =
                    String.format(
                        "%.5f",
                        tp1
                    ),

                tp2 =
                    String.format(
                        "%.5f",
                        tp2
                    ),

                tp3 =
                    String.format(
                        "%.5f",
                        tp3
                    )
            )
        }
    }

    /**
     * Atualiza timing e validade.
     */
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
     * Mostra aviso de mercado ainda
     * não conectado.
     */
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

    /**
     * Define visualmente o estado de erro.
     */
    fun showOffline(
        message: String =
            "Conexão indisponível"
    ) {

        activity.runOnUiThread {

            dashboard?.setOnline(
                false
            )

            Toast.makeText(
                activity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Limpa a referência da interface.
     */
    fun clear() {

        dashboard =
            null
    }
}

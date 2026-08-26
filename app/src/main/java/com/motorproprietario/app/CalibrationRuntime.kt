package com.motorproprietario.app

import android.content.Context

/**
 * Runtime da calibração do Motor Proprietário.
 *
 * Responsabilidade:
 *
 * - carregar a calibração persistida;
 * - fornecer os pesos atuais;
 * - garantir fallback seguro para 50/50;
 * - não executar calibração;
 * - não alterar o estado por conta própria.
 */
class CalibrationRuntime(
    context: Context
) {

    private val provider =
        CalibrationProvider(
            context.applicationContext
        )

    /**
     * Estado atual da calibração.
     */
    fun state():
        CalibrationState {

        return provider.getState()
    }

    /**
     * Peso probabilístico atual.
     */
    fun probabilityWeight():
        Double {

        return provider
            .probabilityWeight()
            .coerceIn(
                0.0,
                1.0
            )
    }

    /**
     * Peso determinístico atual.
     */
    fun deterministicWeight():
        Double {

        return provider
            .deterministicWeight()
            .coerceIn(
                0.0,
                1.0
            )
    }

    /**
     * Retorna os pesos já normalizados.
     */
    fun weights():
        Pair<Double, Double> {

        val probability =
            probabilityWeight()

        val deterministic =
            deterministicWeight()

        val total =
            probability +
                deterministic

        if (
            total <= 0.0 ||
            !total.isFinite()
        ) {

            return Pair(
                0.50,
                0.50
            )
        }

        return Pair(
            probability / total,
            deterministic / total
        )
    }

    /**
     * Indica se existe uma calibração histórica
     * aceita e utilizável.
     */
    fun isCalibrated():
        Boolean {

        return provider
            .hasAcceptedCalibration()
    }

    /**
     * Aplica uma calibração produzida pelo
     * CalibrationEngine.
     *
     * A própria CalibrationState decide se ela
     * poderá ser considerada utilizável.
     */
    fun apply(
        result:
            CalibrationResult
    ):
        CalibrationState {

        return provider.applyResult(
            result
        )
    }

    /**
     * Remove a calibração armazenada.
     *
     * Depois disso o runtime retorna automaticamente
     * para 50/50.
     */
    fun reset() {

        provider.reset()
    }
}

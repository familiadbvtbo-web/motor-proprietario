package com.motorproprietario.app

import android.content.Context

/**
 * Fornece ao Motor os pesos da última calibração válida.
 *
 * Regra fundamental:
 *
 * - calibração aceita e válida -> usa os pesos calibrados;
 * - qualquer outro caso -> 50/50.
 *
 * Esta classe não executa calibração.
 * Ela somente lê o estado persistido.
 */
class CalibrationProvider(
    context: Context
) {

    private val store =
        CalibrationStore(
            context.applicationContext
        )

    /**
     * Retorna o estado atualmente utilizável.
     */
    fun getState():
        CalibrationState {

        val state =
            store.load()

        return if (
            state.isUsable()
        ) {
            state.normalized()
        } else {
            CalibrationState.initial()
        }
    }

    /**
     * Peso probabilístico atualmente utilizado.
     */
    fun probabilityWeight():
        Double {

        return getState()
            .probabilityWeight
    }

    /**
     * Peso determinístico atualmente utilizado.
     */
    fun deterministicWeight():
        Double {

        return getState()
            .deterministicWeight
    }

    /**
     * Retorna os dois pesos já normalizados.
     */
    fun weights():
        Pair<Double, Double> {

        val state =
            getState()

        return Pair(
            state.probabilityWeight,
            state.deterministicWeight
        )
    }

    /**
     * Informa se existe uma calibração aceita
     * e utilizável.
     */
    fun hasAcceptedCalibration():
        Boolean {

        return getState()
            .isUsable()
    }

    /**
     * Salva uma nova calibração somente depois
     * de convertê-la para CalibrationState.
     */
    fun applyResult(
        result:
            CalibrationResult
    ):
        CalibrationState {

        return store.saveResult(
            result
        )
    }

    /**
     * Remove a calibração persistida.
     *
     * Depois disso o Motor volta automaticamente
     * para 50/50.
     */
    fun reset() {

        store.clear()
    }
}

package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class EntryPlanInput(
    val direction: String,
    val currentPrice: Double,
    val metrics: QuantMetrics,
    val timeframe: String,
    val probability: Double,
    val deterministicConfidence: Double,
    val falseSignalRisk: Double,
    val now: Long
)

data class EntryPlanResult(
    val valid: Boolean,
    val timing: String,

    val entry: Double,
    val zoneLow: Double,
    val zoneHigh: Double,

    val stop: Double,

    val tp1: Double,
    val tp2: Double,
    val tp3: Double,

    val rr1: Double,
    val rr2: Double,
    val rr3: Double,

    val validityMinutes: Int,
    val expiresAt: Long,

    val reason: String
)

object EntryPlanEngine {

    private fun positive(
        value: Double
    ): Double {

        return if (
            value.isFinite() &&
            value > 0.0
        ) {
            value
        } else {
            0.0
        }
    }

    private fun clamp(
        value: Double,
        minimum: Double = 0.0,
        maximum: Double = 100.0
    ): Double {

        return value.coerceIn(
            minimum,
            maximum
        )
    }

    private fun roundPrice(
        value: Double
    ): Double {

        return String.format(
            java.util.Locale.US,
            "%.5f",
            value
        ).toDouble()
    }

    /*
     * Validade máxima do sinal.
     *
     * Não significa que a ordem deve permanecer
     * ativa obrigatoriamente durante esse período.
     *
     * É o prazo máximo para o cenário continuar
     * sendo considerado válido pelo Motor.
     */
    private fun validityFor(
        timeframe: String
    ): Int {

        return when (
            timeframe
        ) {

            "M1" ->
                5

            "M5" ->
                15

            "M15" ->
                30

            "M30" ->
                60

            "H1" ->
                120

            "H4" ->
                480

            "D1" ->
                1440

            else ->
                30
        }
    }

    fun calculate(
        input: EntryPlanInput
    ): EntryPlanResult {

        val price =
            positive(
                input.currentPrice
            )

        val atr =
            positive(
                input.metrics.atr
            )

        val probability =
            clamp(
                input.probability
            )

        val deterministic =
            clamp(
                input.deterministicConfidence
            )

        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val validityMinutes =
            validityFor(
                input.timeframe
            )

        val expiresAt =
            input.now +
                validityMinutes *
                60_000L

        /*
         * ==================================
         * VALIDAÇÃO BÁSICA
         * ==================================
         */

        if (
            price <= 0.0
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "PREÇO_INVALIDO"
            )
        }

        if (
            atr <= 0.0
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "ATR_INVALIDO"
            )
        }

        if (
            input.direction !=
                "COMPRA" &&
            input.direction !=
                "VENDA"
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "SEM_DIREÇÃO_OPERACIONAL"
            )
        }

        /*
         * ==================================
         * PROTEÇÃO CONTRA FALSO SINAL
         * ==================================
         */

        if (
            falseRisk >= 65.0
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "RISCO_DE_FALSO_SINAL_ELEVADO"
            )
        }

        /*
         * ==================================
         * DOMINÂNCIA MÍNIMA
         * ==================================
         */

        if (
            probability < 60.0
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "PROBABILIDADE_INSUFICIENTE"
            )
        }

        if (
            deterministic < 45.0
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "DETERMINISMO_INSUFICIENTE"
            )
        }

        /*
         * ==================================
         * ZONA DE ENTRADA
         * ==================================
         *
         * A zona utiliza o ATR para evitar
         * uma entrada excessivamente rígida.
         */

        val zoneHalf =
            max(
                atr * 0.12,
                price * 0.00003
            )

        val entry =
            price

        val zoneLow =
            entry -
                zoneHalf

        val zoneHigh =
            entry +
                zoneHalf

        /*
         * ==================================
         * BUFFER ESTRUTURAL
         * ==================================
         */

        val buffer =
            max(
                atr * 0.15,
                price * 0.00005
            )

        /*
         * ==================================
         * STOP
         * ==================================
         */

        val rawStop =
            if (
                input.direction ==
                    "COMPRA"
            ) {

                val support =
                    positive(
                        input.metrics.support
                    )

                val structuralStop =
                    if (
                        support > 0.0 &&
                        support < price
                    ) {

                        support -
                            buffer

                    } else {

                        price -
                            atr * 1.20
                    }

                /*
                 * Evita stop excessivamente próximo.
                 */
                min(
                    price -
                        atr * 0.85,

                    structuralStop
                )

            } else {

                val resistance =
                    positive(
                        input.metrics.resistance
                    )

                val structuralStop =
                    if (
                        resistance > price
                    ) {

                        resistance +
                            buffer

                    } else {

                        price +
                            atr * 1.20
                    }

                max(
                    price +
                        atr * 0.85,

                    structuralStop
                )
            }

        val stop =
            roundPrice(
                rawStop
            )

        /*
         * ==================================
         * RISCO
         * ==================================
         */

        val risk =
            abs(
                entry -
                    stop
            )

        if (
            risk <= 0.0 ||
            !risk.isFinite()
        ) {

            return invalid(
                price,
                validityMinutes,
                expiresAt,
                "RISCO_INVALIDO"
            )
        }

        /*
         * ==================================
         * TP
         * ==================================
         *
         * Relação:
         *
         * TP1 = 1.8R
         * TP2 = 3.0R
         * TP3 = 4.8R
         */

        val tp1 =
            if (
                input.direction ==
                    "COMPRA"
            ) {

                entry +
                    risk * 1.8

            } else {

                entry -
                    risk * 1.8
            }

        val tp2 =
            if (
                input.direction ==
                    "COMPRA"
            ) {

                entry +
                    risk * 3.0

            } else {

                entry -
                    risk * 3.0
            }

        val tp3 =
            if (
                input.direction ==
                    "COMPRA"
            ) {

                entry +
                    risk * 4.8

            } else {

                entry -
                    risk * 4.8
            }

        /*
         * ==================================
         * TIMING
         * ==================================
         */

        val timing =
            when {

                probability >=
                    80.0 &&
                deterministic >=
                    75.0 &&
                falseRisk < 35.0 ->

                    "ENTRADA FAVORÁVEL"

                probability >=
                    70.0 &&
                deterministic >=
                    60.0 &&
                falseRisk < 50.0 ->

                    "ENTRADA CONDICIONAL"

                probability >=
                    60.0 &&
                deterministic >=
                    45.0 ->

                    "AGUARDAR RETESTE"

                else ->

                    "AGUARDAR"
            }

        /*
         * ==================================
         * VALIDADE
         * ==================================
         *
         * Para evitar considerar qualquer plano
         * como executável indefinidamente.
         */

        val valid =
            timing ==
                "ENTRADA FAVORÁVEL" ||
            timing ==
                "ENTRADA CONDICIONAL"

        val reason =
            when (
                timing
            ) {

                "ENTRADA FAVORÁVEL" ->
                    "PROBABILIDADE + DETERMINISMO + FSI CONFIRMADOS"

                "ENTRADA CONDICIONAL" ->
                    "SINAL CONFIRMADO, AGUARDAR MELHOR TIMING"

                "AGUARDAR RETESTE" ->
                    "DOMINÂNCIA EXISTE, MAS TIMING AINDA NÃO É IDEAL"

                else ->
                    "CONDIÇÕES INSUFICIENTES"
            }

        return EntryPlanResult(

            valid =
                valid,

            timing =
                timing,

            entry =
                roundPrice(
                    entry
                ),

            zoneLow =
                roundPrice(
                    zoneLow
                ),

            zoneHigh =
                roundPrice(
                    zoneHigh
                ),

            stop =
                stop,

            tp1 =
                roundPrice(
                    tp1
                ),

            tp2 =
                roundPrice(
                    tp2
                ),

            tp3 =
                roundPrice(
                    tp3
                ),

            rr1 =
                1.8,

            rr2 =
                3.0,

            rr3 =
                4.8,

            validityMinutes =
                validityMinutes,

            expiresAt =
                expiresAt,

            reason =
                reason
        )
    }

    private fun invalid(
        price: Double,
        minutes: Int,
        expiry: Long,
        reason: String
    ): EntryPlanResult {

        return EntryPlanResult(

            valid =
                false,

            timing =
                "AGUARDAR",

            entry =
                price,

            zoneLow =
                price,

            zoneHigh =
                price,

            stop =
                price,

            tp1 =
                price,

            tp2 =
                price,

            tp3 =
                price,

            rr1 =
                0.0,

            rr2 =
                0.0,

            rr3 =
                0.0,

            validityMinutes =
                minutes,

            expiresAt =
                expiry,

            reason =
                reason
        )
    }
}

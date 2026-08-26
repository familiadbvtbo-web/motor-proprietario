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
    ): Double =
        if (
            value.isFinite() &&
            value > 0.0
        ) {
            value
        } else {
            0.0
        }

    private fun roundPrice(
        value: Double
    ): Double =
        String.format(
            "%.5f",
            value
        ).toDouble()

    private fun validityFor(
        timeframe: String
    ): Int =
        when (timeframe) {

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

        val minutes =
            validityFor(
                input.timeframe
            )

        val expiry =
            input.now +
                minutes *
                60_000L

        /*
         * O plano só é criado quando existe
         * evidência mínima nas duas lógicas.
         */
        if (
            price <= 0.0 ||
            atr <= 0.0 ||
            input.probability < 60.0 ||
            input.deterministicConfidence < 45.0 ||
            input.falseSignalRisk >= 65.0 ||
            input.direction !in
                setOf(
                    "COMPRA",
                    "VENDA"
                )
        ) {

            return invalid(
                price,
                minutes,
                expiry,
                "CONDIÇÕES INSUFICIENTES PARA PLANO DE ENTRADA"
            )
        }

        /*
         * Zona de entrada.
         */
        val buffer =
            max(
                atr * 0.15,
                price * 0.00005
            )

        val zoneHalf =
            max(
                atr * 0.12,
                price * 0.00003
            )

        val entry =
            price

        /*
         * STOP estrutural.
         *
         * COMPRA:
         * procura suporte abaixo do preço.
         *
         * VENDA:
         * procura resistência acima do preço.
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

        val risk =
            abs(
                entry -
                    stop
            )

        if (
            risk <= 0.0 ||
            !stop.isFinite()
        ) {

            return invalid(
                price,
                minutes,
                expiry,
                "DISTÂNCIA DE STOP INVÁLIDA"
            )
        }

        /*
         * TAKE PROFITS.
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
         * TIMING.
         *
         * A probabilidade sozinha não libera
         * entrada.
         *
         * O determinismo também precisa confirmar.
         */
        val timing =
            when {

                input.probability >=
                    80.0 &&
                input.deterministicConfidence >=
                    75.0 ->

                    "ENTRADA FAVORÁVEL"

                input.probability >=
                    70.0 &&
                input.deterministicConfidence >=
                    60.0 ->

                    "ENTRADA CONDICIONAL"

                else ->

                    "AGUARDAR RETESTE"
            }

        return EntryPlanResult(

            valid =
                timing !=
                    "AGUARDAR RETESTE",

            timing =
                timing,

            entry =
                roundPrice(
                    entry
                ),

            zoneLow =
                roundPrice(
                    entry -
                        zoneHalf
                ),

            zoneHigh =
                roundPrice(
                    entry +
                        zoneHalf
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
                minutes,

            expiresAt =
                expiry,

            reason =
                "PLANO BASEADO EM ATR + ESTRUTURA + DOMINÂNCIA"
        )
    }

    private fun invalid(
        price: Double,
        minutes: Int,
        expiry: Long,
        reason: String
    ): EntryPlanResult =

        EntryPlanResult(

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

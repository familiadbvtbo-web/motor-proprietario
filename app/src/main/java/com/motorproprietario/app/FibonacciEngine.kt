package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class FibonacciResult(
    val high: Double,
    val low: Double,

    val level236: Double,
    val level382: Double,
    val level500: Double,
    val level618: Double,
    val level786: Double,

    val extension1272: Double,
    val extension1618: Double,

    val currentPrice: Double,

    val zone: String,
    val bias: String,

    val bullishEvidence: Double,
    val bearishEvidence: Double,

    val strength: Double
)

object FibonacciEngine {

    private fun clamp(
        value: Double,
        minValue: Double = 0.0,
        maxValue: Double = 100.0
    ): Double {

        return value.coerceIn(
            minValue,
            maxValue
        )
    }

    private fun valid(
        value: Double
    ): Boolean {

        return value.isFinite() &&
            value > 0.0
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

    fun calculate(
        candles: List<MarketCandle>?,
        currentPrice: Double
    ): FibonacciResult {

        if (
            candles == null ||
            candles.size < 20 ||
            !valid(currentPrice)
        ) {

            return empty(
                currentPrice
            )
        }

        val window =
            candles.takeLast(
                min(
                    candles.size,
                    150
                )
            )

        val high =
            window.maxOf {
                it.high
            }

        val low =
            window.minOf {
                it.low
            }

        val range =
            high -
                low

        if (
            !valid(high) ||
            !valid(low) ||
            range <= 0.0
        ) {

            return empty(
                currentPrice
            )
        }

        /*
         * ==================================
         * RETRAÇÕES
         * ==================================
         */

        val level236 =
            high -
                range *
                0.236

        val level382 =
            high -
                range *
                0.382

        val level500 =
            high -
                range *
                0.500

        val level618 =
            high -
                range *
                0.618

        val level786 =
            high -
                range *
                0.786

        /*
         * ==================================
         * EXTENSÕES
         * ==================================
         */

        val extension1272 =
            high +
                range *
                0.272

        val extension1618 =
            high +
                range *
                0.618

        /*
         * ==================================
         * PROXIMIDADE DOS NÍVEIS
         * ==================================
         */

        val tolerance =
            max(
                range * 0.025,
                currentPrice * 0.00010
            )

        fun near(
            level: Double
        ): Boolean {

            return abs(
                currentPrice -
                    level
            ) <= tolerance
        }

        /*
         * ==================================
         * ZONA
         * ==================================
         */

        val zone =
            when {

                near(level236) ->
                    "23.6%"

                near(level382) ->
                    "38.2%"

                near(level500) ->
                    "50.0%"

                near(level618) ->
                    "61.8%"

                near(level786) ->
                    "78.6%"

                currentPrice >
                    extension1272 ->
                    "EXTENSÃO_127.2%+"

                currentPrice >
                    extension1618 ->
                    "EXTENSÃO_161.8%+"

                currentPrice >
                    high ->
                    "ACIMA_DA_MÁXIMA"

                currentPrice <
                    low ->
                    "ABAIXO_DA_MÍNIMA"

                else ->
                    "ENTRE_NÍVEIS"
            }

        /*
         * ==================================
         * EVIDÊNCIA DIRECIONAL
         * ==================================
         *
         * Fibonacci não determina direção
         * sozinho.
         *
         * Ele fornece evidência estrutural.
         */

        var bullish =
            50.0

        var bearish =
            50.0

        when (
            zone
        ) {

            "23.6%" -> {

                bullish +=
                    5.0

                bearish +=
                    15.0
            }

            "38.2%" -> {

                bullish +=
                    10.0

                bearish +=
                    10.0
            }

            "50.0%" -> {

                bullish +=
                    8.0

                bearish +=
                    8.0
            }

            "61.8%" -> {

                bullish +=
                    18.0

                bearish +=
                    18.0
            }

            "78.6%" -> {

                bullish +=
                    10.0

                bearish +=
                    10.0
            }

            "EXTENSÃO_127.2%+" -> {

                bullish +=
                    5.0

                bearish +=
                    20.0
            }

            "EXTENSÃO_161.8%+" -> {

                bullish +=
                    2.0

                bearish +=
                    25.0
            }

            "ACIMA_DA_MÁXIMA" -> {

                bullish +=
                    8.0
            }

            "ABAIXO_DA_MÍNIMA" -> {

                bearish +=
                    8.0
            }
        }

        /*
         * ==================================
         * POSIÇÃO NO RANGE
         * ==================================
         */

        val position =
            (
                currentPrice -
                    low
            ) /
                range

        if (
            position <= 0.25
        ) {

            bearish +=
                5.0

        } else if (
            position >= 0.75
        ) {

            bullish +=
                5.0
        }

        /*
         * ==================================
         * NORMALIZAÇÃO
         * ==================================
         */

        bullish =
            clamp(
                bullish
            )

        bearish =
            clamp(
                bearish
            )

        val difference =
            abs(
                bullish -
                    bearish
            )

        val strength =
            clamp(
                40.0 +
                    difference *
                    0.75
            )

        val bias =
            when {

                bullish >
                    bearish + 8.0 ->

                    "COMPRA"

                bearish >
                    bullish + 8.0 ->

                    "VENDA"

                else ->
                    "NEUTRO"
            }

        return FibonacciResult(

            high =
                roundPrice(
                    high
                ),

            low =
                roundPrice(
                    low
                ),

            level236 =
                roundPrice(
                    level236
                ),

            level382 =
                roundPrice(
                    level382
                ),

            level500 =
                roundPrice(
                    level500
                ),

            level618 =
                roundPrice(
                    level618
                ),

            level786 =
                roundPrice(
                    level786
                ),

            extension1272 =
                roundPrice(
                    extension1272
                ),

            extension1618 =
                roundPrice(
                    extension1618
                ),

            currentPrice =
                roundPrice(
                    currentPrice
                ),

            zone =
                zone,

            bias =
                bias,

            bullishEvidence =
                bullish,

            bearishEvidence =
                bearish,

            strength =
                strength
        )
    }

    private fun empty(
        currentPrice: Double
    ): FibonacciResult {

        return FibonacciResult(

            high =
                0.0,

            low =
                0.0,

            level236 =
                0.0,

            level382 =
                0.0,

            level500 =
                0.0,

            level618 =
                0.0,

            level786 =
                0.0,

            extension1272 =
                0.0,

            extension1618 =
                0.0,

            currentPrice =
                currentPrice,

            zone =
                "SEM_DADOS",

            bias =
                "NEUTRO",

            bullishEvidence =
                50.0,

            bearishEvidence =
                50.0,

            strength =
                0.0
        )
    }
}

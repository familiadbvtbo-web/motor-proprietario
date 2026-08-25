package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class FibonacciLevels(
    val swingHigh: Double,
    val swingLow: Double,
    val range: Double,
    val level236: Double,
    val level382: Double,
    val level500: Double,
    val level618: Double,
    val level786: Double
)

data class FibonacciResult(
    val levels: FibonacciLevels,
    val nearestLevel: Double,
    val distanceToNearest: Double,
    val bullishEvidence: Double,
    val bearishEvidence: Double,
    val neutralEvidence: Double,
    val zone: String,
    val bias: String
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

    /**
     * Procura o maior topo e o menor fundo
     * dentro de uma janela recente.
     *
     * Não tenta adivinhar o futuro:
     * utiliza somente candles disponíveis.
     */
    private fun findSwing(
        candles: List<MarketCandle>,
        lookback: Int
    ): Pair<Double, Double> {

        val usable =
            candles.takeLast(
                min(
                    lookback,
                    candles.size
                )
            )

        if (usable.isEmpty()) {
            return 0.0 to 0.0
        }

        val high =
            usable.maxOf {
                it.high
            }

        val low =
            usable.minOf {
                it.low
            }

        return high to low
    }

    private fun levels(
        high: Double,
        low: Double
    ): FibonacciLevels {

        val range =
            high - low

        if (range <= 0.0) {

            return FibonacciLevels(
                swingHigh = high,
                swingLow = low,
                range = 0.0,
                level236 = high,
                level382 = high,
                level500 = high,
                level618 = high,
                level786 = high
            )
        }

        /*
         * Consideramos uma estrutura de alta
         * do fundo para o topo.
         *
         * Os níveis representam retrações
         * a partir do topo.
         */
        return FibonacciLevels(
            swingHigh = high,
            swingLow = low,
            range = range,

            level236 =
                high -
                    range * 0.236,

            level382 =
                high -
                    range * 0.382,

            level500 =
                high -
                    range * 0.500,

            level618 =
                high -
                    range * 0.618,

            level786 =
                high -
                    range * 0.786
        )
    }

    private fun nearestLevel(
        price: Double,
        levels: FibonacciLevels
    ): Pair<Double, Double> {

        val candidates =
            listOf(
                levels.level236,
                levels.level382,
                levels.level500,
                levels.level618,
                levels.level786
            )

        val nearest =
            candidates.minByOrNull {
                abs(
                    price - it
                )
            } ?: price

        return nearest to
            abs(
                price - nearest
            )
    }

    /**
     * Analisa a posição atual do preço
     * em relação aos níveis de Fibonacci.
     */
    fun calculate(
        candles: List<MarketCandle>,
        price: Double,
        lookback: Int = 100
    ): FibonacciResult {

        if (
            candles.isEmpty() ||
            price <= 0.0
        ) {

            val emptyLevels =
                FibonacciLevels(
                    swingHigh = 0.0,
                    swingLow = 0.0,
                    range = 0.0,
                    level236 = 0.0,
                    level382 = 0.0,
                    level500 = 0.0,
                    level618 = 0.0,
                    level786 = 0.0
                )

            return FibonacciResult(
                levels = emptyLevels,
                nearestLevel = 0.0,
                distanceToNearest = 0.0,
                bullishEvidence = 50.0,
                bearishEvidence = 50.0,
                neutralEvidence = 100.0,
                zone = "SEM_DADOS",
                bias = "NEUTRO"
            )
        }

        val swing =
            findSwing(
                candles,
                lookback
            )

        val high =
            swing.first

        val low =
            swing.second

        val fib =
            levels(
                high,
                low
            )

        if (
            fib.range <= 0.0
        ) {

            return FibonacciResult(
                levels = fib,
                nearestLevel = price,
                distanceToNearest = 0.0,
                bullishEvidence = 50.0,
                bearishEvidence = 50.0,
                neutralEvidence = 100.0,
                zone = "SEM_RANGE",
                bias = "NEUTRO"
            )
        }

        val nearest =
            nearestLevel(
                price,
                fib
            )

        val nearestPrice =
            nearest.first

        val distance =
            nearest.second

        /*
         * A distância é normalizada pelo range
         * para funcionar em diferentes ativos.
         */
        val normalizedDistance =
            distance /
                fib.range

        /*
         * Tolerância para considerar que o preço
         * está realmente reagindo ao nível.
         */
        val tolerance =
            0.015

        val near236 =
            abs(
                price -
                    fib.level236
            ) /
                fib.range <= tolerance

        val near382 =
            abs(
                price -
                    fib.level382
            ) /
                fib.range <= tolerance

        val near500 =
            abs(
                price -
                    fib.level500
            ) /
                fib.range <= tolerance

        val near618 =
            abs(
                price -
                    fib.level618
            ) /
                fib.range <= tolerance

        val near786 =
            abs(
                price -
                    fib.level786
            ) /
                fib.range <= tolerance

        /*
         * Quanto mais próximo das zonas 38.2,
         * 50 e 61.8, maior a relevância estrutural.
         */
        var bullish =
            50.0

        var bearish =
            50.0

        var neutral =
            10.0

        when {

            near236 -> {
                bullish += 5.0
                bearish += 5.0
                neutral += 10.0
            }

            near382 -> {
                bullish += 15.0
                bearish += 15.0
            }

            near500 -> {
                bullish += 20.0
                bearish += 20.0
            }

            near618 -> {
                bullish += 25.0
                bearish += 25.0
            }

            near786 -> {
                bullish += 15.0
                bearish += 15.0
            }
        }

        /*
         * Posição do preço dentro do range.
         *
         * Acima de 61.8%:
         * estrutura mais próxima do topo.
         *
         * Abaixo de 38.2%:
         * estrutura mais próxima do fundo.
         */
        val position =
            (
                price -
                    low
            ) /
                fib.range

        when {

            position >= 0.786 -> {
                bullish += 18.0
                neutral += 5.0
            }

            position >= 0.618 -> {
                bullish += 15.0
            }

            position >= 0.500 -> {
                bullish += 8.0
            }

            position <= 0.236 -> {
                bearish += 18.0
                neutral += 5.0
            }

            position <= 0.382 -> {
                bearish += 15.0
            }

            position <= 0.500 -> {
                bearish += 8.0
            }

            else -> {
                neutral += 12.0
            }
        }

        /*
         * Verifica a última vela para procurar
         * rejeição simples do nível.
         */
        val last =
            candles.last()

        val body =
            abs(
                last.close -
                    last.open
            )

        val upperWick =
            last.high -
                max(
                    last.open,
                    last.close
                )

        val lowerWick =
            min(
                last.open,
                last.close
            ) -
                last.low

        val candleRange =
            last.high -
                last.low

        if (
            candleRange > 0.0
        ) {

            /*
             * Rejeição inferior próxima a Fibonacci:
             * evidência compradora.
             */
            if (
                lowerWick >
                    body * 1.5 &&
                normalizedDistance <=
                    tolerance
            ) {
                bullish += 12.0
            }

            /*
             * Rejeição superior próxima a Fibonacci:
             * evidência vendedora.
             */
            if (
                upperWick >
                    body * 1.5 &&
                normalizedDistance <=
                    tolerance
            ) {
                bearish += 12.0
            }
        }

        bullish =
            clamp(
                bullish
            )

        bearish =
            clamp(
                bearish
            )

        neutral =
            clamp(
                neutral
            )

        val bias =
            when {

                bullish >
                    bearish + 10.0 ->
                    "COMPRA"

                bearish >
                    bullish + 10.0 ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        val zone =
            when {

                near236 ->
                    "FIB_23_6"

                near382 ->
                    "FIB_38_2"

                near500 ->
                    "FIB_50_0"

                near618 ->
                    "FIB_61_8"

                near786 ->
                    "FIB_78_6"

                position > 0.786 ->
                    "ACIMA_78_6"

                position > 0.618 ->
                    "ENTRE_61_8_78_6"

                position > 0.500 ->
                    "ENTRE_50_61_8"

                position > 0.382 ->
                    "ENTRE_38_2_50"

                position > 0.236 ->
                    "ENTRE_23_6_38_2"

                else ->
                    "ABAIXO_23_6"
            }

        return FibonacciResult(
            levels = fib,
            nearestLevel = nearestPrice,
            distanceToNearest = distance,
            bullishEvidence = bullish,
            bearishEvidence = bearish,
            neutralEvidence = neutral,
            zone = zone,
            bias = bias
        )
    }
}

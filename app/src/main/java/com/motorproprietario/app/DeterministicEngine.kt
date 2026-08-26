package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class DeterministicInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val falseSignalRisk: Double,

    /*
     * Contexto opcional de preço.
     */
    val currentPrice: Double = 0.0,

    /*
     * Métricas de outros timeframes.
     */
    val higherTimeframes: List<QuantMetrics> = emptyList()
)

data class DeterministicResult(
    val buyScore: Double,
    val sellScore: Double,
    val neutralScore: Double,

    val directionalBias: String,

    val confidence: Double,

    val trapRisk: Double,

    val expansion: Double,

    val accumulation: Double,

    val distribution: Double,

    val exhaustion: Double,

    val liquidityPressure: Double,

    val realizationRisk: Double,

    val timeframeConflict: Double,

    val confirmation: Double
)

object DeterministicEngine {

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

    private fun directional(
        value: Double
    ): Double {

        return clamp(value)
    }

    private fun average(
        values: List<Double>
    ): Double {

        return if (
            values.isEmpty()
        ) {
            50.0
        } else {
            values.average()
        }
    }

    /*
     * ------------------------------------------------
     * 1. PRESSÃO COMPRADORA
     * ------------------------------------------------
     */

    private fun buyPressure(
        m: QuantMetrics
    ): Double {

        val trend =
            directional(m.trend)

        val momentum =
            directional(m.momentum)

        val structure =
            directional(m.structure)

        val candle =
            directional(m.candlePattern)

        val breakout =
            directional(m.breakout)

        val volume =
            directional(m.volume)

        val ema =
            if (
                m.ema9 > m.ema21 &&
                m.ema21 >= m.ema50
            ) {
                90.0
            } else if (
                m.ema9 > m.ema21
            ) {
                70.0
            } else {
                30.0
            }

        val macd =
            if (
                m.macd > m.macdSignal
            ) {
                70.0
            } else {
                30.0
            }

        return clamp(
            trend * 0.18 +
            momentum * 0.12 +
            structure * 0.18 +
            candle * 0.10 +
            breakout * 0.12 +
            volume * 0.10 +
            ema * 0.10 +
            macd * 0.10
        )
    }

    /*
     * ------------------------------------------------
     * 2. PRESSÃO VENDEDORA
     * ------------------------------------------------
     */

    private fun sellPressure(
        m: QuantMetrics
    ): Double {

        val trend =
            100.0 -
                directional(m.trend)

        val momentum =
            100.0 -
                directional(m.momentum)

        val structure =
            100.0 -
                directional(m.structure)

        val candle =
            100.0 -
                directional(m.candlePattern)

        val breakout =
            100.0 -
                directional(m.breakout)

        val volume =
            directional(m.volume)

        val ema =
            if (
                m.ema9 < m.ema21 &&
                m.ema21 <= m.ema50
            ) {
                90.0
            } else if (
                m.ema9 < m.ema21
            ) {
                70.0
            } else {
                30.0
            }

        val macd =
            if (
                m.macd < m.macdSignal
            ) {
                70.0
            } else {
                30.0
            }

        return clamp(
            trend * 0.18 +
            momentum * 0.12 +
            structure * 0.18 +
            candle * 0.10 +
            breakout * 0.12 +
            volume * 0.10 +
            ema * 0.10 +
            macd * 0.10
        )
    }

    /*
     * ------------------------------------------------
     * 3. ACUMULAÇÃO
     *
     * Detecta condições compatíveis com:
     * preço comprimido + estrutura + volume
     * sem exigir que isso seja tratado como
     * prova de atuação institucional.
     * ------------------------------------------------
     */

    private fun accumulation(
        m: QuantMetrics
    ): Double {

        val structure =
            100.0 -
                abs(
                    m.structure -
                        50.0
                ) * 2.0

        val volatilityCompression =
            100.0 -
                m.volatility

        val volumePresence =
            m.volume

        val breakoutPreparation =
            100.0 -
                abs(
                    m.breakout -
                        50.0
                ) * 2.0

        return clamp(
            structure * 0.30 +
            volatilityCompression * 0.25 +
            volumePresence * 0.20 +
            breakoutPreparation * 0.25
        )
    }

    /*
     * ------------------------------------------------
     * 4. DISTRIBUIÇÃO
     * ------------------------------------------------
     */

    private fun distribution(
        m: QuantMetrics
    ): Double {

        val structureExtreme =
            abs(
                m.structure -
                    50.0
            ) * 2.0

        val volatility =
            m.volatility

        val divergence =
            abs(
                m.divergence -
                    50.0
            ) * 2.0

        val momentumExtreme =
            abs(
                m.momentum -
                    50.0
            ) * 2.0

        return clamp(
            structureExtreme * 0.25 +
            volatility * 0.20 +
            divergence * 0.30 +
            momentumExtreme * 0.25
        )
    }

    /*
     * ------------------------------------------------
     * 5. EXPANSÃO
     * ------------------------------------------------
     */

    private fun expansion(
        m: QuantMetrics
    ): Double {

        val breakoutStrength =
            abs(
                m.breakout -
                    50.0
            ) * 2.0

        val volumeExpansion =
            m.volume

        val volatilityExpansion =
            m.volatility

        val trendStrength =
            m.adx

        return clamp(
            breakoutStrength * 0.35 +
            volumeExpansion * 0.20 +
            volatilityExpansion * 0.20 +
            trendStrength * 0.25
        )
    }

    /*
     * ------------------------------------------------
     * 6. EXAUSTÃO
     * ------------------------------------------------
     */

    private fun exhaustion(
        m: QuantMetrics
    ): Double {

        val rsiExtreme =
            when {

                m.rsi >= 75.0 ->
                    90.0

                m.rsi >= 70.0 ->
                    75.0

                m.rsi <= 25.0 ->
                    90.0

                m.rsi <= 30.0 ->
                    75.0

                else ->
                    20.0
            }

        val divergence =
            abs(
                m.divergence -
                    50.0
            ) * 2.0

        val volatility =
            m.volatility

        val candleRejection =
            when {

                m.candlePattern >= 75.0 ->
                    70.0

                m.candlePattern <= 25.0 ->
                    70.0

                else ->
                    20.0
            }

        return clamp(
            rsiExtreme * 0.30 +
            divergence * 0.30 +
            volatility * 0.15 +
            candleRejection * 0.25
        )
    }

    /*
     * ------------------------------------------------
     * 7. LIQUIDEZ / PRESSÃO
     * ------------------------------------------------
     */

    private fun liquidityPressure(
        m: QuantMetrics
    ): Double {

        val breakout =
            abs(
                m.breakout -
                    50.0
            ) * 2.0

        val volume =
            m.volume

        val structure =
            abs(
                m.structure -
                    50.0
            ) * 2.0

        return clamp(
            breakout * 0.40 +
            volume * 0.30 +
            structure * 0.30
        )
    }

    /*
     * ------------------------------------------------
     * 8. RISCO DE REALIZAÇÃO
     * ------------------------------------------------
     */

    private fun realizationRisk(
        m: QuantMetrics
    ): Double {

        val exhaustionValue =
            exhaustion(m)

        val divergence =
            abs(
                m.divergence -
                    50.0
            ) * 2.0

        val volatility =
            m.volatility

        return clamp(
            exhaustionValue * 0.45 +
            divergence * 0.30 +
            volatility * 0.25
        )
    }

    /*
     * ------------------------------------------------
     * 9. CONFLITO ENTRE TIMEFRAMES
     * ------------------------------------------------
     */

    private fun timeframeConflict(
        m: QuantMetrics,
        higher:
            List<QuantMetrics>
    ): Double {

        if (
            higher.isEmpty()
        ) {
            return 0.0
        }

        val currentDirection =
            when {

                m.trend >= 60.0 ->
                    1

                m.trend <= 40.0 ->
                    -1

                else ->
                    0
            }

        if (
            currentDirection == 0
        ) {
            return 50.0
        }

        var conflicts =
            0

        for (
            h in higher
        ) {

            val direction =
                when {

                    h.trend >= 60.0 ->
                        1

                    h.trend <= 40.0 ->
                        -1

                    else ->
                        0
                }

            if (
                direction != 0 &&
                direction !=
                    currentDirection
            ) {
                conflicts++
            }
        }

        return clamp(
            conflicts.toDouble() /
                higher.size *
                100.0
        )
    }

    /*
     * ------------------------------------------------
     * 10. ARMADILHA / FALSO SINAL
     * ------------------------------------------------
     *
     * Não significa "o mercado quer enganar".
     *
     * Significa que os dados apresentam
     * características compatíveis com uma
     * ruptura/reversão pouco confiável.
     * ------------------------------------------------
     */

    private fun trapRisk(
        m: QuantMetrics,
        falseSignalRisk: Double,
        conflict: Double
    ): Double {

        val rejection =
            when {

                m.candlePattern >= 75.0 ->
                    70.0

                m.candlePattern <= 25.0 ->
                    70.0

                else ->
                    20.0
            }

        val divergence =
            abs(
                m.divergence -
                    50.0
            ) * 2.0

        val weakBreakout =
            if (
                abs(
                    m.breakout -
                        50.0
                ) > 30.0 &&
                m.volume < 45.0
            ) {
                85.0
            } else {
                20.0
            }

        return clamp(
            falseSignalRisk * 0.35 +
            conflict * 0.20 +
            divergence * 0.20 +
            rejection * 0.10 +
            weakBreakout * 0.15
        )
    }

    /*
     * ------------------------------------------------
     * 11. CONFIRMAÇÃO DETERMINÍSTICA
     * ------------------------------------------------
     */

    private fun confirmation(
        buy: Double,
        sell: Double,
        expansion: Double,
        accumulation: Double,
        distribution: Double,
        trap: Double,
        mtf: Double
    ): Double {

        val directional =
            abs(
                buy -
                    sell
            )

        val structural =
            max(
                accumulation,
                distribution
            )

        return clamp(
            directional * 0.25 +
            expansion * 0.20 +
            structural * 0.15 +
            mtf * 0.25 +
            (
                100.0 -
                    trap
            ) * 0.15
        )
    }

    /*
     * ------------------------------------------------
     * MOTOR PRINCIPAL
     * ------------------------------------------------
     */

    fun calculate(
        input: DeterministicInput
    ): DeterministicResult {

        val m =
            input.metrics

        val mtf =
            clamp(
                input.mtfConfluence
            )

        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        val buy =
            buyPressure(m)

        val sell =
            sellPressure(m)

        val accumulationValue =
            accumulation(m)

        val distributionValue =
            distribution(m)

        val expansionValue =
            expansion(m)

        val exhaustionValue =
            exhaustion(m)

        val liquidityValue =
            liquidityPressure(m)

        val conflict =
            timeframeConflict(
                m,
                input.higherTimeframes
            )

        val trap =
            trapRisk(
                m,
                falseRisk,
                conflict
            )

        val realization =
            realizationRisk(m)

        /*
         * Penalização determinística.
         *
         * Quanto maior a possibilidade
         * de armadilha/exaustão,
         * menor a força direcional.
         */

        var adjustedBuy =
            buy

        var adjustedSell =
            sell

        adjustedBuy *=
            1.0 -
                trap / 200.0

        adjustedSell *=
            1.0 -
                trap / 200.0

        adjustedBuy *=
            1.0 -
                realization / 300.0

        adjustedSell *=
            1.0 -
                realization / 300.0

        /*
         * Conflito MTF reduz convicção.
         */

        adjustedBuy *=
            1.0 -
                conflict / 250.0

        adjustedSell *=
            1.0 -
                conflict / 250.0

        /*
         * Determinação do lado dominante.
         */

        val difference =
            abs(
                adjustedBuy -
                    adjustedSell
            )

        var neutral =
            100.0 -
                difference

        /*
         * Mercado muito arriscado
         * aumenta neutralidade.
         */

        neutral +=
            trap * 0.35

        neutral +=
            conflict * 0.20

        neutral =
            clamp(
                neutral,
                5.0,
                100.0
            )

        /*
         * Normalização.
         */

        val total =
            adjustedBuy +
                adjustedSell +
                neutral

        val buyProbability =
            if (
                total > 0.0
            ) {
                adjustedBuy /
                    total *
                    100.0
            } else {
                33.33
            }

        val sellProbability =
            if (
                total > 0.0
            ) {
                adjustedSell /
                    total *
                    100.0
            } else {
                33.33
            }

        val neutralProbability =
            if (
                total > 0.0
            ) {
                neutral /
                    total *
                    100.0
            } else {
                33.34
            }

        val directionalBias =
            when {

                buyProbability >=
                    sellProbability &&
                buyProbability >=
                    neutralProbability ->
                    "COMPRA"

                sellProbability >=
                    buyProbability &&
                sellProbability >=
                    neutralProbability ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        val confirmation =
            confirmation(
                buy =
                    buyProbability,

                sell =
                    sellProbability,

                expansion =
                    expansionValue,

                accumulation =
                    accumulationValue,

                distribution =
                    distributionValue,

                trap =
                    trap,

                mtf =
                    mtf
            )

        val confidence =
            clamp(
                (
                    max(
                        buyProbability,
                        sellProbability
                    ) -
                        neutralProbability
                ) *
                    1.10 +
                confirmation * 0.30 -
                trap * 0.25
            )

        return DeterministicResult(

            buyScore =
                clamp(
                    buyProbability
                ),

            sellScore =
                clamp(
                    sellProbability
                ),

            neutralScore =
                clamp(
                    neutralProbability
                ),

            directionalBias =
                directionalBias,

            confidence =
                confidence,

            trapRisk =
                trap,

            expansion =
                expansionValue,

            accumulation =
                accumulationValue,

            distribution =
                distributionValue,

            exhaustion =
                exhaustionValue,

            liquidityPressure =
                liquidityValue,

            realizationRisk =
                realization,

            timeframeConflict =
                conflict,

            confirmation =
                confirmation
        )
    }
}

package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max

data class DeterministicInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val falseSignalRisk: Double,
    val currentPrice: Double = 0.0,
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

        if (!value.isFinite()) {
            return minValue
        }

        return value.coerceIn(
            minValue,
            maxValue
        )
    }

    private fun average(
        values: List<Double>
    ): Double {

        val valid =
            values.filter {
                it.isFinite()
            }

        return if (
            valid.isEmpty()
        ) {
            50.0
        } else {
            valid.average()
        }
    }

    /*
     * ============================================================
     * PRESSÃO COMPRADORA
     * ============================================================
     */

    private fun buyPressure(
        m: QuantMetrics
    ): Double {

        val ema =
            when {

                m.ema9 > m.ema21 &&
                    m.ema21 > m.ema50 ->
                    90.0

                m.ema9 > m.ema21 ->
                    70.0

                m.ema9 < m.ema21 ->
                    30.0

                else ->
                    50.0
            }

        val macd =
            when {

                m.macd > m.macdSignal ->
                    70.0

                m.macd < m.macdSignal ->
                    30.0

                else ->
                    50.0
            }

        /*
         * Volume sozinho não indica compra.
         * Ele apenas confirma quando existe direção.
         */

        val volumeConfirmation =
            when {

                m.volume >= 60.0 &&
                    (
                        m.trend >= 55.0 ||
                        m.momentum >= 55.0 ||
                        m.breakout >= 55.0
                    ) ->
                    m.volume

                else ->
                    50.0
            }

        return clamp(

            m.trend * 0.20 +

            m.momentum * 0.12 +

            m.structure * 0.16 +

            m.candlePattern * 0.10 +

            m.breakout * 0.12 +

            volumeConfirmation * 0.08 +

            ema * 0.12 +

            macd * 0.10
        )
    }

    /*
     * ============================================================
     * PRESSÃO VENDEDORA
     * ============================================================
     */

    private fun sellPressure(
        m: QuantMetrics
    ): Double {

        val trend =
            100.0 -
                clamp(
                    m.trend
                )

        val momentum =
            100.0 -
                clamp(
                    m.momentum
                )

        val structure =
            100.0 -
                clamp(
                    m.structure
                )

        val candle =
            100.0 -
                clamp(
                    m.candlePattern
                )

        val breakout =
            100.0 -
                clamp(
                    m.breakout
                )

        val volumeConfirmation =
            when {

                m.volume >= 60.0 &&
                    (
                        m.trend <= 45.0 ||
                        m.momentum <= 45.0 ||
                        m.breakout <= 45.0
                    ) ->
                    m.volume

                else ->
                    50.0
            }

        val ema =
            when {

                m.ema9 < m.ema21 &&
                    m.ema21 < m.ema50 ->
                    90.0

                m.ema9 < m.ema21 ->
                    70.0

                m.ema9 > m.ema21 ->
                    30.0

                else ->
                    50.0
            }

        val macd =
            when {

                m.macd < m.macdSignal ->
                    70.0

                m.macd > m.macdSignal ->
                    30.0

                else ->
                    50.0
            }

        return clamp(

            trend * 0.20 +

            momentum * 0.12 +

            structure * 0.16 +

            candle * 0.10 +

            breakout * 0.12 +

            volumeConfirmation * 0.08 +

            ema * 0.12 +

            macd * 0.10
        )
    }

    /*
     * ============================================================
     * ACUMULAÇÃO
     * ============================================================
     *
     * Não significa "institucional comprando".
     * É apenas uma leitura quantitativa de compressão +
     * estabilidade + atividade.
     */

    private fun accumulation(
        m: QuantMetrics
    ): Double {

        val structureStability =
            clamp(
                100.0 -
                    abs(
                        m.structure -
                            50.0
                    ) * 2.0
            )

        val compression =
            clamp(
                100.0 -
                    m.volatility
            )

        val volumePresence =
            clamp(
                m.volume
            )

        val breakoutPreparation =
            clamp(
                100.0 -
                    abs(
                        m.breakout -
                            50.0
                    ) * 2.0
            )

        return clamp(

            structureStability * 0.30 +

            compression * 0.25 +

            volumePresence * 0.20 +

            breakoutPreparation * 0.25
        )
    }

    /*
     * ============================================================
     * DISTRIBUIÇÃO
     * ============================================================
     */

    private fun distribution(
        m: QuantMetrics
    ): Double {

        val structureExtreme =
            clamp(
                abs(
                    m.structure -
                        50.0
                ) * 2.0
            )

        val volatility =
            clamp(
                m.volatility
            )

        val divergence =
            clamp(
                abs(
                    m.divergence -
                        50.0
                ) * 2.0
            )

        val momentumExtreme =
            clamp(
                abs(
                    m.momentum -
                        50.0
                ) * 2.0
            )

        return clamp(

            structureExtreme * 0.25 +

            volatility * 0.20 +

            divergence * 0.30 +

            momentumExtreme * 0.25
        )
    }

    /*
     * ============================================================
     * EXPANSÃO
     * ============================================================
     */

    private fun expansion(
        m: QuantMetrics
    ): Double {

        val breakoutStrength =
            clamp(
                abs(
                    m.breakout -
                        50.0
                ) * 2.0
            )

        val volumeExpansion =
            clamp(
                m.volume
            )

        val volatilityExpansion =
            clamp(
                m.volatility
            )

        val trendStrength =
            clamp(
                m.adx
            )

        return clamp(

            breakoutStrength * 0.35 +

            volumeExpansion * 0.20 +

            volatilityExpansion * 0.20 +

            trendStrength * 0.25
        )
    }

    /*
     * ============================================================
     * EXAUSTÃO
     * ============================================================
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
            clamp(
                abs(
                    m.divergence -
                        50.0
                ) * 2.0
            )

        val volatility =
            clamp(
                m.volatility
            )

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
     * ============================================================
     * LIQUIDEZ / PRESSÃO
     * ============================================================
     */

    private fun liquidityPressure(
        m: QuantMetrics
    ): Double {

        val breakout =
            clamp(
                abs(
                    m.breakout -
                        50.0
                ) * 2.0
            )

        val volume =
            clamp(
                m.volume
            )

        val structure =
            clamp(
                abs(
                    m.structure -
                        50.0
                ) * 2.0
            )

        return clamp(

            breakout * 0.40 +

            volume * 0.30 +

            structure * 0.30
        )
    }

    /*
     * ============================================================
     * RISCO DE REALIZAÇÃO
     * ============================================================
     */

    private fun realizationRisk(
        m: QuantMetrics
    ): Double {

        val exhaustionValue =
            exhaustion(
                m
            )

        val divergence =
            clamp(
                abs(
                    m.divergence -
                        50.0
                ) * 2.0
            )

        val volatility =
            clamp(
                m.volatility
            )

        return clamp(

            exhaustionValue * 0.45 +

            divergence * 0.30 +

            volatility * 0.25
        )
    }

    /*
     * ============================================================
     * CONFLITO ENTRE TIMEFRAMES
     * ============================================================
     */

    private fun timeframeConflict(
        m: QuantMetrics,
        higher: List<QuantMetrics>
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

        var directionalHigher =
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
                direction != 0
            ) {

                directionalHigher++

                if (
                    direction !=
                        currentDirection
                ) {

                    conflicts++
                }
            }
        }

        if (
            directionalHigher == 0
        ) {
            return 50.0
        }

        return clamp(
            conflicts.toDouble() /
                directionalHigher.toDouble() *
                100.0
        )
    }

    /*
     * ============================================================
     * ARMADILHA / FALSO SINAL
     * ============================================================
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
            clamp(
                abs(
                    m.divergence -
                        50.0
                ) * 2.0
            )

        val weakBreakout =
            if (
                abs(
                    m.breakout -
                        50.0
                ) >= 30.0 &&
                m.volume < 45.0
            ) {

                85.0

            } else {

                20.0
            }

        return clamp(

            clamp(
                falseSignalRisk
            ) * 0.35 +

            clamp(
                conflict
            ) * 0.20 +

            divergence * 0.20 +

            rejection * 0.10 +

            weakBreakout * 0.15
        )
    }

    /*
     * ============================================================
     * CONFIRMAÇÃO
     * ============================================================
     */

    private fun confirmation(
        buy: Double,
        sell: Double,
        expansion: Double,
        accumulation: Double,
        distribution: Double,
        exhaustion: Double,
        trap: Double,
        realization: Double,
        mtf: Double
    ): Double {

        val directionalAgreement =
            clamp(
                abs(
                    buy -
                        sell
                )
            )

        val preparation =
            max(
                accumulation,
                expansion
            )

        val riskPenalty =
            average(
                listOf(
                    trap,
                    exhaustion,
                    realization
                )
            )

        return clamp(

            directionalAgreement * 0.25 +

            expansion * 0.15 +

            preparation * 0.05 +

            (
                100.0 -
                    distribution
            ) * 0.05 +

            mtf * 0.25 +

            (
                100.0 -
                    riskPenalty
            ) * 0.15 +

            (
                100.0 -
                    trap
            ) * 0.10
        )
    }

    /*
     * ============================================================
     * MOTOR PRINCIPAL
     * ============================================================
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

        /*
         * --------------------------------------------------------
         * PRESSÕES PRIMÁRIAS
         * --------------------------------------------------------
         */

        val buy =
            buyPressure(
                m
            )

        val sell =
            sellPressure(
                m
            )

        /*
         * --------------------------------------------------------
         * FATORES ESTRUTURAIS
         * --------------------------------------------------------
         */

        val accumulationValue =
            accumulation(
                m
            )

        val distributionValue =
            distribution(
                m
            )

        val expansionValue =
            expansion(
                m
            )

        val exhaustionValue =
            exhaustion(
                m
            )

        val liquidityValue =
            liquidityPressure(
                m
            )

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
            realizationRisk(
                m
            )

        /*
         * --------------------------------------------------------
         * AJUSTE DIRECIONAL
         * --------------------------------------------------------
         */

        var adjustedBuy =
            buy

        var adjustedSell =
            sell

        /*
         * Acumulação não cria direção.
         * Apenas reforça o lado que já possui vantagem.
         */

        val accumulationBoost =
            accumulationValue *
                0.08

        when {

            buy > sell ->
                adjustedBuy +=
                    accumulationBoost

            sell > buy ->
                adjustedSell +=
                    accumulationBoost
        }

        /*
         * Expansão também não cria direção.
         */

        val expansionBoost =
            expansionValue *
                0.10

        when {

            buy > sell ->
                adjustedBuy +=
                    expansionBoost

            sell > buy ->
                adjustedSell +=
                    expansionBoost
        }

        /*
         * Distribuição reduz a confiança,
         * mas não transforma automaticamente
         * o mercado em venda.
         */

        val distributionPenalty =
            distributionValue *
                0.10

        adjustedBuy -=
            distributionPenalty

        adjustedSell -=
            distributionPenalty

        /*
         * Exaustão reduz os dois lados.
         */

        val exhaustionFactor =
            (
                1.0 -
                    exhaustionValue /
                    250.0
            ).coerceIn(
                0.55,
                1.0
            )

        adjustedBuy *=
            exhaustionFactor

        adjustedSell *=
            exhaustionFactor

        /*
         * Armadilha reduz confiança.
         */

        val trapFactor =
            (
                1.0 -
                    trap /
                    220.0
            ).coerceIn(
                0.50,
                1.0
            )

        adjustedBuy *=
            trapFactor

        adjustedSell *=
            trapFactor

        /*
         * Realização reduz continuidade.
         */

        val realizationFactor =
            (
                1.0 -
                    realization /
                    300.0
            ).coerceIn(
                0.60,
                1.0
            )

        adjustedBuy *=
            realizationFactor

        adjustedSell *=
            realizationFactor

        /*
         * Conflito entre timeframes reduz a força,
         * mas não destrói completamente a direção.
         */

        val conflictFactor =
            (
                1.0 -
                    conflict /
                    250.0
            ).coerceIn(
                0.60,
                1.0
            )

        adjustedBuy *=
            conflictFactor

        adjustedSell *=
            conflictFactor

        /*
         * Liquidez só confirma o lado já dominante.
         */

        val liquidityConfirmation =
            liquidityValue *
                0.06

        when {

            adjustedBuy > adjustedSell ->
                adjustedBuy +=
                    liquidityConfirmation

            adjustedSell > adjustedBuy ->
                adjustedSell +=
                    liquidityConfirmation
        }

        adjustedBuy =
            max(
                0.0,
                adjustedBuy
            )

        adjustedSell =
            max(
                0.0,
                adjustedSell
            )

        /*
         * ========================================================
         * NOVO CÁLCULO DE NEUTRALIDADE
         * ========================================================
         *
         * A neutralidade NÃO recebe simplesmente:
         *
         *     100 - diferença
         *
         * porque isso fazia o neutro crescer demais.
         *
         * Agora ele depende de:
         *
         * 1. equilíbrio entre compra/venda;
         * 2. conflito MTF;
         * 3. risco de falso sinal;
         * 4. exaustão;
         * 5. falta de expansão.
         */

        val directionalDifference =
            abs(
                adjustedBuy -
                    adjustedSell
            )

        val balanceNeutral =
            when {

                directionalDifference <= 3.0 ->
                    60.0

                directionalDifference <= 7.0 ->
                    42.0

                directionalDifference <= 12.0 ->
                    25.0

                directionalDifference <= 20.0 ->
                    12.0

                else ->
                    4.0
            }

        val riskNeutral =
            (
                trap * 0.30 +
                conflict * 0.20 +
                exhaustionValue * 0.15 +
                realization * 0.10
            )

        val lowExpansionNeutral =
            if (
                expansionValue < 35.0
            ) {

                (
                    35.0 -
                        expansionValue
                ) *
                    0.45

            } else {

                0.0
            }

        val weakMtfNeutral =
            if (
                mtf < 50.0
            ) {

                (
                    50.0 -
                        mtf
                ) *
                    0.20

            } else {

                0.0
            }

        var neutral =
            balanceNeutral +
            riskNeutral * 0.35 +
            lowExpansionNeutral +
            weakMtfNeutral

        /*
         * Limites mais controlados.
         *
         * Neutro pode ser alto quando realmente
         * existe conflito/risco, mas não domina
         * artificialmente qualquer cenário.
         */

        neutral =
            clamp(
                neutral,
                4.0,
                75.0
            )

        /*
         * ========================================================
         * NORMALIZAÇÃO
         * ========================================================
         */

        val total =
            adjustedBuy +
                adjustedSell +
                neutral

        if (
            !total.isFinite() ||
            total <= 0.0
        ) {

            return DeterministicResult(

                buyScore =
                    33.33,

                sellScore =
                    33.33,

                neutralScore =
                    33.34,

                directionalBias =
                    "NEUTRO",

                confidence =
                    0.0,

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
                    0.0
            )
        }

        val buyProbability =
            adjustedBuy /
                total *
                100.0

        val sellProbability =
            adjustedSell /
                total *
                100.0

        val neutralProbability =
            neutral /
                total *
                100.0

        /*
         * ========================================================
         * DIREÇÃO
         * ========================================================
         *
         * Exige vantagem real sobre o neutro.
         */

        val directionalBias =
            when {

                buyProbability >= 55.0 &&
                    buyProbability >
                    sellProbability + 3.0 &&
                    buyProbability >
                    neutralProbability ->
                    "COMPRA"

                sellProbability >= 55.0 &&
                    sellProbability >
                    buyProbability + 3.0 &&
                    sellProbability >
                    neutralProbability ->
                    "VENDA"

                else ->
                    "NEUTRO"
            }

        /*
         * ========================================================
         * CONFIRMAÇÃO
         * ========================================================
         */

        val confirmationValue =
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

                exhaustion =
                    exhaustionValue,

                trap =
                    trap,

                realization =
                    realization,

                mtf =
                    mtf
            )

        /*
         * ========================================================
         * CONFIANÇA
         * ========================================================
         *
         * Não representa taxa histórica de acerto.
         */

        val confidence =
            clamp(

                confirmationValue * 0.40 +

                mtf * 0.20 +

                expansionValue * 0.10 +

                accumulationValue * 0.05 +

                liquidityValue * 0.05 +

                (
                    100.0 -
                        trap
                ) * 0.10 +

                (
                    100.0 -
                        exhaustionValue
                ) * 0.05 +

                (
                    100.0 -
                        conflict
                ) * 0.05
            )

        /*
         * ========================================================
         * RESULTADO FINAL
         * ========================================================
         */

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
                confirmationValue
        )
    }
}

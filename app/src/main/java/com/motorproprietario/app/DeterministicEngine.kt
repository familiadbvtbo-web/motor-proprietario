package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

    private fun directional(
        value: Double
    ): Double {
        return clamp(value)
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
     * ==================================================
     * 1. PRESSÃO COMPRADORA
     * ==================================================
     */

    private fun buyPressure(
        m: QuantMetrics
    ): Double {

        val trend =
            directional(
                m.trend
            )

        val momentum =
            directional(
                m.momentum
            )

        val structure =
            directional(
                m.structure
            )

        val candle =
            directional(
                m.candlePattern
            )

        val breakout =
            directional(
                m.breakout
            )

        val volume =
            directional(
                m.volume
            )

        val ema =
            when {

                m.ema9 > m.ema21 &&
                    m.ema21 >= m.ema50 ->
                    90.0

                m.ema9 > m.ema21 ->
                    70.0

                else ->
                    30.0
            }

        val macd =
            if (
                m.macd >
                m.macdSignal
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
     * ==================================================
     * 2. PRESSÃO VENDEDORA
     * ==================================================
     */

    private fun sellPressure(
        m: QuantMetrics
    ): Double {

        val trend =
            100.0 -
                directional(
                    m.trend
                )

        val momentum =
            100.0 -
                directional(
                    m.momentum
                )

        val structure =
            100.0 -
                directional(
                    m.structure
                )

        val candle =
            100.0 -
                directional(
                    m.candlePattern
                )

        val breakout =
            100.0 -
                directional(
                    m.breakout
                )

        val volume =
            directional(
                m.volume
            )

        val ema =
            when {

                m.ema9 < m.ema21 &&
                    m.ema21 <= m.ema50 ->
                    90.0

                m.ema9 < m.ema21 ->
                    70.0

                else ->
                    30.0
            }

        val macd =
            if (
                m.macd <
                m.macdSignal
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
     * ==================================================
     * 3. ACUMULAÇÃO
     *
     * Mede:
     * - compressão
     * - estabilidade estrutural
     * - presença de volume
     * - preparação de rompimento
     *
     * Não interpreta isso como prova de atuação
     * institucional.
     * ==================================================
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

        val volatilityCompression =
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
            volatilityCompression * 0.25 +
            volumePresence * 0.20 +
            breakoutPreparation * 0.25
        )
    }

    /*
     * ==================================================
     * 4. DISTRIBUIÇÃO
     * ==================================================
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
     * ==================================================
     * 5. EXPANSÃO
     * ==================================================
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
     * ==================================================
     * 6. EXAUSTÃO
     * ==================================================
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
     * ==================================================
     * 7. LIQUIDEZ / PRESSÃO
     * ==================================================
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
     * ==================================================
     * 8. RISCO DE REALIZAÇÃO
     * ==================================================
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
     * ==================================================
     * 9. CONFLITO ENTRE TIMEFRAMES
     * ==================================================
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
     * ==================================================
     * 10. ARMADILHA / FALSO SINAL
     *
     * Aqui entram:
     * - falso sinal externo
     * - conflito MTF
     * - divergência
     * - rejeição
     * - rompimento sem volume
     *
     * Quanto maior, maior o risco.
     * ==================================================
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
                ) > 30.0 &&
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
     * ==================================================
     * 11. CONFIRMAÇÃO
     * ==================================================
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

        val structuralAgreement =
            clamp(
                max(
                    accumulation,
                    distribution
                )
            )

        val riskPenalty =
            clamp(
                average(
                    listOf(
                        trap,
                        exhaustion,
                        realization
                    )
                )
            )

        return clamp(
            directionalAgreement * 0.20 +

            expansion * 0.15 +

            structuralAgreement * 0.10 +

            mtf * 0.25 +

            (
                100.0 -
                    riskPenalty
            ) * 0.15 +

            (
                100.0 -
                    trap
            ) * 0.15
        )
    }

    /*
     * ==================================================
     * 12. MOTOR PRINCIPAL
     * ==================================================
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
         * ------------------------------------------
         * CÁLCULOS DOS MOTORES
         * ------------------------------------------
         */

        val buy =
            buyPressure(
                m
            )

        val sell =
            sellPressure(
                m
            )

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
         * ------------------------------------------
         * BASE DIRECIONAL
         * ------------------------------------------
         */

        var adjustedBuy =
            buy

        var adjustedSell =
            sell

        /*
         * ------------------------------------------
         * ACUMULAÇÃO
         *
         * Acumulação é tratada como condição
         * de preparação, não como direção.
         *
         * Portanto ela aumenta a confirmação
         * somente quando existe pressão direcional.
         * ------------------------------------------
         */

        val accumulationBoost =
            accumulationValue *
                0.10

        if (
            buy >
                sell
        ) {

            adjustedBuy +=
                accumulationBoost

        } else if (
            sell >
                buy
        ) {

            adjustedSell +=
                accumulationBoost
        }

        /*
         * ------------------------------------------
         * EXPANSÃO
         *
         * Expansão confirma movimento quando
         * existe direção definida.
         * ------------------------------------------
         */

        val expansionBoost =
            expansionValue *
                0.12

        if (
            buy >
                sell
        ) {

            adjustedBuy +=
                expansionBoost

        } else if (
            sell >
                buy
        ) {

            adjustedSell +=
                expansionBoost
        }

        /*
         * ------------------------------------------
         * DISTRIBUIÇÃO
         *
         * Não é tratada automaticamente como VENDA.
         * É risco estrutural de continuidade.
         * ------------------------------------------
         */

        val distributionPenalty =
            distributionValue *
                0.12

        adjustedBuy -=
            distributionPenalty

        adjustedSell -=
            distributionPenalty

        /*
         * ------------------------------------------
         * EXAUSTÃO
         *
         * Reduz a força dos dois lados.
         * ------------------------------------------
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
         * ------------------------------------------
         * ARMADILHA
         *
         * Quanto maior o risco:
         * menor a confiança direcional.
         * ------------------------------------------
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
         * ------------------------------------------
         * REALIZAÇÃO
         * ------------------------------------------
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
         * ------------------------------------------
         * CONFLITO MTF
         * ------------------------------------------
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
         * ------------------------------------------
         * LIQUIDEZ
         *
         * Liquidez forte sozinha não determina
         * direção.
         *
         * Ela aumenta a qualidade da confirmação.
         * ------------------------------------------
         */

        val liquidityConfirmation =
            liquidityValue *
                0.08

        if (
            adjustedBuy >
                adjustedSell
        ) {

            adjustedBuy +=
                liquidityConfirmation

        } else if (
            adjustedSell >
                adjustedBuy
        ) {

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
         * ------------------------------------------
         * NEUTRALIDADE
         * ------------------------------------------
         */

        val directionalDifference =
            abs(
                adjustedBuy -
                    adjustedSell
            )

        var neutral =
            100.0 -
                directionalDifference

        /*
         * Risco elevado aumenta neutralidade.
         */

        neutral +=
            trap *
                0.35

        neutral +=
            conflict *
                0.20

        neutral +=
            exhaustionValue *
                0.15

        neutral +=
            realization *
                0.10

        /*
         * Se expansão estiver muito baixa,
         * evita transformar lateralidade em
         * sinal forte.
         */

        if (
            expansionValue <
            30.0
        ) {

            neutral +=
                (
                    30.0 -
                        expansionValue
                ) *
                    0.50
        }

        neutral =
            clamp(
                neutral,
                5.0,
                100.0
            )

        /*
         * ------------------------------------------
         * NORMALIZAÇÃO
         * ------------------------------------------
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

        /*
         * ------------------------------------------
         * DIREÇÃO
         * ------------------------------------------
         */

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

        /*
         * ------------------------------------------
         * CONFIRMAÇÃO
         * ------------------------------------------
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
         * ------------------------------------------
         * CONFIANÇA
         *
         * Não é probabilidade estatística de lucro.
         * É confiança interna do conjunto de sinais.
         * ------------------------------------------
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
         * ------------------------------------------
         * RESULTADO
         * ------------------------------------------
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

package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max

data class ProbabilityCalibration(
    /*
     * Pesos da evidência probabilística.
     *
     * Estes são parâmetros iniciais.
     * NÃO representam taxa de acerto.
     *
     * O CalibrationEngine poderá substituí-los
     * após backtest e validação fora da amostra.
     */

    val trendWeight: Double = 0.22,
    val momentumWeight: Double = 0.16,
    val priceActionWeight: Double = 0.20,
    val volumeWeight: Double = 0.08,
    val mtfWeight: Double = 0.12,
    val fibonacciWeight: Double = 0.07,
    val institutionalWeight: Double = 0.15,

    val mtfBaseFactor: Double = 0.70,
    val mtfRangeFactor: Double = 0.30,

    val falseSignalMaximumPenalty: Double = 0.65,

    val neutralBase: Double = 12.0,
    val falseSignalNeutralWeight: Double = 0.42
) {

    fun normalized(): ProbabilityCalibration {

        val rawWeights =
            listOf(
                trendWeight,
                momentumWeight,
                priceActionWeight,
                volumeWeight,
                mtfWeight,
                fibonacciWeight,
                institutionalWeight
            )
                .map {
                    if (
                        it.isFinite() &&
                        it >= 0.0
                    ) {
                        it
                    } else {
                        0.0
                    }
                }

        val total =
            rawWeights.sum()

        if (
            total <= 0.0
        ) {

            return ProbabilityCalibration()
        }

        return copy(

            trendWeight =
                rawWeights[0] /
                    total,

            momentumWeight =
                rawWeights[1] /
                    total,

            priceActionWeight =
                rawWeights[2] /
                    total,

            volumeWeight =
                rawWeights[3] /
                    total,

            mtfWeight =
                rawWeights[4] /
                    total,

            fibonacciWeight =
                rawWeights[5] /
                    total,

            institutionalWeight =
                rawWeights[6] /
                    total
        )
    }
}

data class ProbabilityInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val falseSignalRisk: Double,

    val fibonacciBullish: Double = 50.0,
    val fibonacciBearish: Double = 50.0,

    val institutionalBullish: Double = 50.0,
    val institutionalBearish: Double = 50.0,

    /*
     * Configuração calibrável.
     *
     * Mantém compatibilidade porque possui
     * valor padrão.
     */
    val calibration:
        ProbabilityCalibration =
            ProbabilityCalibration()
)

data class ProbabilityResult(
    val buyProbability: Double,
    val sellProbability: Double,
    val neutralProbability: Double,

    val directionalBias: String,
    val confidence: Double,

    val rawBuyProbability: Double = 0.0,
    val rawSellProbability: Double = 0.0,

    /*
     * Mantido por compatibilidade com o projeto.
     *
     * Representa a penalização aplicada pelo
     * risco de falso sinal.
     */
    val provisionPenalty: Double = 0.0,

    val evidenceStrength: Double = 0.0
)

object ProbabilityEngine {

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

    private fun normalize(
        buy: Double,
        sell: Double,
        neutral: Double
    ): Triple<Double, Double, Double> {

        val b =
            max(
                0.0,
                buy
            )

        val s =
            max(
                0.0,
                sell
            )

        val n =
            max(
                0.0,
                neutral
            )

        val total =
            b +
                s +
                n

        if (
            total <= 0.0 ||
            !total.isFinite()
        ) {

            return Triple(
                33.33,
                33.33,
                33.34
            )
        }

        return Triple(

            b /
                total *
                100.0,

            s /
                total *
                100.0,

            n /
                total *
                100.0
        )
    }

    private fun evidence(
        value: Double
    ): Double {

        return clamp(
            value
        ) -
            50.0
    }

    fun calculate(
        input: ProbabilityInput
    ): ProbabilityResult {

        val m =
            input.metrics

        val calibration =
            input.calibration.normalized()

        /*
         * ==================================
         * DADOS BASE
         * ==================================
         */

        val trend =
            clamp(
                m.trend
            )

        val momentum =
            clamp(
                m.momentum
            )

        val structure =
            clamp(
                m.structure
            )

        val volume =
            clamp(
                m.volume
            )

        val candle =
            clamp(
                m.candlePattern
            )

        val breakout =
            clamp(
                m.breakout
            )

        val divergence =
            clamp(
                m.divergence
            )

        val mtf =
            clamp(
                input.mtfConfluence
            )

        val falseRisk =
            clamp(
                input.falseSignalRisk
            )

        /*
         * ==================================
         * EVIDÊNCIAS DERIVADAS
         * ==================================
         */

        val macdEvidence =
            when {

                m.macd >
                    m.macdSignal ->
                    65.0

                m.macd <
                    m.macdSignal ->
                    35.0

                else ->
                    50.0
            }

        val emaEvidence =
            when {

                m.ema9 >
                    m.ema21 ->
                    65.0

                m.ema9 <
                    m.ema21 ->
                    35.0

                else ->
                    50.0
            }

        val emaLongEvidence =
            when {

                m.ema21 >
                    m.ema50 ->
                    65.0

                m.ema21 <
                    m.ema50 ->
                    35.0

                else ->
                    50.0
            }

        val rsiEvidence =
            when {

                m.rsi > 55.0 &&
                m.rsi < 70.0 ->
                    65.0

                m.rsi >= 70.0 ->
                    55.0

                m.rsi < 45.0 &&
                m.rsi > 30.0 ->
                    35.0

                m.rsi <= 30.0 ->
                    45.0

                else ->
                    50.0
            }

        val fibonacciBuy =
            clamp(
                input.fibonacciBullish
            )

        val fibonacciSell =
            clamp(
                input.fibonacciBearish
            )

        val institutionalBuy =
            clamp(
                input.institutionalBullish
            )

        val institutionalSell =
            clamp(
                input.institutionalBearish
            )

        /*
         * ==================================
         * GRUPOS DE EVIDÊNCIA
         * ==================================
         *
         * EMA9/21, EMA21/50 e MACD possuem
         * correlação entre si.
         *
         * Por isso não são tratados como
         * três fontes totalmente independentes.
         */

        val trendGroup =
            trend * 0.50 +
            emaEvidence * 0.25 +
            emaLongEvidence * 0.25

        val momentumGroup =
            momentum * 0.45 +
            rsiEvidence * 0.30 +
            macdEvidence * 0.25

        val priceActionGroup =
            structure * 0.35 +
            candle * 0.25 +
            breakout * 0.25 +
            divergence * 0.15

        /*
         * ==================================
         * PROBABILIDADE BRUTA
         * ==================================
         */

        val buyRaw =
            trendGroup *
                calibration.trendWeight +

            momentumGroup *
                calibration.momentumWeight +

            priceActionGroup *
                calibration.priceActionWeight +

            volume *
                calibration.volumeWeight +

            mtf *
                calibration.mtfWeight +

            fibonacciBuy *
                calibration.fibonacciWeight +

            institutionalBuy *
                calibration.institutionalWeight

        val sellRaw =
            (
                100.0 -
                    trendGroup
            ) *
                calibration.trendWeight +

            (
                100.0 -
                    momentumGroup
            ) *
                calibration.momentumWeight +

            (
                100.0 -
                    priceActionGroup
            ) *
                calibration.priceActionWeight +

            (
                100.0 -
                    volume
            ) *
                calibration.volumeWeight +

            (
                100.0 -
                    mtf
            ) *
                calibration.mtfWeight +

            fibonacciSell *
                calibration.fibonacciWeight +

            institutionalSell *
                calibration.institutionalWeight

        /*
         * ==================================
         * DIVERGÊNCIA
         * ==================================
         */

        val buyDivergencePenalty =
            if (
                divergence < 40.0
            ) {

                (
                    50.0 -
                        divergence
                ) *
                    0.35

            } else {

                0.0
            }

        val sellDivergencePenalty =
            if (
                divergence > 60.0
            ) {

                (
                    divergence -
                        50.0
                ) *
                    0.35

            } else {

                0.0
            }

        var buy =
            max(
                0.0,
                buyRaw -
                    buyDivergencePenalty
            )

        var sell =
            max(
                0.0,
                sellRaw -
                    sellDivergencePenalty
            )

        /*
         * ==================================
         * MTF
         * ==================================
         */

        val mtfFactor =
            calibration.mtfBaseFactor +
                mtf /
                100.0 *
                calibration.mtfRangeFactor

        buy *=
            mtfFactor

        sell *=
            mtfFactor

        /*
         * ==================================
         * FALSO SINAL
         * ==================================
         */

        val provisionFactor =
            1.0 -
                (
                    falseRisk /
                        100.0
                ) *
                calibration.falseSignalMaximumPenalty

        val rawBuy =
            buy

        val rawSell =
            sell

        buy *=
            provisionFactor

        sell *=
            provisionFactor

        /*
         * ==================================
         * NEUTRALIDADE
         * ==================================
         */

        val directionalDifference =
            abs(
                buy -
                    sell
            )

        var neutral =
            calibration.neutralBase

        neutral +=
            when {

                directionalDifference <
                    4.0 ->
                    20.0

                directionalDifference <
                    8.0 ->
                    12.0

                directionalDifference <
                    14.0 ->
                    5.0

                else ->
                    0.0
            }

        /*
         * FSI aumenta neutralidade.
         */

        neutral +=
            falseRisk *
                calibration.falseSignalNeutralWeight

        /*
         * Volatilidade elevada com MTF fraco.
         */

        if (
            m.volatility >= 80.0 &&
            mtf < 60.0
        ) {

            neutral +=
                12.0
        }

        /*
         * ADX muito baixo.
         */

        if (
            m.adx < 20.0
        ) {

            neutral +=
                10.0
        }

        neutral =
            clamp(
                neutral,
                5.0,
                92.0
            )

        /*
         * ==================================
         * NORMALIZAÇÃO
         * ==================================
         */

        val normalized =
            normalize(
                buy,
                sell,
                neutral
            )

        val buyProbability =
            normalized.first

        val sellProbability =
            normalized.second

        val neutralProbability =
            normalized.third

        /*
         * ==================================
         * VIÉS
         * ==================================
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
         * ==================================
         * FORÇA DA EVIDÊNCIA
         * ==================================
         */

        val strongest =
            max(
                buyProbability,
                sellProbability
            )

        val evidenceStrength =
            clamp(

                abs(
                    buyRaw -
                        sellRaw
                ) *
                    1.15 +

                mtf *
                    0.25 +

                m.adx *
                    0.15 -

                falseRisk *
                    0.35
            )

        /*
         * ==================================
         * CONFIANÇA
         * ==================================
         *
         * Não é taxa histórica de acerto.
         *
         * É uma medida interna de força do
         * cenário atual.
         */

        val confidence =
            clamp(

                (
                    strongest -
                        neutralProbability
                ) *
                    1.20 +

                evidenceStrength *
                    0.25 -

                falseRisk *
                    0.20
            )

        val provisionPenalty =
            clamp(

                falseRisk *
                    calibration
                        .falseSignalMaximumPenalty
            )

        return ProbabilityResult(

            buyProbability =
                buyProbability,

            sellProbability =
                sellProbability,

            neutralProbability =
                neutralProbability,

            directionalBias =
                directionalBias,

            confidence =
                confidence,

            rawBuyProbability =
                clamp(
                    rawBuy
                ),

            rawSellProbability =
                clamp(
                    rawSell
                ),

            provisionPenalty =
                provisionPenalty,

            evidenceStrength =
                evidenceStrength
        )
    }
}

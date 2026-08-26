package com.motorproprietario.app

import kotlin.math.abs

data class DeterministicRiskInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val fibonacci: FibonacciResult? = null,
    val dataQuality: String = "GOOD",
    val price: Double = 0.0,
    val spread: Double = 0.0
)

data class DeterministicRiskResult(
    val falseSignalRisk: Double,
    val riskLevel: String,
    val blocked: Boolean,
    val reasons: List<String>,
    val bullishRisk: Double,
    val bearishRisk: Double,
    val signalQuality: Double
)

/*
 * Camada determinística do Motor.
 *
 * NÃO representa uma previsão estatística.
 *
 * Ela verifica condições observáveis do mercado
 * que podem indicar:
 *
 * - falso rompimento
 * - conflito entre indicadores
 * - exaustão
 * - baixa confirmação
 * - conflito MTF
 * - deterioração da qualidade do sinal
 *
 * Quanto maior o risco determinístico,
 * maior a necessidade de aguardar confirmação.
 */
object DeterministicRiskEngine {

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

    fun calculate(
        input: DeterministicRiskInput
    ): DeterministicRiskResult {

        val m =
            input.metrics

        var risk = 0.0

        var bullishRisk = 0.0

        var bearishRisk = 0.0

        val reasons =
            mutableListOf<String>()

        /*
         * 1. QUALIDADE DOS DADOS
         */

        if (
            input.dataQuality != "GOOD"
        ) {

            risk += 30.0

            reasons.add(
                "DADOS_NAO_IDEAIS"
            )
        }

        /*
         * 2. CONFLUÊNCIA MTF
         */

        val mtf =
            clamp(
                input.mtfConfluence
            )

        when {

            mtf < 40.0 -> {

                risk += 22.0

                reasons.add(
                    "CONFLITO_MTF_ALTO"
                )
            }

            mtf < 60.0 -> {

                risk += 12.0

                reasons.add(
                    "CONFLUENCIA_MTF_FRACA"
                )
            }
        }

        /*
         * 3. EMA x MACD
         */

        val emaBull =
            m.ema9 >
                m.ema21

        val emaBear =
            m.ema9 <
                m.ema21

        val macdBull =
            m.macd >
                m.macdSignal

        val macdBear =
            m.macd <
                m.macdSignal

        if (
            emaBull &&
            macdBear
        ) {

            risk += 12.0

            bearishRisk +=
                8.0

            reasons.add(
                "MACD_CONTRA_EMAS"
            )
        }

        if (
            emaBear &&
            macdBull
        ) {

            risk += 12.0

            bullishRisk +=
                8.0

            reasons.add(
                "MACD_CONTRA_EMAS"
            )
        }

        /*
         * 4. RSI EXTREMO
         */

        if (
            m.rsi >= 75.0
        ) {

            risk += 10.0

            bullishRisk +=
                10.0

            reasons.add(
                "RSI_SOBRECOMPRADO"
            )
        }

        if (
            m.rsi <= 25.0
        ) {

            risk += 10.0

            bearishRisk +=
                10.0

            reasons.add(
                "RSI_SOBREVENDIDO"
            )
        }

        /*
         * 5. DIVERGÊNCIA
         */

        if (
            m.divergence <= 30.0
        ) {

            risk += 15.0

            bullishRisk +=
                15.0

            reasons.add(
                "DIVERGENCIA_BAIXISTA"
            )
        }

        if (
            m.divergence >= 70.0
        ) {

            risk += 15.0

            bearishRisk +=
                15.0

            reasons.add(
                "DIVERGENCIA_ALTISTA"
            )
        }

        /*
         * 6. ROMPIMENTO SEM VOLUME
         *
         * Esse é um dos principais componentes
         * da identificação de falso sinal.
         */

        if (
            m.breakout >= 85.0 &&
            m.volume < 55.0
        ) {

            risk += 14.0

            bullishRisk +=
                14.0

            reasons.add(
                "ROMPIMENTO_SEM_VOLUME"
            )
        }

        if (
            m.breakout <= 15.0 &&
            m.volume < 55.0
        ) {

            risk += 14.0

            bearishRisk +=
                14.0

            reasons.add(
                "ROMPIMENTO_SEM_VOLUME"
            )
        }

        /*
         * 7. VOLUME FRACO
         */

        if (
            m.volume < 35.0
        ) {

            risk += 10.0

            reasons.add(
                "VOLUME_FRACO"
            )
        }

        /*
         * Volume extremo não bloqueia
         * automaticamente.
         *
         * Pode representar expansão legítima
         * ou exaustão.
         */

        if (
            m.volume >= 85.0
        ) {

            reasons.add(
                "VOLUME_ANORMAL"
            )
        }

        /*
         * 8. ADX
         */

        if (
            m.adx < 18.0
        ) {

            risk += 15.0

            reasons.add(
                "TENDENCIA_MUITO_FRACA"
            )

        } else if (
            m.adx < 25.0
        ) {

            risk += 7.0

            reasons.add(
                "TENDENCIA_FRACA"
            )
        }

        /*
         * 9. VOLATILIDADE
         */

        if (
            m.volatility >= 85.0
        ) {

            risk += 15.0

            reasons.add(
                "VOLATILIDADE_EXTREMA"
            )

        } else if (
            m.volatility >= 70.0
        ) {

            risk += 8.0

            reasons.add(
                "VOLATILIDADE_ALTA"
            )
        }

        /*
         * 10. CANDLE CONTRA A ESTRUTURA
         */

        if (
            emaBull &&
            m.candlePattern <= 25.0
        ) {

            risk += 12.0

            bullishRisk +=
                12.0

            reasons.add(
                "CANDLE_CONTRA_COMPRA"
            )
        }

        if (
            emaBear &&
            m.candlePattern >= 75.0
        ) {

            risk += 12.0

            bearishRisk +=
                12.0

            reasons.add(
                "CANDLE_CONTRA_VENDA"
            )
        }

        /*
         * 11. ESTRUTURA EXTREMA
         */

        if (
            m.structure >= 90.0
        ) {

            risk += 8.0

            bullishRisk +=
                8.0

            reasons.add(
                "PRECO_PROXIMO_DO_TOPO"
            )
        }

        if (
            m.structure <= 10.0
        ) {

            risk += 8.0

            bearishRisk +=
                8.0

            reasons.add(
                "PRECO_PROXIMO_DO_FUNDO"
            )
        }

        /*
         * 12. BOLLINGER
         */

        if (
            input.price > 0.0
        ) {

            if (
                input.price >
                    m.bollingerUpper
            ) {

                risk += 5.0

                reasons.add(
                    "PRECO_ACIMA_BANDA_SUPERIOR"
                )
            }

            if (
                input.price <
                    m.bollingerLower
            ) {

                risk += 5.0

                reasons.add(
                    "PRECO_ABAIXO_BANDA_INFERIOR"
                )
            }
        }

        /*
         * 13. FIBONACCI
         */

        val fib =
            input.fibonacci

        if (
            fib != null
        ) {

            if (
                fib.bias == "COMPRA" &&
                emaBear
            ) {

                risk += 8.0

                reasons.add(
                    "FIBONACCI_CONTRA_TENDENCIA"
                )
            }

            if (
                fib.bias == "VENDA" &&
                emaBull
            ) {

                risk += 8.0

                reasons.add(
                    "FIBONACCI_CONTRA_TENDENCIA"
                )
            }

            if (
                fib.zone == "SEM_DADOS" ||
                fib.zone == "SEM_RANGE"
            ) {

                risk += 5.0

                reasons.add(
                    "FIBONACCI_SEM_ESTRUTURA"
                )
            }
        }

        /*
         * 14. SPREAD
         */

        if (
            input.spread > 0.0 &&
            m.atr > 0.0
        ) {

            val spreadRatio =
                input.spread /
                    m.atr

            if (
                spreadRatio > 0.25
            ) {

                risk += 15.0

                reasons.add(
                    "SPREAD_ELEVADO"
                )

            } else if (
                spreadRatio > 0.10
            ) {

                risk += 6.0

                reasons.add(
                    "SPREAD_ACIMA_DO_NORMAL"
                )
            }
        }

        /*
         * 15. LIMITAÇÃO
         */

        risk =
            clamp(
                risk
            )

        /*
         * BLOQUEIO
         */

        val blocked =
            risk >= 65.0 ||
            input.dataQuality != "GOOD"

        val riskLevel =
            when {

                risk >= 65.0 ->
                    "ALTO"

                risk >= 40.0 ->
                    "MODERADO"

                risk >= 20.0 ->
                    "BAIXO"

                else ->
                    "MUITO BAIXO"
            }

        val signalQuality =
            clamp(
                100.0 -
                    risk
            )

        return DeterministicRiskResult(

            falseSignalRisk =
                risk,

            riskLevel =
                riskLevel,

            blocked =
                blocked,

            reasons =
                reasons.distinct(),

            bullishRisk =
                clamp(
                    bullishRisk
                ),

            bearishRisk =
                clamp(
                    bearishRisk
                ),

            signalQuality =
                signalQuality
        )
    }
}

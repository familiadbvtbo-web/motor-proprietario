package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class ProvisionInput(
    val metrics: QuantMetrics,
    val mtfConfluence: Double,
    val fibonacci: FibonacciResult? = null,
    val dataQuality: String = "GOOD",
    val price: Double = 0.0,
    val spread: Double = 0.0
)

data class ProvisionResult(
    val falseSignalRisk: Double,
    val riskLevel: String,
    val blocked: Boolean,
    val reasons: List<String>,
    val bullishRisk: Double,
    val bearishRisk: Double,
    val signalQuality: Double
)

object ProvisionEngine {

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
        input: ProvisionInput
    ): ProvisionResult {

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
         * 2. CONFLUÊNCIA MULTI-TIMEFRAME
         *
         * Quanto menor a concordância,
         * maior o risco de sinal falso.
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
         * 3. MACD x EMA
         *
         * Se tendência das médias e MACD
         * apontam para lados diferentes,
         * reduzimos a confiabilidade.
         */
        val emaBull =
            m.ema9 > m.ema21

        val emaBear =
            m.ema9 < m.ema21

        val macdBull =
            m.macd > m.macdSignal

        val macdBear =
            m.macd < m.macdSignal

        if (
            emaBull &&
            macdBear
        ) {

            risk += 12.0
            bearishRisk += 8.0

            reasons.add(
                "MACD_CONTRA_EMAS"
            )
        }

        if (
            emaBear &&
            macdBull
        ) {

            risk += 12.0
            bullishRisk += 8.0

            reasons.add(
                "MACD_CONTRA_EMAS"
            )
        }

        /*
         * 4. RSI EM ZONA EXTREMA
         *
         * Extremos não significam automaticamente
         * reversão, mas aumentam o cuidado.
         */
        if (
            m.rsi >= 75.0
        ) {

            risk += 10.0
            bullishRisk += 10.0

            reasons.add(
                "RSI_SOBRECOMPRADO"
            )
        }

        if (
            m.rsi <= 25.0
        ) {

            risk += 10.0
            bearishRisk += 10.0

            reasons.add(
                "RSI_SOBREVENDIDO"
            )
        }

        /*
         * 5. DIVERGÊNCIA
         *
         * O valor do projeto está centralizado
         * em 50.
         *
         * 25 = risco para compra
         * 75 = risco para venda
         */
        if (
            m.divergence <= 30.0
        ) {

            risk += 15.0
            bullishRisk += 15.0

            reasons.add(
                "DIVERGENCIA_BAIXISTA"
            )
        }

        if (
            m.divergence >= 70.0
        ) {

            risk += 15.0
            bearishRisk += 15.0

            reasons.add(
                "DIVERGENCIA_ALTISTA"
            )
        }

        /*
         * 6. BREAKOUT
         *
         * Rompimento extremo sem confirmação
         * pode produzir falso rompimento.
         */
        if (
            m.breakout >= 85.0
        ) {

            if (
                m.volume < 55.0
            ) {

                risk += 14.0
                bullishRisk += 14.0

                reasons.add(
                    "ROMPIMENTO_SEM_VOLUME"
                )
            }
        }

        if (
            m.breakout <= 15.0
        ) {

            if (
                m.volume < 55.0
            ) {

                risk += 14.0
                bearishRisk += 14.0

                reasons.add(
                    "ROMPIMENTO_SEM_VOLUME"
                )
            }
        }

        /*
         * 7. VOLUME
         *
         * Volume muito abaixo da referência
         * reduz a confiabilidade do movimento.
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
         * Volume extremamente elevado pode representar
         * expansão legítima ou evento de exaustão.
         * Portanto não bloqueamos automaticamente.
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
         *
         * ADX baixo indica ausência de tendência clara.
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
         * 10. PADRÃO DE CANDLE
         *
         * Candle contrário à tendência
         * aumenta o provisionamento.
         */
        if (
            emaBull &&
            m.candlePattern <= 25.0
        ) {

            risk += 12.0
            bullishRisk += 12.0

            reasons.add(
                "CANDLE_CONTRA_COMPRA"
            )
        }

        if (
            emaBear &&
            m.candlePattern >= 75.0
        ) {

            risk += 12.0
            bearishRisk += 12.0

            reasons.add(
                "CANDLE_CONTRA_VENDA"
            )
        }

        /*
         * 11. ESTRUTURA
         *
         * Extremidade estrutural pode significar
         * continuação ou exaustão.
         */
        if (
            m.structure >= 90.0
        ) {

            risk += 8.0
            bullishRisk += 8.0

            reasons.add(
                "PRECO_PROXIMO_DO_TOPO"
            )
        }

        if (
            m.structure <= 10.0
        ) {

            risk += 8.0
            bearishRisk += 8.0

            reasons.add(
                "PRECO_PROXIMO_DO_FUNDO"
            )
        }

        /*
         * 12. BOLLINGER
         *
         * Preço fora da banda pode significar
         * força ou exaustão. Por isso é alerta,
         * não bloqueio automático.
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
                fib.zone ==
                    "SEM_DADOS" ||
                fib.zone ==
                    "SEM_RANGE"
            ) {

                risk += 5.0

                reasons.add(
                    "FIBONACCI_SEM_ESTRUTURA"
                )
            }
        }

        /*
         * 14. SPREAD
         *
         * Se BID/ASK estiver disponível,
         * detectamos spread anormal em relação ao ATR.
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
         * LIMITAÇÃO DO RISCO
         */
        risk =
            clamp(
                risk
            )

        /*
         * Bloqueio somente quando o risco
         * realmente compromete a qualidade.
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

        /*
         * Qualidade é inversamente proporcional
         * ao risco.
         */
        val signalQuality =
            clamp(
                100.0 -
                    risk
            )

        /*
         * Remove duplicidades das mensagens.
         */
        val uniqueReasons =
            reasons.distinct()

        return ProvisionResult(
            falseSignalRisk =
                risk,

            riskLevel =
                riskLevel,

            blocked =
                blocked,

            reasons =
                uniqueReasons,

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

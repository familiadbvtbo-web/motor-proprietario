package com.motorproprietario.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class SignalInput(
    val price: Double,
    val previousPrice: Double,
    val volume: Double,
    val averageVolume: Double
)

data class SignalResult(
    val isFalseSignal: Boolean,
    val confidence: Double,
    val reason: String
)

class FalseSignalDetector {

    companion object {

        private const val MIN_PRICE_CHANGE_PERCENT = 0.30
        private const val VERY_WEAK_PRICE_CHANGE_PERCENT = 0.10

        private const val LOW_VOLUME_RATIO = 0.50
        private const val VERY_LOW_VOLUME_RATIO = 0.30

        private const val HIGH_VOLUME_RATIO = 1.50

        private const val MIN_CONFIDENCE = 0.0
        private const val MAX_CONFIDENCE = 1.0

        private fun clamp(
            value: Double,
            minValue: Double = MIN_CONFIDENCE,
            maxValue: Double = MAX_CONFIDENCE
        ): Double {
            if (!value.isFinite()) {
                return minValue
            }

            return value.coerceIn(
                minValue,
                maxValue
            )
        }
    }

    fun analyze(
        input: SignalInput
    ): SignalResult {

        /*
         * 1. VALIDAÇÃO DOS DADOS
         *
         * Não devemos analisar sinal utilizando
         * preço, volume ou média inválidos.
         */

        if (
            !input.price.isFinite() ||
            !input.previousPrice.isFinite() ||
            !input.volume.isFinite() ||
            !input.averageVolume.isFinite()
        ) {
            return SignalResult(
                isFalseSignal = true,
                confidence = 1.0,
                reason = "Dados de mercado inválidos"
            )
        }

        if (
            input.price <= 0.0 ||
            input.previousPrice <= 0.0
        ) {
            return SignalResult(
                isFalseSignal = true,
                confidence = 1.0,
                reason = "Preço inválido"
            )
        }

        /*
         * Volume negativo não faz sentido.
         */

        if (
            input.volume < 0.0
        ) {
            return SignalResult(
                isFalseSignal = true,
                confidence = 1.0,
                reason = "Volume inválido"
            )
        }

        /*
         * 2. VARIAÇÃO REAL DO PREÇO
         *
         * percentual = (preço atual - anterior)
         *              / preço anterior * 100
         */

        val priceChange =
            (
                (
                    input.price -
                        input.previousPrice
                ) /
                    input.previousPrice
            ) *
                100.0

        val absolutePriceChange =
            abs(priceChange)

        /*
         * 3. RELAÇÃO DO VOLUME
         *
         * volumeRatio = volume atual /
         *               volume médio
         */

        val volumeRatio =
            if (
                input.averageVolume > 0.0
            ) {
                input.volume /
                    input.averageVolume
            } else {
                0.0
            }

        /*
         * 4. CLASSIFICAÇÃO DO MOVIMENTO
         */

        val veryWeakMovement =
            absolutePriceChange <
                VERY_WEAK_PRICE_CHANGE_PERCENT

        val weakMovement =
            absolutePriceChange <
                MIN_PRICE_CHANGE_PERCENT

        /*
         * 5. CLASSIFICAÇÃO DO VOLUME
         */

        val veryLowVolume =
            volumeRatio <
                VERY_LOW_VOLUME_RATIO

        val lowVolume =
            volumeRatio <
                LOW_VOLUME_RATIO

        val strongVolume =
            volumeRatio >=
                HIGH_VOLUME_RATIO

        /*
         * 6. MOVIMENTO FRACO + VOLUME FRACO
         *
         * Este é o principal cenário clássico
         * de sinal sem confirmação.
         */

        if (
            veryWeakMovement &&
            veryLowVolume
        ) {

            return SignalResult(
                isFalseSignal = true,
                confidence = 0.95,
                reason =
                    "Movimento muito fraco com volume muito abaixo da média"
            )
        }

        /*
         * 7. MOVIMENTO FRACO + VOLUME BAIXO
         */

        if (
            weakMovement &&
            lowVolume
        ) {

            return SignalResult(
                isFalseSignal = true,
                confidence = 0.88,
                reason =
                    "Movimento fraco com volume abaixo da média"
            )
        }

        /*
         * 8. MOVIMENTO MUITO FRACO
         *
         * Mesmo sem volume extremamente baixo,
         * uma variação praticamente inexistente
         * não deve receber grande confiança.
         */

        if (
            veryWeakMovement &&
            !strongVolume
        ) {

            return SignalResult(
                isFalseSignal = true,
                confidence = 0.72,
                reason =
                    "Variação de preço muito pequena para confirmar o sinal"
            )
        }

        /*
         * 9. VOLUME MUITO BAIXO
         *
         * Um movimento com volume extremamente baixo
         * é suspeito, mesmo que o preço tenha se deslocado.
         */

        if (
            veryLowVolume
        ) {

            val confidence =
                clamp(
                    0.70 +
                        min(
                            absolutePriceChange /
                                2.0,
                            0.20
                        )
                )

            return SignalResult(
                isFalseSignal = true,
                confidence = confidence,
                reason =
                    "Volume muito abaixo da média; movimento sem confirmação"
            )
        }

        /*
         * 10. MOVIMENTO FRACO SEM VOLUME DE CONFIRMAÇÃO
         */

        if (
            weakMovement &&
            !strongVolume
        ) {

            return SignalResult(
                isFalseSignal = true,
                confidence = 0.65,
                reason =
                    "Variação pequena sem volume suficiente para confirmação"
            )
        }

        /*
         * 11. MOVIMENTO FORTE COM VOLUME FORTE
         *
         * Aqui o sinal NÃO deve ser marcado como falso
         * apenas porque existe volatilidade.
         *
         * O detector apenas verifica a qualidade básica
         * do movimento.
         */

        if (
            absolutePriceChange >=
                MIN_PRICE_CHANGE_PERCENT &&
            strongVolume
        ) {

            return SignalResult(
                isFalseSignal = false,
                confidence = 0.90,
                reason =
                    "Movimento confirmado por volume acima da média"
            )
        }

        /*
         * 12. MOVIMENTO NORMAL
         */

        if (
            absolutePriceChange >=
                MIN_PRICE_CHANGE_PERCENT
        ) {

            return SignalResult(
                isFalseSignal = false,
                confidence = 0.70,
                reason =
                    "Movimento de preço com confirmação parcial"
            )
        }

        /*
         * 13. CASO NEUTRO
         *
         * Não há evidência suficiente para chamar
         * o sinal de verdadeiro ou falso.
         */

        return SignalResult(
            isFalseSignal = false,
            confidence = 0.50,
            reason =
                "Não há evidência suficiente de falso sinal"
        )
    }
}

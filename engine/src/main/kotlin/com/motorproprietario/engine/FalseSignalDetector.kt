package com.motorproprietario.engine

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

    fun analyze(input: SignalInput): SignalResult {

        if (input.previousPrice <= 0.0) {
            return SignalResult(
                isFalseSignal = true,
                confidence = 1.0,
                reason = "Preço anterior inválido"
            )
        }

        val priceChange =
            ((input.price - input.previousPrice) / input.previousPrice) * 100.0

        val volumeRatio =
            if (input.averageVolume > 0.0) {
                input.volume / input.averageVolume
            } else {
                0.0
            }

        val weakMovement = kotlin.math.abs(priceChange) < 0.30
        val lowVolume = volumeRatio < 0.50

        val falseSignal = weakMovement && lowVolume

        val confidence = when {
            falseSignal -> 0.85
            lowVolume -> 0.65
            weakMovement -> 0.55
            else -> 0.20
        }

        val reason = when {
            falseSignal ->
                "Movimento fraco com volume abaixo da média"
            lowVolume ->
                "Volume abaixo da média"
            weakMovement ->
                "Variação de preço muito pequena"
            else ->
                "Sinal sem indícios fortes de falsidade"
        }

        return SignalResult(
            isFalseSignal = falseSignal,
            confidence = confidence,
            reason = reason
        )
    }
}

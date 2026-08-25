package com.motorproprietario.app

data class ScoreInput(
    val structure: Double,
    val trend: Double,
    val momentum: Double,
    val volume: Double,
    val volatility: Double,
    val fsi: Double,
    val multiTimeframe: Double
)

data class ScoreResult(
    val score: Double,
    val band: String
)

object ScoreEngine {

    private fun clamp(value: Double): Double {
        return value.coerceIn(0.0, 100.0)
    }

    fun calculate(input: ScoreInput): ScoreResult {

        val structure = clamp(input.structure)
        val trend = clamp(input.trend)
        val momentum = clamp(input.momentum)
        val volume = clamp(input.volume)
        val volatility = clamp(input.volatility)
        val fsi = clamp(input.fsi)
        val multiTimeframe = clamp(input.multiTimeframe)

        val score =
            structure * 0.20 +
            trend * 0.15 +
            momentum * 0.10 +
            volume * 0.10 +
            volatility * 0.10 +
            fsi * 0.20 +
            multiTimeframe * 0.15

        return ScoreResult(
            score = score.coerceIn(0.0, 100.0),
            band = classify(score)
        )
    }

    private fun classify(score: Double): String {
        return when {
            score < 40.0 -> "MUITO FRACO"
            score < 55.0 -> "FRACO"
            score < 65.0 -> "NEUTRO"
            score < 75.0 -> "MODERADO"
            score < 85.0 -> "FORTE"
            else -> "MUITO FORTE"
        }
    }
}

package com.motorproprietario.engine

data class ProprietaryScore(
    val score: Double,
    val level: String
)

class ProprietaryScoreCalculator {

    fun calculate(
        confluenceScore: Double,
        sequenceScore: Double,
        timeframeStrength: Double
    ): ProprietaryScore {

        val score = (
            confluenceScore * 0.40 +
            sequenceScore * 0.40 +
            timeframeStrength * 0.20
        ).coerceIn(0.0, 1.0)

        val level = when {
            score >= 0.80 -> "FORTE"
            score >= 0.65 -> "MODERADO"
            score >= 0.50 -> "FRACO"
            else -> "NEUTRO"
        }

        return ProprietaryScore(
            score = score,
            level = level
        )
    }
}

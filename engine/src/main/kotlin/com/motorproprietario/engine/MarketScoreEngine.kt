package com.motorproprietario.engine

data class MarketScore(
    val score: Double,
    val signal: String,
    val risk: String
)

class MarketScoreEngine {

    private val scoreCalculator = ProprietaryScoreCalculator()
    private val riskCalculator = RiskCalculator()

    fun calculate(
        confluenceScore: Double,
        sequenceScore: Double,
        timeframeStrength: Double,
        reversalProbability: Double,
        volatility: Double
    ): MarketScore {

        val proprietary = scoreCalculator.calculate(
            confluenceScore = confluenceScore,
            sequenceScore = sequenceScore,
            timeframeStrength = timeframeStrength
        )

        val risk = riskCalculator.calculate(
            proprietaryScore = proprietary.score,
            reversalProbability = reversalProbability,
            volatility = volatility
        )

        val signal = when {
            proprietary.score >= 0.80 && reversalProbability >= 0.70 -> "ENTRADA FORTE"
            proprietary.score >= 0.65 -> "ENTRADA MODERADA"
            proprietary.score >= 0.50 -> "OBSERVAÇÃO"
            else -> "AGUARDAR"
        }

        return MarketScore(
            score = proprietary.score,
            signal = signal,
            risk = risk.riskLevel
        )
    }
}

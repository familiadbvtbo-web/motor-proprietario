package com.motorproprietario.engine

data class RiskResult(
    val riskScore: Double,
    val riskLevel: String
)

class RiskCalculator {

    fun calculate(
        proprietaryScore: Double,
        reversalProbability: Double,
        volatility: Double
    ): RiskResult {

        val risk = (
            (1.0 - proprietaryScore) * 0.45 +
            (1.0 - reversalProbability) * 0.25 +
            volatility.coerceIn(0.0, 1.0) * 0.30
        ).coerceIn(0.0, 1.0)

        val level = when {
            risk >= 0.75 -> "ALTO"
            risk >= 0.50 -> "MODERADO"
            risk >= 0.25 -> "BAIXO"
            else -> "MUITO BAIXO"
        }

        return RiskResult(
            riskScore = risk,
            riskLevel = level
        )
    }
}

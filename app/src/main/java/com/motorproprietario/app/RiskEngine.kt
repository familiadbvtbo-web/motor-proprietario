package com.motorproprietario.app

data class RiskInput(
    val equity: Double,
    val riskPercent: Double,
    val entry: Double,
    val stop: Double
)

data class RiskResult(
    val riskAmount: Double,
    val stopDistance: Double,
    val positionSize: Double,
    val valid: Boolean
)

object RiskEngine {

    fun calculate(input: RiskInput): RiskResult {

        val equity = input.equity.coerceAtLeast(0.0)
        val riskPercent = input.riskPercent.coerceIn(0.0, 5.0)
        val stopDistance = kotlin.math.abs(input.entry - input.stop)

        val riskAmount =
            equity * (riskPercent / 100.0)

        val positionSize =
            if (stopDistance > 0.0) {
                riskAmount / stopDistance
            } else {
                0.0
            }

        val valid =
            equity > 0.0 &&
            riskAmount > 0.0 &&
            stopDistance > 0.0

        return RiskResult(
            riskAmount = riskAmount,
            stopDistance = stopDistance,
            positionSize = positionSize,
            valid = valid
        )
    }
}

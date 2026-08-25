package com.motorproprietario.app

data class AlertPolicy(
    val minScoreChange: Double = 10.0,
    val cooldownMinutes: Int = 15,
    val requireGoodData: Boolean = true
)

fun shouldAlert(previousScore: Double, currentScore: Double, quality: String, policy: AlertPolicy): Boolean {
    if (policy.requireGoodData && quality != "GOOD") return false
    return kotlin.math.abs(currentScore - previousScore) >= policy.minScoreChange
}

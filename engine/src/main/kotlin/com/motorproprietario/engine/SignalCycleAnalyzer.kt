package com.motorproprietario.engine

data class CycleAnalysis(
    val direction: String,
    val cycleStrength: Double,
    val reversalProbability: Double,
    val status: String
)

class SignalCycleAnalyzer {

    fun analyze(
        sequence: SignalSequence,
        currentDirection: String
    ): CycleAnalysis {

        val strength = sequence.sequenceScore

        val reversalProbability = when {
            sequence.consecutiveFailures >= 5 -> 0.85
            sequence.consecutiveFailures >= 4 -> 0.75
            sequence.consecutiveFailures >= 3 -> 0.60
            sequence.consecutiveFailures >= 2 -> 0.40
            else -> 0.15
        }

        val status = when {
            reversalProbability >= 0.80 -> "REVERSÃO FORTE"
            reversalProbability >= 0.60 -> "REVERSÃO EM FORMAÇÃO"
            reversalProbability >= 0.40 -> "ATENÇÃO"
            else -> "SEM CONFIRMAÇÃO"
        }

        return CycleAnalysis(
            direction = currentDirection,
            cycleStrength = strength,
            reversalProbability = reversalProbability,
            status = status
        )
    }
}

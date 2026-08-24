package com.motorproprietario.engine

data class SignalSequence(
    val falseSignalCount: Int,
    val consecutiveFailures: Int,
    val sequenceScore: Double
)

class SequenceCalculator {

    fun calculate(results: List<FalseSignalResult>): SignalSequence {

        var consecutive = 0
        var maximumConsecutive = 0

        for (result in results) {
            if (result.isFalse) {
                consecutive++
                maximumConsecutive = maxOf(maximumConsecutive, consecutive)
            } else {
                consecutive = 0
            }
        }

        val falseCount = results.count { it.isFalse }

        val score = when {
            maximumConsecutive >= 5 -> 1.0
            maximumConsecutive >= 4 -> 0.85
            maximumConsecutive >= 3 -> 0.70
            maximumConsecutive >= 2 -> 0.50
            falseCount >= 1 -> 0.25
            else -> 0.0
        }

        return SignalSequence(
            falseSignalCount = falseCount,
            consecutiveFailures = maximumConsecutive,
            sequenceScore = score
        )
    }
}

package com.motorproprietario.engine

data class ConfluenceResult(
    val confirmed: Boolean,
    val score: Double,
    val signals: List<String>
)

class ConfluenceAnalyzer {

    fun analyze(
        pattern: PatternResult,
        falseSignal: Boolean
    ): ConfluenceResult {

        val signals = mutableListOf<String>()

        if (pattern.detected) {
            signals.add(pattern.description)
        }

        if (!falseSignal) {
            signals.add("Sem falso sinal identificado")
        }

        val score = when {
            pattern.detected && !falseSignal -> 0.85
            pattern.detected -> 0.60
            else -> 0.30
        }

        return ConfluenceResult(
            confirmed = score >= 0.70,
            score = score,
            signals = signals
        )
    }
}

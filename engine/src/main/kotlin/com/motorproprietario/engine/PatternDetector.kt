package com.motorproprietario.engine

data class PatternResult(
    val detected: Boolean,
    val confidence: Double,
    val description: String
)

class PatternDetector {

    fun analyze(values: List<Double>): PatternResult {
        if (values.size < 3) {
            return PatternResult(
                detected = false,
                confidence = 0.0,
                description = "Dados insuficientes"
            )
        }

        val last = values.last()
        val previous = values[values.size - 2]

        val change = if (previous != 0.0) {
            (last - previous) / previous
        } else {
            0.0
        }

        return when {
            change > 0.02 -> PatternResult(
                detected = true,
                confidence = 0.70,
                description = "Movimento de alta detectado"
            )

            change < -0.02 -> PatternResult(
                detected = true,
                confidence = 0.70,
                description = "Movimento de baixa detectado"
            )

            else -> PatternResult(
                detected = false,
                confidence = 0.50,
                description = "Sem padrão significativo"
            )
        }
    }
}

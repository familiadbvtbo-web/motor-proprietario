package com.motorproprietario.engine

data class TimeframeResult(
    val timeframe: String,
    val direction: String,
    val strength: Double
)

class MultiTimeframeAnalyzer {

    fun analyze(
        timeframes: Map<String, List<Double>>
    ): List<TimeframeResult> {

        return timeframes.map { (timeframe, values) ->

            if (values.size < 2) {
                TimeframeResult(timeframe, "NEUTRO", 0.0)
            } else {
                val previous = values[values.size - 2]
                val current = values.last()

                val change = if (previous != 0.0) {
                    (current - previous) / previous
                } else {
                    0.0
                }

                val direction = when {
                    change > 0.02 -> "ALTA"
                    change < -0.02 -> "BAIXA"
                    else -> "NEUTRO"
                }

                TimeframeResult(
                    timeframe = timeframe,
                    direction = direction,
                    strength = kotlin.math.abs(change)
                )
            }
        }
    }
}

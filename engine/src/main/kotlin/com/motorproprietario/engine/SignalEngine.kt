package com.motorproprietario.engine

data class SignalResult(
    val signal: String,
    val confidence: Double,
    val reason: String
)

class SignalEngine {

    fun generate(
        pattern: PatternResult,
        confluence: ConfluenceResult,
        timeframes: List<TimeframeResult>
    ): SignalResult {

        val bullish = timeframes.count { it.direction == "ALTA" }
        val bearish = timeframes.count { it.direction == "BAIXA" }

        return when {
            confluence.confirmed && bullish > bearish -> SignalResult(
                "COMPRA",
                confluence.score,
                "Confluência confirmada com predominância de alta"
            )

            confluence.confirmed && bearish > bullish -> SignalResult(
                "VENDA",
                confluence.score,
                "Confluência confirmada com predominância de baixa"
            )

            else -> SignalResult(
                "NEUTRO",
                0.50,
                "Sem confirmação suficiente"
            )
        }
    }
}

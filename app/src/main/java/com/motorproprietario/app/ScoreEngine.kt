package com.motorproprietario.app

data class ScoreInput(
    val structure: Double,
    val trend: Double,
    val momentum: Double,
    val volume: Double,
    val volatility: Double,
    val fsi: Double,
    val multiTimeframe: Double
)

data class ScoreResult(
    val score: Double,
    val band: String
)

object ScoreEngine {

    private fun clamp(
        value: Double
    ): Double {
        return value.coerceIn(
            0.0,
            100.0
        )
    }

    fun calculate(
        input: ScoreInput
    ): ScoreResult {

        val structure =
            clamp(input.structure)

        val trend =
            clamp(input.trend)

        val momentum =
            clamp(input.momentum)

        val volume =
            clamp(input.volume)

        val volatility =
            clamp(input.volatility)

        val fsi =
            clamp(input.fsi)

        val mtf =
            clamp(input.multiTimeframe)

        /*
         * Força bruta do cenário.
         */
        val baseScore =
            structure * 0.22 +
            trend * 0.22 +
            momentum * 0.14 +
            volume * 0.10 +
            volatility * 0.08 +
            mtf * 0.24

        /*
         * FSI é risco.
         * Portanto entra como penalização.
         */
        val fsiPenalty =
            fsi * 0.35

        val score =
            (
                baseScore -
                    fsiPenalty
            ).coerceIn(
                0.0,
                100.0
            )

        return ScoreResult(
            score = score,
            band = classify(score)
        )
    }

    private fun classify(
        score: Double
    ): String {

        return when {
            score < 25.0 ->
                "MUITO FRACO"

            score < 40.0 ->
                "FRACO"

            score < 55.0 ->
                "NEUTRO"

            score < 70.0 ->
                "MODERADO"

            score < 85.0 ->
                "FORTE"

            else ->
                "MUITO FORTE"
        }
    }
}

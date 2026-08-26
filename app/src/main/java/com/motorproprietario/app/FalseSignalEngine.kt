package com.motorproprietario.app

data class FalseSignalInput(
    val structureContradiction: Double,
    val momentumDivergence: Double,
    val volumeMismatch: Double,
    val confirmationFailure: Double,
    val timeframeConflict: Double
)

data class FalseSignalResult(
    val risk: Double,
    val level: String,
    val blocked: Boolean
)

object FalseSignalEngine {

    fun evaluate(
        input: FalseSignalInput
    ): FalseSignalResult {

        val structure =
            input.structureContradiction
                .coerceIn(0.0, 100.0)

        val momentum =
            input.momentumDivergence
                .coerceIn(0.0, 100.0)

        val volume =
            input.volumeMismatch
                .coerceIn(0.0, 100.0)

        val confirmation =
            input.confirmationFailure
                .coerceIn(0.0, 100.0)

        val timeframe =
            input.timeframeConflict
                .coerceIn(0.0, 100.0)

        val values =
            listOf(
                structure,
                momentum,
                volume,
                confirmation,
                timeframe
            )

        val baseRisk =
            structure * 0.25 +
            momentum * 0.20 +
            volume * 0.15 +
            confirmation * 0.20 +
            timeframe * 0.20

        /*
         * Vários sinais de alerta simultâneos
         * aumentam a possibilidade de armadilha.
         */
        val severeFactors =
            values.count {
                it >= 70.0
            }

        val interaction =
            when {
                severeFactors >= 4 -> 15.0
                severeFactors >= 3 -> 10.0
                severeFactors >= 2 -> 5.0
                else -> 0.0
            }

        /*
         * Um único fator extremamente elevado
         * também merece atenção.
         */
        val extremeFactor =
            if (
                values.any {
                    it >= 90.0
                }
            ) {
                5.0
            } else {
                0.0
            }

        val normalizedRisk =
            (
                baseRisk +
                    interaction +
                    extremeFactor
            ).coerceIn(
                0.0,
                100.0
            )

        val level =
            when {
                normalizedRisk >= 90.0 ->
                    "CRÍTICO"

                normalizedRisk >= 75.0 ->
                    "MUITO ALTO"

                normalizedRisk >= 60.0 ->
                    "ALTO"

                normalizedRisk >= 40.0 ->
                    "MODERADO"

                normalizedRisk >= 20.0 ->
                    "BAIXO"

                else ->
                    "MUITO BAIXO"
            }

        return FalseSignalResult(
            risk = normalizedRisk,
            level = level,
            blocked =
                normalizedRisk >= 75.0
        )
    }
}

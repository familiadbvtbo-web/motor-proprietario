package com.motorproprietario.app

data class FsiInput(
    val structureContradiction: Double,
    val momentumDivergence: Double,
    val volumeMismatch: Double,
    val confirmationFailure: Double,
    val timeframeConflict: Double
)

data class FsiResult(
    val value: Double,
    val level: String,
    val blocked: Boolean,
    val extraConfirmationRequired: Boolean
)

object FsiEngine {

    private fun clamp(value: Double): Double {
        return value.coerceIn(0.0, 100.0)
    }

    fun calculate(input: FsiInput): FsiResult {

        val structure = clamp(input.structureContradiction)
        val momentum = clamp(input.momentumDivergence)
        val volume = clamp(input.volumeMismatch)
        val confirmation = clamp(input.confirmationFailure)
        val timeframe = clamp(input.timeframeConflict)

        val fsi =
            structure * 0.25 +
            momentum * 0.20 +
            volume * 0.15 +
            confirmation * 0.20 +
            timeframe * 0.20

        val normalized = clamp(fsi)

        return when {
            normalized >= 80.0 -> FsiResult(
                value = normalized,
                level = "CRÍTICO",
                blocked = true,
                extraConfirmationRequired = true
            )

            normalized >= 60.0 -> FsiResult(
                value = normalized,
                level = "ALTO",
                blocked = false,
                extraConfirmationRequired = true
            )

            normalized >= 40.0 -> FsiResult(
                value = normalized,
                level = "MODERADO",
                blocked = false,
                extraConfirmationRequired = false
            )

            normalized >= 20.0 -> FsiResult(
                value = normalized,
                level = "BAIXO",
                blocked = false,
                extraConfirmationRequired = false
            )

            else -> FsiResult(
                value = normalized,
                level = "MUITO BAIXO",
                blocked = false,
                extraConfirmationRequired = false
            )
        }
    }
}

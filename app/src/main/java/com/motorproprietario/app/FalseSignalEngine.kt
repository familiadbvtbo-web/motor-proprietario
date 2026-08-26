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
    val blocked: Boolean,

    val structureRisk: Double,
    val momentumRisk: Double,
    val volumeRisk: Double,
    val confirmationRisk: Double,
    val timeframeRisk: Double,

    val severeFactors: Int,
    val extremeFactorDetected: Boolean
)

object FalseSignalEngine {

    private fun clamp(
        value: Double
    ): Double {

        return value.coerceIn(
            0.0,
            100.0
        )
    }

    fun evaluate(
        input: FalseSignalInput
    ): FalseSignalResult {

        val structure =
            clamp(
                input.structureContradiction
            )

        val momentum =
            clamp(
                input.momentumDivergence
            )

        val volume =
            clamp(
                input.volumeMismatch
            )

        val confirmation =
            clamp(
                input.confirmationFailure
            )

        val timeframe =
            clamp(
                input.timeframeConflict
            )

        val values =
            listOf(
                structure,
                momentum,
                volume,
                confirmation,
                timeframe
            )

        /*
         * ==================================
         * RISCO BASE
         * ==================================
         *
         * Os pesos mantêm a importância relativa
         * da estrutura, momentum, confirmação,
         * timeframe e volume.
         */

        val baseRisk =
            structure * 0.25 +
            momentum * 0.20 +
            volume * 0.15 +
            confirmation * 0.20 +
            timeframe * 0.20

        /*
         * ==================================
         * FATORES SEVEROS
         * ==================================
         */

        val severeFactors =
            values.count {
                it >= 70.0
            }

        /*
         * Quando vários fatores estão elevados
         * simultaneamente, existe interação entre
         * os problemas.
         */

        val interaction =
            when {

                severeFactors >= 4 ->
                    15.0

                severeFactors >= 3 ->
                    10.0

                severeFactors >= 2 ->
                    5.0

                else ->
                    0.0
            }

        /*
         * ==================================
         * FATOR EXTREMO
         * ==================================
         */

        val extremeFactorDetected =
            values.any {
                it >= 90.0
            }

        val extremeFactor =
            if (
                extremeFactorDetected
            ) {
                5.0
            } else {
                0.0
            }

        /*
         * ==================================
         * CONFLITO ESTRUTURAL
         * ==================================
         *
         * Estrutura + momentum muito ruins
         * ao mesmo tempo merecem penalização
         * adicional.
         */

        val structureMomentumConflict =
            if (
                structure >= 70.0 &&
                momentum >= 70.0
            ) {
                5.0
            } else {
                0.0
            }

        /*
         * ==================================
         * CONFIRMAÇÃO + TIMEFRAME
         * ==================================
         *
         * Um sinal sem confirmação em vários
         * timeframes é especialmente vulnerável.
         */

        val confirmationTimeframeConflict =
            if (
                confirmation >= 70.0 &&
                timeframe >= 70.0
            ) {
                5.0
            } else {
                0.0
            }

        val normalizedRisk =
            (
                baseRisk +
                    interaction +
                    extremeFactor +
                    structureMomentumConflict +
                    confirmationTimeframeConflict
            ).coerceIn(
                0.0,
                100.0
            )

        /*
         * ==================================
         * CLASSIFICAÇÃO
         * ==================================
         */

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

        /*
         * ==================================
         * BLOQUEIO
         * ==================================
         *
         * O bloqueio é deliberadamente conservador.
         *
         * O APK é analista:
         * quando o risco é muito alto,
         * ele não apresenta o cenário como
         * executável.
         */

        val blocked =
            normalizedRisk >= 75.0

        return FalseSignalResult(

            risk =
                normalizedRisk,

            level =
                level,

            blocked =
                blocked,

            structureRisk =
                structure,

            momentumRisk =
                momentum,

            volumeRisk =
                volume,

            confirmationRisk =
                confirmation,

            timeframeRisk =
                timeframe,

            severeFactors =
                severeFactors,

            extremeFactorDetected =
                extremeFactorDetected
        )
    }
}

package com.motorproprietario.app

data class FinalMotorInput(
    val market: MarketData,
    val now: Long,
    val sequence: SequenceInput,
    val sequenceStage: SequenceStage,
    val falseSignal: FalseSignalInput,

    /*
     * Probabilidade calculada pelo motor.
     * Opcional para preservar compatibilidade.
     */
    val probability: ProbabilityResult? = null
)

data class FinalMotorResult(
    val score: ScoreResult,
    val fsi: FsiResult,
    val falseSignal: FalseSignalResult,
    val sequence: SequenceResult,
    val decision: DecisionResult,
    val marketUsable: Boolean,
    val probability: ProbabilityResult? = null
)

object FinalMotorEngine {

    fun evaluate(
        input: FinalMotorInput
    ): FinalMotorResult {

        val marketUsable =
            input.market.isUsable(
                input.now
            )

        val fsi =
            FsiEngine.calculate(
                FsiInput(
                    structureContradiction =
                        input.falseSignal
                            .structureContradiction,

                    momentumDivergence =
                        input.falseSignal
                            .momentumDivergence,

                    volumeMismatch =
                        input.falseSignal
                            .volumeMismatch,

                    confirmationFailure =
                        input.falseSignal
                            .confirmationFailure,

                    timeframeConflict =
                        input.falseSignal
                            .timeframeConflict
                )
            )

        val falseSignal =
            FalseSignalEngine.evaluate(
                input.falseSignal
            )

        val score =
            ScoreEngine.calculate(
                ScoreInput(
                    structure =
                        input.market.structure,

                    trend =
                        input.market.trend,

                    momentum =
                        input.market.momentum,

                    volume =
                        input.market.volume,

                    volatility =
                        input.market.volatility,

                    /*
                     * Agora FSI é risco.
                     */
                    fsi =
                        fsi.value,

                    multiTimeframe =
                        input.market.multiTimeframe
                )
            )

        val sequence =
            SequenceEngine.advance(
                input.sequenceStage,
                input.sequence
            )

        val decision =
            if (
                !marketUsable
            ) {

                DecisionResult(
                    decision = "AGUARDAR",
                    reason = "MARKET_DATA_INVALID",
                    executableInPaper = false
                )

            } else if (
                falseSignal.blocked
            ) {

                DecisionResult(
                    decision = "AGUARDAR",
                    reason = "FALSE_SIGNAL_BLOCK",
                    executableInPaper = false
                )

            } else {

                DecisionEngine.evaluate(
                    DecisionInput(
                        score =
                            score.score,

                        fsi =
                            fsi,

                        sequenceConfirmed =
                            sequence.confirmed,

                        probability =
                            input.probability
                    )
                )
            }

        return FinalMotorResult(
            score =
                score,

            fsi =
                fsi,

            falseSignal =
                falseSignal,

            sequence =
                sequence,

            decision =
                decision,

            marketUsable =
                marketUsable,

            probability =
                input.probability
        )
    }
}

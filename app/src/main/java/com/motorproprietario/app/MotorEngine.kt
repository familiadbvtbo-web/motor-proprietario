package com.motorproprietario.app

data class MotorInput(
    val scoreInput: ScoreInput,
    val fsiInput: FsiInput,
    val sequenceInput: SequenceInput,
    val currentStage: SequenceStage
)

data class MotorResult(
    val score: ScoreResult,
    val fsi: FsiResult,
    val sequence: SequenceResult,
    val decision: DecisionResult
)

object MotorEngine {

    fun evaluate(input: MotorInput): MotorResult {

        val scoreResult =
            ScoreEngine.calculate(input.scoreInput)

        val fsiResult =
            FsiEngine.calculate(input.fsiInput)

        val sequenceResult =
            SequenceEngine.advance(
                input.currentStage,
                input.sequenceInput
            )

        val decisionResult =
            DecisionEngine.evaluate(
                DecisionInput(
                    score = scoreResult.score,
                    fsi = fsiResult,
                    sequenceConfirmed = sequenceResult.confirmed
                )
            )

        return MotorResult(
            score = scoreResult,
            fsi = fsiResult,
            sequence = sequenceResult,
            decision = decisionResult
        )
    }
}

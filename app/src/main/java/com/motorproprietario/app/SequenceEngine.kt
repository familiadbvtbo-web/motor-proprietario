package com.motorproprietario.app

enum class SequenceStage {
    S0,
    S1,
    S2,
    S3,
    S4
}

data class SequenceInput(
    val signalDetected: Boolean,
    val confirmation: Boolean,
    val continuation: Boolean,
    val invalidated: Boolean
)

data class SequenceResult(
    val stage: SequenceStage,
    val confirmed: Boolean
)

object SequenceEngine {

    fun advance(
        current: SequenceStage,
        input: SequenceInput
    ): SequenceResult {

        if (input.invalidated) {
            return SequenceResult(
                stage = SequenceStage.S0,
                confirmed = false
            )
        }

        return when (current) {

            SequenceStage.S0 -> {
                if (input.signalDetected) {
                    SequenceResult(
                        stage = SequenceStage.S1,
                        confirmed = false
                    )
                } else {
                    SequenceResult(
                        stage = SequenceStage.S0,
                        confirmed = false
                    )
                }
            }

            SequenceStage.S1 -> {
                if (input.confirmation) {
                    SequenceResult(
                        stage = SequenceStage.S2,
                        confirmed = false
                    )
                } else {
                    SequenceResult(
                        stage = SequenceStage.S1,
                        confirmed = false
                    )
                }
            }

            SequenceStage.S2 -> {
                if (input.continuation) {
                    SequenceResult(
                        stage = SequenceStage.S3,
                        confirmed = false
                    )
                } else {
                    SequenceResult(
                        stage = SequenceStage.S2,
                        confirmed = false
                    )
                }
            }

            SequenceStage.S3 -> {
                if (input.continuation) {
                    SequenceResult(
                        stage = SequenceStage.S4,
                        confirmed = true
                    )
                } else {
                    SequenceResult(
                        stage = SequenceStage.S3,
                        confirmed = false
                    )
                }
            }

            SequenceStage.S4 -> {
                SequenceResult(
                    stage = SequenceStage.S4,
                    confirmed = true
                )
            }
        }
    }
}

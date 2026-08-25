package com.motorproprietario.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotorEngineTest {

    @Test
    fun fsiCriticoBloqueiaDecisao() {

        val result = MotorEngine.evaluate(
            MotorInput(
                scoreInput = ScoreInput(
                    structure = 100.0,
                    trend = 100.0,
                    momentum = 100.0,
                    volume = 100.0,
                    volatility = 100.0,
                    fsi = 100.0,
                    multiTimeframe = 100.0
                ),
                fsiInput = FsiInput(
                    structureContradiction = 100.0,
                    momentumDivergence = 100.0,
                    volumeMismatch = 100.0,
                    confirmationFailure = 100.0,
                    timeframeConflict = 100.0
                ),
                sequenceInput = SequenceInput(
                    signalDetected = true,
                    confirmation = true,
                    continuation = true,
                    invalidated = false
                ),
                currentStage = SequenceStage.S3
            )
        )

        assertTrue(result.fsi.blocked)
        assertEquals("AGUARDAR", result.decision.decision)
        assertFalse(result.decision.executableInPaper)
    }

    @Test
    fun sequenciaNaoConfirmadaNaoEntra() {

        val result = MotorEngine.evaluate(
            MotorInput(
                scoreInput = ScoreInput(
                    structure = 90.0,
                    trend = 90.0,
                    momentum = 90.0,
                    volume = 90.0,
                    volatility = 90.0,
                    fsi = 10.0,
                    multiTimeframe = 90.0
                ),
                fsiInput = FsiInput(
                    structureContradiction = 0.0,
                    momentumDivergence = 0.0,
                    volumeMismatch = 0.0,
                    confirmationFailure = 0.0,
                    timeframeConflict = 0.0
                ),
                sequenceInput = SequenceInput(
                    signalDetected = true,
                    confirmation = false,
                    continuation = false,
                    invalidated = false
                ),
                currentStage = SequenceStage.S0
            )
        )

        assertEquals("AGUARDAR", result.decision.decision)
        assertFalse(result.decision.executableInPaper)
    }

    @Test
    fun scoreNeutroAguarda() {

        val result = MotorEngine.evaluate(
            MotorInput(
                scoreInput = ScoreInput(
                    structure = 50.0,
                    trend = 50.0,
                    momentum = 50.0,
                    volume = 50.0,
                    volatility = 50.0,
                    fsi = 50.0,
                    multiTimeframe = 50.0
                ),
                fsiInput = FsiInput(
                    structureContradiction = 30.0,
                    momentumDivergence = 30.0,
                    volumeMismatch = 30.0,
                    confirmationFailure = 30.0,
                    timeframeConflict = 30.0
                ),
                sequenceInput = SequenceInput(
                    signalDetected = true,
                    confirmation = true,
                    continuation = true,
                    invalidated = false
                ),
                currentStage = SequenceStage.S3
            )
        )

        assertEquals("AGUARDAR", result.decision.decision)
    }
}

package com.motorproprietario.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FinalMotorEngineTest {

    @Test
    fun staleMarketDataCannotProduceExecutableDecision() {

        val now = 1_000_000L

        val result = FinalMotorEngine.evaluate(
            FinalMotorInput(
                market = MarketData(
                    asset = "TEST",
                    timestamp = now - 120_000L,
                    price = 100.0,
                    structure = 90.0,
                    trend = 90.0,
                    momentum = 90.0,
                    volume = 90.0,
                    volatility = 90.0,
                    fsi = 10.0,
                    multiTimeframe = 90.0,
                    dataQuality = "GOOD"
                ),
                now = now,
                sequence = SequenceInput(
                    signalDetected = true,
                    confirmation = true,
                    continuation = true,
                    invalidated = false
                ),
                sequenceStage = SequenceStage.S3,
                falseSignal = FalseSignalInput(
                    structureContradiction = 0.0,
                    momentumDivergence = 0.0,
                    volumeMismatch = 0.0,
                    confirmationFailure = 0.0,
                    timeframeConflict = 0.0
                )
            )
        )

        assertFalse(result.marketUsable)
        assertEquals(
            "AGUARDAR",
            result.decision.decision
        )
    }
}

package com.motorproprietario.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveGateTest {

    @Test
    fun paperModeAlwaysBlocksLive() {

        val result = LiveGate.evaluate(
            LiveGateInput(
                enabledByConfiguration = true,
                devicePassed = true,
                dataGood = true,
                marketFresh = true,
                killSwitchActive = false,
                paperMode = true,
                decision = "COMPRA"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.reason == "PAPER_MODE")
    }

    @Test
    fun disabledConfigurationBlocksLive() {

        val result = LiveGate.evaluate(
            LiveGateInput(
                enabledByConfiguration = false,
                devicePassed = true,
                dataGood = true,
                marketFresh = true,
                killSwitchActive = false,
                paperMode = false,
                decision = "COMPRA"
            )
        )

        assertFalse(result.allowed)
    }

    @Test
    fun killSwitchBlocksLive() {

        val result = LiveGate.evaluate(
            LiveGateInput(
                enabledByConfiguration = true,
                devicePassed = true,
                dataGood = true,
                marketFresh = true,
                killSwitchActive = true,
                paperMode = false,
                decision = "COMPRA"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.reason == "KILL_SWITCH")
    }

    @Test
    fun validLiveConditionsCanPassGate() {

        val result = LiveGate.evaluate(
            LiveGateInput(
                enabledByConfiguration = true,
                devicePassed = true,
                dataGood = true,
                marketFresh = true,
                killSwitchActive = false,
                paperMode = false,
                decision = "COMPRA"
            )
        )

        assertTrue(result.allowed)
        assertTrue(result.reason == "LIVE_GATE_PASSED")
    }
}

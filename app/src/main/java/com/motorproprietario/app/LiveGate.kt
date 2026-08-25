package com.motorproprietario.app

data class LiveGateInput(
    val enabledByConfiguration: Boolean,
    val devicePassed: Boolean,
    val dataGood: Boolean,
    val marketFresh: Boolean,
    val killSwitchActive: Boolean,
    val paperMode: Boolean,
    val decision: String
)

data class LiveGateResult(
    val allowed: Boolean,
    val reason: String
)

object LiveGate {

    fun evaluate(input: LiveGateInput): LiveGateResult {

        if (input.paperMode) {
            return LiveGateResult(
                false,
                "PAPER_MODE"
            )
        }

        if (!input.enabledByConfiguration) {
            return LiveGateResult(
                false,
                "LIVE_DISABLED"
            )
        }

        if (!input.devicePassed) {
            return LiveGateResult(
                false,
                "DEVICE_GATE"
            )
        }

        if (!input.dataGood) {
            return LiveGateResult(
                false,
                "DATA_QUALITY"
            )
        }

        if (!input.marketFresh) {
            return LiveGateResult(
                false,
                "STALE_DATA"
            )
        }

        if (input.killSwitchActive) {
            return LiveGateResult(
                false,
                "KILL_SWITCH"
            )
        }

        if (
            input.decision != "COMPRA" &&
            input.decision != "VENDA"
        ) {
            return LiveGateResult(
                false,
                "NO_EXECUTABLE_DECISION"
            )
        }

        return LiveGateResult(
            true,
            "LIVE_GATE_PASSED"
        )
    }
}

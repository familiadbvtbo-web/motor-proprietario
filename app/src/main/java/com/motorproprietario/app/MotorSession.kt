package com.motorproprietario.app

data class MotorSessionResult(
    val motor: FinalMotorResult,
    val risk: RiskResult?,
    val paperTrade: PaperTradeResult?
)

object MotorSession {

    fun evaluate(
        input: FinalMotorInput,
        equity: Double,
        riskPercent: Double,
        entry: Double,
        stop: Double,
        target: Double,
        quantity: Double,
        now: Long
    ): MotorSessionResult {

        val motor = FinalMotorEngine.evaluate(input)

        if (motor.decision.decision == "AGUARDAR") {
            return MotorSessionResult(
                motor = motor,
                risk = null,
                paperTrade = null
            )
        }

        val risk = RiskEngine.calculate(
            RiskInput(
                equity = equity,
                riskPercent = riskPercent,
                entry = entry,
                stop = stop
            )
        )

        if (!risk.valid) {
            return MotorSessionResult(
                motor = motor.copy(
                    decision = DecisionResult(
                        decision = "AGUARDAR",
                        reason = "RISK_INVALID",
                        executableInPaper = false
                    )
                ),
                risk = risk,
                paperTrade = null
            )
        }

        if (!motor.decision.executableInPaper) {
            return MotorSessionResult(
                motor = motor,
                risk = risk,
                paperTrade = null
            )
        }

        val paperTrade = PaperTradingEngine.open(
            asset = input.market.asset,
            side = motor.decision.decision,
            entry = entry,
            stop = stop,
            target = target,
            quantity = quantity,
            now = now
        )

        return MotorSessionResult(
            motor = motor,
            risk = risk,
            paperTrade = paperTrade
        )
    }
}

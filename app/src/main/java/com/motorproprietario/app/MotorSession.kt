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

        /*
         * Primeira barreira:
         * dados de mercado precisam estar utilizáveis.
         *
         * Para Forex, quando bid/ask estiverem disponíveis,
         * validamos também a integridade desses dados.
         */
        val marketUsable = if (
            input.market.bid > 0.0 &&
            input.market.ask > 0.0
        ) {
            input.market.isForexUsable(now)
        } else {
            input.market.isUsable(now)
        }

        if (!marketUsable) {

            val motor = FinalMotorEngine.evaluate(input)

            val blockedMotor = motor.copy(
                decision = DecisionResult(
                    decision = "AGUARDAR",
                    reason = "MARKET_DATA_INVALID",
                    executableInPaper = false
                )
            )

            return MotorSessionResult(
                motor = blockedMotor,
                risk = null,
                paperTrade = null
            )
        }

        /*
         * Executa o núcleo proprietário.
         */
        val motor = FinalMotorEngine.evaluate(input)

        /*
         * Qualquer bloqueio do motor encerra o ciclo.
         */
        if (motor.decision.decision == "AGUARDAR") {

            return MotorSessionResult(
                motor = motor,
                risk = null,
                paperTrade = null
            )
        }

        /*
         * Calcula o risco somente depois
         * de o sinal passar pelo motor.
         */
        val risk = RiskEngine.calculate(
            RiskInput(
                equity = equity,
                riskPercent = riskPercent,
                entry = entry,
                stop = stop
            )
        )

        /*
         * Risco inválido bloqueia a operação.
         */
        if (!risk.valid) {

            val blockedMotor = motor.copy(
                decision = DecisionResult(
                    decision = "AGUARDAR",
                    reason = "RISK_INVALID",
                    executableInPaper = false
                )
            )

            return MotorSessionResult(
                motor = blockedMotor,
                risk = risk,
                paperTrade = null
            )
        }

        /*
         * Segurança adicional:
         * somente decisões explicitamente liberadas
         * para Paper Trading podem chegar ao simulador.
         */
        if (!motor.decision.executableInPaper) {

            return MotorSessionResult(
                motor = motor,
                risk = risk,
                paperTrade = null
            )
        }

        /*
         * Paper Trading.
         *
         * Nenhuma ordem real é enviada.
         */
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

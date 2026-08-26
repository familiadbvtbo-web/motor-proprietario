package com.motorproprietario.app

data class FinalMotorInput(
    val market: MarketData,
    val now: Long,
    val sequence: SequenceInput,
    val sequenceStage: SequenceStage,
    val falseSignal: FalseSignalInput,

    val probability: ProbabilityResult? = null,

    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0,

    val deterministicRisk: DeterministicRiskResult? = null
)

data class FinalMotorResult(
    val score: ScoreResult,
    val fsi: FsiResult,
    val falseSignal: FalseSignalResult,
    val sequence: SequenceResult,
    val decision: DecisionResult,
    val marketUsable: Boolean,

    val probability: ProbabilityResult? = null,

    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0,

    val deterministicRisk: DeterministicRiskResult? = null
)

object FinalMotorEngine {

    fun evaluate(
        input: FinalMotorInput
    ): FinalMotorResult {

        /*
         * ==================================
         * 1. QUALIDADE DOS DADOS
         * ==================================
         */

        val marketUsable =
            input.market.isUsable(
                input.now
            )

        /*
         * ==================================
         * 2. FSI
         * ==================================
         */

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

        /*
         * ==================================
         * 3. FALSO SINAL
         * ==================================
         */

        val falseSignal =
            FalseSignalEngine.evaluate(
                input.falseSignal
            )

        /*
         * ==================================
         * 4. SCORE QUANTITATIVO
         * ==================================
         */

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

                    fsi =
                        fsi.value,

                    multiTimeframe =
                        input.market.multiTimeframe
                )
            )

        /*
         * ==================================
         * 5. SEQUÊNCIA
         * ==================================
         */

        val sequence =
            SequenceEngine.advance(
                input.sequenceStage,
                input.sequence
            )

        /*
         * ==================================
         * 6. RISCO DETERMINÍSTICO
         * ==================================
         *
         * Se já foi calculado pelo chamador,
         * utilizamos diretamente.
         *
         * Isso evita recalcular a mesma camada
         * e permite que a decisão final utilize
         * o mesmo risco apresentado na interface.
         */

        val deterministicRisk =
            input.deterministicRisk

        /*
         * ==================================
         * 7. RISCO FINAL
         * ==================================
         *
         * O maior risco entre:
         *
         * FSI
         * Falso Sinal
         * Determinismo
         *
         * protege a decisão.
         */

        val finalFalseSignalRisk =
            maxOf(

                fsi.value,

                falseSignal.risk,

                deterministicRisk
                    ?.falseSignalRisk
                    ?: 0.0
            )

        /*
         * ==================================
         * 8. BLOQUEIO DETERMINÍSTICO
         * ==================================
         */

        val deterministicBlocked =
            deterministicRisk
                ?.blocked
                ?: false

        /*
         * ==================================
         * 9. DECISÃO FINAL
         * ==================================
         */

        val decision =
            when {

                /*
                 * Dados inválidos.
                 */

                !marketUsable -> {

                    DecisionResult(

                        decision =
                            "AGUARDAR",

                        reason =
                            "MARKET_DATA_INVALID",

                        executableInPaper =
                            false,

                        buyProbability =
                            input.probability
                                ?.buyProbability
                                ?: 0.0,

                        sellProbability =
                            input.probability
                                ?.sellProbability
                                ?: 0.0,

                        neutralProbability =
                            input.probability
                                ?.neutralProbability
                                ?: 100.0,

                        deterministicConfidence =
                            input.deterministicConfidence,

                        falseSignalRisk =
                            finalFalseSignalRisk,

                        mtfConfluence =
                            input.market
                                .multiTimeframe
                    )
                }

                /*
                 * Risco determinístico crítico.
                 */

                deterministicBlocked -> {

                    DecisionResult(

                        decision =
                            "AGUARDAR",

                        reason =
                            "DETERMINISTIC_RISK_BLOCK",

                        executableInPaper =
                            false,

                        buyProbability =
                            input.probability
                                ?.buyProbability
                                ?: 0.0,

                        sellProbability =
                            input.probability
                                ?.sellProbability
                                ?: 0.0,

                        neutralProbability =
                            input.probability
                                ?.neutralProbability
                                ?: 100.0,

                        deterministicConfidence =
                            input.deterministicConfidence,

                        falseSignalRisk =
                            finalFalseSignalRisk,

                        mtfConfluence =
                            input.market
                                .multiTimeframe
                    )
                }

                /*
                 * Falso sinal crítico.
                 */

                falseSignal.blocked ||
                fsi.blocked -> {

                    DecisionResult(

                        decision =
                            "AGUARDAR",

                        reason =
                            "FALSE_SIGNAL_BLOCK",

                        executableInPaper =
                            false,

                        buyProbability =
                            input.probability
                                ?.buyProbability
                                ?: 0.0,

                        sellProbability =
                            input.probability
                                ?.sellProbability
                                ?: 0.0,

                        neutralProbability =
                            input.probability
                                ?.neutralProbability
                                ?: 100.0,

                        deterministicConfidence =
                            input.deterministicConfidence,

                        falseSignalRisk =
                            finalFalseSignalRisk,

                        mtfConfluence =
                            input.market
                                .multiTimeframe
                    )
                }

                /*
                 * Caso normal.
                 *
                 * A decisão recebe:
                 *
                 * Score
                 * Probabilidade
                 * Determinismo
                 * FSI
                 * MTF
                 * Sequência
                 */

                else -> {

                    DecisionEngine.evaluate(

                        DecisionInput(

                            score =
                                score.score,

                            fsi =
                                fsi,

                            sequenceConfirmed =
                                sequence.confirmed,

                            probability =
                                input.probability,

                            deterministicBuy =
                                input.deterministicBuy,

                            deterministicSell =
                                input.deterministicSell,

                            deterministicNeutral =
                                input.deterministicNeutral,

                            deterministicConfidence =
                                input.deterministicConfidence,

                            falseSignalRisk =
                                finalFalseSignalRisk,

                            mtfConfluence =
                                input.market
                                    .multiTimeframe
                        )
                    )
                }
            }

        /*
         * ==================================
         * 10. RESULTADO COMPLETO
         * ==================================
         */

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
                input.probability,

            deterministicBuy =
                input.deterministicBuy,

            deterministicSell =
                input.deterministicSell,

            deterministicNeutral =
                input.deterministicNeutral,

            deterministicConfidence =
                input.deterministicConfidence,

            deterministicRisk =
                deterministicRisk
        )
    }
}

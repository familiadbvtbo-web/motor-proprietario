package com.motorproprietario.app

data class FinalMotorInput(
    val market: MarketData,
    val now: Long,
    val sequence: SequenceInput,
    val sequenceStage: SequenceStage,
    val falseSignal: FalseSignalInput,

    /*
     * Resultado do motor probabilístico.
     */
    val probability: ProbabilityResult? = null,

    /*
     * Resultado do motor determinístico.
     *
     * Opcional nesta etapa para manter
     * compatibilidade com o projeto atual.
     */
    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0
)

data class FinalMotorResult(
    val score: ScoreResult,
    val fsi: FsiResult,
    val falseSignal: FalseSignalResult,
    val sequence: SequenceResult,
    val decision: DecisionResult,
    val marketUsable: Boolean,
    val probability: ProbabilityResult? = null,

    /*
     * Resultado determinístico utilizado
     * na decisão final.
     */
    val deterministicBuy: Double = 50.0,
    val deterministicSell: Double = 50.0,
    val deterministicNeutral: Double = 50.0,
    val deterministicConfidence: Double = 50.0
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

                    /*
                     * O FSI é risco.
                     */
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
         * 6. RISCO FINAL DE FALSO SINAL
         * ==================================
         *
         * O maior valor entre FSI e o motor
         * de falso sinal passa a proteger
         * a decisão.
         */

        val falseSignalRisk =
            maxOf(
                fsi.value,
                falseSignal.risk
            )

        /*
         * ==================================
         * 7. DECISÃO FINAL
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
                            falseSignalRisk,

                        mtfConfluence =
                            input.market.multiTimeframe
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
                            falseSignalRisk,

                        mtfConfluence =
                            input.market.multiTimeframe
                    )
                }

                /*
                 * Caso normal:
                 *
                 * Probabilidade +
                 * Determinismo +
                 * FSI +
                 * MTF +
                 * Sequência.
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
                                falseSignalRisk,

                            mtfConfluence =
                                input.market.multiTimeframe
                        )
                    )
                }
            }

        /*
         * ==================================
         * 8. RESULTADO COMPLETO
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
                input.deterministicConfidence
        )
    }
}

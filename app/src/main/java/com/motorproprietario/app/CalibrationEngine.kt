package com.motorproprietario.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CalibrationEngine {

    /**
     * Executa a calibração dos pesos
     *
     * probabilityWeight + deterministicWeight = 1.0
     *
     * O objetivo desta primeira versão é:
     *
     * 1. testar candidatos;
     * 2. medir desempenho no treino;
     * 3. validar os melhores candidatos;
     * 4. testar a configuração escolhida fora da amostra;
     * 5. somente então marcar como aceita.
     */
    fun calibrate(
        signals: List<BacktestSignal>,
        candles: List<MarketCandle>,
        config: BacktestConfig
    ): CalibrationResult {

        if (
            signals.isEmpty() ||
            candles.isEmpty()
        ) {

            return rejectedResult(
                reason =
                    "DADOS_INSUFICIENTES"
            )
        }

        val orderedSignals =
            signals
                .sortedBy {
                    it.timestamp
                }

        if (
            orderedSignals.size < 30
        ) {

            return rejectedResult(
                reason =
                    "AMOSTRA_INSUFICIENTE_MINIMO_30_SINAIS"
            )
        }

        val split =
            splitData(
                orderedSignals
            )

        if (
            split.training.isEmpty() ||
            split.validation.isEmpty() ||
            split.test.isEmpty()
        ) {

            return rejectedResult(
                reason =
                    "DIVISAO_TREINO_VALIDACAO_TESTE_INVALIDA"
            )
        }

        val candidates =
            generateCandidates()

        if (
            candidates.isEmpty()
        ) {

            return rejectedResult(
                reason =
                    "NENHUM_CANDIDATO_GERADO"
            )
        }

        /*
         * ==================================
         * TREINAMENTO
         * ==================================
         */

        val trainingResults =
            candidates.map { candidate ->

                val candidateSignals =
                    applyWeights(
                        split.training,
                        candidate
                    )

                val report =
                    BacktestEngine.evaluateSignals(
                        signals =
                            candidateSignals,

                        candles =
                            candles,

                        config =
                            config
                    )

                CalibrationCandidate(
                    probabilityWeight =
                        candidate.first,

                    deterministicWeight =
                        candidate.second,

                    metrics =
                        report.metrics
                )
            }

        val viableTraining =
            trainingResults
                .filter {
                    isViable(
                        it.metrics
                    )
                }

        if (
            viableTraining.isEmpty()
        ) {

            return rejectedResult(
                reason =
                    "NENHUM_CANDIDATO_VIAVEL_NO_TREINO"
            )
        }

        /*
         * Não escolhemos simplesmente o maior lucro.
         *
         * O score considera:
         *
         * expectancy;
         * profit factor;
         * drawdown;
         * win rate;
         * quantidade de operações.
         */
        val orderedTraining =
            viableTraining
                .sortedByDescending {
                    candidateScore(
                        it.metrics
                    )
                }

        /*
         * ==================================
         * VALIDAÇÃO
         * ==================================
         */

        val validationCandidates =
            orderedTraining
                .take(
                    min(
                        10,
                        orderedTraining.size
                    )
                )

        val validationResults =
            validationCandidates.map { candidate ->

                val candidateSignals =
                    applyWeights(
                        split.validation,
                        candidate
                    )

                val report =
                    BacktestEngine.evaluateSignals(
                        signals =
                            candidateSignals,

                        candles =
                            candles,

                        config =
                            config
                    )

                CalibrationCandidate(
                    probabilityWeight =
                        candidate.probabilityWeight,

                    deterministicWeight =
                        candidate.deterministicWeight,

                    metrics =
                        report.metrics
                )
            }

        val viableValidation =
            validationResults
                .filter {
                    isViable(
                        it.metrics
                    )
                }

        if (
            viableValidation.isEmpty()
        ) {

            return rejectedResult(
                reason =
                    "NENHUM_CANDIDATO_SUPEROU_VALIDACAO"
            )
        }

        val bestValidation =
            viableValidation
                .maxByOrNull {
                    candidateScore(
                        it.metrics
                    )
                }
                ?: return rejectedResult(
                    reason =
                        "FALHA_AO_SELECIONAR_VALIDACAO"
                )

        /*
         * ==================================
         * TESTE FORA DA AMOSTRA
         * ==================================
         */

        val testSignals =
            applyWeights(
                split.test,
                bestValidation
            )

        val testReport =
            BacktestEngine.evaluateSignals(
                signals =
                    testSignals,

                candles =
                    candles,

                config =
                    config
            )

        val testMetrics =
            testReport.metrics

        /*
         * ==================================
         * ROBUSTEZ
         * ==================================
         */

        val trainingMetrics =
            trainingResults
                .firstOrNull {
                    abs(
                        it.probabilityWeight -
                            bestValidation.probabilityWeight
                    ) < 0.000001 &&
                    abs(
                        it.deterministicWeight -
                            bestValidation.deterministicWeight
                    ) < 0.000001
                }
                ?.metrics
                ?: BacktestMetrics()

        val accepted =
            passesFinalValidation(
                training =
                    trainingMetrics,

                validation =
                    bestValidation.metrics,

                test =
                    testMetrics
            )

        val reason =
            if (
                accepted
            ) {

                "CONFIGURACAO_SUPEROU_TREINO_VALIDACAO_E_TESTE"

            } else {

                "CONFIGURACAO_REJEITADA_POR_FALTA_DE_ROBUSTEZ"
            }

        return CalibrationResult(

            selectedProbabilityWeight =
                bestValidation
                    .probabilityWeight,

            selectedDeterministicWeight =
                bestValidation
                    .deterministicWeight,

            trainingMetrics =
                trainingMetrics,

            validationMetrics =
                bestValidation.metrics,

            testMetrics =
                testMetrics,

            candidatesEvaluated =
                candidates.size,

            accepted =
                accepted,

            reason =
                reason
        )
    }

    /**
     * Gera pesos candidatos.
     *
     * Não existe preferência prévia por 60/40.
     *
     * O intervalo completo de 0/100 até 100/0
     * é testado em passos de 5%.
     */
    private fun generateCandidates():
        List<Pair<Double, Double>> {

        val candidates =
            mutableListOf<
                Pair<Double, Double>
            >()

        for (
            probabilityPercent in
                0..100 step 5
        ) {

            val probabilityWeight =
                probabilityPercent /
                    100.0

            val deterministicWeight =
                1.0 -
                    probabilityWeight

            candidates.add(
                Pair(
                    probabilityWeight,
                    deterministicWeight
                )
            )
        }

        return candidates
    }

    /**
     * Aplica os pesos candidatos aos sinais.
     *
     * Importante:
     *
     * O sinal histórico já contém as evidências
     * originais.
     *
     * O candidato apenas recalcula a dominância
     * direcional antes do backtest.
     */
    private fun applyWeights(
        signals: List<BacktestSignal>,
        candidate: CalibrationCandidate
    ): List<BacktestSignal> {

        return applyWeights(
            signals,
            Pair(
                candidate.probabilityWeight,
                candidate.deterministicWeight
            )
        )
    }

    private fun applyWeights(
        signals: List<BacktestSignal>,
        candidate: Pair<Double, Double>
    ): List<BacktestSignal> {

        val probabilityWeight =
            candidate.first

        val deterministicWeight =
            candidate.second

        return signals.map { signal ->

            val buy =
                signal.probabilityBuy *
                    probabilityWeight +

                signal.deterministicBuy *
                    deterministicWeight

            val sell =
                signal.probabilitySell *
                    probabilityWeight +

                signal.deterministicSell *
                    deterministicWeight

            val neutral =
                signal.probabilityNeutral *
                    probabilityWeight +

                signal.deterministicNeutral *
                    deterministicWeight

            /*
             * O modelo de backtest precisa preservar
             * a direção que foi produzida pelo Motor.
             *
             * Se o novo peso mudar a direção para
             * neutro, o sinal deixa de ser negociável.
             */
            val direction =
                when {

                    buy >=
                        sell &&
                    buy >=
                        neutral ->
                        BacktestDirection.BUY

                    sell >=
                        buy &&
                    sell >=
                        neutral ->
                        BacktestDirection.SELL

                    else ->
                        BacktestDirection.NEUTRAL
                }

            signal.copy(
                direction =
                    direction,

                probabilityWeight =
                    probabilityWeight,

                deterministicWeight =
                    deterministicWeight
            )
        }
    }

    /**
     * Divide cronologicamente:
     *
     * 60% treino
     * 20% validação
     * 20% teste
     *
     * Nunca embaralha os sinais.
     */
    private fun splitData(
        signals:
            List<BacktestSignal>
    ): CalibrationSplit {

        val size =
            signals.size

        val trainingEnd =
            (
                size *
                    0.60
            )
                .toInt()
                .coerceAtLeast(1)

        val validationEnd =
            (
                size *
                    0.80
            )
                .toInt()
                .coerceAtLeast(
                    trainingEnd + 1
                )
                .coerceAtMost(
                    size
                )

        val training =
            signals.subList(
                0,
                trainingEnd
            )

        val validation =
            signals.subList(
                trainingEnd,
                validationEnd
            )

        val test =
            signals.subList(
                validationEnd,
                size
            )

        return CalibrationSplit(
            training =
                training,

            validation =
                validation,

            test =
                test
        )
    }

    /**
     * Score interno usado para comparar candidatos.
     *
     * Não é uma taxa de acerto.
     */
    private fun candidateScore(
        metrics: BacktestMetrics
    ): Double {

        val expectancy =
            metrics.expectancyR
                .coerceIn(
                    -5.0,
                    5.0
                )

        val profitFactor =
            if (
                metrics.profitFactor
                    .isFinite()
            ) {

                metrics.profitFactor
                    .coerceIn(
                        0.0,
                        5.0
                    )

            } else {

                5.0
            }

        val winRate =
            metrics.winRate
                .coerceIn(
                    0.0,
                    100.0
                )

        val drawdown =
            metrics.maximumDrawdownR
                .coerceAtLeast(
                    0.0
                )

        val sample =
            metrics.executedSignals

        /*
         * Penaliza amostras muito pequenas.
         */
        val sampleFactor =
            when {

                sample >= 500 ->
                    1.0

                sample >= 200 ->
                    0.90

                sample >= 100 ->
                    0.75

                sample >= 50 ->
                    0.55

                else ->
                    0.35
            }

        return (

            expectancy *
                35.0 +

            profitFactor *
                10.0 +

            winRate *
                0.20 -

            drawdown *
                8.0

        ) *
            sampleFactor
    }

    /**
     * Filtro mínimo para evitar que o calibrador
     * escolha uma configuração claramente ruim.
     */
    private fun isViable(
        metrics:
            BacktestMetrics
    ): Boolean {

        return (

            metrics.executedSignals >=
                30 &&

            metrics.expectancyR >
                0.0 &&

            metrics.totalR >
                0.0 &&

            metrics.maximumDrawdownR <
                50.0

        )
    }

    /**
     * Teste final de robustez.
     *
     * O desempenho não precisa ser idêntico
     * nos três períodos, mas precisa permanecer
     * positivo e não pode sofrer degradação
     * extrema fora da amostra.
     */
    private fun passesFinalValidation(
        training:
            BacktestMetrics,

        validation:
            BacktestMetrics,

        test:
            BacktestMetrics
    ): Boolean {

        if (
            training.executedSignals < 30 ||
            validation.executedSignals < 20 ||
            test.executedSignals < 20
        ) {
            return false
        }

        if (
            training.expectancyR <= 0.0 ||
            validation.expectancyR <= 0.0 ||
            test.expectancyR <= 0.0
        ) {
            return false
        }

        if (
            test.totalR <= 0.0
        ) {
            return false
        }

        /*
         * Evita uma configuração que seja excelente
         * no treino e desabe no teste.
         */
        val trainingExpectancy =
            training.expectancyR
                .coerceAtLeast(
                    0.0001
                )

        val testRatio =
            test.expectancyR /
                trainingExpectancy

        if (
            testRatio < 0.35
        ) {
            return false
        }

        /*
         * O drawdown não pode ser desproporcional
         * ao resultado final.
         */
        if (
            test.maximumDrawdownR >
                0.0 &&
            test.totalR /
                test.maximumDrawdownR <
                0.10
        ) {
            return false
        }

        return true
    }

    private fun rejectedResult(
        reason:
            String
    ): CalibrationResult {

        return CalibrationResult(

            selectedProbabilityWeight =
                0.50,

            selectedDeterministicWeight =
                0.50,

            trainingMetrics =
                BacktestMetrics(),

            validationMetrics =
                BacktestMetrics(),

            testMetrics =
                BacktestMetrics(),

            candidatesEvaluated =
                0,

            accepted =
                false,

            reason =
                reason
        )
    }

    private data class CalibrationSplit(

        val training:
            List<BacktestSignal>,

        val validation:
            List<BacktestSignal>,

        val test:
            List<BacktestSignal>
    )
}

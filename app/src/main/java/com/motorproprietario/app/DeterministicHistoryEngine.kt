package com.motorproprietario.app

import android.content.Context
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class HistoricalDeterminism(
    val confidence: Double,
    val trapRisk: Double,
    val samples: Int,
    val wins: Int,
    val falseSignals: Int
)

private data class PendingSignal(
    val historyKey: String,
    val direction: String,
    val entryPrice: Double,
    val atr: Double,
    val createdAt: Long,
    val expiryAt: Long
)

class DeterministicHistoryEngine(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "deterministic_history",
            Context.MODE_PRIVATE
        )

    /*
     * ============================================================
     * SINAIS PENDENTES
     * ============================================================
     *
     * Guarda sinais reais para avaliação posterior.
     *
     * Não existe preço artificial.
     * O resultado somente é definido quando um preço posterior
     * realmente chega.
     */
    private val pendingSignals =
        mutableListOf<PendingSignal>()

    /*
     * Evita registrar o mesmo contexto repetidamente
     * em intervalos muito curtos.
     */
    private val lastObservation =
        mutableMapOf<String, Long>()

    /*
     * ============================================================
     * LIMITES DO HISTÓRICO
     * ============================================================
     */

    private val maximumSequenceSize =
        12

    /*
     * Quantidade máxima de falsos consecutivos considerada
     * para formação de uma sequência forte.
     */
    private val maximumFalseSequence =
        6

    private fun clamp(
        value: Double,
        minValue: Double = 0.0,
        maxValue: Double = 100.0
    ): Double {

        if (!value.isFinite()) {
            return minValue
        }

        return value.coerceIn(
            minValue,
            maxValue
        )
    }

    private fun timeframeMilliseconds(
        timeframe: String
    ): Long {

        return when (
            timeframe.uppercase()
        ) {

            "M1" ->
                60_000L

            "M5" ->
                300_000L

            "M15" ->
                900_000L

            "M30" ->
                1_800_000L

            "H1" ->
                3_600_000L

            "H4" ->
                14_400_000L

            "D1" ->
                86_400_000L

            else ->
                900_000L
        }
    }

    /*
     * ============================================================
     * ASSINATURA DO CONTEXTO
     * ============================================================
     *
     * O histórico não memoriza apenas COMPRA/VENDA.
     *
     * Ele memoriza o contexto em que o sinal apareceu.
     */
    private fun buildHistoryKey(
        symbol: String,
        timeframe: String,
        metrics: QuantMetrics,
        direction: String,
        stage: String
    ): String {

        val trend =
            when {

                metrics.trend >= 70.0 ->
                    "TREND_UP_STRONG"

                metrics.trend >= 55.0 ->
                    "TREND_UP"

                metrics.trend <= 30.0 ->
                    "TREND_DOWN_STRONG"

                metrics.trend <= 45.0 ->
                    "TREND_DOWN"

                else ->
                    "TREND_NEUTRAL"
            }

        val rsi =
            when {

                metrics.rsi >= 70.0 ->
                    "RSI_OVERBOUGHT"

                metrics.rsi >= 55.0 ->
                    "RSI_BULL"

                metrics.rsi <= 30.0 ->
                    "RSI_OVERSOLD"

                metrics.rsi <= 45.0 ->
                    "RSI_BEAR"

                else ->
                    "RSI_NEUTRAL"
            }

        val breakout =
            when {

                metrics.breakout >= 70.0 ->
                    "BREAKOUT_UP"

                metrics.breakout <= 30.0 ->
                    "BREAKOUT_DOWN"

                else ->
                    "BREAKOUT_NEUTRAL"
            }

        val structure =
            when {

                metrics.structure >= 70.0 ->
                    "STRUCTURE_UP_STRONG"

                metrics.structure >= 55.0 ->
                    "STRUCTURE_UP"

                metrics.structure <= 30.0 ->
                    "STRUCTURE_DOWN_STRONG"

                metrics.structure <= 45.0 ->
                    "STRUCTURE_DOWN"

                else ->
                    "STRUCTURE_NEUTRAL"
            }

        val volume =
            when {

                metrics.volume >= 70.0 ->
                    "VOLUME_HIGH"

                metrics.volume <= 30.0 ->
                    "VOLUME_LOW"

                else ->
                    "VOLUME_NORMAL"
            }

        val volatility =
            when {

                metrics.volatility >= 70.0 ->
                    "VOL_HIGH"

                metrics.volatility <= 30.0 ->
                    "VOL_LOW"

                else ->
                    "VOL_NORMAL"
            }

        return listOf(
            symbol,
            timeframe,
            direction,
            stage,
            trend,
            rsi,
            breakout,
            structure,
            volume,
            volatility
        ).joinToString("|")
    }

    /*
     * ============================================================
     * HISTÓRICO BÁSICO
     * ============================================================
     */

    private fun wins(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.wins",
            0
        )
    }

    private fun falseSignals(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.false",
            0
        )
    }

    /*
     * ============================================================
     * SEQUÊNCIA HISTÓRICA
     * ============================================================
     *
     * W = sinal favorável
     * F = falso/adverso/expirado
     *
     * Exemplo:
     *
     * W W F W F F F
     *
     * Os últimos F consecutivos representam uma possível
     * sequência de armadilhas.
     */
    private fun sequence(
        key: String
    ): String {

        return preferences.getString(
            "$key.sequence",
            ""
        )
            ?: ""
    }

    private fun registerSequence(
        key: String,
        success: Boolean
    ) {

        val current =
            sequence(key)

        val result =
            if (success) {
                "W"
            } else {
                "F"
            }

        val updated =
            (
                current +
                    result
                )
                .takeLast(
                    maximumSequenceSize
                )

        preferences.edit()
            .putString(
                "$key.sequence",
                updated
            )
            .apply()
    }

    private fun registerResult(
        key: String,
        success: Boolean
    ) {

        val currentWins =
            wins(key)

        val currentFalse =
            falseSignals(key)

        preferences.edit()
            .putInt(
                "$key.wins",
                if (success) {
                    currentWins + 1
                } else {
                    currentWins
                }
            )
            .putInt(
                "$key.false",
                if (!success) {
                    currentFalse + 1
                } else {
                    currentFalse
                }
            )
            .apply()

        registerSequence(
            key,
            success
        )
    }

    /*
     * ============================================================
     * ÚLTIMA SEQUÊNCIA DE FALSOS
     * ============================================================
     *
     * Mede somente os F consecutivos no final da sequência.
     */
    private fun consecutiveFalseCount(
        key: String
    ): Int {

        val history =
            sequence(key)

        if (history.isEmpty()) {
            return 0
        }

        var count =
            0

        for (
            index in history.length - 1 downTo 0
        ) {

            if (
                history[index] == 'F'
            ) {

                count++

                if (
                    count >=
                    maximumFalseSequence
                ) {
                    break
                }

            } else {
                break
            }
        }

        return count
    }

    /*
     * ============================================================
     * FORÇA DA SEQUÊNCIA DE FALSOS
     * ============================================================
     *
     * 0 falsos = 0
     * 1 falso   = baixo
     * 2 falsos  = relevante
     * 3 falsos  = forte
     * 4+        = muito forte
     */
    private fun falseSequenceScore(
        key: String
    ): Double {

        val count =
            consecutiveFalseCount(
                key
            )

        return when {

            count <= 0 ->
                0.0

            count == 1 ->
                20.0

            count == 2 ->
                40.0

            count == 3 ->
                65.0

            count == 4 ->
                80.0

            count == 5 ->
                90.0

            else ->
                95.0
        }
    }

    /*
     * ============================================================
     * TAXA HISTÓRICA DE FALSO SINAL
     * ============================================================
     */
    private fun historicalFalseRate(
        key: String
    ): Double {

        val w =
            wins(key)

        val f =
            falseSignals(key)

        val samples =
            w + f

        if (
            samples <= 0
        ) {
            return 50.0
        }

        /*
         * Suavização.
         *
         * Evita que um único evento produza
         * uma leitura extrema.
         */
        return clamp(
            (
                f + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0
        )
    }

    /*
     * ============================================================
     * AVALIAÇÃO DOS SINAIS PENDENTES
     * ============================================================
     *
     * Utiliza somente preços reais recebidos posteriormente.
     */
    private fun evaluatePendingSignals(
        currentPrice: Double,
        now: Long
    ) {

        if (
            !currentPrice.isFinite() ||
            currentPrice <= 0.0
        ) {
            return
        }

        val iterator =
            pendingSignals.iterator()

        while (
            iterator.hasNext()
        ) {

            val signal =
                iterator.next()

            val movement =
                currentPrice -
                    signal.entryPrice

            val threshold =
                max(
                    signal.atr * 0.50,
                    signal.entryPrice * 0.0005
                )

            if (
                threshold <= 0.0
            ) {

                iterator.remove()

                continue
            }

            val favorable =
                if (
                    signal.direction ==
                    "COMPRA"
                ) {

                    movement >=
                        threshold

                } else {

                    movement <=
                        -threshold
                }

            val adverse =
                if (
                    signal.direction ==
                    "COMPRA"
                ) {

                    movement <=
                        -threshold

                } else {

                    movement >=
                        threshold
                }

            if (
                favorable
            ) {

                registerResult(
                    signal.historyKey,
                    true
                )

                iterator.remove()

            } else if (
                adverse ||
                now >= signal.expiryAt
            ) {

                registerResult(
                    signal.historyKey,
                    false
                )

                iterator.remove()
            }
        }
    }

    /*
     * ============================================================
     * EVIDÊNCIA DE REVERSÃO
     * ============================================================
     *
     * Procura evidências de movimento contrário à direção
     * dominante anterior.
     *
     * Isto NÃO significa que uma reversão está garantida.
     */
    private fun reversalEvidence(
        metrics: QuantMetrics,
        direction: String
    ): Double {

        if (
            direction != "COMPRA" &&
            direction != "VENDA"
        ) {
            return 0.0
        }

        val oppositeTrend =
            if (
                direction == "COMPRA"
            ) {

                when {

                    metrics.trend <= 30.0 ->
                        100.0

                    metrics.trend <= 40.0 ->
                        75.0

                    metrics.trend <= 45.0 ->
                        55.0

                    else ->
                        20.0
                }

            } else {

                when {

                    metrics.trend >= 70.0 ->
                        100.0

                    metrics.trend >= 60.0 ->
                        75.0

                    metrics.trend >= 55.0 ->
                        55.0

                    else ->
                        20.0
                }
            }

        val oppositeMomentum =
            if (
                direction == "COMPRA"
            ) {

                100.0 -
                    metrics.momentum

            } else {

                metrics.momentum
            }

        val oppositeStructure =
            if (
                direction == "COMPRA"
            ) {

                100.0 -
                    metrics.structure

            } else {

                metrics.structure
            }

        val oppositeBreakout =
            if (
                direction == "COMPRA"
            ) {

                100.0 -
                    metrics.breakout

            } else {

                metrics.breakout
            }

        val candleRejection =
            when {

                metrics.candlePattern >=
                    75.0 ->
                    80.0

                metrics.candlePattern <=
                    25.0 ->
                    80.0

                else ->
                    20.0
            }

        return clamp(

            oppositeTrend * 0.30 +

            oppositeMomentum * 0.20 +

            oppositeStructure * 0.20 +

            oppositeBreakout * 0.15 +

            candleRejection * 0.15
        )
    }

    /*
     * ============================================================
     * CAPTURE SCORE
     * ============================================================
     *
     * Combina:
     *
     * 1. histórico de falso sinal;
     * 2. sequência de falsos;
     * 3. risco atual de armadilha;
     * 4. pressão de liquidez;
     * 5. conflito estrutural.
     *
     * É uma variável interna do motor.
     */
    private fun captureScore(
        key: String,
        deterministic: DeterministicResult
    ): Double {

        val falseRate =
            historicalFalseRate(
                key
            )

        val sequenceRisk =
            falseSequenceScore(
                key
            )

        val currentTrap =
            deterministic.trapRisk

        val liquidity =
            deterministic.liquidityPressure

        val conflict =
            deterministic.timeframeConflict

        val historicalWeight =
            min(
                1.0,
                (
                    wins(key) +
                        falseSignals(key)
                ) /
                    20.0
            )

        val historicalComponent =
            falseRate *
                (
                    0.55 +
                        historicalWeight *
                        0.45
                )

        return clamp(

            historicalComponent * 0.25 +

            sequenceRisk * 0.30 +

            currentTrap * 0.20 +

            liquidity * 0.15 +

            conflict * 0.10
        )
    }

    /*
     * ============================================================
     * REALIZAÇÃO PÓS-CAPTURA
     * ============================================================
     *
     * Esta é a camada 6.
     *
     * Não basta haver FSI.
     *
     * O motor procura:
     *
     * CAPTURA
     * +
     * EXAUSTÃO
     * +
     * DIVERGÊNCIA
     * +
     * LIQUIDEZ
     * +
     * REVERSÃO
     *
     * para formar um risco de realização.
     */
    private fun realizationAfterCapture(
        key: String,
        metrics: QuantMetrics,
        deterministic: DeterministicResult,
        currentFalseSignalRisk: Double
    ): Double {

        val capture =
            captureScore(
                key,
                deterministic
            )

        val sequence =
            falseSequenceScore(
                key
            )

        val reversal =
            reversalEvidence(
                metrics,
                deterministic.directionalBias
            )

        val exhaustion =
            deterministic.exhaustion

        val divergence =
            abs(
                metrics.divergence -
                    50.0
            ) * 2.0

        val liquidity =
            deterministic.liquidityPressure

        val currentFsi =
            clamp(
                currentFalseSignalRisk
            )

        /*
         * A realização não pode nascer somente do FSI.
         *
         * A reversão recebe peso significativo.
         */
        return clamp(

            capture * 0.20 +

            sequence * 0.15 +

            exhaustion * 0.20 +

            divergence * 0.10 +

            liquidity * 0.10 +

            reversal * 0.20 +

            currentFsi * 0.05
        )
    }

    /*
     * ============================================================
     * APLICAÇÃO DO HISTÓRICO
     * ============================================================
     */
    fun apply(
        symbol: String,
        timeframe: String,
        stage: String,
        metrics: QuantMetrics,
        deterministic: DeterministicResult,
        currentPrice: Double,
        now: Long
    ): DeterministicResult {

        /*
         * Primeiro avaliamos os sinais antigos.
         */
        evaluatePendingSignals(
            currentPrice,
            now
        )

        if (
            currentPrice <= 0.0 ||
            !currentPrice.isFinite()
        ) {
            return deterministic
        }

        val direction =
            deterministic.directionalBias

        if (
            direction != "COMPRA" &&
            direction != "VENDA"
        ) {
            return deterministic
        }

        val key =
            buildHistoryKey(
                symbol =
                    symbol,

                timeframe =
                    timeframe,

                metrics =
                    metrics,

                direction =
                    direction,

                stage =
                    stage
            )

        /*
         * ========================================================
         * REGISTRO DE NOVO SINAL
         * ========================================================
         */

        val spacing =
            max(
                30_000L,
                timeframeMilliseconds(
                    timeframe
                ) / 2L
            )

        val previous =
            lastObservation[key]

        val tooSoon =
            previous != null &&
                now -
                    previous <
                spacing

        /*
         * Só registra sinal suficientemente forte.
         */
        if (
            !tooSoon &&
            deterministic.confidence >=
                55.0
        ) {

            val atr =
                max(
                    metrics.atr,
                    currentPrice * 0.0001
                )

            pendingSignals.add(
                PendingSignal(

                    historyKey =
                        key,

                    direction =
                        direction,

                    entryPrice =
                        currentPrice,

                    atr =
                        atr,

                    createdAt =
                        now,

                    expiryAt =
                        now +
                            timeframeMilliseconds(
                                timeframe
                            ) * 10L
                )
            )

            lastObservation[key] =
                now
        }

        /*
         * ========================================================
         * HISTÓRICO ATUAL
         * ========================================================
         */

        val currentWins =
            wins(key)

        val currentFalse =
            falseSignals(key)

        val samples =
            currentWins +
                currentFalse

        /*
         * Sem histórico suficiente, ainda podemos calcular
         * a camada de captura atual, mas não deixamos o
         * histórico dominar o resultado.
         */
        val historicalWeight =
            min(
                1.0,
                samples /
                    20.0
            )

        val reliability =
            (
                currentWins + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0

        val historicalTrap =
            clamp(
                100.0 -
                    reliability
            )

        /*
         * ========================================================
         * CAPTURA
         * ========================================================
         */

        val capture =
            captureScore(
                key,
                deterministic
            )

        /*
         * ========================================================
         * REALIZAÇÃO PÓS-CAPTURA
         * ========================================================
         */

        val realization =
            realizationAfterCapture(

                key =
                    key,

                metrics =
                    metrics,

                deterministic =
                    deterministic,

                currentFalseSignalRisk =
                    deterministic.trapRisk
            )

        /*
         * ========================================================
         * PRESSÃO HISTÓRICA
         * ========================================================
         */

        val trapPressure =
            historicalTrap *
                historicalWeight

        var buy =
            deterministic.buyScore

        var sell =
            deterministic.sellScore

        var neutral =
            deterministic.neutralScore

        /*
         * ========================================================
         * HISTÓRICO RUIM
         * ========================================================
         *
         * Quanto maior o histórico de falsos sinais,
         * menor a confiança direcional.
         */
        if (
            trapPressure > 50.0
        ) {

            val penalty =
                (
                    trapPressure -
                        50.0
                ) *
                    0.25

            val factor =
                (
                    1.0 -
                        penalty /
                        100.0
                ).coerceIn(
                    0.50,
                    1.0
                )

            buy *=
                factor

            sell *=
                factor

            neutral +=
                penalty
        }

        /*
         * ========================================================
         * HISTÓRICO FAVORÁVEL
         * ========================================================
         *
         * Histórico bom gera apenas pequeno reforço.
         */
        else {

            val reinforcement =
                (
                    50.0 -
                        trapPressure
                ) *
                    0.10

            if (
                direction ==
                "COMPRA"
            ) {

                buy +=
                    reinforcement

            } else {

                sell +=
                    reinforcement
            }
        }

        /*
         * ========================================================
         * CAPTURE SCORE
         * ========================================================
         *
         * Quando a captura fica elevada, reduzimos a força
         * direcional porque o padrão atual está ficando menos
         * confiável.
         */
        if (
            capture >=
                60.0
        ) {

            val capturePenalty =
                (
                    capture -
                        60.0
                ) *
                    0.22

            val factor =
                (
                    1.0 -
                        capturePenalty /
                        100.0
                ).coerceIn(
                    0.55,
                    1.0
                )

            buy *=
                factor

            sell *=
                factor

            neutral +=
                capturePenalty
        }

        /*
         * ========================================================
         * REALIZAÇÃO PÓS-CAPTURA
         * ========================================================
         *
         * Este é o ponto novo.
         *
         * Realização alta não significa automaticamente
         * "vender" ou "comprar".
         *
         * Primeiro reduzimos a confiança da direção atual.
         *
         * Somente quando a evidência de reversão é suficiente,
         * damos pequeno peso à direção oposta.
         */
        if (
            realization >=
                65.0
        ) {

            val realizationPenalty =
                (
                    realization -
                        60.0
                ) *
                    0.30

            val factor =
                (
                    1.0 -
                        realizationPenalty /
                        100.0
                ).coerceIn(
                    0.45,
                    1.0
                )

            buy *=
                factor

            sell *=
                factor

            neutral +=
                realizationPenalty

            /*
             * Evidência de reversão.
             */
            val reversal =
                reversalEvidence(
                    metrics,
                    direction
                )

            if (
                reversal >=
                    65.0 &&
                realization >=
                    70.0
            ) {

                val oppositeBoost =
                    min(
                        12.0,
                        (
                            realization -
                                65.0
                        ) *
                            0.25
                    )

                if (
                    direction ==
                    "COMPRA"
                ) {

                    sell +=
                        oppositeBoost

                } else {

                    buy +=
                        oppositeBoost
                }
            }
        }

        /*
         * ========================================================
         * NORMALIZAÇÃO
         * ========================================================
         */

        val total =
            buy.coerceAtLeast(
                0.0
            ) +
            sell.coerceAtLeast(
                0.0
            ) +
            neutral.coerceAtLeast(
                0.0
            )

        if (
            total <= 0.0
        ) {
            return deterministic
        }

        buy =
            buy.coerceAtLeast(
                0.0
            ) /
                total *
                100.0

        sell =
            sell.coerceAtLeast(
                0.0
            ) /
                total *
                100.0

        neutral =
            neutral.coerceAtLeast(
                0.0
            ) /
                total *
                100.0

        /*
         * ========================================================
         * DIREÇÃO HISTÓRICA FINAL
         * ========================================================
         */

        val finalDirection =
            when {

                buy >= sell &&
                    buy >= neutral ->

                    "COMPRA"

                sell >= buy &&
                    sell >= neutral ->

                    "VENDA"

                else ->

                    "NEUTRO"
            }

        /*
         * ========================================================
         * CONFIANÇA
         * ========================================================
         */

        val historicalAdjustment =
            (
                reliability -
                    50.0
            ) *
                0.25 *
                historicalWeight

        val capturePenaltyConfidence =
            capture *
                0.06

        val realizationPenaltyConfidence =
            realization *
                0.08

        val confidence =
            clamp(

                deterministic.confidence +

                historicalAdjustment -

                trapPressure *
                    0.08 -

                capturePenaltyConfidence -

                realizationPenaltyConfidence
            )

        /*
         * ========================================================
         * TRAP RISK FINAL
         * ========================================================
         *
         * Combina o trap atual com:
         *
         * - histórico;
         * - sequência de falsos;
         * - captura.
         */
        val sequenceRisk =
            falseSequenceScore(
                key
            )

        val finalTrapRisk =
            clamp(

                deterministic.trapRisk *
                    0.45 +

                historicalTrap *
                    historicalWeight *
                    0.20 +

                sequenceRisk *
                    0.15 +

                capture *
                    0.20
            )

        /*
         * ========================================================
         * REALIZATION RISK FINAL
         * ========================================================
         */
        val finalRealizationRisk =
            clamp(

                deterministic.realizationRisk *
                    0.55 +

                realization *
                    0.45
            )

        /*
         * ========================================================
         * RETORNO
         * ========================================================
         *
         * Mantemos a mesma estrutura de DeterministicResult.
         *
         * Nenhuma alteração necessária no MainActivity.
         */
        return deterministic.copy(

            buyScore =
                clamp(
                    buy
                ),

            sellScore =
                clamp(
                    sell
                ),

            neutralScore =
                clamp(
                    neutral
                ),

            directionalBias =
                finalDirection,

            confidence =
                confidence,

            trapRisk =
                finalTrapRisk,

            realizationRisk =
                finalRealizationRisk
        )
    }

    /*
     * ============================================================
     * ESTATÍSTICAS
     * ============================================================
     */
    fun statistics(
        symbol: String,
        timeframe: String,
        stage: String,
        metrics: QuantMetrics,
        direction: String
    ): HistoricalDeterminism {

        val key =
            buildHistoryKey(
                symbol =
                    symbol,

                timeframe =
                    timeframe,

                metrics =
                    metrics,

                direction =
                    direction,

                stage =
                    stage
            )

        val currentWins =
            wins(key)

        val currentFalse =
            falseSignals(key)

        val samples =
            currentWins +
                currentFalse

        if (
            samples == 0
        ) {

            return HistoricalDeterminism(

                confidence =
                    50.0,

                trapRisk =
                    50.0,

                samples =
                    0,

                wins =
                    0,

                falseSignals =
                    0
            )
        }

        val reliability =
            (
                currentWins + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0

        val historicalTrap =
            clamp(
                100.0 -
                    reliability
            )

        val sequenceRisk =
            falseSequenceScore(
                key
            )

        val combinedTrap =
            clamp(

                historicalTrap *
                    0.60 +

                sequenceRisk *
                    0.40
            )

        return HistoricalDeterminism(

            confidence =
                clamp(
                    reliability
                ),

            trapRisk =
                combinedTrap,

            samples =
                samples,

            wins =
                currentWins,

            falseSignals =
                currentFalse
        )
    }
}

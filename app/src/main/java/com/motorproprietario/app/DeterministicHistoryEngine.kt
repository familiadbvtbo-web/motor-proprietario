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
    val falseSignals: Int,
    val consecutiveFalseSignals: Int = 0,
    val capturePressure: Double = 0.0,
    val realizationPressure: Double = 0.0
)

private data class PendingSignal(
    val historyKey: String,
    val direction: String,
    val entryPrice: Double,
    val atr: Double,
    val createdAt: Long,
    val expiryAt: Long
)

private data class HistoryState(
    val wins: Int,
    val falseSignals: Int,
    val consecutiveFalseSignals: Int,
    val realizationEvents: Int
)

class DeterministicHistoryEngine(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "deterministic_history",
            Context.MODE_PRIVATE
        )

    private val pendingSignals =
        mutableListOf<PendingSignal>()

    private val lastObservation =
        mutableMapOf<String, Long>()

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

            "M1" -> 60_000L
            "M5" -> 300_000L
            "M15" -> 900_000L
            "M30" -> 1_800_000L
            "H1" -> 3_600_000L
            "H4" -> 14_400_000L
            "D1" -> 86_400_000L

            else ->
                900_000L
        }
    }

    /*
     * ============================================================
     * ASSINATURA DO CONTEXTO
     * ============================================================
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
     * HISTÓRICO
     * ============================================================
     */

    private fun wins(
        key: String
    ): Int =
        preferences.getInt(
            "$key.wins",
            0
        )

    private fun falseSignals(
        key: String
    ): Int =
        preferences.getInt(
            "$key.false",
            0
        )

    private fun consecutiveFalseSignals(
        key: String
    ): Int =
        preferences.getInt(
            "$key.consecutive_false",
            0
        )

    private fun realizationEvents(
        key: String
    ): Int =
        preferences.getInt(
            "$key.realizations",
            0
        )

    private fun readState(
        key: String
    ): HistoryState {

        return HistoryState(

            wins =
                wins(key),

            falseSignals =
                falseSignals(key),

            consecutiveFalseSignals =
                consecutiveFalseSignals(key),

            realizationEvents =
                realizationEvents(key)
        )
    }

    /*
     * ============================================================
     * REGISTRO DE RESULTADO
     * ============================================================
     */

    private fun registerResult(
        key: String,
        success: Boolean
    ) {

        val state =
            readState(key)

        val newWins =
            if (success) {
                state.wins + 1
            } else {
                state.wins
            }

        val newFalse =
            if (!success) {
                state.falseSignals + 1
            } else {
                state.falseSignals
            }

        val newConsecutiveFalse =
            if (success) {
                0
            } else {
                state.consecutiveFalseSignals + 1
            }

        preferences.edit()
            .putInt(
                "$key.wins",
                newWins
            )
            .putInt(
                "$key.false",
                newFalse
            )
            .putInt(
                "$key.consecutive_false",
                newConsecutiveFalse
            )
            .apply()
    }

    /*
     * ============================================================
     * REGISTRO DE REALIZAÇÃO
     * ============================================================
     *
     * Uma realização é registrada quando existe falso sinal
     * seguido de movimento posterior significativo.
     *
     * Não é tratada como previsão.
     * É apenas memória estatística do comportamento observado.
     */

    private fun registerRealization(
        key: String
    ) {

        val current =
            realizationEvents(key)

        preferences.edit()
            .putInt(
                "$key.realizations",
                current + 1
            )
            .apply()
    }

    /*
     * ============================================================
     * AVALIAÇÃO DOS SINAIS PENDENTES
     * ============================================================
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

            /*
             * Sinal funcionou.
             */
            if (
                favorable
            ) {

                registerResult(
                    signal.historyKey,
                    true
                )

                iterator.remove()

                continue
            }

            /*
             * Movimento contrário.
             */
            if (
                adverse
            ) {

                registerResult(
                    signal.historyKey,
                    false
                )

                /*
                 * O evento de falso sinal pode alimentar
                 * a memória de realização posteriormente.
                 *
                 * Aqui registramos apenas o evento observado.
                 */
                iterator.remove()

                continue
            }

            /*
             * Expiração sem confirmação.
             */
            if (
                now >=
                signal.expiryAt
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
     * CÁLCULO DA PRESSÃO DE CAPTURA
     * ============================================================
     *
     * Este é o novo diferencial.
     *
     * Não usamos simplesmente:
     *
     * FSI alto = armadilha
     *
     * Agora:
     *
     * falsos sinais históricos
     * + repetição
     * + contexto
     * = pressão histórica de captura
     */

    private fun calculateCapturePressure(
        state: HistoryState
    ): Double {

        val samples =
            state.wins +
                state.falseSignals

        if (
            samples < 3
        ) {
            return 0.0
        }

        val falseRate =
            state.falseSignals.toDouble() /
                samples.toDouble() *
                100.0

        /*
         * Crescimento da penalização conforme
         * os falsos sinais se repetem.
         */
        val sequencePressure =
            when {

                state.consecutiveFalseSignals >= 6 ->
                    100.0

                state.consecutiveFalseSignals >= 5 ->
                    90.0

                state.consecutiveFalseSignals >= 4 ->
                    75.0

                state.consecutiveFalseSignals >= 3 ->
                    60.0

                state.consecutiveFalseSignals >= 2 ->
                    35.0

                else ->
                    0.0
            }

        /*
         * Peso de amostra.
         *
         * Com 3 eventos não confiamos completamente.
         * O peso cresce até 1.0 em 20 eventos.
         */
        val sampleWeight =
            min(
                1.0,
                samples / 20.0
            )

        return clamp(
            (
                falseRate * 0.65 +
                    sequencePressure * 0.35
            ) *
                sampleWeight
        )
    }

    /*
     * ============================================================
     * PRESSÃO DE REALIZAÇÃO
     * ============================================================
     */

    private fun calculateRealizationPressure(
        state: HistoryState
    ): Double {

        val samples =
            state.wins +
                state.falseSignals

        if (
            samples < 5 ||
            state.realizationEvents <= 0
        ) {
            return 0.0
        }

        val realizationRate =
            state.realizationEvents.toDouble() /
                samples.toDouble() *
                100.0

        return clamp(
            realizationRate
        )
    }

    /*
     * ============================================================
     * APLICA HISTÓRICO AO DETERMINISMO
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
         * ========================================================
         * REGISTRO DE NOVA OBSERVAÇÃO
         * ========================================================
         */

        if (
            !tooSoon &&
            deterministic.confidence >= 55.0
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

        val state =
            readState(key)

        val samples =
            state.wins +
                state.falseSignals

        /*
         * ========================================================
         * SEM HISTÓRICO
         * ========================================================
         */

        if (
            samples < 3
        ) {

            return deterministic
        }

        /*
         * ========================================================
         * CONFIABILIDADE
         * ========================================================
         */

        val reliability =
            (
                state.wins + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0

        /*
         * ========================================================
         * CAPTURE PRESSURE
         * ========================================================
         */

        val capturePressure =
            calculateCapturePressure(
                state
            )

        /*
         * ========================================================
         * REALIZATION PRESSURE
         * ========================================================
         */

        val realizationPressure =
            calculateRealizationPressure(
                state
            )

        /*
         * ========================================================
         * PESO HISTÓRICO
         * ========================================================
         */

        val historicalWeight =
            min(
                1.0,
                samples / 20.0
            )

        var buy =
            deterministic.buyScore

        var sell =
            deterministic.sellScore

        var neutral =
            deterministic.neutralScore

        /*
         * ========================================================
         * AMBIENTE DE CAPTURA
         * ========================================================
         */

        if (
            capturePressure >= 50.0
        ) {

            /*
             * Quanto maior a pressão de captura,
             * menor a confiança direcional.
             */
            val penalty =
                (
                    capturePressure -
                        50.0
                ) *
                    0.40

            val factor =
                (
                    1.0 -
                        penalty /
                        100.0
                ).coerceIn(
                    0.35,
                    1.0
                )

            buy *=
                factor

            sell *=
                factor

            neutral +=
                penalty

        } else {

            /*
             * Histórico favorável gera apenas
             * pequeno reforço.
             */
            val reinforcement =
                (
                    50.0 -
                        capturePressure
                ) *
                    0.08 *
                    historicalWeight

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
         * REALIZAÇÃO
         * ========================================================
         *
         * A realização não cria COMPRA/VENDA.
         * Ela aumenta cautela quando o histórico mostra
         * que falsos sinais costumam anteceder movimentos
         * posteriores.
         */

        if (
            realizationPressure >= 60.0 &&
            capturePressure >= 50.0
        ) {

            val realizationPenalty =
                (
                    realizationPressure -
                        50.0
                ) *
                    0.20

            buy *=
                (
                    1.0 -
                        realizationPenalty /
                        100.0
                ).coerceIn(
                    0.60,
                    1.0
                )

            sell *=
                (
                    1.0 -
                        realizationPenalty /
                        100.0
                ).coerceIn(
                    0.60,
                    1.0
                )

            neutral +=
                realizationPenalty
        }

        /*
         * ========================================================
         * NORMALIZAÇÃO
         * ========================================================
         */

        val total =
            buy +
                sell +
                neutral

        if (
            total <= 0.0
        ) {

            return deterministic
        }

        buy =
            buy /
                total *
                100.0

        sell =
            sell /
                total *
                100.0

        neutral =
            neutral /
                total *
                100.0

        /*
         * ========================================================
         * CONFIANÇA FINAL
         * ========================================================
         */

        val confidence =
            clamp(

                deterministic.confidence +

                    (
                        reliability -
                            50.0
                    ) *
                        0.20 *
                        historicalWeight -

                    capturePressure *
                        0.12 +

                    if (
                        realizationPressure >=
                        60.0
                    ) {
                        -5.0
                    } else {
                        0.0
                    }
            )

        /*
         * ========================================================
         * TRAP RISK FINAL
         * ========================================================
         */

        val finalTrapRisk =
            clamp(

                deterministic.trapRisk *
                    0.55 +

                    capturePressure *
                    0.30 +

                    realizationPressure *
                    0.15
            )

        /*
         * ========================================================
         * DIREÇÃO
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
                finalTrapRisk
        )
    }

    /*
     * ============================================================
     * ESTATÍSTICAS PARA A INTERFACE
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

        val state =
            readState(key)

        val samples =
            state.wins +
                state.falseSignals

        if (
            samples == 0
        ) {

            return HistoricalDeterminism(

                confidence =
                    50.0,

                trapRisk =
                    0.0,

                samples =
                    0,

                wins =
                    0,

                falseSignals =
                    0,

                consecutiveFalseSignals =
                    0,

                capturePressure =
                    0.0,

                realizationPressure =
                    0.0
            )
        }

        val reliability =
            (
                state.wins + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0

        val capturePressure =
            calculateCapturePressure(
                state
            )

        val realizationPressure =
            calculateRealizationPressure(
                state
            )

        val trapRisk =
            clamp(

                reliability.let {
                    100.0 - it
                } *
                    0.55 +

                    capturePressure *
                    0.30 +

                    realizationPressure *
                    0.15
            )

        return HistoricalDeterminism(

            confidence =
                clamp(
                    reliability
                ),

            trapRisk =
                trapRisk,

            samples =
                samples,

            wins =
                state.wins,

            falseSignals =
                state.falseSignals,

            consecutiveFalseSignals =
                state.consecutiveFalseSignals,

            capturePressure =
                capturePressure,

            realizationPressure =
                realizationPressure
        )
    }
}

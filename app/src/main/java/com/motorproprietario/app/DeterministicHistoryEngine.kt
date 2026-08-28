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
    val consecutiveFalseSignals: Int,
    val capturePressure: Double,
    val realizationPressure: Double
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
     * O histórico não memoriza somente COMPRA/VENDA.
     *
     * Memoriza o contexto em que o sinal apareceu.
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
     * NOVO:
     * HISTÓRICO DE FALSOS CONSECUTIVOS
     * ============================================================
     *
     * A sequência representa quantos falsos sinais ocorreram
     * consecutivamente naquele contexto.
     *
     * Exemplo:
     *
     * falso → 1
     * falso → 2
     * falso → 3
     *
     * Um acerto quebra a sequência.
     */
    private fun consecutiveFalseSignals(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.consecutiveFalse",
            0
        )
    }

    /*
     * ============================================================
     * NOVO:
     * MAIOR SEQUÊNCIA HISTÓRICA
     * ============================================================
     */
    private fun maxConsecutiveFalseSignals(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.maxConsecutiveFalse",
            0
        )
    }

    private fun registerResult(
        key: String,
        success: Boolean
    ) {

        val currentWins =
            wins(key)

        val currentFalse =
            falseSignals(key)

        val currentConsecutive =
            consecutiveFalseSignals(key)

        val currentMax =
            maxConsecutiveFalseSignals(key)

        val newConsecutive =
            if (success) {
                0
            } else {
                currentConsecutive + 1
            }

        val newMax =
            max(
                currentMax,
                newConsecutive
            )

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
            .putInt(
                "$key.consecutiveFalse",
                newConsecutive
            )
            .putInt(
                "$key.maxConsecutiveFalse",
                newMax
            )
            .apply()
    }

    /*
     * ============================================================
     * AVALIAÇÃO DOS SINAIS PENDENTES
     * ============================================================
     *
     * Usa somente preço real recebido posteriormente.
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
     * PRESSÃO DE CAPTURA
     * ============================================================
     *
     * Não assume intenção institucional.
     *
     * Mede se o histórico apresenta repetição suficiente
     * de sinais que falharam para justificar cautela.
     */
    private fun calculateCapturePressure(
        samples: Int,
        falseSignals: Int,
        consecutiveFalse: Int,
        maxConsecutive: Int
    ): Double {

        if (
            samples <= 0
        ) {
            return 0.0
        }

        val falseRate =
            falseSignals.toDouble() /
                samples.toDouble() *
                100.0

        val currentSequencePressure =
            min(
                100.0,
                consecutiveFalse *
                    20.0
            )

        val historicalSequencePressure =
            min(
                100.0,
                maxConsecutive *
                    12.0
            )

        return clamp(
            falseRate * 0.45 +
            currentSequencePressure * 0.35 +
            historicalSequencePressure * 0.20
        )
    }

    /*
     * ============================================================
     * PRESSÃO DE REALIZAÇÃO
     * ============================================================
     *
     * Surge quando existe histórico ruim + sequência de falsos.
     *
     * Não significa que uma realização irá obrigatoriamente ocorrer.
     */
    private fun calculateRealizationPressure(
        trapPressure: Double,
        falseSignals: Int,
        samples: Int,
        consecutiveFalse: Int
    ): Double {

        if (
            samples <= 0
        ) {
            return 0.0
        }

        val falseRate =
            falseSignals.toDouble() /
                samples.toDouble() *
                100.0

        val sequenceFactor =
            min(
                100.0,
                consecutiveFalse *
                    25.0
            )

        return clamp(
            trapPressure * 0.45 +
            falseRate * 0.25 +
            sequenceFactor * 0.30
        )
    }

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
         * Registra o sinal somente quando existe
         * confiança mínima.
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

        val currentWins =
            wins(key)

        val currentFalse =
            falseSignals(key)

        val samples =
            currentWins +
                currentFalse

        val consecutiveFalse =
            consecutiveFalseSignals(key)

        val maxConsecutive =
            maxConsecutiveFalseSignals(key)

        /*
         * --------------------------------------------------------
         * Sem histórico suficiente:
         * não altera o motor original.
         * --------------------------------------------------------
         */
        if (
            samples < 3
        ) {
            return deterministic
        }

        /*
         * --------------------------------------------------------
         * CONFIABILIDADE HISTÓRICA
         * --------------------------------------------------------
         *
         * Suavização para impedir que poucas amostras
         * dominem o motor.
         */
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

        val historicalWeight =
            min(
                1.0,
                samples /
                    20.0
            )

        /*
         * --------------------------------------------------------
         * CAPTURA HISTÓRICA
         * --------------------------------------------------------
         */
        val capturePressure =
            calculateCapturePressure(
                samples =
                    samples,

                falseSignals =
                    currentFalse,

                consecutiveFalse =
                    consecutiveFalse,

                maxConsecutive =
                    maxConsecutive
            )

        /*
         * A pressão histórica é combinada com
         * a força de utilização do histórico.
         */
        val trapPressure =
            clamp(
                max(
                    historicalTrap *
                        historicalWeight,

                    capturePressure *
                        historicalWeight
                )
            )

        /*
         * --------------------------------------------------------
         * REALIZAÇÃO
         * --------------------------------------------------------
         */
        val realizationPressure =
            calculateRealizationPressure(
                trapPressure =
                    trapPressure,

                falseSignals =
                    currentFalse,

                samples =
                    samples,

                consecutiveFalse =
                    consecutiveFalse
            )

        var buy =
            deterministic.buyScore

        var sell =
            deterministic.sellScore

        var neutral =
            deterministic.neutralScore

        /*
         * --------------------------------------------------------
         * HISTÓRICO RUIM
         * --------------------------------------------------------
         *
         * Quanto maior a pressão de captura,
         * menor a força direcional.
         */
        if (
            trapPressure > 50.0
        ) {

            val penalty =
                (
                    trapPressure -
                        50.0
                ) *
                    0.30

            val factor =
                (
                    1.0 -
                        penalty /
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
                penalty
        }

        /*
         * --------------------------------------------------------
         * HISTÓRICO FAVORÁVEL
         * --------------------------------------------------------
         *
         * Reforço pequeno.
         *
         * Nunca transforma histórico em garantia.
         */
        else {

            val reinforcement =
                (
                    50.0 -
                        trapPressure
                ) *
                    0.08

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
         * --------------------------------------------------------
         * PRESSÃO DE REALIZAÇÃO
         * --------------------------------------------------------
         *
         * Quando o histórico sugere ambiente de captura,
         * aumenta neutralidade.
         *
         * Não cria uma nova direção artificial.
         */
        if (
            realizationPressure >=
                60.0
        ) {

            val realizationPenalty =
                (
                    realizationPressure -
                        50.0
                ) *
                    0.20

            neutral +=
                realizationPenalty
        }

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
         * --------------------------------------------------------
         * CONFIANÇA
         * --------------------------------------------------------
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

                    trapPressure *
                        0.08 -

                    realizationPressure *
                        0.05
            )

        /*
         * --------------------------------------------------------
         * RISCO FINAL DE ARMADILHA
         * --------------------------------------------------------
         */
        val finalTrapRisk =
            clamp(
                deterministic.trapRisk *
                    0.55 +

                historicalTrap *
                    historicalWeight *
                    0.20 +

                capturePressure *
                    historicalWeight *
                    0.25
            )

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
     * ESTATÍSTICAS
     * ============================================================
     *
     * Usadas pelo MainActivity para a análise detalhada.
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

        val consecutiveFalse =
            consecutiveFalseSignals(key)

        val maxConsecutive =
            maxConsecutiveFalseSignals(key)

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

        val capturePressure =
            calculateCapturePressure(
                samples =
                    samples,

                falseSignals =
                    currentFalse,

                consecutiveFalse =
                    consecutiveFalse,

                maxConsecutive =
                    maxConsecutive
            )

        val realizationPressure =
            calculateRealizationPressure(
                trapPressure =
                    capturePressure,

                falseSignals =
                    currentFalse,

                samples =
                    samples,

                consecutiveFalse =
                    consecutiveFalse
            )

        return HistoricalDeterminism(

            confidence =
                clamp(
                    reliability
                ),

            trapRisk =
                clamp(
                    max(
                        historicalTrap,
                        capturePressure
                    )
                ),

            samples =
                samples,

            wins =
                currentWins,

            falseSignals =
                currentFalse,

            consecutiveFalseSignals =
                consecutiveFalse,

            capturePressure =
                capturePressure,

            realizationPressure =
                realizationPressure
        )
    }
}

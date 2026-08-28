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

private data class PendingDeterministicSignal(
    val key: String,
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

    private val pending =
        mutableListOf<PendingDeterministicSignal>()

    private val lastObserved =
        mutableMapOf<String, Long>()

    private val timeframeIntervals =
        mapOf(
            "M1" to 60_000L,
            "M5" to 300_000L,
            "M15" to 900_000L,
            "M30" to 1_800_000L,
            "H1" to 3_600_000L,
            "H4" to 14_400_000L,
            "D1" to 86_400_000L
        )

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

    private fun interval(
        timeframe: String
    ): Long {

        return timeframeIntervals[
            timeframe.uppercase()
        ] ?: 900_000L
    }

    private fun fingerprint(
        metrics: QuantMetrics,
        direction: String,
        sequenceStage: SequenceStage
    ): String {

        val trend =
            when {
                metrics.trend >= 70.0 -> "T3"
                metrics.trend >= 55.0 -> "T2"
                metrics.trend <= 30.0 -> "T0"
                metrics.trend <= 45.0 -> "T1"
                else -> "TN"
            }

        val rsi =
            when {
                metrics.rsi >= 70.0 -> "RO"
                metrics.rsi >= 55.0 -> "RB"
                metrics.rsi <= 30.0 -> "RU"
                metrics.rsi <= 45.0 -> "RS"
                else -> "RN"
            }

        val breakout =
            when {
                metrics.breakout >= 70.0 -> "BO"
                metrics.breakout <= 30.0 -> "BS"
                else -> "BN"
            }

        val structure =
            when {
                metrics.structure >= 70.0 -> "ST3"
                metrics.structure >= 55.0 -> "ST2"
                metrics.structure <= 30.0 -> "ST0"
                metrics.structure <= 45.0 -> "ST1"
                else -> "STN"
            }

        val mtf =
            when {
                metrics.trend >= 60.0 -> "UP"
                metrics.trend <= 40.0 -> "DN"
                else -> "NE"
            }

        return listOf(
            direction,
            sequenceStage.name,
            trend,
            rsi,
            breakout,
            structure,
            mtf
        ).joinToString("|")
    }

    private fun readWins(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.wins",
            0
        )
    }

    private fun readFalseSignals(
        key: String
    ): Int {

        return preferences.getInt(
            "$key.false",
            0
        )
    }

    private fun saveResult(
        key: String,
        win: Boolean
    ) {

        val wins =
            readWins(key)

        val falseSignals =
            readFalseSignals(key)

        preferences.edit()
            .putInt(
                "$key.wins",
                if (win) {
                    wins + 1
                } else {
                    wins
                }
            )
            .putInt(
                "$key.false",
                if (!win) {
                    falseSignals + 1
                } else {
                    falseSignals
                }
            )
            .apply()
    }

    private fun evaluatePending(
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
            pending.iterator()

        while (
            iterator.hasNext()
        ) {

            val signal =
                iterator.next()

            if (
                now <
                    signal.createdAt +
                    min(
                        3L * interval(
                            signal.key
                                .substringAfterLast("@")
                        ),
                        3_600_000L
                    )
            ) {
                continue
            }

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
                    movement >= threshold
                } else {
                    movement <= -threshold
                }

            val adverse =
                if (
                    signal.direction ==
                    "COMPRA"
                ) {
                    movement <= -threshold
                } else {
                    movement >= threshold
                }

            if (
                favorable
            ) {

                saveResult(
                    signal.key,
                    true
                )

                iterator.remove()

            } else if (
                adverse ||
                now >= signal.expiryAt
            ) {

                saveResult(
                    signal.key,
                    false
                )

                iterator.remove()
            }
        }
    }

    fun apply(
        symbol: String,
        timeframe: String,
        sequenceStage: SequenceStage,
        metrics: QuantMetrics,
        deterministic: DeterministicResult,
        currentPrice: Double,
        now: Long
    ): DeterministicResult {

        evaluatePending(
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

        val historyKey =
            "$symbol|$timeframe|" +
                fingerprint(
                    metrics,
                    direction,
                    sequenceStage
                )

        val observationKey =
            "$historyKey@$timeframe"

        val minimumSpacing =
            max(
                30_000L,
                interval(timeframe) / 2L
            )

        val previousObservation =
            lastObserved[
                observationKey
            ]

        val alreadyObserved =
            previousObservation != null &&
                now -
                    previousObservation <
                minimumSpacing

        if (
            !alreadyObserved &&
            deterministic.confidence >= 55.0
        ) {

            val atr =
                max(
                    metrics.atr,
                    currentPrice * 0.0001
                )

            pending.add(
                PendingDeterministicSignal(
                    key =
                        observationKey,

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
                            interval(
                                timeframe
                            ) * 10L
                )
            )

            lastObserved[
                observationKey
            ] = now
        }

        val wins =
            readWins(
                historyKey
            )

        val falseSignals =
            readFalseSignals(
                historyKey
            )

        val samples =
            wins +
                falseSignals

        if (
            samples < 3
        ) {
            return deterministic
        }

        /*
         * Taxa histórica de acerto.
         *
         * Suavização evita que 1 único
         * resultado domine o motor.
         */
        val reliability =
            (
                wins + 1.0
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
         * Quanto maior a repetição de falhas
         * naquele padrão, maior a neutralização
         * da força direcional.
         */
        val historicalWeight =
            min(
                1.0,
                samples / 20.0
            )

        val trapPressure =
            historicalTrap *
                historicalWeight

        var buy =
            deterministic.buyScore

        var sell =
            deterministic.sellScore

        var neutral =
            deterministic.neutralScore

        val directionalPenalty =
            (
                trapPressure -
                    50.0
            ) *
                0.25

        if (
            directionalPenalty > 0.0
        ) {

            val factor =
                (
                    1.0 -
                        directionalPenalty /
                        100.0
                ).coerceIn(
                    0.50,
                    1.0
                )

            buy *= factor
            sell *= factor

            neutral +=
                directionalPenalty

        } else {

            val reinforcement =
                (
                    -directionalPenalty
                ) *
                    0.15

            if (
                direction == "COMPRA"
            ) {
                buy += reinforcement
            } else {
                sell += reinforcement
            }

            neutral -=
                reinforcement
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

        val historicalConfidence =
            clamp(
                reliability
            )

        val confidence =
            clamp(
                deterministic.confidence +
                    (
                        historicalConfidence -
                            50.0
                    ) *
                        0.35 *
                        historicalWeight -
                    trapPressure *
                        0.10
            )

        val finalTrap =
            clamp(
                deterministic.trapRisk *
                    0.65 +
                    historicalTrap *
                    historicalWeight *
                    0.35
            )

        return deterministic.copy(

            buyScore =
                clamp(buy),

            sellScore =
                clamp(sell),

            neutralScore =
                clamp(neutral),

            directionalBias =
                when {
                    buy >= sell &&
                        buy >= neutral ->
                        "COMPRA"

                    sell >= buy &&
                        sell >= neutral ->
                        "VENDA"

                    else ->
                        "NEUTRO"
                },

            confidence =
                confidence,

            trapRisk =
                finalTrap
        )
    }

    fun statistics(
        symbol: String,
        timeframe: String,
        metrics: QuantMetrics,
        direction: String,
        sequenceStage: SequenceStage
    ): HistoricalDeterminism {

        val key =
            "$symbol|$timeframe|" +
                fingerprint(
                    metrics,
                    direction,
                    sequenceStage
                )

        val wins =
            readWins(key)

        val falseSignals =
            readFalseSignals(key)

        val samples =
            wins +
                falseSignals

        if (
            samples == 0
        ) {
            return HistoricalDeterminism(
                confidence = 50.0,
                trapRisk = 50.0,
                samples = 0,
                wins = 0,
                falseSignals = 0
            )
        }

        val reliability =
            (
                wins + 1.0
            ) /
                (
                    samples + 2.0
                ) *
                100.0

        return HistoricalDeterminism(
            confidence =
                clamp(
                    reliability
                ),

            trapRisk =
                clamp(
                    100.0 -
                        reliability
                ),

            samples =
                samples,

            wins =
                wins,

            falseSignals =
                falseSignals
        )
    }
}

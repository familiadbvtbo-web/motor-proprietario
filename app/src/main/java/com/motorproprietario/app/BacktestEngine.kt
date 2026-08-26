package com.motorproprietario.app

import kotlin.math.abs

object BacktestEngine {

    /**
     * Executa o backtest de uma sequência de sinais
     * históricos contra candles reais.
     *
     * O mecanismo não cria sinais.
     * Ele apenas mede o que teria acontecido
     * com sinais já fornecidos pelo Motor.
     */
    fun evaluateSignals(
        signals: List<BacktestSignal>,
        candles: List<MarketCandle>,
        config: BacktestConfig
    ): BacktestReport {

        if (
            signals.isEmpty() ||
            candles.isEmpty()
        ) {

            return emptyReport(
                config
            )
        }

        val orderedCandles =
            candles.sortedBy {
                it.timestamp
            }

        val orderedSignals =
            signals.sortedBy {
                it.timestamp
            }

        val results =
            mutableListOf<BacktestTradeResult>()

        var openSignals =
            0

        for (
            signal in orderedSignals
        ) {

            if (
                signal.symbol !=
                    config.symbol
            ) {
                continue
            }

            if (
                signal.timeframe !=
                    config.timeframe
            ) {
                continue
            }

            if (
                openSignals >=
                    config.maxOpenSignals
            ) {
                continue
            }

            val result =
                evaluateSingleSignal(
                    signal,
                    orderedCandles,
                    config
                )

            if (
                result.outcome !=
                    BacktestOutcome.NO_TRADE
            ) {

                results.add(
                    result
                )

                openSignals =
                    0
            }
        }

        val metrics =
            calculateMetrics(
                results
            )

        return BacktestReport(
            config =
                config,

            metrics =
                metrics,

            trades =
                results
        )
    }

    /**
     * Avalia um único sinal contra o histórico.
     */
    private fun evaluateSingleSignal(
        signal: BacktestSignal,
        candles: List<MarketCandle>,
        config: BacktestConfig
    ): BacktestTradeResult {

        val entry =
            signal.entry

        val stop =
            signal.stop

        if (
            entry <= 0.0 ||
            stop <= 0.0
        ) {

            return noTrade(
                signal,
                "PRECO_OU_STOP_INVALIDO"
            )
        }

        val risk =
            abs(
                entry -
                    stop
            )

        if (
            risk <= 0.0 ||
            !risk.isFinite()
        ) {

            return noTrade(
                signal,
                "RISCO_INVALIDO"
            )
        }

        val startIndex =
            candles.indexOfFirst {
                it.timestamp >=
                    signal.timestamp
            }

        if (
            startIndex < 0
        ) {

            return noTrade(
                signal,
                "SEM_CANDLE_APOS_SINAL"
            )
        }

        val validityMs =
            validityFor(
                signal.timeframe
            )

        val expiry =
            signal.timestamp +
                validityMs

        var maximumFavorable =
            0.0

        var maximumAdverse =
            0.0

        var tp1Reached =
            false

        var tp2Reached =
            false

        var tp3Reached =
            false

        for (
            index in startIndex until candles.size
        ) {

            val candle =
                candles[index]

            if (
                candle.timestamp <
                    signal.timestamp
            ) {
                continue
            }

            if (
                candle.timestamp >
                    expiry
            ) {
                break
            }

            val favorable =
                favorableExcursionR(
                    signal,
                    candle,
                    risk
                )

            val adverse =
                adverseExcursionR(
                    signal,
                    candle,
                    risk
                )

            maximumFavorable =
                maxOf(
                    maximumFavorable,
                    favorable
                )

            maximumAdverse =
                maxOf(
                    maximumAdverse,
                    adverse
                )

            /*
             * ==================================
             * AMBIGUIDADE INTRACANDLE
             * ==================================
             *
             * Se stop e alvo forem tocados no mesmo
             * candle, não assumimos automaticamente
             * que o alvo veio primeiro.
             *
             * A regra conservadora é considerar
             * STOP primeiro.
             */

            val stopHit =
                stopHit(
                    signal,
                    candle
                )

            val tp1Hit =
                targetHit(
                    signal.direction,
                    candle,
                    signal.tp1
                )

            val tp2Hit =
                targetHit(
                    signal.direction,
                    candle,
                    signal.tp2
                )

            val tp3Hit =
                targetHit(
                    signal.direction,
                    candle,
                    signal.tp3
                )

            if (
                tp1Hit
            ) {
                tp1Reached =
                    true
            }

            if (
                tp2Hit
            ) {
                tp2Reached =
                    true
            }

            if (
                tp3Hit
            ) {
                tp3Reached =
                    true
            }

            /*
             * STOP tem prioridade na mesma vela.
             */

            if (
                stopHit
            ) {

                return buildResult(
                    signal =
                        signal,

                    outcome =
                        BacktestOutcome.LOSS,

                    exitTimestamp =
                        candle.timestamp,

                    exitPrice =
                        stop,

                    realizedR =
                        -1.0,

                    targetReached =
                        when {
                            tp2Reached -> 2
                            tp1Reached -> 1
                            else -> 0
                        },

                    maximumFavorable =
                        maximumFavorable,

                    maximumAdverse =
                        maximumAdverse,

                    reason =
                        "STOP_ATINGIDO",

                    config =
                        config
                )
            }

            /*
             * TP3 encerra a operação.
             */

            if (
                tp3Hit
            ) {

                return buildResult(
                    signal =
                        signal,

                    outcome =
                        BacktestOutcome.WIN,

                    exitTimestamp =
                        candle.timestamp,

                    exitPrice =
                        signal.tp3,

                    realizedR =
                        signal.rr3,

                    targetReached =
                        3,

                    maximumFavorable =
                        maximumFavorable,

                    maximumAdverse =
                        maximumAdverse,

                    reason =
                        "TP3_ATINGIDO",

                    config =
                        config
                )
            }

            /*
             * TP2 encerra a operação nesta primeira
             * versão integral do backtest.
             *
             * A futura versão de gestão de posição
             * poderá permitir parcial TP1/TP2/TP3.
             */

            if (
                tp2Hit
            ) {

                return buildResult(
                    signal =
                        signal,

                    outcome =
                        BacktestOutcome.WIN,

                    exitTimestamp =
                        candle.timestamp,

                    exitPrice =
                        signal.tp2,

                    realizedR =
                        signal.rr2,

                    targetReached =
                        2,

                    maximumFavorable =
                        maximumFavorable,

                    maximumAdverse =
                        maximumAdverse,

                    reason =
                        "TP2_ATINGIDO",

                    config =
                        config
                )
            }

            if (
                tp1Hit
            ) {

                return buildResult(
                    signal =
                        signal,

                    outcome =
                        BacktestOutcome.WIN,

                    exitTimestamp =
                        candle.timestamp,

                    exitPrice =
                        signal.tp1,

                    realizedR =
                        signal.rr1,

                    targetReached =
                        1,

                    maximumFavorable =
                        maximumFavorable,

                    maximumAdverse =
                        maximumAdverse,

                    reason =
                        "TP1_ATINGIDO",

                    config =
                        config
                )
            }
        }

        /*
         * Nenhum alvo ou stop foi atingido
         * dentro da validade.
         *
         * Encerramos no último candle válido.
         */

        val lastCandle =
            candles.lastOrNull {
                it.timestamp >=
                    signal.timestamp &&
                it.timestamp <=
                    expiry
            }

        if (
            lastCandle == null
        ) {

            return noTrade(
                signal,
                "SEM_DADOS_NA_JANELA"
            )
        }

        val exitPrice =
            lastCandle.close

        val realizedR =
            if (
                signal.direction ==
                    BacktestDirection.BUY
            ) {

                (
                    exitPrice -
                        entry
                ) /
                    risk

            } else if (
                signal.direction ==
                    BacktestDirection.SELL
            ) {

                (
                    entry -
                        exitPrice
                ) /
                    risk

            } else {

                0.0
            }

        val outcome =
            when {

                realizedR > 0.05 ->
                    BacktestOutcome.EXPIRED

                realizedR < -0.05 ->
                    BacktestOutcome.EXPIRED

                else ->
                    BacktestOutcome.BREAKEVEN
            }

        return buildResult(
            signal =
                signal,

            outcome =
                outcome,

            exitTimestamp =
                lastCandle.timestamp,

            exitPrice =
                exitPrice,

            realizedR =
                realizedR,

            targetReached =
                when {
                    tp3Reached -> 3
                    tp2Reached -> 2
                    tp1Reached -> 1
                    else -> 0
                },

            maximumFavorable =
                maximumFavorable,

            maximumAdverse =
                maximumAdverse,

            reason =
                "EXPIRADO",

            config =
                config
        )
    }

    private fun targetHit(
        direction: BacktestDirection,
        candle: MarketCandle,
        target: Double
    ): Boolean {

        if (
            target <= 0.0
        ) {
            return false
        }

        return when (
            direction
        ) {

            BacktestDirection.BUY ->
                candle.high >=
                    target

            BacktestDirection.SELL ->
                candle.low <=
                    target

            BacktestDirection.NEUTRAL ->
                false
        }
    }

    private fun stopHit(
        signal: BacktestSignal,
        candle: MarketCandle
    ): Boolean {

        return when (
            signal.direction
        ) {

            BacktestDirection.BUY ->
                candle.low <=
                    signal.stop

            BacktestDirection.SELL ->
                candle.high >=
                    signal.stop

            BacktestDirection.NEUTRAL ->
                false
        }
    }

    private fun favorableExcursionR(
        signal: BacktestSignal,
        candle: MarketCandle,
        risk: Double
    ): Double {

        if (
            risk <= 0.0
        ) {
            return 0.0
        }

        return when (
            signal.direction
        ) {

            BacktestDirection.BUY ->

                (
                    candle.high -
                        signal.entry
                ) /
                    risk

            BacktestDirection.SELL ->

                (
                    signal.entry -
                        candle.low
                ) /
                    risk

            BacktestDirection.NEUTRAL ->
                0.0
        }
    }

    private fun adverseExcursionR(
        signal: BacktestSignal,
        candle: MarketCandle,
        risk: Double
    ): Double {

        if (
            risk <= 0.0
        ) {
            return 0.0
        }

        return when (
            signal.direction
        ) {

            BacktestDirection.BUY ->

                (
                    signal.entry -
                        candle.low
                ) /
                    risk

            BacktestDirection.SELL ->

                (
                    candle.high -
                        signal.entry
                ) /
                    risk

            BacktestDirection.NEUTRAL ->
                0.0
        }
    }

    private fun buildResult(
        signal: BacktestSignal,
        outcome: BacktestOutcome,
        exitTimestamp: Long,
        exitPrice: Double,
        realizedR: Double,
        targetReached: Int,
        maximumFavorable: Double,
        maximumAdverse: Double,
        reason: String,
        config: BacktestConfig
    ): BacktestTradeResult {

        val rawReturn =
            when (
                signal.direction
            ) {

                BacktestDirection.BUY ->

                    (
                        exitPrice -
                            signal.entry
                    ) /
                        signal.entry *
                        100.0

                BacktestDirection.SELL ->

                    (
                        signal.entry -
                            exitPrice
                    ) /
                        signal.entry *
                        100.0

                BacktestDirection.NEUTRAL ->
                    0.0
            }

        val cost =
            if (
                config.includeCosts
            ) {

                config.costPercent

            } else {

                0.0
            }

        val returnPercent =
            rawReturn -
                cost

        return BacktestTradeResult(

            signal =
                signal,

            outcome =
                outcome,

            exitTimestamp =
                exitTimestamp,

            exitPrice =
                exitPrice,

            realizedR =
                realizedR,

            returnPercent =
                returnPercent,

            targetReached =
                targetReached,

            maximumFavorableExcursionR =
                maximumFavorable,

            maximumAdverseExcursionR =
                maximumAdverse,

            reason =
                reason
        )
    }

    private fun calculateMetrics(
        trades: List<BacktestTradeResult>
    ): BacktestMetrics {

        if (
            trades.isEmpty()
        ) {

            return BacktestMetrics()
        }

        val wins =
            trades.count {
                it.outcome ==
                    BacktestOutcome.WIN
            }

        val losses =
            trades.count {
                it.outcome ==
                    BacktestOutcome.LOSS
            }

        val breakevens =
            trades.count {
                it.outcome ==
                    BacktestOutcome.BREAKEVEN
            }

        val expired =
            trades.count {
                it.outcome ==
                    BacktestOutcome.EXPIRED
            }

        val invalidated =
            trades.count {
                it.outcome ==
                    BacktestOutcome.INVALIDATED
            }

        val executed =
            trades.count {
                it.outcome !=
                    BacktestOutcome.NO_TRADE
            }

        val totalR =
            trades.sumOf {
                it.realizedR
            }

        val averageR =
            if (
                executed > 0
            ) {

                totalR /
                    executed

            } else {

                0.0
            }

        val winRate =
            if (
                executed > 0
            ) {

                wins.toDouble() /
                    executed *
                    100.0

            } else {

                0.0
            }

        val lossRate =
            if (
                executed > 0
            ) {

                losses.toDouble() /
                    executed *
                    100.0

            } else {

                0.0
            }

        val positiveR =
            trades
                .filter {
                    it.realizedR > 0.0
                }
                .sumOf {
                    it.realizedR
                }

        val negativeR =
            trades
                .filter {
                    it.realizedR < 0.0
                }
                .sumOf {
                    abs(
                        it.realizedR
                    )
                }

        val profitFactor =
            if (
                negativeR > 0.0
            ) {

                positiveR /
                    negativeR

            } else if (
                positiveR > 0.0
            ) {

                Double.POSITIVE_INFINITY

            } else {

                0.0
            }

        val averageWin =
            trades
                .filter {
                    it.realizedR > 0.0
                }
                .map {
                    it.realizedR
                }
                .averageOrZero()

        val averageLoss =
            trades
                .filter {
                    it.realizedR < 0.0
                }
                .map {
                    it.realizedR
                }
                .averageOrZero()

        val expectancy =
            if (
                executed > 0
            ) {

                (
                    wins.toDouble() /
                        executed
                ) *
                    averageWin +

                (
                    losses.toDouble() /
                        executed
                ) *
                    averageLoss

            } else {

                0.0
            }

        /*
         * ==================================
         * DRAWDOWN
         * ==================================
         */

        var equity =
            0.0

        var peak =
            0.0

        var maximumDrawdown =
            0.0

        for (
            trade in trades
        ) {

            equity +=
                trade.realizedR

            if (
                equity >
                    peak
            ) {

                peak =
                    equity
            }

            val drawdown =
                peak -
                    equity

            if (
                drawdown >
                    maximumDrawdown
            ) {

                maximumDrawdown =
                    drawdown
            }
        }

        val best =
            trades.maxOfOrNull {
                it.realizedR
            }
                ?: 0.0

        val worst =
            trades.minOfOrNull {
                it.realizedR
            }
                ?: 0.0

        val averageReturn =
            trades
                .map {
                    it.returnPercent
                }
                .averageOrZero()

        return BacktestMetrics(

            totalSignals =
                trades.size,

            executedSignals =
                executed,

            wins =
                wins,

            losses =
                losses,

            breakevens =
                breakevens,

            expired =
                expired,

            invalidated =
                invalidated,

            winRate =
                winRate,

            lossRate =
                lossRate,

            averageR =
                averageR,

            totalR =
                totalR,

            profitFactor =
                profitFactor,

            maximumDrawdownR =
                maximumDrawdown,

            averageWinR =
                averageWin,

            averageLossR =
                averageLoss,

            expectancyR =
                expectancy,

            averageReturnPercent =
                averageReturn,

            bestTradeR =
                best,

            worstTradeR =
                worst
        )
    }

    private fun validityFor(
        timeframe: String
    ): Long {

        return when (
            timeframe
        ) {

            "M1" ->
                5L *
                    60_000L

            "M5" ->
                15L *
                    60_000L

            "M15" ->
                30L *
                    60_000L

            "M30" ->
                60L *
                    60_000L

            "H1" ->
                120L *
                    60_000L

            "H4" ->
                480L *
                    60_000L

            "D1" ->
                1440L *
                    60_000L

            else ->
                30L *
                    60_000L
        }
    }

    private fun noTrade(
        signal: BacktestSignal,
        reason: String
    ): BacktestTradeResult {

        return BacktestTradeResult(

            signal =
                signal,

            outcome =
                BacktestOutcome.NO_TRADE,

            exitTimestamp =
                signal.timestamp,

            exitPrice =
                signal.entry,

            realizedR =
                0.0,

            returnPercent =
                0.0,

            reason =
                reason
        )
    }

    private fun emptyReport(
        config: BacktestConfig
    ): BacktestReport {

        return BacktestReport(

            config =
                config,

            metrics =
                BacktestMetrics(),

            trades =
                emptyList()
        )
    }

    private fun List<Double>.averageOrZero():
        Double {

        return if (
            isEmpty()
        ) {

            0.0

        } else {

            average()
        }
    }
}

package com.motorproprietario.app

/**
 * Resultado possível de uma operação simulada.
 */
enum class BacktestOutcome {
    WIN,
    LOSS,
    BREAKEVEN,
    EXPIRED,
    INVALIDATED,
    NO_TRADE
}

/**
 * Direção do sinal histórico.
 */
enum class BacktestDirection {
    BUY,
    SELL,
    NEUTRAL
}

/**
 * Um sinal produzido pelo Motor em determinado
 * ponto do histórico.
 *
 * Este modelo guarda o estado do Motor naquele
 * instante para permitir auditoria posterior.
 */
data class BacktestSignal(
    val id: Long,

    val symbol: String,
    val timeframe: String,

    val timestamp: Long,

    val direction: BacktestDirection,

    val entry: Double,
    val stop: Double,

    val tp1: Double,
    val tp2: Double,
    val tp3: Double,

    val probabilityBuy: Double,
    val probabilitySell: Double,
    val probabilityNeutral: Double,

    val deterministicBuy: Double,
    val deterministicSell: Double,
    val deterministicNeutral: Double,

    val deterministicConfidence: Double,

    val falseSignalRisk: Double,
    val mtfConfluence: Double,

    val sequenceConfirmed: Boolean,

    /**
     * Pesos utilizados pelo Motor no momento
     * em que o sinal foi produzido.
     */
    val probabilityWeight: Double,
    val deterministicWeight: Double
)

/**
 * Resultado final de um sinal histórico.
 */
data class BacktestTradeResult(
    val signal: BacktestSignal,

    val outcome: BacktestOutcome,

    val exitTimestamp: Long,

    val exitPrice: Double,

    /**
     * Retorno em múltiplos de risco.
     *
     * Exemplo:
     * +1.0 = ganhou 1R
     * -1.0 = perdeu 1R
     */
    val realizedR: Double,

    /**
     * Resultado percentual relativo à entrada.
     */
    val returnPercent: Double,

    /**
     * Indica qual alvo foi atingido.
     *
     * 0 = nenhum
     * 1 = TP1
     * 2 = TP2
     * 3 = TP3
     */
    val targetReached: Int = 0,

    /**
     * Máxima excursão favorável.
     */
    val maximumFavorableExcursionR: Double = 0.0,

    /**
     * Máxima excursão adversa.
     */
    val maximumAdverseExcursionR: Double = 0.0,

    val reason: String = ""
)

/**
 * Janela usada no backtest.
 */
data class BacktestPeriod(
    val startTimestamp: Long,
    val endTimestamp: Long
)

/**
 * Configuração do backtest.
 */
data class BacktestConfig(

    val symbol: String,

    val timeframe: String,

    val period: BacktestPeriod,

    /**
     * Capital inicial é usado apenas para
     * simulação estatística.
     */
    val initialCapital: Double = 10_000.0,

    /**
     * Risco percentual hipotético por sinal.
     *
     * Não representa recomendação financeira.
     */
    val riskPerTradePercent: Double = 1.0,

    /**
     * Número máximo de sinais simultaneamente
     * considerados no modelo.
     */
    val maxOpenSignals: Int = 1,

    /**
     * Se verdadeiro, custos podem ser aplicados
     * posteriormente pelo BacktestEngine.
     */
    val includeCosts: Boolean = true,

    /**
     * Custo estimado por operação em percentual.
     */
    val costPercent: Double = 0.0
)

/**
 * Estatísticas agregadas de um backtest.
 */
data class BacktestMetrics(

    val totalSignals: Int = 0,

    val executedSignals: Int = 0,

    val wins: Int = 0,

    val losses: Int = 0,

    val breakevens: Int = 0,

    val expired: Int = 0,

    val invalidated: Int = 0,

    val winRate: Double = 0.0,

    val lossRate: Double = 0.0,

    val averageR: Double = 0.0,

    val totalR: Double = 0.0,

    val profitFactor: Double = 0.0,

    val maximumDrawdownR: Double = 0.0,

    val averageWinR: Double = 0.0,

    val averageLossR: Double = 0.0,

    val expectancyR: Double = 0.0,

    val averageReturnPercent: Double = 0.0,

    val bestTradeR: Double = 0.0,

    val worstTradeR: Double = 0.0
)

/**
 * Resultado completo de uma execução de backtest.
 */
data class BacktestReport(

    val config: BacktestConfig,

    val metrics: BacktestMetrics,

    val trades: List<BacktestTradeResult>,

    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Dados utilizados posteriormente pelo
 * CalibrationEngine.
 */
data class CalibrationCandidate(

    val probabilityWeight: Double,

    val deterministicWeight: Double,

    val metrics: BacktestMetrics
)

/**
 * Resultado da calibração.
 *
 * O CalibrationEngine poderá comparar vários
 * candidatos e escolher o mais robusto.
 */
data class CalibrationResult(

    val selectedProbabilityWeight: Double,

    val selectedDeterministicWeight: Double,

    val trainingMetrics: BacktestMetrics,

    val validationMetrics: BacktestMetrics,

    val testMetrics: BacktestMetrics,

    val candidatesEvaluated: Int,

    val accepted: Boolean,

    val reason: String,

    val calibratedAt: Long =
        System.currentTimeMillis()
)

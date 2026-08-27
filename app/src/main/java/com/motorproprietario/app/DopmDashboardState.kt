package com.motorproprietario.app

/**
 * Estado REAL que a interface DOPM recebe do motor.
 *
 * A UI não cria sinais, probabilidades ou preços.
 * Se um dado ainda não existe, permanece null.
 */
data class DopmDashboardState(
    val online: Boolean = false,
    val api: String = "TWELVE DATA",
    val asset: String = "EUR/USD",
    val timeframe: String = "M15",
    val bestTimeframe: String? = null,
    val price: Double? = null,

    val buyProbability: Double? = null,
    val sellProbability: Double? = null,
    val neutralProbability: Double? = null,

    val probabilityConfidence: Double? = null,
    val deterministicConfidence: Double? = null,
    val mtfConfluence: Double? = null,

    val decision: String = "AGUARDAR",

    val entry: Double? = null,
    val stop: Double? = null,
    val tp1: Double? = null,
    val tp2: Double? = null,
    val tp3: Double? = null,

    val timing: String = "AGUARDAR",
    val validityMinutes: Int? = null,
    val expiresAt: Long? = null,

    val fi: Double? = null,
    val fsi: Double? = null,
    val rsi: Double? = null,
    val macd: Double? = null,
    val ema: Double? = null,
    val adx: Double? = null,

    val dataQuality: String? = null,
    val sequenceStage: String? = null,
    val detailedAnalysis: String? = null
)

/**
 * Validação da disponibilidade de dados para o plano operacional.
 *
 * Não calcula nada.
 * Não cria valores.
 * Apenas verifica se existem dados reais suficientes.
 */
object DopmDashboardStateValidator {

    fun canShowOperationalPlan(
        state: DopmDashboardState
    ): Boolean {
        return state.online &&
            state.price != null &&
            state.buyProbability != null &&
            state.sellProbability != null &&
            state.neutralProbability != null &&
            state.probabilityConfidence != null &&
            state.deterministicConfidence != null &&
            state.decision != "AGUARDAR" &&
            state.entry != null &&
            state.stop != null &&
            state.tp1 != null &&
            state.tp2 != null &&
            state.tp3 != null
    }
}

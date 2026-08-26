package com.motorproprietario.app

data class BetaState(
    val connected: Boolean = false,

    val dataQuality: String = "UNKNOWN",

    /*
     * O APK é exclusivamente analista.
     * Execução de ordens permanece desativada.
     */
    val paperMode: Boolean = true,

    val executionEnabled: Boolean = false,

    /*
     * Estado atual da análise.
     */
    val analyzing: Boolean = false,

    val currentAsset: String = "",

    val currentTimeframe: String = "M15",

    val direction: String = "NEUTRO",

    val score: Double = 50.0,

    val buyProbability: Double = 33.33,

    val sellProbability: Double = 33.33,

    val neutralProbability: Double = 33.34,

    val deterministicConfidence: Double = 50.0,

    val falseSignalRisk: Double = 0.0,

    val mtfConfluence: Double = 50.0,

    /*
     * Entrada analítica.
     */
    val entryValid: Boolean = false,

    val entryPrice: Double = 0.0,

    val stopPrice: Double = 0.0,

    val takeProfit1: Double = 0.0,

    val takeProfit2: Double = 0.0,

    val takeProfit3: Double = 0.0,

    /*
     * Sequência proprietária.
     */
    val sequenceStage: SequenceStage =
        SequenceStage.S0,

    val sequenceConfirmed: Boolean = false,

    /*
     * Controle temporal.
     */
    val lastUpdate: Long = 0L,

    val analysisStartedAt: Long = 0L,

    val signalExpiresAt: Long = 0L
) {

    val isUsable: Boolean
        get() =
            connected &&
            dataQuality == "GOOD" &&
            currentAsset.isNotBlank()

    val hasDirectionalSignal: Boolean
        get() =
            direction == "COMPRA" ||
            direction == "VENDA"

    val executionBlocked: Boolean
        get() =
            !executionEnabled
}

object BetaStateFactory {

    fun initial(): BetaState =
        BetaState(
            connected = false,
            dataQuality = "UNKNOWN",
            paperMode = true,
            executionEnabled = false,
            analyzing = false,
            currentAsset = "",
            currentTimeframe = "M15",
            direction = "NEUTRO",
            score = 50.0,
            buyProbability = 33.33,
            sellProbability = 33.33,
            neutralProbability = 33.34,
            deterministicConfidence = 50.0,
            falseSignalRisk = 0.0,
            mtfConfluence = 50.0,
            entryValid = false,
            entryPrice = 0.0,
            stopPrice = 0.0,
            takeProfit1 = 0.0,
            takeProfit2 = 0.0,
            takeProfit3 = 0.0,
            sequenceStage = SequenceStage.S0,
            sequenceConfirmed = false,
            lastUpdate = 0L,
            analysisStartedAt = 0L,
            signalExpiresAt = 0L
        )
}

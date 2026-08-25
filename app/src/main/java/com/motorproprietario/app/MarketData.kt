package com.motorproprietario.app

data class MarketData(
    val asset: String,
    val timestamp: Long,
    val price: Double,
    val structure: Double,
    val trend: Double,
    val momentum: Double,
    val volume: Double,
    val volatility: Double,
    val fsi: Double,
    val multiTimeframe: Double,
    val dataQuality: String,

    // Dados recebidos do mercado
    val bid: Double = 0.0,
    val ask: Double = 0.0,
    val spread: Double = 0.0,

    // Identificação da origem do dado
    val source: String = "UNKNOWN"
) {

    /**
     * Converte timestamp Unix em segundos para milissegundos.
     *
     * O MT5/Python normalmente fornece Unix timestamp em segundos.
     * O Android trabalha com System.currentTimeMillis().
     */
    fun timestampMs(): Long {
        return when {
            timestamp <= 0L -> 0L
            timestamp < 10_000_000_000L -> timestamp * 1000L
            else -> timestamp
        }
    }

    fun isFresh(
        now: Long,
        maxAgeMs: Long = 60_000L
    ): Boolean {

        val normalizedTimestamp = timestampMs()

        return normalizedTimestamp > 0L &&
                now >= normalizedTimestamp &&
                now - normalizedTimestamp <= maxAgeMs
    }

    fun isUsable(now: Long): Boolean {

        return price > 0.0 &&
                dataQuality == "GOOD" &&
                isFresh(now)
    }

    /**
     * Validação adicional para dados provenientes do Forex.
     */
    fun isForexUsable(now: Long): Boolean {

        return isUsable(now) &&
                asset.isNotBlank() &&
                bid > 0.0 &&
                ask > 0.0 &&
                ask >= bid
    }
}

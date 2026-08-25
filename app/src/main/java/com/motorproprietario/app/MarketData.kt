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
    val dataQuality: String
) {
    fun isFresh(
        now: Long,
        maxAgeMs: Long = 60_000L
    ): Boolean =
        timestamp > 0L &&
        now >= timestamp &&
        now - timestamp <= maxAgeMs

    fun isUsable(now: Long): Boolean =
        price > 0.0 &&
        dataQuality == "GOOD" &&
        isFresh(now)
}

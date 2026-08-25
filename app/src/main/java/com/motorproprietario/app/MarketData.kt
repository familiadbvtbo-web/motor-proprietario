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
     * Converte automaticamente Unix timestamp em segundos
     * para milissegundos.
     *
     * Se o timestamp já estiver em milissegundos,
     * mantém o valor original.
     */
    fun timestampMs(): Long {

        return when {

            timestamp <= 0L ->
                0L

            timestamp < 10_000_000_000L ->
                timestamp * 1000L

            else ->
                timestamp
        }
    }

    /**
     * Verifica se o timestamp ainda está dentro
     * da janela de validade do motor.
     *
     * 120 segundos foi padronizado com o
     * RealtimeMarketAnalyzer.
     */
    fun isFresh(
        now: Long,
        maxAgeMs: Long = 120_000L
    ): Boolean {

        val normalizedTimestamp =
            timestampMs()

        return normalizedTimestamp > 0L &&
                now >= normalizedTimestamp &&
                now - normalizedTimestamp <= maxAgeMs
    }

    /**
     * Verificação principal de utilização do mercado.
     *
     * O dado precisa:
     * - possuir preço válido;
     * - estar marcado como GOOD;
     * - possuir timestamp válido;
     * - estar dentro da janela de 120 segundos.
     */
    fun isUsable(
        now: Long
    ): Boolean {

        return price > 0.0 &&
                dataQuality == "GOOD" &&
                isFresh(
                    now,
                    120_000L
                )
    }

    /**
     * Validação específica para Forex.
     *
     * Além das validações normais,
     * exige BID e ASK válidos.
     */
    fun isForexUsable(
        now: Long
    ): Boolean {

        return isUsable(now) &&
                asset.isNotBlank() &&
                bid > 0.0 &&
                ask > 0.0 &&
                ask >= bid
    }
}

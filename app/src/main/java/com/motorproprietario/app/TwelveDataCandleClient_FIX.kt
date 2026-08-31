package com.motorproprietario.app

data class MarketCandle(
    val datetime: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

/*
 * Fachada usada pelo MainActivity.
 * Não remove MarketCandle: ele é o modelo comum usado pelo Analyzer.
 */
class TwelveDataCandleClient {

    companion object {
        const val MAX_CANDLES = 5000
        const val DEFAULT_CANDLES = 5000
    }

    private val resolver by lazy { SymbolResolver() }

    private val router by lazy {
        MarketDataRouter(
            TwelveDataRawCandleClient(),
            AlphaVantageClient(
                resolveAlphaKey()
            )
        )
    }

    fun getCandles(
        symbol: String,
        timeframe: String,
        outputSize: Int = DEFAULT_CANDLES
    ): List<MarketCandle> {

        val requested = symbol.trim()

        require(requested.isNotBlank()) {
            "SYMBOL_EMPTY"
        }

        val resolved =
            try {
                resolver.resolve(requested).symbol
            } catch (_: Exception) {
                AssetRegistry.twelveDataSymbol(requested)
            }

        return try {
            router.getCandles(
                assetId = resolved,
                timeframe = timeframe,
                outputSize = outputSize.coerceIn(1, MAX_CANDLES)
            ).candles
        } catch (first: Exception) {
            if (!resolved.equals(requested, ignoreCase = true)) {
                router.getCandles(
                    assetId = requested,
                    timeframe = timeframe,
                    outputSize = outputSize.coerceIn(1, MAX_CANDLES)
                ).candles
            } else {
                throw first
            }
        }
    }

    private fun resolveAlphaKey(): String =
        try {
            val field =
                Class.forName("com.motorproprietario.app.ApiConfig")
                    .getDeclaredField("ALPHA_VANTAGE_API_KEY")

            field.isAccessible = true
            field.get(null)?.toString()?.trim() ?: ""
        } catch (_: Exception) {
            ""
        }
}

package com.motorproprietario.app

class MarketDataRouter(
    private val twelveData: TwelveDataCandleClient,
    private val alphaVantage: AlphaVantageClient
) {
    data class Result(
        val candles: List<MarketCandle>,
        val source: String,
        val fallbackUsed: Boolean
    )

    fun getCandles(
        assetId: String,
        timeframe: String,
        outputSize: Int = TwelveDataCandleClient.DEFAULT_CANDLES
    ): Result {
        val symbol = AssetRegistry.twelveDataSymbol(assetId)

        try {
            val candles = twelveData.getCandles(symbol, timeframe, outputSize)
            if (candles.isNotEmpty())
                return Result(candles, "TWELVE DATA", false)
        } catch (_: Exception) {}

        val asset = AssetRegistry.get(assetId)
        val tf = timeframe.uppercase()

        if (asset?.alphaVantageSymbol != null &&
            (tf == "D1" || tf == "W1" || tf == "MN1")) {
            val candles = alphaVantage.getDailyCandles(
                asset.alphaVantageSymbol, outputSize
            )
            if (candles.isNotEmpty())
                return Result(candles, "ALPHA VANTAGE", true)
        }

        throw RuntimeException("ATIVO_NAO_DISPONIVEL: $assetId/$timeframe")
    }
}

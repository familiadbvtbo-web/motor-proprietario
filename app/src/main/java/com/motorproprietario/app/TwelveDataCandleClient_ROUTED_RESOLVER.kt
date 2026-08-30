package com.motorproprietario.app

/*
 * Cliente público mantido para o MainActivity atual.
 *
 * Fluxo:
 * MainActivity
 *   -> TwelveDataCandleClient
 *   -> SymbolResolver
 *   -> símbolo real Twelve Data
 *   -> MarketDataRouter
 *   -> candles
 *
 * O símbolo resolvido é usado somente quando a busca
 * consegue confirmar um instrumento. Se não conseguir,
 * o símbolo original continua sendo tentado, preservando
 * EUR/USD e BTC/USD.
 */
class TwelveDataCandleClient {

    companion object {
        const val MAX_CANDLES = 5000
        const val DEFAULT_CANDLES = 5000
    }

    private val resolver by lazy {
        SymbolResolver()
    }

    private val router by lazy {
        MarketDataRouter()
    }

    fun getCandles(
        symbol: String,
        timeframe: String,
        outputSize: Int = DEFAULT_CANDLES
    ): List<MarketCandle> {

        val requested =
            symbol.trim()

        require(requested.isNotBlank()) {
            "SYMBOL_EMPTY"
        }

        val resolvedSymbol =
            try {
                resolver.resolve(requested).symbol
            } catch (_: Exception) {
                /*
                 * Compatibilidade: ativos que já funcionam
                 * continuam usando o símbolo original.
                 */
                requested
            }

        /*
         * Primeiro tenta o símbolo confirmado.
         */
        try {

            return router
                .getCandles(
                    assetId = resolvedSymbol,
                    timeframe = timeframe,
                    outputSize =
                        outputSize.coerceIn(
                            1,
                            MAX_CANDLES
                        )
                )
                .candles

        } catch (firstError: Exception) {

            /*
             * Se o SymbolResolver encontrou um símbolo que
             * não funciona para aquele endpoint/timeframe,
             * tenta o símbolo solicitado originalmente.
             */
            if (
                !resolvedSymbol.equals(
                    requested,
                    ignoreCase = true
                )
            ) {

                return router
                    .getCandles(
                        assetId = requested,
                        timeframe = timeframe,
                        outputSize =
                            outputSize.coerceIn(
                                1,
                                MAX_CANDLES
                            )
                    )
                    .candles
            }

            throw firstError
        }
    }
}

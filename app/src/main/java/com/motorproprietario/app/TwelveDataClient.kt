package com.motorproprietario.app

import org.json.JSONObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.util.concurrent.CompletionStage

data class RealTimeQuote(
    val symbol: String,
    val timestamp: Long,
    val price: Double,
    val dayVolume: Double?
)

class TwelveDataClient(
    private val apiKey: String
) {

    private var socket: WebSocket? = null

    private var listener:
        ((RealTimeQuote) -> Unit)? = null

    fun connect(
        symbols: List<String>,
        onQuote: (RealTimeQuote) -> Unit,
        onError: (Throwable) -> Unit
    ) {

        listener = onQuote

        if (apiKey.isBlank()) {
            onError(
                IllegalStateException(
                    "TWELVE_DATA_API_KEY_NOT_CONFIGURED"
                )
            )
            return
        }

        val endpoint =
            "wss://ws.twelvedata.com/v1/quotes/price" +
            "?apikey=$apiKey"

        try {

            socket =
                HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(
                        URI.create(endpoint),
                        object : WebSocket.Listener {

                            private val buffer =
                                StringBuilder()

                            override fun onOpen(
                                webSocket: WebSocket
                            ) {

                                val cleanSymbols =
                                    symbols
                                        .filter {
                                            it.isNotBlank()
                                        }
                                        .distinct()

                                if (cleanSymbols.isEmpty()) {
                                    onError(
                                        IllegalArgumentException(
                                            "NO_SYMBOLS"
                                        )
                                    )
                                    return
                                }

                                val payload =
                                    JSONObject()
                                        .put(
                                            "action",
                                            "subscribe"
                                        )
                                        .put(
                                            "params",
                                            JSONObject()
                                                .put(
                                                    "symbols",
                                                    cleanSymbols
                                                        .joinToString(",")
                                                )
                                        )
                                        .toString()

                                webSocket.sendText(
                                    payload,
                                    true
                                )
                            }

                            override fun onText(
                                webSocket: WebSocket,
                                data: CharSequence,
                                last: Boolean
                            ): CompletionStage<*>? {

                                buffer.append(data)

                                if (last) {

                                    val message =
                                        buffer.toString()

                                    buffer.setLength(0)

                                    try {

                                        processMessage(
                                            message
                                        )

                                    } catch (
                                        error: Exception
                                    ) {

                                        onError(error)
                                    }
                                }

                                return null
                            }

                            override fun onError(
                                webSocket: WebSocket,
                                error: Throwable
                            ) {

                                onError(error)
                            }
                        }
                    )
                    .join()

        } catch (error: Exception) {

            onError(error)
        }
    }

    private fun processMessage(
        message: String
    ) {

        val json =
            JSONObject(message)

        val event =
            json.optString("event")

        if (event != "price") {
            return
        }

        val symbol =
            json.optString("symbol")

        val price =
            json.optDouble(
                "price",
                Double.NaN
            )

        val timestamp =
            json.optLong(
                "timestamp",
                0L
            )

        val volume =
            if (
                json.has("day_volume") &&
                !json.isNull("day_volume")
            ) {
                json.optDouble(
                    "day_volume",
                    Double.NaN
                )
            } else {
                null
            }

        if (
            symbol.isBlank() ||
            !price.isFinite() ||
            price <= 0.0 ||
            timestamp <= 0L
        ) {
            return
        }

        listener?.invoke(
            RealTimeQuote(
                symbol = symbol,
                timestamp = timestamp * 1000L,
                price = price,
                dayVolume =
                    volume?.takeIf {
                        it.isFinite()
                    }
            )
        )
    }

    fun disconnect() {

        socket?.sendClose(
            WebSocket.NORMAL_CLOSURE,
            "client_shutdown"
        )

        socket = null
    }
}

package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RealTimeQuote(
    val symbol: String,
    val timestamp: Long,
    val price: Double,
    val dayVolume: Double?
)

class TwelveDataClient {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

    private var webSocket: WebSocket? = null

    private var listener:
        ((RealTimeQuote) -> Unit)? = null

    private var errorListener:
        ((Throwable) -> Unit)? = null

    fun connect(
        symbols: List<String>,
        onQuote: (RealTimeQuote) -> Unit,
        onError: (Throwable) -> Unit
    ) {

        listener = onQuote
        errorListener = onError

        disconnect()

        if (symbols.isEmpty()) {

            onError(
                IllegalArgumentException(
                    "Nenhum ativo informado."
                )
            )

            return
        }

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (apiKey.isBlank()) {

            onError(
                IllegalStateException(
                    "TWELVE_DATA_API_KEY não configurada."
                )
            )

            return
        }

        val cleanSymbols =
            symbols
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }
                .distinct()

        val endpoint =
            "wss://ws.twelvedata.com/v1/quotes/price" +
            "?apikey=$apiKey"

        val request =
            Request.Builder()
                .url(endpoint)
                .build()

        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {

                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response
                    ) {

                        val subscribe =
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

                        webSocket.send(
                            subscribe
                        )
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String
                    ) {

                        processMessage(
                            text
                        )
                    }

                    override fun onClosing(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String
                    ) {

                        webSocket.close(
                            1000,
                            "Encerrando conexão"
                        )
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String
                    ) {

                        this@TwelveDataClient.webSocket =
                            null
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {

                        this@TwelveDataClient.webSocket =
                            null

                        errorListener?.invoke(
                            t
                        )
                    }
                }
            )
    }

    private fun processMessage(
        message: String
    ) {

        try {

            val json =
                JSONObject(message)

            /*
             * Mensagens que não são preços,
             * como heartbeat/status/error,
             * não entram na análise.
             */

            val event =
                json.optString(
                    "event"
                )

            if (
                event != "price"
            ) {
                return
            }

            val symbol =
                json.optString(
                    "symbol"
                )

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

            val dayVolume =
                if (
                    json.has(
                        "day_volume"
                    ) &&
                    !json.isNull(
                        "day_volume"
                    )
                ) {

                    json.optDouble(
                        "day_volume",
                        Double.NaN
                    )

                } else {

                    null
                }

            if (
                symbol.isBlank()
            ) {
                return
            }

            if (
                !price.isFinite() ||
                price <= 0.0
            ) {
                return
            }

            val timestampMs =
                if (timestamp > 0L) {

                    if (
                        timestamp < 10_000_000_000L
                    ) {
                        timestamp * 1000L
                    } else {
                        timestamp
                    }

                } else {

                    System.currentTimeMillis()
                }

            listener?.invoke(
                RealTimeQuote(
                    symbol = symbol,
                    timestamp = timestampMs,
                    price = price,
                    dayVolume =
                        dayVolume?.takeIf {
                            it.isFinite()
                        }
                )
            )

        } catch (
            error: Exception
        ) {

            errorListener?.invoke(
                error
            )
        }
    }

    fun disconnect() {

        webSocket?.close(
            1000,
            "Cliente encerrado"
        )

        webSocket = null
    }

    fun isConnected(): Boolean =
        webSocket != null
}

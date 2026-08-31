package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class RealTimeQuote(
    val symbol: String,
    val price: Double,
    val timestamp: Long
)

class TwelveDataClient {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

    private val resolver by lazy { SymbolResolver() }

    @Volatile private var socket: WebSocket? = null
    @Volatile private var requestedSymbol = ""
    @Volatile private var resolvedSymbol = ""
    @Volatile private var reconnectAttempt = 0

    private val stopped = AtomicBoolean(true)

    private var onQuoteCallback: ((RealTimeQuote) -> Unit)? = null
    private var onErrorCallback: ((Exception) -> Unit)? = null

    @Synchronized
    fun connect(
        symbols: List<String>,
        onQuote: (RealTimeQuote) -> Unit,
        onError: (Exception) -> Unit
    ) {
        disconnect()

        val requested =
            symbols.firstOrNull()?.trim().orEmpty()

        if (requested.isBlank()) {
            onError(IllegalArgumentException("NENHUM_ATIVO_SELECIONADO"))
            return
        }

        requestedSymbol = requested
        onQuoteCallback = onQuote
        onErrorCallback = onError
        reconnectAttempt = 0
        stopped.set(false)

        openSocket()
    }

    private fun openSocket() {

        if (stopped.get()) return

        val apiKey = ApiConfig.TWELVE_DATA_API_KEY

        if (apiKey.isBlank()) {
            onErrorCallback?.invoke(
                IllegalStateException("TWELVE_DATA_API_KEY não configurada")
            )
            return
        }

        Thread {
            try {

                resolvedSymbol =
                    try {
                        resolver.resolve(requestedSymbol).symbol
                    } catch (_: Exception) {
                        AssetRegistry.twelveDataSymbol(requestedSymbol)
                    }

                val url =
                    "wss://ws.twelvedata.com/v1/quotes/price" +
                        "?apikey=" +
                        URLEncoder.encode(apiKey, "UTF-8")

                val listener =
                    object : WebSocketListener() {

                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response
                        ) {
                            if (stopped.get()) {
                                webSocket.close(1000, "STOPPED")
                                return
                            }

                            reconnectAttempt = 0
                            socket = webSocket

                            val message =
                                JSONObject()
                                    .put("action", "subscribe")
                                    .put(
                                        "params",
                                        JSONObject()
                                            .put("symbols", resolvedSymbol)
                                    )
                                    .toString()

                            webSocket.send(message)
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String
                        ) {
                            handleMessage(text)
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?
                        ) {
                            if (socket === webSocket) socket = null
                            if (!stopped.get()) scheduleReconnect(t)
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String
                        ) {
                            if (socket === webSocket) socket = null
                            if (!stopped.get()) {
                                scheduleReconnect(
                                    RuntimeException(
                                        "WEBSOCKET_CLOSED_$code: $reason"
                                    )
                                )
                            }
                        }
                    }

                socket =
                    client.newWebSocket(
                        Request.Builder()
                            .url(url)
                            .build(),
                        listener
                    )

            } catch (error: Exception) {
                if (!stopped.get()) scheduleReconnect(error)
            }
        }.start()
    }

    private fun handleMessage(text: String) {

        try {
            val json = JSONObject(text)

            if (
                json.optString("event")
                    .equals("price", ignoreCase = true)
            ) {

                val price =
                    json.optString("price")
                        .toDoubleOrNull()
                        ?: return

                if (!price.isFinite() || price <= 0.0) return

                val rawTimestamp =
                    json.optLong("timestamp", 0L)

                val timestamp =
                    if (rawTimestamp > 0L) {
                        if (rawTimestamp < 10_000_000_000L)
                            rawTimestamp * 1000L
                        else
                            rawTimestamp
                    } else {
                        System.currentTimeMillis()
                    }

                onQuoteCallback?.invoke(
                    RealTimeQuote(
                        symbol = requestedSymbol,
                        price = price,
                        timestamp = timestamp
                    )
                )
            }

            if (
                json.optString("event")
                    .equals("subscribe-status", ignoreCase = true) &&
                json.optString("status")
                    .equals("error", ignoreCase = true)
            ) {
                onErrorCallback?.invoke(
                    RuntimeException(
                        json.optString(
                            "message",
                            "SUBSCRIBE_ERROR"
                        )
                    )
                )
            }

        } catch (error: Exception) {
            onErrorCallback?.invoke(error)
        }
    }

    private fun scheduleReconnect(
        cause: Throwable
    ) {

        if (stopped.get()) return

        val attempt =
            reconnectAttempt.coerceAtMost(5)

        val delay =
            minOf(
                30_000L,
                1_000L shl attempt
            )

        reconnectAttempt =
            (reconnectAttempt + 1).coerceAtMost(10)

        Thread {
            try {
                Thread.sleep(delay)
            } catch (_: InterruptedException) {
                return@Thread
            }

            if (!stopped.get()) openSocket()
        }.start()
    }

    @Synchronized
    fun disconnect() {

        stopped.set(true)

        socket?.close(
            1000,
            "CLIENT_DISCONNECT"
        )

        socket = null
        requestedSymbol = ""
        resolvedSymbol = ""
        reconnectAttempt = 0
        onQuoteCallback = null
        onErrorCallback = null
    }
}

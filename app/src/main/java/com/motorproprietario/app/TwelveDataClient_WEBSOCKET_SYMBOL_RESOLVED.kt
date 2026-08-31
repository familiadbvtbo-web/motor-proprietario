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

/*
 * Twelve Data em tempo real.
 *
 * Mantém WebSocket real + SymbolResolver.
 *
 * Fluxo:
 * ativo solicitado
 *   -> SymbolResolver
 *   -> símbolo Twelve Data confirmado
 *   -> WebSocket /v1/quotes/price
 *   -> preço/timestamp
 *   -> MainActivity
 *
 * A Twelve Data documenta esse endpoint e o evento subscribe.
 */
class TwelveDataClient {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(10, TimeUnit.SECONDS)
            .build()

    private val resolver by lazy {
        SymbolResolver()
    }

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var requestedSymbol: String = ""

    @Volatile
    private var resolvedSymbol: String = ""

    @Volatile
    private var reconnectAttempt = 0

    private val stopped =
        AtomicBoolean(true)

    private var onQuoteCallback:
        ((RealTimeQuote) -> Unit)? = null

    private var onErrorCallback:
        ((Exception) -> Unit)? = null

    @Synchronized
    fun connect(
        symbols: List<String>,
        onQuote: (RealTimeQuote) -> Unit,
        onError: (Exception) -> Unit
    ) {

        disconnect()

        if (symbols.isEmpty()) {
            onError(
                IllegalArgumentException(
                    "NENHUM_ATIVO_SELECIONADO"
                )
            )
            return
        }

        val requested =
            symbols.first().trim()

        if (requested.isBlank()) {
            onError(
                IllegalArgumentException(
                    "SYMBOL_EMPTY"
                )
            )
            return
        }

        requestedSymbol =
            requested

        onQuoteCallback =
            onQuote

        onErrorCallback =
            onError

        reconnectAttempt = 0

        stopped.set(false)

        openSocket()
    }

    private fun openSocket() {

        if (stopped.get()) {
            return
        }

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (apiKey.isBlank()) {

            onErrorCallback?.invoke(
                IllegalStateException(
                    "TWELVE_DATA_API_KEY não configurada"
                )
            )

            return
        }

        Thread {

            try {

                /*
                 * Resolve somente uma vez por conexão.
                 * O resolver possui cache próprio.
                 */
                resolvedSymbol =
                    try {

                        resolver
                            .resolve(
                                requestedSymbol
                            )
                            .symbol

                    } catch (_: Exception) {

                        /*
                         * Mantém compatibilidade com os
                         * símbolos que já funcionam.
                         */
                        AssetRegistry
                            .twelveDataSymbol(
                                requestedSymbol
                            )
                    }

                val encodedKey =
                    URLEncoder.encode(
                        apiKey,
                        "UTF-8"
                    )

                val url =
                    "wss://ws.twelvedata.com/" +
                        "v1/quotes/price" +
                        "?apikey=$encodedKey"

                val request =
                    Request.Builder()
                        .url(url)
                        .build()

                val listener =
                    object : WebSocketListener() {

                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response
                        ) {

                            if (stopped.get()) {
                                webSocket.close(
                                    1000,
                                    "STOPPED"
                                )
                                return
                            }

                            reconnectAttempt = 0

                            socket =
                                webSocket

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
                                                resolvedSymbol
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

                            handleMessage(
                                text
                            )
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?
                        ) {

                            if (
                                socket === webSocket
                            ) {
                                socket = null
                            }

                            if (
                                !stopped.get()
                            ) {

                                scheduleReconnect(
                                    t
                                )
                            }
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String
                        ) {

                            if (
                                socket === webSocket
                            ) {
                                socket = null
                            }

                            if (
                                !stopped.get()
                            ) {

                                scheduleReconnect(
                                    RuntimeException(
                                        "WEBSOCKET_CLOSED_" +
                                            "$code: $reason"
                                    )
                                )
                            }
                        }
                    }

                val ws =
                    client.newWebSocket(
                        request,
                        listener
                    )

                socket =
                    ws

            } catch (error: Exception) {

                if (
                    !stopped.get()
                ) {

                    scheduleReconnect(
                        error
                    )
                }
            }

        }.start()
    }

    private fun handleMessage(
        text: String
    ) {

        try {

            val json =
                JSONObject(text)

            val event =
                json.optString(
                    "event"
                )

            /*
             * Status de subscribe não é cotação.
             */
            if (
                event.equals(
                    "subscribe-status",
                    ignoreCase = true
                )
            ) {

                val status =
                    json.optString(
                        "status"
                    )

                if (
                    status.equals(
                        "error",
                        ignoreCase = true
                    )
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

                return
            }

            /*
             * O evento de preço contém símbolo,
             * timestamp e price.
             */
            if (
                event.equals(
                    "price",
                    ignoreCase = true
                )
            ) {

                val price =
                    json.optString(
                        "price"
                    )
                        .toDoubleOrNull()

                val timestampSeconds =
                    when {

                        json.has(
                            "timestamp"
                        ) ->
                            json.optLong(
                                "timestamp",
                                0L
                            )

                        else ->
                            0L
                    }

                if (
                    price == null ||
                    !price.isFinite() ||
                    price <= 0.0
                ) {
                    return
                }

                val timestamp =
                    if (
                        timestampSeconds > 0L
                    ) {

                        if (
                            timestampSeconds <
                            10_000_000_000L
                        ) {
                            timestampSeconds *
                                1000L
                        } else {
                            timestampSeconds
                        }

                    } else {
                        System.currentTimeMillis()
                    }

                onQuoteCallback?.invoke(

                    RealTimeQuote(

                        symbol =
                            requestedSymbol,

                        price =
                            price,

                        timestamp =
                            timestamp
                    )
                )
            }

        } catch (error: Exception) {

            onErrorCallback?.invoke(
                error
            )
        }
    }

    private fun scheduleReconnect(
        cause: Throwable
    ) {

        if (
            stopped.get()
        ) {
            return
        }

        /*
         * Backoff:
         * 1s, 2s, 4s, 8s, 16s, 30s máximo.
         */
        val attempt =
            reconnectAttempt
                .coerceAtMost(5)

        val delay =
            minOf(
                30_000L,
                1_000L shl attempt
            )

        reconnectAttempt =
            (reconnectAttempt + 1)
                .coerceAtMost(10)

        Thread {

            try {

                Thread.sleep(
                    delay
                )

            } catch (_: InterruptedException) {
                return@Thread
            }

            if (
                !stopped.get()
            ) {

                openSocket()
            }

        }.start()
    }

    @Synchronized
    fun disconnect() {

        stopped.set(true)

        socket?.close(
            1000,
            "CLIENT_DISCONNECT"
        )

        socket =
            null

        requestedSymbol =
            ""

        resolvedSymbol =
            ""

        reconnectAttempt =
            0

        onQuoteCallback =
            null

        onErrorCallback =
            null
    }
}

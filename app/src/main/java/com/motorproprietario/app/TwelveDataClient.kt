package com.motorproprietario.app

import android.os.Handler
import android.os.Looper
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
            .connectTimeout(
                15,
                TimeUnit.SECONDS
            )
            .readTimeout(
                0,
                TimeUnit.MILLISECONDS
            )
            .pingInterval(
                10,
                TimeUnit.SECONDS
            )
            .build()

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var webSocket:
        WebSocket? = null

    private var quoteListener:
        ((RealTimeQuote) -> Unit)? = null

    private var errorListener:
        ((Throwable) -> Unit)? = null

    private var subscribedSymbols:
        List<String> = emptyList()

    private var reconnectAttempts =
        0

    private var manuallyDisconnected =
        false

    private var connecting =
        false

    private val reconnectRunnable =
        Runnable {

            if (
                !manuallyDisconnected &&
                subscribedSymbols.isNotEmpty()
            ) {
                connectInternal(
                    subscribedSymbols
                )
            }
        }

    fun connect(
        symbols: List<String>,
        onQuote: (RealTimeQuote) -> Unit,
        onError: (Throwable) -> Unit
    ) {

        quoteListener =
            onQuote

        errorListener =
            onError

        manuallyDisconnected =
            false

        reconnectAttempts =
            0

        subscribedSymbols =
            symbols
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }
                .distinct()

        handler.removeCallbacks(
            reconnectRunnable
        )

        disconnectSocketOnly()

        if (
            subscribedSymbols.isEmpty()
        ) {

            errorListener?.invoke(
                IllegalArgumentException(
                    "Nenhum ativo informado."
                )
            )

            return
        }

        connectInternal(
            subscribedSymbols
        )
    }

    private fun connectInternal(
        symbols: List<String>
    ) {

        if (
            manuallyDisconnected
        ) {
            return
        }

        if (
            connecting
        ) {
            return
        }

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (
            apiKey.isBlank()
        ) {

            errorListener?.invoke(
                IllegalStateException(
                    "TWELVE_DATA_API_KEY não configurada."
                )
            )

            return
        }

        connecting =
            true

        val url =
            "wss://ws.twelvedata.com/v1/quotes/price" +
                "?apikey=$apiKey"

        val request =
            Request.Builder()
                .url(url)
                .build()

        webSocket =
            client.newWebSocket(
                request,
                createWebSocketListener(
                    symbols
                )
            )
    }

    private fun createWebSocketListener(
        symbols: List<String>
    ): WebSocketListener {

        return object :
            WebSocketListener() {

            override fun onOpen(
                webSocket: WebSocket,
                response: Response
            ) {

                connecting =
                    false

                reconnectAttempts =
                    0

                val subscription =
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
                                    symbols.joinToString(
                                        ","
                                    )
                                )
                        )

                webSocket.send(
                    subscription.toString()
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

                connecting =
                    false

                if (
                    this@TwelveDataClient
                        .webSocket ===
                    webSocket
                ) {

                    this@TwelveDataClient
                        .webSocket =
                        null
                }

                scheduleReconnect()
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {

                connecting =
                    false

                if (
                    this@TwelveDataClient
                        .webSocket ===
                    webSocket
                ) {

                    this@TwelveDataClient
                        .webSocket =
                        null
                }

                errorListener?.invoke(
                    t
                )

                scheduleReconnect()
            }
        }
    }

    private fun processMessage(
        message: String
    ) {

        try {

            val json =
                JSONObject(
                    message
                )

            val event =
                json.optString(
                    "event"
                )

            /*
             * O WebSocket envia outros eventos
             * além das cotações.
             */
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

            /*
             * Proteção contra dados inválidos.
             */
            if (
                symbol.isBlank()
            ) {
                return
            }

            if (
                !price.isFinite()
            ) {
                return
            }

            if (
                price <= 0.0
            ) {
                return
            }

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
                    ).takeIf {
                        it.isFinite()
                    }

                } else {
                    null
                }

            /*
             * Twelve Data pode entregar timestamp
             * em segundos ou milissegundos.
             */
            val timestampMs =
                when {

                    timestamp <= 0L ->
                        System.currentTimeMillis()

                    timestamp <
                        10_000_000_000L ->

                        timestamp *
                            1_000L

                    else ->
                        timestamp
                }

            val quote =
                RealTimeQuote(

                    symbol =
                        symbol,

                    timestamp =
                        timestampMs,

                    price =
                        price,

                    dayVolume =
                        dayVolume
                )

            quoteListener?.invoke(
                quote
            )

        } catch (
            error: Exception
        ) {

            errorListener?.invoke(
                error
            )
        }
    }

    private fun scheduleReconnect() {

        if (
            manuallyDisconnected
        ) {
            return
        }

        if (
            subscribedSymbols.isEmpty()
        ) {
            return
        }

        handler.removeCallbacks(
            reconnectRunnable
        )

        reconnectAttempts =
            (
                reconnectAttempts + 1
            ).coerceAtMost(
                6
            )

        val delay =
            when (
                reconnectAttempts
            ) {

                1 ->
                    2_000L

                2 ->
                    4_000L

                3 ->
                    8_000L

                4 ->
                    15_000L

                5 ->
                    30_000L

                else ->
                    60_000L
            }

        handler.postDelayed(
            reconnectRunnable,
            delay
        )
    }

    private fun disconnectSocketOnly() {

        connecting =
            false

        webSocket?.close(
            1000,
            "Reconectando"
        )

        webSocket =
            null
    }

    fun disconnect() {

        manuallyDisconnected =
            true

        connecting =
            false

        handler.removeCallbacks(
            reconnectRunnable
        )

        disconnectSocketOnly()
    }

    fun isConnected():
        Boolean {

        return webSocket != null &&
            !connecting
    }
}

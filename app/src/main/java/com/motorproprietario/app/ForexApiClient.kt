package com.motorproprietario.app

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class ForexTickData(
    val symbol: String,
    val timestamp: Long,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val dataQuality: String
) {

    val price: Double
        get() =
            (bid + ask) / 2.0
}

class ForexApiClient(
    private val baseUrl: String,
    private val apiToken: String = "",
    private val timeoutMs: Int = 5000
) {

    fun getTick(): ForexTickData {

        val normalizedBaseUrl =
            baseUrl.trimEnd('/')

        if (
            normalizedBaseUrl.isBlank()
        ) {
            throw IllegalArgumentException(
                "FOREX_API_BASE_URL não configurada."
            )
        }

        val url =
            URL(
                "$normalizedBaseUrl/tick"
            )

        val connection =
            url.openConnection()
                as HttpURLConnection

        try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                timeoutMs.coerceAtLeast(
                    1000
                )

            connection.readTimeout =
                timeoutMs.coerceAtLeast(
                    1000
                )

            connection.useCaches =
                false

            connection.instanceFollowRedirects =
                true

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            connection.setRequestProperty(
                "Cache-Control",
                "no-cache"
            )

            if (
                apiToken.isNotBlank()
            ) {

                connection.setRequestProperty(
                    "X-API-Token",
                    apiToken
                )
            }

            val responseCode =
                connection.responseCode

            if (
                responseCode !in 200..299
            ) {

                throw RuntimeException(
                    "FOREX_API_HTTP_$responseCode"
                )
            }

            val response =
                BufferedReader(
                    InputStreamReader(
                        connection.inputStream
                    )
                ).use {
                    it.readText()
                }

            if (
                response.isBlank()
            ) {

                throw RuntimeException(
                    "FOREX_API_EMPTY_RESPONSE"
                )
            }

            val json =
                try {

                    JSONObject(
                        response
                    )

                } catch (
                    error: Exception
                ) {

                    throw RuntimeException(
                        "FOREX_API_INVALID_JSON",
                        error
                    )
                }

            if (
                !json.optBoolean(
                    "ok",
                    false
                )
            ) {

                throw RuntimeException(
                    json.optString(
                        "error",
                        "FOREX_API_ERROR"
                    )
                )
            }

            val symbol =
                json.optString(
                    "symbol"
                ).trim()

            if (
                symbol.isBlank()
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_SYMBOL"
                )
            }

            val timestamp =
                json.optLong(
                    "timestamp",
                    0L
                )

            if (
                timestamp <= 0L
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_TIMESTAMP"
                )
            }

            val bid =
                json.optDouble(
                    "bid",
                    Double.NaN
                )

            val ask =
                json.optDouble(
                    "ask",
                    Double.NaN
                )

            val suppliedSpread =
                json.optDouble(
                    "spread",
                    Double.NaN
                )

            /*
             * ==================================
             * VALIDAÇÃO DO QUOTE
             * ==================================
             */

            if (
                !bid.isFinite() ||
                !ask.isFinite()
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_QUOTE"
                )
            }

            if (
                bid <= 0.0 ||
                ask <= 0.0
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_QUOTE"
                )
            }

            if (
                ask < bid
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_BID_ASK"
                )
            }

            /*
             * O spread recebido pela API é utilizado
             * quando válido. Caso contrário, calculamos
             * a partir de bid/ask.
             */

            val calculatedSpread =
                ask -
                    bid

            val spread =
                if (
                    suppliedSpread.isFinite() &&
                    suppliedSpread >= 0.0
                ) {

                    suppliedSpread

                } else {

                    calculatedSpread
                }

            if (
                !spread.isFinite() ||
                spread < 0.0
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_SPREAD"
                )
            }

            /*
             * ==================================
             * QUALIDADE DOS DADOS
             * ==================================
             *
             * Nunca promovemos automaticamente um
             * dado ruim para GOOD.
             */

            val rawQuality =
                json.optString(
                    "data_quality",
                    "BAD"
                )
                    .trim()
                    .uppercase()

            val dataQuality =
                when (
                    rawQuality
                ) {

                    "GOOD" ->
                        "GOOD"

                    "OK" ->
                        "GOOD"

                    "MEDIUM" ->
                        "MEDIUM"

                    "WARNING" ->
                        "MEDIUM"

                    else ->
                        "BAD"
                }

            /*
             * ==================================
             * TIMESTAMP
             * ==================================
             *
             * Aceita segundos ou milissegundos.
             */

            val timestampMs =
                when {

                    timestamp <
                        10_000_000_000L ->

                        timestamp *
                            1000L

                    else ->
                        timestamp
                }

            if (
                timestampMs <= 0L
            ) {

                throw RuntimeException(
                    "FOREX_INVALID_TIMESTAMP"
                )
            }

            /*
             * ==================================
             * VALIDAÇÃO DE STALENESS
             * ==================================
             *
             * Um tick muito antigo não deve ser
             * tratado como preço atual.
             */

            val now =
                System.currentTimeMillis()

            val age =
                kotlin.math.abs(
                    now -
                        timestampMs
                )

            val maximumAge =
                TimeUnit.MINUTES
                    .toMillis(5)

            val finalQuality =
                if (
                    age >
                        maximumAge
                ) {

                    "BAD"

                } else {

                    dataQuality
                }

            return ForexTickData(

                symbol =
                    symbol,

                timestamp =
                    timestampMs,

                bid =
                    bid,

                ask =
                    ask,

                spread =
                    spread,

                dataQuality =
                    finalQuality
            )

        } finally {

            connection.disconnect()
        }
    }
}

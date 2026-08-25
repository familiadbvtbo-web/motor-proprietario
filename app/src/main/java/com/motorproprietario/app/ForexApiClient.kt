package com.motorproprietario.app

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ForexTickData(
    val symbol: String,
    val timestamp: Long,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val dataQuality: String
) {
    val price: Double
        get() = (bid + ask) / 2.0
}

class ForexApiClient(
    private val baseUrl: String,
    private val apiToken: String = "",
    private val timeoutMs: Int = 5000
) {

    fun getTick(): ForexTickData {

        val url = URL(
            "$baseUrl/tick"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.connectTimeout =
                timeoutMs

            connection.readTimeout =
                timeoutMs

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (apiToken.isNotBlank()) {
                connection.setRequestProperty(
                    "X-API-Token",
                    apiToken
                )
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                throw RuntimeException(
                    "FOREX_API_HTTP_$responseCode"
                )
            }

            val reader =
                BufferedReader(
                    InputStreamReader(
                        connection.inputStream
                    )
                )

            val response =
                reader.use {
                    it.readText()
                }

            val json =
                JSONObject(response)

            if (!json.optBoolean("ok", false)) {
                throw RuntimeException(
                    json.optString(
                        "error",
                        "FOREX_API_ERROR"
                    )
                )
            }

            val symbol =
                json.getString("symbol")

            val timestamp =
                json.getLong("timestamp")

            val bid =
                json.getDouble("bid")

            val ask =
                json.getDouble("ask")

            val spread =
                json.getDouble("spread")

            val dataQuality =
                json.optString(
                    "data_quality",
                    "BAD"
                )

            if (
                bid <= 0.0 ||
                ask <= 0.0 ||
                ask < bid
            ) {
                throw RuntimeException(
                    "FOREX_INVALID_QUOTE"
                )
            }

            return ForexTickData(
                symbol = symbol,
                timestamp = timestamp,
                bid = bid,
                ask = ask,
                spread = spread,
                dataQuality = dataQuality
            )

        } finally {

            connection.disconnect()
        }
    }
}

package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class MarketCandle(
    val datetime: String,
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

class TwelveDataCandleClient {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    private val intervals =
        mapOf(
            "M1" to "1min",
            "M5" to "5min",
            "M15" to "15min",
            "M30" to "30min",
            "H1" to "1h",
            "H4" to "4h",
            "D1" to "1day"
        )

    fun getCandles(
        symbol: String,
        timeframe: String,
        outputSize: Int = 200
    ): List<MarketCandle> {

        val interval =
            intervals[timeframe.uppercase()]
                ?: throw IllegalArgumentException(
                    "TIMEFRAME_INVALID: $timeframe"
                )

        val encodedSymbol =
            URLEncoder.encode(
                symbol,
                "UTF-8"
            )

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "TWELVE_DATA_API_KEY não configurada"
            )
        }

        val url =
            "https://api.twelvedata.com/time_series" +
            "?symbol=$encodedSymbol" +
            "&interval=$interval" +
            "&outputsize=$outputSize" +
            "&order=ASC" +
            "&apikey=$apiKey"

        val request =
            Request.Builder()
                .url(url)
                .get()
                .addHeader(
                    "Accept",
                    "application/json"
                )
                .build()

        client.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {
                    throw RuntimeException(
                        "HTTP ${response.code}"
                    )
                }

                val body =
                    response.body?.string()
                        ?: throw RuntimeException(
                            "RESPOSTA_VAZIA"
                        )

                val json =
                    JSONObject(body)

                if (
                    json.optString(
                        "status"
                    ) == "error"
                ) {

                    throw RuntimeException(
                        json.optString(
                            "message",
                            "TWELVE_DATA_ERROR"
                        )
                    )
                }

                val values =
                    json.optJSONArray(
                        "values"
                    )
                        ?: throw RuntimeException(
                            "CANDLES_NAO_ENCONTRADOS"
                        )

                val candles =
                    ArrayList<MarketCandle>(
                        values.length()
                    )

                for (
                    index in 0 until values.length()
                ) {

                    val item =
                        values.getJSONObject(
                            index
                        )

                    val datetime =
                        item.optString(
                            "datetime"
                        )

                    val timestamp =
                        item.optLong(
                            "timestamp",
                            0L
                        )

                    val open =
                        item.getString(
                            "open"
                        ).toDouble()

                    val high =
                        item.getString(
                            "high"
                        ).toDouble()

                    val low =
                        item.getString(
                            "low"
                        ).toDouble()

                    val close =
                        item.getString(
                            "close"
                        ).toDouble()

                    val volume =
                        item.optString(
                            "volume",
                            "0"
                        ).toDoubleOrNull()
                            ?: 0.0

                    candles.add(
                        MarketCandle(
                            datetime =
                                datetime,
                            timestamp =
                                timestamp,
                            open =
                                open,
                            high =
                                high,
                            low =
                                low,
                            close =
                                close,
                            volume =
                                volume
                        )
                    )
                }

                return candles
            }
    }

    fun getAllTimeframes(
        symbol: String,
        outputSize: Int = 200
    ): Map<String, List<MarketCandle>> {

        val result =
            LinkedHashMap<
                String,
                List<MarketCandle>
            >()

        for (
            timeframe in intervals.keys
        ) {

            result[timeframe] =
                getCandles(
                    symbol =
                        symbol,
                    timeframe =
                        timeframe,
                    outputSize =
                        outputSize
                )
        }

        return result
    }
}

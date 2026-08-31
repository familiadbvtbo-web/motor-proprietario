package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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

    companion object {
        const val MAX_CANDLES = 5000
        const val DEFAULT_CANDLES = 5000
    }

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val resolver by lazy {
        SymbolResolver()
    }

    private val intervals =
        mapOf(
            "M1" to "1min",
            "M5" to "5min",
            "M15" to "15min",
            "M30" to "30min",
            "H1" to "1h",
            "H4" to "4h",
            "D1" to "1day",
            "W1" to "1week",
            "MN1" to "1month"
        )

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

        val resolved =
            try {
                resolver.resolve(requested).symbol
            } catch (_: Exception) {
                AssetRegistry.twelveDataSymbol(requested)
            }

        return getCandlesDirect(
            symbol = resolved,
            timeframe = timeframe,
            outputSize = outputSize
        )
    }

    private fun getCandlesDirect(
        symbol: String,
        timeframe: String,
        outputSize: Int
    ): List<MarketCandle> {

        val tf =
            timeframe.trim()
                .uppercase(Locale.US)

        if (tf == "Y1") {
            return getAnnualCandles(
                symbol,
                outputSize.coerceIn(
                    1,
                    MAX_CANDLES
                )
            )
        }

        val interval =
            intervals[tf]
                ?: throw IllegalArgumentException(
                    "TIMEFRAME_INVALID: $timeframe"
                )

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "TWELVE_DATA_API_KEY não configurada"
            )
        }

        val encodedSymbol =
            URLEncoder.encode(
                symbol,
                "UTF-8"
            )

        val url =
            "https://api.twelvedata.com/time_series" +
                "?symbol=$encodedSymbol" +
                "&interval=$interval" +
                "&outputsize=${
                    outputSize.coerceIn(
                        1,
                        MAX_CANDLES
                    )
                }" +
                "&order=ASC" +
                "&timezone=UTC" +
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

        client.newCall(
            request
        ).execute().use { response ->

            if (!response.isSuccessful) {
                throw RuntimeException(
                    "TWELVE_DATA_HTTP_${response.code}"
                )
            }

            val json =
                JSONObject(
                    response.body?.string()
                        ?: throw RuntimeException(
                            "TWELVE_DATA_EMPTY"
                        )
                )

            if (
                json.optString(
                    "status"
                ).equals(
                    "error",
                    ignoreCase = true
                )
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

            val result =
                ArrayList<MarketCandle>(
                    values.length()
                )

            for (
                i in 0 until values.length()
            ) {

                val item =
                    values.optJSONObject(i)
                        ?: continue

                val datetime =
                    item.optString(
                        "datetime"
                    )

                val timestamp =
                    parseTimestamp(
                        datetime
                    )

                val open =
                    item.optString(
                        "open"
                    ).toDoubleOrNull()

                val high =
                    item.optString(
                        "high"
                    ).toDoubleOrNull()

                val low =
                    item.optString(
                        "low"
                    ).toDoubleOrNull()

                val close =
                    item.optString(
                        "close"
                    ).toDoubleOrNull()

                val volume =
                    item.optString(
                        "volume"
                    ).toDoubleOrNull()
                        ?: 0.0

                if (
                    timestamp > 0L &&
                    open != null &&
                    high != null &&
                    low != null &&
                    close != null &&
                    open.isFinite() &&
                    high.isFinite() &&
                    low.isFinite() &&
                    close.isFinite() &&
                    high >= low &&
                    open in low..high &&
                    close in low..high
                ) {

                    result.add(
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
                                volume.coerceAtLeast(
                                    0.0
                                )
                        )
                    )
                }
            }

            if (result.isEmpty()) {
                throw RuntimeException(
                    "CANDLES_VALIDOS_NAO_ENCONTRADOS"
                )
            }

            return result
                .distinctBy {
                    it.timestamp
                }
                .sortedBy {
                    it.timestamp
                }
                .takeLast(
                    outputSize.coerceIn(
                        1,
                        MAX_CANDLES
                    )
                )
        }
    }

    private fun getAnnualCandles(
        symbol: String,
        outputSize: Int
    ): List<MarketCandle> {

        val daily =
            getCandlesDirect(
                symbol = symbol,
                timeframe = "D1",
                outputSize =
                    minOf(
                        MAX_CANDLES,
                        outputSize * 370
                    )
            )

        return daily
            .groupBy {
                utcYear(it.timestamp)
            }
            .toSortedMap()
            .values
            .mapNotNull { candles ->

                val sorted =
                    candles.sortedBy {
                        it.timestamp
                    }

                if (
                    sorted.isEmpty()
                ) {
                    return@mapNotNull null
                }

                MarketCandle(
                    datetime =
                        "${utcYear(
                            sorted.first().timestamp
                        )}-01-01",

                    timestamp =
                        sorted.first().timestamp,

                    open =
                        sorted.first().open,

                    high =
                        sorted.maxOf {
                            it.high
                        },

                    low =
                        sorted.minOf {
                            it.low
                        },

                    close =
                        sorted.last().close,

                    volume =
                        sorted.sumOf {
                            it.volume
                        }
                )
            }
            .takeLast(
                outputSize
            )
    }

    private fun utcYear(
        timestamp: Long
    ): Int {

        val calendar =
            java.util.Calendar.getInstance(
                TimeZone.getTimeZone(
                    "UTC"
                )
            )

        calendar.timeInMillis =
            timestamp

        return calendar.get(
            java.util.Calendar.YEAR
        )
    }

    private fun parseTimestamp(
        value: String
    ): Long {

        val formats =
            listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd"
            )

        for (
            pattern in formats
        ) {

            try {

                val parser =
                    SimpleDateFormat(
                        pattern,
                        Locale.US
                    )

                parser.isLenient =
                    false

                parser.timeZone =
                    TimeZone.getTimeZone(
                        "UTC"
                    )

                return parser
                    .parse(value)
                    ?.time
                    ?: continue

            } catch (_: Exception) {
                // tenta o próximo formato
            }
        }

        return 0L
    }
}

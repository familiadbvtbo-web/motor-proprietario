package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val intervals =
        linkedMapOf(
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

    companion object {
        /*
         * Limite operacional do Motor.
         *
         * O cliente aceita de 1 a 5000 candles por chamada.
         * O cache do ativo/timeframe é responsável por manter
         * os dados separados de cada combinação.
         */
        const val MAX_CANDLES = 5000
        const val DEFAULT_CANDLES = 5000
    }

    fun getCandles(
        symbol: String,
        timeframe: String,
        outputSize: Int = DEFAULT_CANDLES
    ): List<MarketCandle> {

        val normalized =
            timeframe.trim().uppercase(Locale.US)

        if (normalized == "Y1") {
            return getAnnualCandles(
                symbol = symbol,
                outputSize = outputSize
            )
        }

        val interval =
            intervals[normalized]
                ?: throw IllegalArgumentException(
                    "TIMEFRAME_INVALID: $timeframe"
                )

        val requestedSize =
            outputSize.coerceIn(
                1,
                MAX_CANDLES
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
                symbol.trim(),
                "UTF-8"
            )

        val url =
            "https://api.twelvedata.com/time_series" +
                "?symbol=$encodedSymbol" +
                "&interval=$interval" +
                "&outputsize=$requestedSize" +
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
                    json.optString("status")
                        .equals(
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
                    json.optJSONArray("values")
                        ?: throw RuntimeException(
                            "CANDLES_NAO_ENCONTRADOS"
                        )

                val result =
                    ArrayList<MarketCandle>(
                        values.length()
                    )

                for (
                    index in 0 until values.length()
                ) {

                    val item =
                        try {
                            values.getJSONObject(index)
                        } catch (_: Exception) {
                            continue
                        }

                    val datetime =
                        item.optString("datetime")

                    val timestamp =
                        parseTimestamp(
                            item,
                            datetime
                        )

                    val open =
                        parseDouble(
                            item,
                            "open"
                        )

                    val high =
                        parseDouble(
                            item,
                            "high"
                        )

                    val low =
                        parseDouble(
                            item,
                            "low"
                        )

                    val close =
                        parseDouble(
                            item,
                            "close"
                        )

                    val volume =
                        parseDouble(
                            item,
                            "volume"
                        ) ?: 0.0

                    if (
                        !isValidCandle(
                            timestamp,
                            open,
                            high,
                            low,
                            close,
                            volume
                        )
                    ) {
                        continue
                    }

                    result.add(
                        MarketCandle(
                            datetime = datetime,
                            timestamp = timestamp,
                            open = open!!,
                            high = high!!,
                            low = low!!,
                            close = close!!,
                            volume = volume
                        )
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
                        requestedSize
                    )
            }
    }

    private fun parseDouble(
        item: JSONObject,
        key: String
    ): Double? {

        val raw =
            item.optString(key)

        if (raw.isBlank()) {
            return null
        }

        return raw.toDoubleOrNull()
    }

    private fun parseTimestamp(
        item: JSONObject,
        datetime: String
    ): Long {

        val apiTimestamp =
            item.optLong(
                "timestamp",
                0L
            )

        if (apiTimestamp > 0L) {
            return normalizeTimestamp(
                apiTimestamp
            )
        }

        return parseDatetimeToTimestamp(
            datetime
        )
    }

    private fun normalizeTimestamp(
        timestamp: Long
    ): Long {

        if (timestamp <= 0L) {
            return 0L
        }

        return if (
            timestamp < 10_000_000_000L
        ) {
            timestamp * 1000L
        } else {
            timestamp
        }
    }

    private fun isValidCandle(
        timestamp: Long,
        open: Double?,
        high: Double?,
        low: Double?,
        close: Double?,
        volume: Double
    ): Boolean {

        if (timestamp <= 0L) {
            return false
        }

        if (
            open == null ||
            high == null ||
            low == null ||
            close == null
        ) {
            return false
        }

        if (
            !open.isFinite() ||
            !high.isFinite() ||
            !low.isFinite() ||
            !close.isFinite() ||
            !volume.isFinite()
        ) {
            return false
        }

        if (
            open <= 0.0 ||
            high <= 0.0 ||
            low <= 0.0 ||
            close <= 0.0
        ) {
            return false
        }

        if (
            high < low ||
            high < maxOf(open, close) ||
            low > minOf(open, close)
        ) {
            return false
        }

        return volume >= 0.0
    }

    private fun parseDatetimeToTimestamp(
        datetime: String
    ): Long {

        if (datetime.isBlank()) {
            return 0L
        }

        val formats =
            listOf(
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd"
            )

        for (pattern in formats) {

            try {

                val formatter =
                    SimpleDateFormat(
                        pattern,
                        Locale.US
                    )

                formatter.isLenient = false
                formatter.timeZone =
                    TimeZone.getTimeZone("UTC")

                val date =
                    formatter.parse(datetime)

                if (date != null) {
                    return date.time
                }

            } catch (_: Exception) {
                // tenta o formato seguinte
            }
        }

        return 0L
    }

    private fun getAnnualCandles(
        symbol: String,
        outputSize: Int
    ): List<MarketCandle> {

        val monthly =
            getCandles(
                symbol = symbol,
                timeframe = "MN1",
                outputSize = MAX_CANDLES
            )

        if (monthly.isEmpty()) {
            return emptyList()
        }

        val calendar =
            Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            )

        val grouped =
            LinkedHashMap<
                Int,
                MutableList<MarketCandle>
            >()

        for (candle in monthly) {

            calendar.timeInMillis =
                candle.timestamp

            val year =
                calendar.get(Calendar.YEAR)

            grouped
                .getOrPut(year) {
                    mutableListOf()
                }
                .add(candle)
        }

        val annual =
            ArrayList<MarketCandle>()

        for (
            (year, sourceCandles) in grouped
        ) {

            val candles =
                sourceCandles.sortedBy {
                    it.timestamp
                }

            if (candles.isEmpty()) {
                continue
            }

            val first =
                candles.first()

            val last =
                candles.last()

            annual.add(
                MarketCandle(
                    datetime =
                        "$year-01-01",
                    timestamp =
                        first.timestamp,
                    open =
                        first.open,
                    high =
                        candles.maxOf {
                            it.high
                        },
                    low =
                        candles.minOf {
                            it.low
                        },
                    close =
                        last.close,
                    volume =
                        candles.sumOf {
                            it.volume
                        }
                )
            )
        }

        return annual
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

    fun getAllTimeframes(
        symbol: String,
        outputSize: Int = DEFAULT_CANDLES
    ): Map<
        String,
        List<MarketCandle>
    > {

        val result =
            LinkedHashMap<
                String,
                List<MarketCandle>
            >()

        val requestedSize =
            outputSize.coerceIn(
                1,
                MAX_CANDLES
            )

        val timeframes =
            listOf(
                "M1",
                "M5",
                "M15",
                "M30",
                "H1",
                "H4",
                "D1",
                "W1",
                "MN1",
                "Y1"
            )

        for (timeframe in timeframes) {

            try {

                result[timeframe] =
                    getCandles(
                        symbol = symbol,
                        timeframe = timeframe,
                        outputSize = requestedSize
                    )

            } catch (_: Exception) {

                /*
                 * Um timeframe indisponível não invalida
                 * os demais.
                 */
                result[timeframe] =
                    emptyList()
            }
        }

        return result
    }
}

package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
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
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    /*
     * Timeframes fornecidos diretamente pela Twelve Data.
     */
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

    /*
     * Limite máximo histórico do Motor.
     *
     * Não significa que sempre existirão 1.000 candles.
     * Se a fonte possuir menos histórico, usamos somente
     * o histórico realmente disponível.
     */
    companion object {

        const val MAX_CANDLES =
            1000

        const val DEFAULT_CANDLES =
            1000
    }

    fun getCandles(
        symbol: String,
        timeframe: String,
        outputSize: Int = DEFAULT_CANDLES
    ): List<MarketCandle> {

        val normalizedTimeframe =
            timeframe.uppercase(Locale.US)

        /*
         * Y1 não é solicitado diretamente.
         *
         * Ele será construído a partir dos candles mensais.
         */
        if (
            normalizedTimeframe ==
            "Y1"
        ) {

            return getAnnualCandles(
                symbol =
                    symbol,
                outputSize =
                    outputSize
            )
        }

        val interval =
            intervals[
                normalizedTimeframe
            ]
                ?: throw IllegalArgumentException(
                    "TIMEFRAME_INVALID: $timeframe"
                )

        val requestedSize =
            outputSize.coerceIn(
                1,
                MAX_CANDLES
            )

        val encodedSymbol =
            URLEncoder.encode(
                symbol,
                "UTF-8"
            )

        val apiKey =
            ApiConfig.TWELVE_DATA_API_KEY

        if (
            apiKey.isBlank()
        ) {

            throw IllegalStateException(
                "TWELVE_DATA_API_KEY não configurada"
            )
        }

        val url =
            "https://api.twelvedata.com/time_series" +
            "?symbol=$encodedSymbol" +
            "&interval=$interval" +
            "&outputsize=$requestedSize" +
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

                if (
                    !response.isSuccessful
                ) {

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
                    JSONObject(
                        body
                    )

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
                    index in
                    0 until values.length()
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

                    if (
                        timestamp > 0L &&
                        open.isFinite() &&
                        high.isFinite() &&
                        low.isFinite() &&
                        close.isFinite()
                    ) {

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
                }

                return candles
                    .sortedBy {
                        it.timestamp
                    }
                    .takeLast(
                        MAX_CANDLES
                    )
            }
    }

    /*
     * ============================================================
     * ANUAL
     * ============================================================
     *
     * Y1 é construído a partir dos candles mensais.
     *
     * Não criamos preço artificial.
     *
     * Cada candle anual contém:
     *
     * OPEN  = primeiro open do ano
     * HIGH  = maior high do ano
     * LOW   = menor low do ano
     * CLOSE = último close do ano
     * VOLUME = soma dos volumes disponíveis
     */
    private fun getAnnualCandles(
        symbol: String,
        outputSize: Int
    ): List<MarketCandle> {

        val monthly =
            getCandles(
                symbol =
                    symbol,

                timeframe =
                    "MN1",

                outputSize =
                    MAX_CANDLES
            )

        if (
            monthly.isEmpty()
        ) {

            return emptyList()
        }

        val calendar =
            Calendar.getInstance(
                TimeZone.getTimeZone(
                    "UTC"
                )
            )

        val grouped =
            LinkedHashMap<
                Int,
                MutableList<MarketCandle>
            >()

        for (
            candle in monthly
        ) {

            calendar.timeInMillis =
                candle.timestamp

            val year =
                calendar.get(
                    Calendar.YEAR
                )

            grouped
                .getOrPut(
                    year
                ) {
                    mutableListOf()
                }
                .add(
                    candle
                )
        }

        val annual =
            ArrayList<MarketCandle>()

        for (
            entry in grouped
        ) {

            val candles =
                entry.value
                    .sortedBy {
                        it.timestamp
                    }

            if (
                candles.isEmpty()
            ) {
                continue
            }

            val first =
                candles.first()

            val last =
                candles.last()

            annual.add(
                MarketCandle(
                    datetime =
                        "${entry.key}-01-01",

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

    /*
     * ============================================================
     * TODOS OS TIMEFRAMES
     * ============================================================
     */
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

        for (
            timeframe in timeframes
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

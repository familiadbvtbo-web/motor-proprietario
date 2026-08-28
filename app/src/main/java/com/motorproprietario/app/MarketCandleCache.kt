package com.motorproprietario.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MarketCandleCache(
    context: Context
) {

    companion object {
        private const val PREFS =
            "motor_market_candle_cache"

        private const val MAX_CANDLES =
            1000
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun key(
        symbol: String,
        timeframe: String
    ): String {

        val safeSymbol =
            symbol
                .uppercase()
                .replace(
                    "/",
                    "_"
                )
                .replace(
                    " ",
                    "_"
                )

        val safeTimeframe =
            timeframe
                .uppercase()

        return "${safeSymbol}_${safeTimeframe}"
    }

    fun get(
        symbol: String,
        timeframe: String
    ): List<MarketCandle> {

        val json =
            preferences.getString(
                key(
                    symbol,
                    timeframe
                ),
                null
            )
                ?: return emptyList()

        return try {

            val array =
                JSONArray(json)

            val result =
                ArrayList<MarketCandle>(
                    array.length()
                )

            for (
                index in
                0 until array.length()
            ) {

                val item =
                    array.getJSONObject(
                        index
                    )

                result.add(
                    MarketCandle(
                        datetime =
                            item.optString(
                                "datetime"
                            ),

                        timestamp =
                            item.optLong(
                                "timestamp"
                            ),

                        open =
                            item.optDouble(
                                "open"
                            ),

                        high =
                            item.optDouble(
                                "high"
                            ),

                        low =
                            item.optDouble(
                                "low"
                            ),

                        close =
                            item.optDouble(
                                "close"
                            ),

                        volume =
                            item.optDouble(
                                "volume"
                            )
                    )
                )
            }

            result
                .filter {
                    it.timestamp > 0L
                }
                .sortedBy {
                    it.timestamp
                }
                .takeLast(
                    MAX_CANDLES
                )

        } catch (
            error: Exception
        ) {

            emptyList()
        }
    }

    fun save(
        symbol: String,
        timeframe: String,
        candles: List<MarketCandle>
    ) {

        val normalized =
            candles
                .filter {
                    it.timestamp > 0L &&
                    it.open.isFinite() &&
                    it.high.isFinite() &&
                    it.low.isFinite() &&
                    it.close.isFinite()
                }
                .distinctBy {
                    it.timestamp
                }
                .sortedBy {
                    it.timestamp
                }
                .takeLast(
                    MAX_CANDLES
                )

        val array =
            JSONArray()

        for (
            candle in normalized
        ) {

            val item =
                JSONObject()

            item.put(
                "datetime",
                candle.datetime
            )

            item.put(
                "timestamp",
                candle.timestamp
            )

            item.put(
                "open",
                candle.open
            )

            item.put(
                "high",
                candle.high
            )

            item.put(
                "low",
                candle.low
            )

            item.put(
                "close",
                candle.close
            )

            item.put(
                "volume",
                candle.volume
            )

            array.put(
                item
            )
        }

        preferences.edit()
            .putString(
                key(
                    symbol,
                    timeframe
                ),
                array.toString()
            )
            .apply()
    }

    fun merge(
        symbol: String,
        timeframe: String,
        freshCandles: List<MarketCandle>
    ): List<MarketCandle> {

        val existing =
            get(
                symbol,
                timeframe
            )

        val merged =
            ArrayList<MarketCandle>(
                existing.size +
                    freshCandles.size
            )

        merged.addAll(
            existing
        )

        merged.addAll(
            freshCandles
        )

        val result =
            merged
                .filter {
                    it.timestamp > 0L
                }
                .distinctBy {
                    it.timestamp
                }
                .sortedBy {
                    it.timestamp
                }
                .takeLast(
                    MAX_CANDLES
                )

        save(
            symbol,
            timeframe,
            result
        )

        return result
    }

    fun hasData(
        symbol: String,
        timeframe: String
    ): Boolean {

        return get(
            symbol,
            timeframe
        ).isNotEmpty()
    }

    fun size(
        symbol: String,
        timeframe: String
    ): Int {

        return get(
            symbol,
            timeframe
        ).size
    }

    fun clearAsset(
        symbol: String
    ) {

        val prefix =
            symbol
                .uppercase()
                .replace(
                    "/",
                    "_"
                )
                .replace(
                    " ",
                    "_"
                ) + "_"

        val editor =
            preferences.edit()

        preferences.all.keys
            .filter {
                it.startsWith(
                    prefix
                )
            }
            .forEach {
                editor.remove(it)
            }

        editor.apply()
    }

    fun clearAll() {

        preferences.edit()
            .clear()
            .apply()
    }
}

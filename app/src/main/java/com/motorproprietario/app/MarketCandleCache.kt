package com.motorproprietario.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Cache persistente separado por ATIVO + TIMEFRAME.
 *
 * Estrutura:
 * cache/
 *   market_candles/
 *     EUR_USD/
 *       M1.json
 *       M5.json
 *       ...
 *     BTC_USD/
 *       M1.json
 *       ...
 *
 * Cada combinação possui no máximo 5.000 candles.
 *
 * O cache:
 * - não mistura ativos;
 * - não mistura timeframes;
 * - elimina candles duplicados pelo timestamp;
 * - mantém ordem cronológica;
 * - faz merge incremental;
 * - preserva histórico entre reinícios do aplicativo.
 */
class MarketCandleCache(
    context: Context
) {

    companion object {
        const val MAX_CANDLES = 5000

        private const val DIRECTORY =
            "market_candles"

        private const val VERSION =
            2
    }

    private val root =
        File(
            context.applicationContext.cacheDir,
            DIRECTORY
        )

    init {
        if (!root.exists()) {
            root.mkdirs()
        }
    }

    @Synchronized
    fun get(
        symbol: String,
        timeframe: String
    ): List<MarketCandle> {

        val file =
            fileFor(
                symbol,
                timeframe
            )

        if (!file.exists()) {
            return emptyList()
        }

        return try {

            readFile(file)

        } catch (_: Exception) {

            /*
             * Um cache corrompido não pode derrubar
             * a análise. Remove somente aquele
             * ativo/timeframe.
             */
            runCatching {
                file.delete()
            }

            emptyList()
        }
    }

    /**
     * Substitui o histórico daquele ativo/timeframe.
     */
    @Synchronized
    fun put(
        symbol: String,
        timeframe: String,
        candles: List<MarketCandle>
    ): List<MarketCandle> {

        val normalized =
            normalize(
                candles
            )

        writeFile(
            fileFor(
                symbol,
                timeframe
            ),
            normalized
        )

        return normalized
    }

    /**
     * Faz merge incremental.
     *
     * Candles com o mesmo timestamp são substituídos
     * pelo candle mais recente recebido.
     *
     * O resultado é limitado a 5.000 candles.
     */
    @Synchronized
    fun merge(
        symbol: String,
        timeframe: String,
        freshCandles: List<MarketCandle>
    ): List<MarketCandle> {

        if (freshCandles.isEmpty()) {
            return get(
                symbol,
                timeframe
            )
        }

        val current =
            get(
                symbol,
                timeframe
            )

        val byTimestamp =
            LinkedHashMap<Long, MarketCandle>()

        for (candle in current) {

            if (isValid(candle)) {
                byTimestamp[
                    candle.timestamp
                ] = candle
            }
        }

        for (candle in freshCandles) {

            if (isValid(candle)) {
                byTimestamp[
                    candle.timestamp
                ] = candle
            }
        }

        val merged =
            byTimestamp.values
                .sortedBy {
                    it.timestamp
                }
                .takeLast(
                    MAX_CANDLES
                )

        writeFile(
            fileFor(
                symbol,
                timeframe
            ),
            merged
        )

        return merged
    }

    /**
     * Retorna a quantidade armazenada para
     * uma combinação ativo/timeframe.
     */
    @Synchronized
    fun size(
        symbol: String,
        timeframe: String
    ): Int {

        return get(
            symbol,
            timeframe
        ).size
    }

    /**
     * Informa se já existe histórico suficiente.
     */
    @Synchronized
    fun hasData(
        symbol: String,
        timeframe: String,
        minimumCandles: Int = 1
    ): Boolean {

        return size(
            symbol,
            timeframe
        ) >=
            minimumCandles.coerceAtLeast(1)
    }

    /**
     * Apaga somente um ativo/timeframe.
     */
    @Synchronized
    fun clear(
        symbol: String,
        timeframe: String
    ) {

        runCatching {
            fileFor(
                symbol,
                timeframe
            ).delete()
        }
    }

    /**
     * Apaga todo o histórico armazenado.
     */
    @Synchronized
    fun clearAll() {

        if (!root.exists()) {
            return
        }

        root.listFiles()
            ?.forEach { directory ->

                if (directory.isDirectory) {

                    directory
                        .listFiles()
                        ?.forEach {
                            it.delete()
                        }

                    directory.delete()

                } else {

                    directory.delete()
                }
            }
    }

    private fun normalize(
        candles: List<MarketCandle>
    ): List<MarketCandle> {

        val byTimestamp =
            LinkedHashMap<Long, MarketCandle>()

        for (candle in candles) {

            if (isValid(candle)) {

                byTimestamp[
                    candle.timestamp
                ] = candle
            }
        }

        return byTimestamp.values
            .sortedBy {
                it.timestamp
            }
            .takeLast(
                MAX_CANDLES
            )
    }

    private fun isValid(
        candle: MarketCandle
    ): Boolean {

        if (
            candle.timestamp <= 0L
        ) {
            return false
        }

        if (
            !candle.open.isFinite() ||
            !candle.high.isFinite() ||
            !candle.low.isFinite() ||
            !candle.close.isFinite() ||
            !candle.volume.isFinite()
        ) {
            return false
        }

        if (
            candle.open <= 0.0 ||
            candle.high <= 0.0 ||
            candle.low <= 0.0 ||
            candle.close <= 0.0
        ) {
            return false
        }

        if (
            candle.high < candle.low
        ) {
            return false
        }

        if (
            candle.high <
            maxOf(
                candle.open,
                candle.close
            )
        ) {
            return false
        }

        if (
            candle.low >
            minOf(
                candle.open,
                candle.close
            )
        ) {
            return false
        }

        return candle.volume >= 0.0
    }

    private fun fileFor(
        symbol: String,
        timeframe: String
    ): File {

        val safeSymbol =
            sanitizeSymbol(
                symbol
            )

        val safeTimeframe =
            timeframe
                .trim()
                .uppercase()
                .replace(
                    Regex("[^A-Z0-9]"),
                    "_"
                )

        val directory =
            File(
                root,
                safeSymbol
            )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return File(
            directory,
            "$safeTimeframe.json"
        )
    }

    private fun sanitizeSymbol(
        symbol: String
    ): String {

        val cleaned =
            symbol
                .trim()
                .uppercase()
                .replace(
                    Regex("[^A-Z0-9]+"),
                    "_"
                )
                .trim('_')

        return if (
            cleaned.isBlank()
        ) {
            "UNKNOWN_ASSET"
        } else {
            cleaned
        }
    }

    private fun readFile(
        file: File
    ): List<MarketCandle> {

        val text =
            file.readText(
                Charsets.UTF_8
            )

        if (text.isBlank()) {
            return emptyList()
        }

        val rootObject =
            JSONObject(text)

        val version =
            rootObject.optInt(
                "version",
                1
            )

        /*
         * Versões antigas continuam legíveis.
         */
        if (
            version <= 0
        ) {
            return emptyList()
        }

        val array =
            rootObject.optJSONArray(
                "candles"
            )
                ?: JSONArray()

        val result =
            ArrayList<MarketCandle>(
                array.length()
            )

        for (
            index in 0 until array.length()
        ) {

            val item =
                runCatching {
                    array.getJSONObject(index)
                }.getOrNull()
                    ?: continue

            val timestamp =
                normalizeTimestamp(
                    item.optLong(
                        "timestamp",
                        0L
                    )
                )

            val datetime =
                item.optString(
                    "datetime"
                )

            val open =
                item.optDouble(
                    "open",
                    Double.NaN
                )

            val high =
                item.optDouble(
                    "high",
                    Double.NaN
                )

            val low =
                item.optDouble(
                    "low",
                    Double.NaN
                )

            val close =
                item.optDouble(
                    "close",
                    Double.NaN
                )

            val volume =
                item.optDouble(
                    "volume",
                    0.0
                )

            val candle =
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

            if (
                isValid(candle)
            ) {
                result.add(
                    candle
                )
            }
        }

        return normalize(
            result
        )
    }

    private fun writeFile(
        file: File,
        candles: List<MarketCandle>
    ) {

        val normalized =
            normalize(
                candles
            )

        val json =
            JSONObject()

        json.put(
            "version",
            VERSION
        )

        json.put(
            "maxCandles",
            MAX_CANDLES
        )

        val array =
            JSONArray()

        for (candle in normalized) {

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

        json.put(
            "candles",
            array
        )

        val temporary =
            File(
                file.parentFile,
                "${file.name}.tmp"
            )

        temporary.writeText(
            json.toString(),
            Charsets.UTF_8
        )

        /*
         * Escrita atômica simples:
         * só substitui o arquivo principal depois
         * de terminar a gravação.
         */
        if (
            file.exists() &&
            !file.delete()
        ) {
            temporary.delete()
            throw IllegalStateException(
                "CACHE_DELETE_FAILED"
            )
        }

        if (
            !temporary.renameTo(file)
        ) {
            temporary.delete()
            throw IllegalStateException(
                "CACHE_RENAME_FAILED"
            )
        }
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
}

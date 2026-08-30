package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ResolvedSymbol(
    val requested: String,
    val symbol: String,
    val name: String,
    val exchange: String?,
    val country: String?,
    val type: String?,
    val score: Int
)

class SymbolResolver {

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    private val cache =
        HashMap<String, ResolvedSymbol>()

    @Synchronized
    fun resolve(
        requestedSymbol: String
    ): ResolvedSymbol {

        val requested =
            requestedSymbol.trim()

        require(requested.isNotBlank()) {
            "SYMBOL_EMPTY"
        }

        cache[requested.uppercase()]?.let {
            return it
        }

        val key =
            ApiConfig.TWELVE_DATA_API_KEY

        require(key.isNotBlank()) {
            "TWELVE_DATA_API_KEY não configurada"
        }

        val query =
            URLEncoder.encode(
                requested,
                "UTF-8"
            )

        val url =
            "https://api.twelvedata.com/symbol_search" +
                "?symbol=$query" +
                "&outputsize=20" +
                "&apikey=$key"

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
                        "SYMBOL_SEARCH_HTTP_${response.code}"
                    )
                }

                val body =
                    response.body?.string()
                        ?: throw RuntimeException(
                            "SYMBOL_SEARCH_EMPTY"
                        )

                if (
                    body.contains(
                        "\"status\":\"error\"",
                        ignoreCase = true
                    )
                ) {
                    throw RuntimeException(
                        "SYMBOL_SEARCH_ERROR"
                    )
                }

                val array =
                    JSONArray(body)

                if (array.length() == 0) {
                    throw RuntimeException(
                        "SYMBOL_NOT_FOUND: $requested"
                    )
                }

                val best =
                    chooseBest(
                        requested,
                        array
                    )
                        ?: throw RuntimeException(
                            "SYMBOL_NOT_FOUND: $requested"
                        )

                cache[
                    requested.uppercase()
                ] = best

                return best
            }
    }

    private fun chooseBest(
        requested: String,
        results: JSONArray
    ): ResolvedSymbol? {

        val normalized =
            normalize(requested)

        var best:
            ResolvedSymbol? = null

        for (
            index in 0 until results.length()
        ) {

            val item =
                results.optJSONObject(
                    index
                )
                    ?: continue

            val symbol =
                item.optString(
                    "symbol"
                ).trim()

            if (symbol.isBlank()) {
                continue
            }

            val name =
                item.optString(
                    "instrument_name"
                ).ifBlank {
                    item.optString(
                        "name"
                    )
                }

            val exchange =
                item.optString(
                    "exchange"
                ).ifBlank {
                    null
                }

            val country =
                item.optString(
                    "country"
                ).ifBlank {
                    null
                }

            val type =
                item.optString(
                    "type"
                ).ifBlank {
                    null
                }

            val score =
                score(
                    normalized,
                    symbol,
                    name
                )

            val candidate =
                ResolvedSymbol(
                    requested =
                        requested,

                    symbol =
                        symbol,

                    name =
                        name.ifBlank {
                            symbol
                        },

                    exchange =
                        exchange,

                    country =
                        country,

                    type =
                        type,

                    score =
                        score
                )

            if (
                best == null ||
                candidate.score >
                    best.score
            ) {
                best = candidate
            }
        }

        return best
    }

    private fun score(
        requested: String,
        symbol: String,
        name: String
    ): Int {

        val symbolNormalized =
            normalize(symbol)

        val nameNormalized =
            normalize(name)

        if (
            symbolNormalized ==
            requested
        ) {
            return 100
        }

        if (
            symbolNormalized.startsWith(
                requested
            )
        ) {
            return 85
        }

        if (
            symbolNormalized.contains(
                requested
            )
        ) {
            return 70
        }

        if (
            nameNormalized.contains(
                requested
            )
        ) {
            return 60
        }

        return 10
    }

    private fun normalize(
        value: String
    ): String {

        return value
            .trim()
            .uppercase()
            .replace(
                Regex("[^A-Z0-9]"),
                ""
            )
    }

    @Synchronized
    fun clearCache() {
        cache.clear()
    }
}

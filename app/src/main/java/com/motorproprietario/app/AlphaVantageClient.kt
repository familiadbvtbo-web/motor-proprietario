package com.motorproprietario.app

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

class AlphaVantageClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun request(function: String, params: Map<String,String>): JSONObject {
        require(apiKey.isNotBlank()) { "ALPHA_VANTAGE_API_KEY não configurada" }
        val query = buildString {
            append("function=").append(URLEncoder.encode(function,"UTF-8"))
            for ((k,v) in params) {
                append('&').append(URLEncoder.encode(k,"UTF-8"))
                    .append('=').append(URLEncoder.encode(v,"UTF-8"))
            }
            append("&apikey=").append(URLEncoder.encode(apiKey,"UTF-8"))
        }
        client.newCall(Request.Builder()
            .url("https://www.alphavantage.co/query?$query")
            .get().addHeader("Accept","application/json").build())
            .execute().use { response ->
                if (!response.isSuccessful) throw RuntimeException("ALPHA_HTTP_${response.code}")
                val json = JSONObject(response.body?.string() ?: throw RuntimeException("ALPHA_EMPTY_RESPONSE"))
                json.optString("Error Message").takeIf { it.isNotBlank() }?.let { throw RuntimeException(it) }
                json.optString("Note").takeIf { it.isNotBlank() }?.let { throw RuntimeException("ALPHA_RATE_LIMIT") }
                return json
            }
    }

    fun getDailyCandles(symbol: String, outputSize: Int = 100): List<MarketCandle> {
        val json = request("TIME_SERIES_DAILY", mapOf(
            "symbol" to symbol,
            "outputsize" to if (outputSize > 100) "full" else "compact"
        ))
        val series = json.optJSONObject("Time Series (Daily)")
            ?: throw RuntimeException("ALPHA_DAILY_NOT_AVAILABLE")
        val result = ArrayList<MarketCandle>()
        val keys = series.keys()
        while (keys.hasNext()) {
            val date = keys.next()
            val x = series.optJSONObject(date) ?: continue
            val o=x.optString("1. open").toDoubleOrNull()
            val h=x.optString("2. high").toDoubleOrNull()
            val l=x.optString("3. low").toDoubleOrNull()
            val c=x.optString("4. close").toDoubleOrNull()
            val v=x.optString("5. volume").toDoubleOrNull() ?: 0.0
            val ts=parseDate(date)
            if (o!=null && h!=null && l!=null && c!=null && ts>0)
                result.add(MarketCandle(date,ts,o,h,l,c,v))
        }
        return result.sortedBy { it.timestamp }.takeLast(outputSize.coerceAtLeast(1))
    }

    private fun parseDate(value: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient=false
            timeZone=java.util.TimeZone.getTimeZone("UTC")
        }.parse(value)?.time ?: 0L
    } catch (_: Exception) { 0L }
}

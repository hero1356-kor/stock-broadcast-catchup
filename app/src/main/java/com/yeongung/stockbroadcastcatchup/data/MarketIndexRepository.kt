package com.yeongung.stockbroadcastcatchup.data

import com.yeongung.stockbroadcastcatchup.model.IndexQuote
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MarketIndexRepository {
    private val quoteRequests = listOf(
        MarketIndexRequest("^KS11", "코스피"),
        MarketIndexRequest("^KQ11", "코스닥"),
        MarketIndexRequest("NQ=F", "나스닥 선물"),
        MarketIndexRequest("KRW=X", "USD/KRW"),
    )

    suspend fun currentIndices(): MarketIndexResult = withContext(Dispatchers.IO) {
        val quotes = quoteRequests.map { request -> fetchQuote(request) }
        val updatedAt = quotes.mapNotNull { it.updatedAtEpochSeconds }.maxOrNull()
        MarketIndexResult(
            quotes = quotes.map { it.quote },
            updatedAtLabel = updatedAt?.let { formatUpdatedAt(it) } ?: "방금",
        )
    }

    private fun fetchQuote(request: MarketIndexRequest): FetchedMarketIndexQuote {
        val encodedSymbol = URLEncoder.encode(request.symbol, "UTF-8")
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedSymbol?range=1d&interval=1m"
        val response = openUrl(url)
        val meta = JSONObject(response)
            .getJSONObject("chart")
            .getJSONArray("result")
            .getJSONObject(0)
            .getJSONObject("meta")

        val price = meta.optDouble("regularMarketPrice", Double.NaN)
        val previousClose = meta.optDouble(
            "chartPreviousClose",
            meta.optDouble("previousClose", Double.NaN),
        )
        val updatedAt = meta.optLong("regularMarketTime", 0L).takeIf { it > 0L }

        if (price.isNaN() || previousClose.isNaN() || previousClose == 0.0) {
            throw IOException("No usable quote for ${request.symbol}")
        }

        val changePercent = ((price - previousClose) / previousClose) * 100.0
        return FetchedMarketIndexQuote(
            quote = IndexQuote(
                name = request.displayName,
                value = valueFormat.format(price),
                change = "${percentFormat.format(abs(changePercent))}%",
                isUp = changePercent >= 0.0,
            ),
            updatedAtEpochSeconds = updatedAt,
        )
    }

    private fun openUrl(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("User-Agent", "stock-broadcast-catchup/0.1")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun formatUpdatedAt(epochSeconds: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
        return formatter.format(Date(epochSeconds * 1000L))
    }

    private companion object {
        val valueFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val percentFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
}

data class MarketIndexResult(
    val quotes: List<IndexQuote>,
    val updatedAtLabel: String,
)

private data class MarketIndexRequest(
    val symbol: String,
    val displayName: String,
)

private data class FetchedMarketIndexQuote(
    val quote: IndexQuote,
    val updatedAtEpochSeconds: Long?,
)

package com.yeongung.stockbroadcastcatchup.input

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

interface SttClient {
    val isConfigured: Boolean

    suspend fun transcribe(wavAudio: ByteArray): String
}

class ClovaCsrSttClient(
    context: Context,
    private val configStore: ClovaCsrConfigStore = ClovaCsrConfigStore(context),
) : SttClient {
    override val isConfigured: Boolean
        get() = configStore.current().isConfigured

    override suspend fun transcribe(wavAudio: ByteArray): String = withContext(Dispatchers.IO) {
        val config = configStore.current()
        check(config.isConfigured) { "CLOVA CSR 인증 정보가 설정되지 않았습니다." }

        val connection = (URL(endpointWithLanguage(config.endpoint)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Content-Type", "application/octet-stream")
            setRequestProperty("X-NCP-APIGW-API-KEY-ID", config.clientId)
            setRequestProperty("X-NCP-APIGW-API-KEY", config.clientSecret)
        }

        try {
            connection.outputStream.use { output ->
                output.write(wavAudio)
            }

            val responseCode = connection.responseCode
            val responseText = connection.readResponse(responseCode)
            if (responseCode !in HTTP_SUCCESS_RANGE) {
                throw IOException("CLOVA CSR 전사 실패 HTTP $responseCode: ${responseText.take(MAX_ERROR_BODY_LENGTH)}")
            }

            JSONObject(responseText).optString("text").trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun endpointWithLanguage(endpoint: String): String {
        val normalizedEndpoint = endpoint.ifBlank { DEFAULT_ENDPOINT }
        if (normalizedEndpoint.contains("lang=")) return normalizedEndpoint

        val separator = if (normalizedEndpoint.contains("?")) "&" else "?"
        return "$normalizedEndpoint${separator}lang=Kor"
    }

    private fun HttpURLConnection.readResponse(responseCode: Int): String {
        val stream = if (responseCode in HTTP_SUCCESS_RANGE) inputStream else errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private companion object {
        const val DEFAULT_ENDPOINT = "https://naveropenapi.apigw.ntruss.com/recog/v1/stt"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 45_000
        const val MAX_ERROR_BODY_LENGTH = 240
        val HTTP_SUCCESS_RANGE = 200..299
    }
}

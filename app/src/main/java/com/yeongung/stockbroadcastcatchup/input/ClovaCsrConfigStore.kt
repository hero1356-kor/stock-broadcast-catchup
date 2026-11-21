package com.yeongung.stockbroadcastcatchup.input

import android.content.Context
import com.yeongung.stockbroadcastcatchup.BuildConfig

data class ClovaCsrConfig(
    val clientId: String,
    val clientSecret: String,
    val endpoint: String,
) {
    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}

class ClovaCsrConfigStore(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun current(): ClovaCsrConfig = ClovaCsrConfig(
        clientId = BuildConfig.CLOVA_CSR_CLIENT_ID.trim(),
        clientSecret = BuildConfig.CLOVA_CSR_CLIENT_SECRET.trim(),
        endpoint = BuildConfig.CLOVA_CSR_ENDPOINT.trim().ifBlank { DEFAULT_ENDPOINT },
    )

    private companion object {
        const val DEFAULT_ENDPOINT = "https://naveropenapi.apigw.ntruss.com/recog/v1/stt"
    }
}

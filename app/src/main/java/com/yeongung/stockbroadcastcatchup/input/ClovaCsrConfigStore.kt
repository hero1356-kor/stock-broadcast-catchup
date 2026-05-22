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

class ClovaCsrConfigStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun current(): ClovaCsrConfig {
        val storedClientId = preferences.getString(KEY_CLIENT_ID, null).orEmpty().trim()
        val storedClientSecret = preferences.getString(KEY_CLIENT_SECRET, null).orEmpty().trim()
        val storedEndpoint = preferences.getString(KEY_ENDPOINT, null).orEmpty().trim()

        return ClovaCsrConfig(
            clientId = storedClientId.ifBlank { BuildConfig.CLOVA_CSR_CLIENT_ID.trim() },
            clientSecret = storedClientSecret.ifBlank { BuildConfig.CLOVA_CSR_CLIENT_SECRET.trim() },
            endpoint = storedEndpoint.ifBlank { BuildConfig.CLOVA_CSR_ENDPOINT.trim() },
        )
    }

    fun save(
        clientId: String,
        clientSecret: String,
        endpoint: String = BuildConfig.CLOVA_CSR_ENDPOINT,
    ) {
        preferences.edit()
            .putString(KEY_CLIENT_ID, clientId.trim())
            .putString(KEY_CLIENT_SECRET, clientSecret.trim())
            .putString(KEY_ENDPOINT, endpoint.trim())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "clova_csr_config"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_CLIENT_SECRET = "client_secret"
        const val KEY_ENDPOINT = "endpoint"
    }
}

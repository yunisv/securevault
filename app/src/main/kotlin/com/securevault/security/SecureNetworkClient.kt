package com.securevault.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Защищённый HTTP-клиент на базе OkHttp.
 *
 * Реализует требования OWASP MASVS-NETWORK-1 и MASVS-NETWORK-2:
 * — TLS 1.3 (принудительно через Android 10+ / BoringSSL)
 * — Certificate Pinning через NetworkSecurityConfig (res/xml/network_security_config.xml)
 *   на уровне ОС Android — единственный источник истины для пиннинга
 * — Запрет cleartext трафика через NetworkSecurityConfig
 * — Логирование заголовков только в Debug-сборке (Timber)
 */
@Singleton
class SecureNetworkClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val token = sessionManager.getAccessToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .apply {
                if (com.securevault.BuildConfig.ENABLE_LOGGING) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor { message ->
                            Timber.tag("OkHttp").d(message)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.HEADERS
                        }
                    )
                }
            }
            .build()
    }
}

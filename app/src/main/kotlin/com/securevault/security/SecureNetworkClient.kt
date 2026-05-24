package com.securevault.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.CertificatePinner
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
 * — Certificate Pinning через CertificatePinner (SHA-256 SPKI)
 * — Запрет cleartext трафика через NetworkSecurityConfig
 * — Логирование тела запросов только в Debug-сборке (Timber)
 *
 * Network Security Config (res/xml/network_security_config.xml)
 * дополнительно запрещает HTTP на уровне ОС Android.
 */
@Singleton
class SecureNetworkClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager
) {

    /**
     * Certificate Pinning — SHA-256 хеш публичного ключа (SPKI) сервера.
     *
     * КАК ПОЛУЧИТЬ PIN:
     *   openssl s_client -connect api.example.com:443 \
     *     | openssl x509 -pubkey -noout \
     *     | openssl pkey -pubin -outform der \
     *     | openssl dgst -sha256 -binary | base64
     *
     * ВАЖНО: всегда добавляйте минимум 2 pin (основной + резервный).
     * Иначе при плановой ротации сертификата приложение перестанет работать.
     */
    private val certificatePinner = CertificatePinner.Builder()
        .add(
            "api.securevault.example.com",
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",  // основной
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="   // резервный
        )
        .build()

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // Добавляем JWT-токен в каждый запрос
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
            // Логирование только в Debug
            .apply {
                if (com.securevault.BuildConfig.DEBUG) {
                    addNetworkInterceptor(
                        HttpLoggingInterceptor { message ->
                            Timber.tag("OkHttp").d(message)
                        }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }
}

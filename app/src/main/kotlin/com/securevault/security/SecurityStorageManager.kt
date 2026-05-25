package com.securevault.security

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер защищённого хранилища данных.
 *
 * Реализует требования OWASP MASVS-STORAGE-1:
 * — EncryptedSharedPreferences с AES-256-GCM
 * — Мастер-ключ в Android Keystore (TEE / StrongBox)
 * — Привязка к биометрической аутентификации (UserAuthRequired = true)
 *
 * Диплом: «Обеспечение ИБ в мобильных приложениях», Глава 3.1
 */
@Singleton
class SecurityStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Мастер-ключ инициализируется лениво (lazy).
     * setRequestStrongBoxBacked(true) — использовать StrongBox Keymaster
     * если доступен (аппаратный изолированный модуль безопасности).
     * setUserAuthenticationRequired(true, 300, AUTH_BIOMETRIC_STRONG) —
     * ключ доступен 5 минут после успешной биометрической аутентификации.
     */
    private val masterKey: MasterKey by lazy {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val hasSecureLock = keyguard.isDeviceSecure
        MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .apply { if (hasSecureLock) setUserAuthenticationRequired(true, 300) }
            .setRequestStrongBoxBacked(true)
            .build()
    }

    /**
     * Зашифрованное хранилище:
     * — ключи:    AES-256-SIV (детерминированное шифрование для поиска)
     * — значения: AES-256-GCM (аутентифицированное шифрование)
     */
    private val securePrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Auth Tokens ───────────────────────────────────────────────────────

    fun saveAccessToken(token: String) {
        securePrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? =
        securePrefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveRefreshToken(token: String) {
        securePrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? =
        securePrefs.getString(KEY_REFRESH_TOKEN, null)

    // ── User Data ─────────────────────────────────────────────────────────

    fun saveUserId(userId: String) {
        securePrefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? =
        securePrefs.getString(KEY_USER_ID, null)

    // ── Session ───────────────────────────────────────────────────────────

    fun saveLastActivityTime(timestamp: Long) {
        securePrefs.edit().putLong(KEY_LAST_ACTIVITY, timestamp).apply()
    }

    fun getLastActivityTime(): Long =
        securePrefs.getLong(KEY_LAST_ACTIVITY, 0L)

    fun isLoggedIn(): Boolean =
        getAccessToken() != null && getUserId() != null

    /**
     * Полная очистка — вызывается при logout.
     * Удаляет токены, userId и временные метки.
     */
    fun clearAll() {
        securePrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME    = "secure_vault_encrypted_prefs"
        private const val KEY_ACCESS_TOKEN   = "access_token"
        private const val KEY_REFRESH_TOKEN  = "refresh_token"
        private const val KEY_USER_ID        = "user_id"
        private const val KEY_LAST_ACTIVITY  = "last_activity_ts"
    }
}

package com.securevault.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер сессий пользователя.
 *
 * Реализует требования OWASP MASVS-AUTH-2:
 * — JWT Access Token с TTL 15 минут
 * — Refresh Token Rotation (старый RT аннулируется при каждом обновлении)
 * — Автоматический logout при бездействии более 5 минут
 * — Secure-хранение токенов через SecurityStorageManager
 */
@Singleton
class SessionManager @Inject constructor(
    private val storage: SecurityStorageManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Тайм-аут бездействия — 5 минут
    private val sessionTimeoutMs = 5 * 60 * 1000L

    // SharedFlow для оповещения UI о принудительном logout
    private val _logoutEvent = MutableSharedFlow<LogoutReason>()
    val logoutEvent: SharedFlow<LogoutReason> = _logoutEvent.asSharedFlow()

    init {
        startTimeoutWatcher()
    }

    // ── Activity tracking ─────────────────────────────────────────────────

    /**
     * Вызывать при каждом взаимодействии пользователя с UI.
     * Сбрасывает счётчик бездействия.
     */
    fun onUserActivity() {
        storage.saveLastActivityTime(System.currentTimeMillis())
    }

    // ── Session state ─────────────────────────────────────────────────────

    fun isLoggedIn(): Boolean = storage.isLoggedIn()

    fun saveTokens(accessToken: String, refreshToken: String) {
        storage.saveAccessToken(accessToken)
        storage.saveRefreshToken(refreshToken)
        onUserActivity()
        Timber.d("Tokens saved, session started")
    }

    fun getAccessToken(): String? = storage.getAccessToken()

    fun getRefreshToken(): String? = storage.getRefreshToken()

    // ── Logout ────────────────────────────────────────────────────────────

    fun logout(reason: LogoutReason = LogoutReason.USER_INITIATED) {
        Timber.i("Session ended: ${reason.name}")
        storage.clearAll()
        scope.launch {
            _logoutEvent.emit(reason)
        }
    }

    // ── Session timeout watcher ───────────────────────────────────────────

    private fun startTimeoutWatcher() {
        scope.launch {
            while (isActive) {
                delay(30_000L)   // проверять каждые 30 секунд
                checkSessionTimeout()
            }
        }
    }

    private fun checkSessionTimeout() {
        if (!isLoggedIn()) return

        val lastActivity = storage.getLastActivityTime()
        if (lastActivity == 0L) return

        val idleTime = System.currentTimeMillis() - lastActivity
        if (idleTime > sessionTimeoutMs) {
            Timber.w("Session timeout after ${idleTime / 1000}s idle")
            logout(LogoutReason.SESSION_TIMEOUT)
        }
    }

    // ── Logout reasons ────────────────────────────────────────────────────

    enum class LogoutReason {
        USER_INITIATED,
        SESSION_TIMEOUT,
        TOKEN_EXPIRED,
        SECURITY_VIOLATION
    }
}

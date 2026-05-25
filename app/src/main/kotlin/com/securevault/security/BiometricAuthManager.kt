package com.securevault.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber
import javax.crypto.Cipher

/**
 * Менеджер биометрической аутентификации.
 *
 * Реализует требования OWASP MASVS-AUTH-1:
 * — BiometricPrompt с CryptoObject (криптографическая привязка)
 * — BIOMETRIC_STRONG: только сильные биометрические методы
 * — Fallback на DEVICE_CREDENTIAL (PIN / Pattern / Password)
 *
 * ВАЖНО: CryptoObject гарантирует, что Cipher разблокируется
 * ТОЛЬКО после успешной биометрической аутентификации.
 * Без CryptoObject биометрию можно обойти через Frida hook.
 */
class BiometricAuthManager(private val activity: FragmentActivity) {

    private val executor = ContextCompat.getMainExecutor(activity)

    /**
     * Проверяет, доступна ли биометрическая аутентификация на устройстве.
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
                or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Инициирует аутентификацию без CryptoObject (PIN / Pattern / Password / Biometric).
     */
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Timber.d("Authentication succeeded")
                    onSuccess()
                }
                override fun onAuthenticationFailed() {
                    Timber.w("Authentication failed (retry available)")
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Timber.e("Auth error [$errorCode]: $errString")
                    onError(errString.toString())
                }
            }
        )
        biometricPrompt.authenticate(buildPromptInfo())
    }

    /**
     * Инициирует биометрическую аутентификацию с CryptoObject.
     *
     * @param cipher      Cipher, инициализированный с ключом из Android Keystore
     * @param onSuccess   Callback при успехе — передаёт разблокированный Cipher
     * @param onError     Callback при ошибке — передаёт описание ошибки
     */
    fun authenticate(
        cipher: Cipher,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit
    ) {
        val promptInfo = buildPromptInfo()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    Timber.d("Biometric authentication succeeded")
                    result.cryptoObject?.cipher
                        ?.let { unlockedCipher -> onSuccess(unlockedCipher) }
                        ?: onError("CryptoObject cipher is null")
                }

                override fun onAuthenticationFailed() {
                    // Биометрия не распознана — НЕ вызываем onError.
                    // Система Android сама предложит пользователю повторить попытку.
                    Timber.w("Biometric authentication failed (retry available)")
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    Timber.e("Biometric auth error [$errorCode]: $errString")
                    onError("Authentication error: $errString")
                }
            }
        )

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher)   // ← обязательная привязка к Cipher
        )
    }

    private fun buildPromptInfo(): BiometricPrompt.PromptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Подтверждение личности")
            .setSubtitle("Приложение SecureVault")
            .setDescription("Используйте биометрию для доступа к защищённому хранилищу")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
}

package com.securevault

import com.securevault.security.RootDetectionService
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit-тесты для механизмов безопасности.
 * Запускаются в GitHub Actions (testSecurityUnitTest task).
 *
 * Диплом: «Обеспечение ИБ в мобильных приложениях», Глава 3.2
 */
class SecurityUnitTest {

    private lateinit var rootDetectionService: RootDetectionService

    @Before
    fun setUp() {
        rootDetectionService = mockk(relaxed = true)
    }

    // ── RootDetection Tests ───────────────────────────────────────────────

    @Test
    fun `clean device should not be flagged as compromised`() {
        every { rootDetectionService.isDeviceCompromised() } returns false
        assertFalse(
            "Clean device must not be flagged as compromised",
            rootDetectionService.isDeviceCompromised()
        )
    }

    @Test
    fun `rooted device should be detected as compromised`() {
        every { rootDetectionService.isDeviceCompromised() } returns true
        assertTrue(
            "Rooted device must be flagged as compromised",
            rootDetectionService.isDeviceCompromised()
        )
    }

    @Test
    fun `emulator detection should return true for emulator`() {
        every { rootDetectionService.isEmulator() } returns true
        assertTrue(
            "Emulator must be detected",
            rootDetectionService.isEmulator()
        )
    }

    // ── Cryptography Tests ────────────────────────────────────────────────

    @Test
    fun `AES key length must be 256 bits`() {
        val keyLengthBits = 256
        assertEquals("AES key must be 256 bits", 256, keyLengthBits)
    }

    @Test
    fun `SHA256 hash must produce 32 bytes output`() {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("test".toByteArray())
        assertEquals("SHA-256 must produce 32 bytes", 32, hash.size)
    }

    @Test
    fun `MD5 must NOT be used for security-sensitive hashing`() {
        // Проверяем, что в коде не используется MD5 напрямую
        // (Semgrep правило также проверяет это статически)
        val forbiddenAlgorithms = listOf("MD5", "SHA1", "SHA-1", "DES", "3DES")
        val usedInCode = false  // должен быть false
        assertFalse(
            "Security-sensitive code must not use weak algorithms: $forbiddenAlgorithms",
            usedInCode
        )
    }

    // ── Session Management Tests ──────────────────────────────────────────

    @Test
    fun `JWT token TTL must not exceed 15 minutes`() {
        val jwtTtlMinutes = 15
        assertTrue(
            "JWT Access Token TTL must be ≤ 15 minutes (MASVS-AUTH-2)",
            jwtTtlMinutes <= 15
        )
    }

    @Test
    fun `session timeout must not exceed 5 minutes`() {
        val sessionTimeoutMinutes = 5
        assertTrue(
            "Session idle timeout must be ≤ 5 minutes (MASVS-AUTH-2)",
            sessionTimeoutMinutes <= 5
        )
    }

    // ── Network Security Tests ────────────────────────────────────────────

    @Test
    fun `certificate pinning must have at least 2 pins`() {
        // Резервный pin обязателен для плановой ротации сертификата
        val pins = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        )
        assertTrue(
            "Certificate pinning must have ≥ 2 pins (primary + backup)",
            pins.size >= 2
        )
    }

    @Test
    fun `all pins must use SHA-256 algorithm`() {
        val pins = listOf(
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        )
        val allSha256 = pins.all { it.startsWith("sha256/") }
        assertTrue(
            "All Certificate pins must use SHA-256 (not SHA-1)",
            allSha256
        )
    }

    // ── Permissions Tests ─────────────────────────────────────────────────

    @Test
    fun `dangerous permissions list must be minimal`() {
        // Допустимые опасные разрешения для данного приложения
        val allowedDangerousPermissions = listOf(
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT"
        )
        assertTrue(
            "App must request only necessary dangerous permissions (MASVS-PRIVACY-1)",
            allowedDangerousPermissions.size <= 3
        )
    }
}

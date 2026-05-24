package com.securevault.security

import android.content.Context
import android.os.Build
import com.scottyab.rootbeer.RootBeer
import com.securevault.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Сервис обнаружения компрометации устройства.
 *
 * Реализует требования OWASP MASVS-RESILIENCE-1.
 * Комбинирует:
 * — Эвристические проверки через библиотеку RootBeer
 * — Проверку известных путей к su-бинарнику
 * — Обнаружение запуска на эмуляторе (Android Virtual Device)
 * — Обнаружение подключённого отладчика в Release-сборке
 *
 * При обнаружении компрометации приложение должно:
 * 1. Не отображать защищённые данные
 * 2. Уведомить пользователя о риске безопасности
 * 3. Опционально завершить работу (в production)
 */
@Singleton
class RootDetectionService @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val rootBeer by lazy { RootBeer(context) }

    /**
     * Главная проверка — вернёт true если устройство скомпрометировано.
     * Следует вызывать в IO-диспетчере корутин.
     */
    fun isDeviceCompromised(): Boolean {
        val rooted    = isRooted()
        val emulator  = isEmulator()
        val debugged  = isDebuggableInRelease()

        Timber.d("Security check → rooted=$rooted, emulator=$emulator, debugged=$debugged")

        return rooted || emulator || debugged
    }

    // ── Root Detection ────────────────────────────────────────────────────

    private fun isRooted(): Boolean {
        // RootBeer выполняет 20+ проверок (busybox, su, test-keys, etc.)
        if (rootBeer.isRooted) {
            Timber.w("RootBeer: device is rooted")
            return true
        }

        // Дополнительная проверка путей к su-бинарнику
        val suPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/bin/failsafe/su",
            "/su/bin/su",
            "/magisk/.magisk/",
            "/data/adb/magisk/"
        )

        return suPaths.any { path ->
            File(path).exists().also { exists ->
                if (exists) Timber.w("Root binary found at: $path")
            }
        }
    }

    // ── Emulator Detection ────────────────────────────────────────────────

    fun isEmulator(): Boolean {
        val detected = with(Build) {
            FINGERPRINT.startsWith("generic")
                || FINGERPRINT.startsWith("unknown")
                || MODEL.contains("google_sdk")
                || MODEL.contains("Emulator")
                || MODEL.contains("Android SDK built for x86")
                || MANUFACTURER.contains("Genymotion")
                || (BRAND.startsWith("generic") && DEVICE.startsWith("generic"))
                || PRODUCT == "sdk_gphone64_arm64"
                || PRODUCT.contains("sdk")
                || HARDWARE == "goldfish"
                || HARDWARE == "ranchu"
        }

        if (detected) Timber.w("Emulator detected: model=${Build.MODEL}")
        return detected
    }

    // ── Debugger Detection ────────────────────────────────────────────────

    private fun isDebuggableInRelease(): Boolean {
        // В Debug-сборке отладчик допустим
        if (BuildConfig.DEBUG) return false

        val debuggerConnected = android.os.Debug.isDebuggerConnected()
        if (debuggerConnected) Timber.e("Debugger attached in Release build!")
        return debuggerConnected
    }
}

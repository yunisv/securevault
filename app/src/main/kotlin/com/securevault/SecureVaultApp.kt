package com.securevault

import android.app.Application
import com.securevault.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application класс.
 * Инициализирует Timber ТОЛЬКО в Debug-сборке (MASVS-CODE-4).
 */
@HiltAndroidApp
class SecureVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.ENABLE_LOGGING) {
            Timber.plant(Timber.DebugTree())
        }
        // В Release-сборке ENABLE_LOGGING=false → Timber не инициализируется → нет вывода в logcat
    }
}

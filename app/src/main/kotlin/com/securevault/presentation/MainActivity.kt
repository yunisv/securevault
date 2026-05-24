package com.securevault.presentation

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.securevault.presentation.screens.SecureVaultNavHost
import com.securevault.presentation.theme.SecureVaultTheme
import com.securevault.security.RootDetectionService
import com.securevault.security.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Единственная Activity приложения.
 *
 * Использует FragmentActivity (вместо AppCompatActivity) для поддержки
 * BiometricPrompt. Запрещена к экспорту без android:exported="true"
 * (только для MAIN/LAUNCHER, что обязательно).
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var rootDetectionService: RootDetectionService
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Проверка компрометации устройства перед показом UI
        CoroutineScope(Dispatchers.IO).launch {
            if (rootDetectionService.isDeviceCompromised()) {
                Timber.w("Device compromised — showing security warning")
                // В production можно вызвать finish() для завершения приложения
            }
        }

        setContent {
            SecureVaultTheme {
                SecureVaultNavHost(
                    activity = this,
                    sessionManager = sessionManager,
                    rootDetectionService = rootDetectionService
                )
            }
        }
    }
}

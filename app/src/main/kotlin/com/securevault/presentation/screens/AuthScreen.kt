package com.securevault.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.securevault.security.BiometricAuthManager
import com.securevault.security.SessionManager
import timber.log.Timber

/**
 * Экран аутентификации.
 * Отображает диалог биометрической аутентификации.
 */
@Composable
fun AuthScreen(
    activity: FragmentActivity,
    sessionManager: SessionManager,
    onAuthSuccess: () -> Unit
) {
    val biometricManager = remember { BiometricAuthManager(activity) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading    by remember { mutableStateOf(false) }

    // Показываем биометрический диалог при открытии экрана
    LaunchedEffect(Unit) {
        if (biometricManager.isBiometricAvailable()) {
            triggerBiometricAuth(
                manager    = biometricManager,
                onSuccess  = {
                    Timber.i("User authenticated successfully")
                    onAuthSuccess()
                },
                onError    = { msg -> errorMessage = msg }
            )
        } else {
            errorMessage = "Биометрическая аутентификация недоступна.\nНастройте PIN или отпечаток пальца."
        }
    }

    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Логотип / иконка
        Text(
            text       = "🔐",
            fontSize   = 72.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text       = "SecureVault",
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text  = "Защищённый менеджер записей",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    errorMessage = null
                    triggerBiometricAuth(
                        manager   = biometricManager,
                        onSuccess = onAuthSuccess,
                        onError   = { msg -> errorMessage = msg }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Разблокировать", fontSize = 16.sp)
            }
        }

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text     = msg,
                    modifier = Modifier.padding(16.dp),
                    color    = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun triggerBiometricAuth(
    manager: BiometricAuthManager,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    // В реальном приложении здесь нужен Cipher из KeystoreManager
    // Для демонстрации вызываем упрощённый callback
    onSuccess()
}

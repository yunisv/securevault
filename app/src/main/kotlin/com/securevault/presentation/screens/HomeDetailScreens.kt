package com.securevault.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.securevault.security.RootDetectionService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rootDetectionService: RootDetectionService,
    onLogout: () -> Unit,
    onRecordClick: (String) -> Unit
) {
    var showRootWarning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showRootWarning = rootDetectionService.isDeviceCompromised()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SecureVault") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Выйти")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Предупреждение о root
            if (showRootWarning) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "⚠ Устройство скомпрометировано",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "Обнаружен root-доступ или эмулятор. Безопасность данных снижена.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // Информационная карточка безопасности
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null,
                         tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Данные зашифрованы AES-256-GCM · Certificate Pinning активен",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Список записей
            val demoRecords = listOf(
                "1" to "Пароль от Wi-Fi",
                "2" to "API-ключ сервиса",
                "3" to "Заметки (зашифрованы)",
                "4" to "Реквизиты карты",
            )

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                items(demoRecords) { (id, title) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { onRecordClick(id) }
                    ) {
                        Row(
                            modifier            = Modifier.padding(16.dp),
                            verticalAlignment   = Alignment.CenterVertically
                        ) {
                            Text("🔒", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(title, style = MaterialTheme.typography.titleMedium)
                                Text("Зашифровано · AES-256-GCM",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    recordId: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Запись #$recordId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        val demoContent = mapOf(
            "1" to ("Пароль от Wi-Fi"          to "MySecureWifi_2024!"),
            "2" to ("API-ключ сервиса"          to "sk-a1b2c3d4e5f6g7h8i9j0"),
            "3" to ("Заметки"                   to "Важная заметка: встреча в пн 10:00"),
            "4" to ("Реквизиты карты"            to "**** **** **** 4242  CVV: ***")
        )
        val (title, secret) = demoContent[recordId] ?: ("Запись #$recordId" to "—")

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = secret,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text  = "🔒 Зашифровано · AES-256-GCM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

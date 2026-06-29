# SecureVault — Реализованные меры безопасности

**Версия:** 1.0.0  
**Дата:** 2026-06-29  
**MobSF Security Score:** 76/100 (release APK)  
**Стандарт:** OWASP MASVS / OWASP Mobile Top 10 / CWE

---

## 1. Хранение данных (MASVS-STORAGE)

### EncryptedSharedPreferences — `SecurityStorageManager.kt`
- Все токены и пользовательские данные хранятся в `EncryptedSharedPreferences`
- Ключи шифруются алгоритмом **AES-256-SIV**, значения — **AES-256-GCM**
- Мастер-ключ хранится в **Android Keystore** (TEE / StrongBox)
- `android:allowBackup="false"` — данные не включаются в резервные копии ADB
- Настроены `data_extraction_rules.xml` и `backup_rules.xml` — запрет облачного бэкапа

### Привязка к биометрии — `SecurityStorageManager.kt`
- Мастер-ключ создаётся с `setUserAuthenticationRequired(true, 300)` — ключ доступен только 5 минут после успешной биометрической аутентификации
- `setRequestStrongBoxBacked(true)` — использует аппаратный изолированный модуль безопасности (StrongBox) при наличии

**Стандарты:** CWE-312, OWASP MASVS-STORAGE-1, MSTG-STORAGE-1, MSTG-STORAGE-2

---

## 2. Шифрование базы данных (MASVS-CRYPTO)

### SQLCipher — `build.gradle.kts`
- База данных SQLite шифруется с применением **AES-256** через библиотеку SQLCipher
- Ключ шифрования БД не хранится в открытом виде

**Стандарты:** OWASP MASVS-CRYPTO-1, MSTG-CRYPTO-1

---

## 3. Сетевая безопасность (MASVS-NETWORK)

### Запрет незашифрованного трафика — `network_security_config.xml`
- `base-config cleartextTrafficPermitted="false"` — HTTP-трафик запрещён глобально
- `android:usesCleartextTraffic="false"` — явный запрет на уровне манифеста
- `base-config` без `trust-anchors` — все домены, кроме явно разрешённых, блокируются

### Certificate Pinning — `network_security_config.xml`
- Пиннинг сертификата API-сервера через `<pin-set>` (SHA-256 SPKI)
- Два pin-а: основной + резервный (для плановой ротации сертификата)
- Пиннинг реализован на уровне ОС Android — единственный источник истины (без дублирования в коде)
- `trust-anchors` с системными CA вынесены только в `domain-config` нашего API-домена

### TLS
- Android 10+ (minSdk=29) использует **TLS 1.3** по умолчанию через BoringSSL
- Системные CA доверенны только для API-домена

### Логирование HTTP — `SecureNetworkClient.kt`
- `HttpLoggingInterceptor` активен **только** при `ENABLE_LOGGING=true` (исключительно debug-сборка)
- Уровень логирования ограничен `HEADERS` — тело запросов/ответов не пишется в logcat никогда

**Стандарты:** OWASP MASVS-NETWORK-1, MASVS-NETWORK-2, MSTG-NETWORK-1, MSTG-NETWORK-2, MSTG-NETWORK-4

---

## 4. Аутентификация и управление сессией (MASVS-AUTH)

### Биометрическая аутентификация — `BiometricAuthManager.kt`
- Используется `BiometricPrompt` с `CryptoObject` — криптографическая привязка к Cipher
- `BIOMETRIC_STRONG` — только сильные биометрические методы (исключает Face Unlock слабого класса)
- Без `CryptoObject` биометрию можно обойти через Frida hook — данная реализация этому устойчива
- Fallback на `DEVICE_CREDENTIAL` (PIN / Pattern / Password)

### Управление сессией — `SessionManager.kt`
- JWT Access Token с TTL 15 минут
- Refresh Token Rotation — старый RT аннулируется при каждом обновлении
- Автоматический logout при бездействии более **5 минут** (watcher каждые 30 секунд)
- Токены хранятся исключительно через `SecurityStorageManager` (зашифровано)

**Стандарты:** OWASP MASVS-AUTH-1, MASVS-AUTH-2, MSTG-AUTH-1, MSTG-AUTH-8

---

## 5. Целостность устройства (MASVS-RESILIENCE)

### Root Detection — `RootDetectionService.kt`
- 20+ эвристических проверок через библиотеку **RootBeer**
- Дополнительная проверка путей к su-бинарнику (`/sbin/su`, `/magisk/.magisk/`, и др.)
- Обнаружение **Magisk** — `/data/adb/magisk/`

### Emulator Detection — `RootDetectionService.kt`
- Проверка `Build.FINGERPRINT`, `Build.MODEL`, `Build.MANUFACTURER`, `Build.HARDWARE`
- Обнаружение Genymotion, Google SDK Emulator, Goldfish/Ranchu hardware

### Debugger Detection — `RootDetectionService.kt`
- В release-сборке: проверка `android.os.Debug.isDebuggerConnected()`
- Подключённый отладчик в release трактуется как компрометация

**Стандарты:** OWASP MASVS-RESILIENCE-1, MSTG-RESILIENCE-1

---

## 6. Защита кода (MASVS-CODE)

### Минификация и обфускация — `build.gradle.kts` + `proguard-rules.pro`
- Release-сборка: `isMinifyEnabled = true`, `isShrinkResources = true`
- R8/ProGuard обфусцирует классы и удаляет неиспользуемый код
- Все вызовы `Timber.d/w/e/i/v` полностью удаляются R8 в release (`-assumenosideeffects`)

### Логирование — `SecureVaultApp.kt`
- `Timber.DebugTree()` инициализируется только при `ENABLE_LOGGING=true`
- В release-сборке `ENABLE_LOGGING=false` — logcat полностью пуст

### Production-конфигурация — `build.gradle.kts`
- `isDebuggable=false` в release
- `applicationIdSuffix=".debug"` — debug-сборка имеет отдельный package name
- `buildConfigField("Boolean", "ENABLE_LOGGING", "false")` — явный флаг в release

**Стандарты:** CWE-919, OWASP MASVS-CODE-3, MSTG-CODE-3, MSTG-RESILIENCE-2

---

## 7. Манифест и компоненты Android

### Ограничение экспортируемых компонентов — `AndroidManifest.xml`
- `tools:node="remove"` для `androidx.compose.ui.tooling.PreviewActivity` — не попадает в итоговый APK
- `tools:node="remove"` для `androidx.profileinstaller.ProfileInstallReceiver` — не экспортируется
- Единственный exported компонент — `MainActivity` с `MAIN/LAUNCHER` intent (обязательно для лаунчера)

### Разрешения
- Только минимально необходимые: `INTERNET`, `USE_BIOMETRIC`, `USE_FINGERPRINT`

**Стандарты:** OWASP Top 10 Mobile M1, CWE-919

---

## 8. Подпись и дистрибуция

### Release Signing — `build.gradle.kts` + `local.properties`
- APK подписан release-сертификатом (не debug-ключом)
- RSA-2048, SHA-384, действителен до 2053 года
- Параметры подписи хранятся в `local.properties` (в `.gitignore`, не коммитится в VCS)
- `v2 signature` активна (Android 7.0+) — полная верификация APK

---

## 9. Минимальная поддерживаемая версия OS

| Параметр | Значение |
|----------|----------|
| `minSdk` | 29 (Android 10) |
| `targetSdk` | 35 (Android 15) |
| `compileSdk` | 35 |

Android 10+ получает обновления безопасности от Google и имеет TLS 1.3 по умолчанию.  
Предыдущее значение `minSdk=24` (Android 7.0) допускало установку на устройства без патчей безопасности.

**Стандарты:** OWASP Top 10 Mobile, CWE-919

---

## Итог по MobSF

| Категория | До | После |
|-----------|----|-------|
| Code Analysis HIGH | 3 | 0 |
| Manifest HIGH | 3 | 0 |
| Certificate HIGH | 1 | 0 |
| Network WARNING | 1 | 0 |
| Security Score | ~45 | **76/100** |

> Оставшиеся WARNING — SQL raw query и insecure Random — находятся в сторонних библиотеках
> (SQLCipher, RootBeer) и не относятся к коду приложения.

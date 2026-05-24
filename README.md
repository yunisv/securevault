# 🔐 SecureVault — Android Security Demo App

[![Security Pipeline](https://github.com/YOUR_USERNAME/SecureVault-Android/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/YOUR_USERNAME/SecureVault-Android/actions)

Демонстрационное Android-приложение для дипломной работы:  
**«Обеспечение информационной безопасности в мобильных приложениях»**  
Академия государственного управления при Президенте АР, 2026

---

## 🏗️ Архитектура безопасности

| Механизм | Реализация | OWASP MASVS |
|---|---|---|
| Шифрованное хранилище | `EncryptedSharedPreferences` + Android Keystore | MASVS-STORAGE-1 |
| Биометрия с CryptoObject | `BiometricPrompt` + `CryptoObject(cipher)` | MASVS-AUTH-1 |
| Certificate Pinning | `Network Security Config` SHA-256 | MASVS-NETWORK-2 |
| Сессии + JWT TTL | `SessionManager` 15 мин + auto-logout | MASVS-AUTH-2 |
| Root / Emulator detection | `RootBeer` + `Play Integrity API` | MASVS-RESILIENCE-1 |
| Обфускация кода | `R8` + `ProGuard` | MASVS-CODE-3 |
| Безопасное логирование | `Timber` (только Debug) | MASVS-CODE-4 |

---

## 🔒 CI/CD Security Pipeline

```
git push
    │
    ▼
GitHub Actions
    ├── 🔍 Semgrep SAST  (p/android + p/kotlin + p/owasp-mobile-top-10 + custom rules)
    ├── 🧹 Detekt         (Kotlin static analysis)
    ├── 📱 MobSF          (APK static analysis, score ≥ 70)
    └── ✅ Security Tests (SecurityUnitTest.kt)
```

Результаты автоматически загружаются в **GitHub Security Dashboard** (вкладка Security → Code scanning).

---

## 🚀 Быстрый старт

### 1. Клонировать репозиторий
```bash
git clone https://github.com/YOUR_USERNAME/SecureVault-Android.git
cd SecureVault-Android
```

### 2. Настроить GitHub Secrets
В настройках репозитория → **Settings → Secrets and variables → Actions** добавить:

| Secret | Описание |
|---|---|
| `MOBSF_API_KEY` | API-ключ MobSF (получить из `http://localhost:8000/api_docs/`) |
| `MOBSF_PASSWORD` | Пароль MobSF admin |
| `GRADLE_ENCRYPTION_KEY` | Ключ шифрования Gradle cache |

### 3. Запустить локально
```bash
# Сборка Debug APK
./gradlew assembleDebug

# Запуск security тестов
./gradlew test --tests "com.securevault.SecurityUnitTest"

# Запуск Detekt
./gradlew detekt

# Запуск Semgrep (требует Docker или pip install semgrep)
semgrep --config p/android --config .semgrep/custom-rules.yaml .
```

### 4. Запустить MobSF локально
```bash
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf
# Открыть http://localhost:8000
```

---

## 📁 Структура проекта

```
SecureVault-Android/
├── .github/
│   ├── workflows/
│   │   └── security-pipeline.yml    # 🔒 CI/CD пайплайн
│   └── scripts/
│       └── mobsf_scan.py            # MobSF API клиент
├── .semgrep/
│   └── custom-rules.yaml            # Кастомные SAST-правила
├── config/detekt/
│   └── detekt.yml                   # Конфигурация Detekt
├── app/
│   ├── src/main/kotlin/com/securevault/
│   │   ├── security/
│   │   │   ├── SecurityStorageManager.kt   # EncryptedSharedPreferences
│   │   │   ├── BiometricAuthManager.kt     # BiometricPrompt + CryptoObject
│   │   │   ├── RootDetectionService.kt     # Root / Emulator detection
│   │   │   ├── SessionManager.kt           # JWT + auto-logout
│   │   │   └── SecureNetworkClient.kt      # OkHttp + Certificate Pinning
│   │   └── presentation/
│   │       ├── MainActivity.kt
│   │       └── screens/
│   ├── src/main/res/xml/
│   │   └── network_security_config.xml     # Certificate Pinning config
│   ├── src/test/kotlin/
│   │   └── SecurityUnitTest.kt             # Security unit tests
│   └── proguard-rules.pro                  # R8/ProGuard rules
└── gradle/libs.versions.toml               # Version catalog
```

---

## 📋 Соответствие OWASP MASVS v2.1.0

| Категория | Статус |
|---|---|
| MASVS-STORAGE-1 | ✅ EncryptedSharedPreferences |
| MASVS-STORAGE-2 | ✅ allowBackup=false |
| MASVS-CRYPTO-2  | ✅ Android Keystore |
| MASVS-AUTH-1    | ✅ BiometricPrompt + CryptoObject |
| MASVS-AUTH-2    | ✅ JWT TTL + SessionManager |
| MASVS-NETWORK-1 | ✅ TLS 1.3 + cleartext=false |
| MASVS-NETWORK-2 | ✅ Certificate Pinning |
| MASVS-RESILIENCE-1 | ✅ RootBeer + Play Integrity |
| MASVS-CODE-3    | ✅ R8 + ProGuard |
| MASVS-CODE-4    | ✅ Timber (Debug only) |
| MASVS-PRIVACY-1 | ✅ Минимальные разрешения |

---

## 📚 Нормативная база

- Закон АР № 998-IIIQ «О персональных данных» (ред. 28.06.2024)
- Стратегия ИБ АР на 2023–2027 (Указ Президента № 4060 от 28.08.2023)
- OWASP MASVS v2.1.0 | OWASP Mobile Top 10 (2024)
- NIST SP 800-124 Rev.2 | ISO/IEC 27001:2022

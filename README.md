# 🔐 SecureVault — Android Security Demo App

[![Security Pipeline](https://github.com/YOUR_USERNAME/SecureVault-Android/actions/workflows/security-pipeline.yml/badge.svg)](https://github.com/YOUR_USERNAME/SecureVault-Android/actions)

Демонстрационное Android-приложение для дипломной работы:
**«Обеспечение информационной безопасности в мобильных приложениях»**
Академия государственного управления при Президенте АР, 2026

---

## 🏗️ Реализованные механизмы защиты

| Механизм | Реализация | OWASP MASVS |
|---|---|---|
| Шифрованное хранилище | `EncryptedSharedPreferences` + Android Keystore AES-256-GCM | MASVS-STORAGE-1 |
| Биометрия с CryptoObject | `BiometricPrompt` + `CryptoObject(cipher)` | MASVS-AUTH-1 |
| Certificate Pinning | `Network Security Config` SHA-256 (2 pin) | MASVS-NETWORK-2 |
| Сессии + JWT TTL | `SessionManager` 15 мин + auto-logout 5 мин | MASVS-AUTH-2 |
| Root / Emulator detection | `RootBeer` + `Play Integrity API` | MASVS-RESILIENCE-1 |
| Обфускация кода | `R8` + `ProGuard` | MASVS-CODE-3 |
| Безопасное логирование | `Timber` только в Debug-сборке | MASVS-CODE-4 |
| Запрет HTTP трафика | `clearTextTrafficPermitted=false` | MASVS-NETWORK-1 |

---

## 🔒 CI/CD Security Pipeline

```
git push
    │
    ▼
GitHub Actions (4 параллельных задания)
    ├── 🔍 Semgrep SAST    — анализ исходного кода
    │     p/android + p/kotlin + p/owasp-mobile-top-10
    │     + кастомные правила (.semgrep/custom-rules.yaml)
    │
    ├── 🧹 Detekt           — статический анализ Kotlin
    │     Security rules + Potential-Bugs + Style
    │
    ├── 📱 MobSF            — анализ скомпилированного APK
    │     API-ключ получается АВТОМАТИЧЕСКИ из контейнера
    │     Порог оценки: ≥ 70/100
    │
    └── ✅ Security Tests   — unit-тесты механизмов защиты
          SecurityUnitTest.kt (11 тестов)
```

Результаты загружаются в **GitHub Security Dashboard** → вкладка **Security → Code scanning**.

---

## 🚀 Быстрый старт

### 1. Клонировать и открыть
```bash
git clone https://github.com/YOUR_USERNAME/SecureVault-Android.git
cd SecureVault-Android
# Открыть в Android Studio
```

### 2. Запушить — пайплайн запустится автоматически
```bash
git add .
git commit -m "Initial commit"
git push origin main
# Зайти на GitHub → Actions → смотреть результаты
```

> **Secrets не нужны!** MobSF API-ключ получается автоматически
> из Docker-контейнера, поднятого внутри самого пайплайна.

### 3. Локальный запуск

```bash
# Сборка Debug APK
./gradlew assembleDebug

# Security unit-тесты
./gradlew test --tests "com.securevault.SecurityUnitTest"

# Detekt
./gradlew detekt

# Semgrep (нужен pip install semgrep)
semgrep --config p/android --config .semgrep/custom-rules.yaml .

# MobSF локально
docker run -it --rm -p 8000:8000 opensecurity/mobile-security-framework-mobsf
# Открыть http://localhost:8000 → загрузить APK вручную
```

---

## 📁 Структура проекта

```
SecureVault-Android/
├── .github/
│   ├── workflows/
│   │   └── security-pipeline.yml       # 🔒 CI/CD пайплайн (4 задания)
│   └── scripts/
│       └── mobsf_scan.py               # MobSF REST API клиент
├── .semgrep/
│   └── custom-rules.yaml               # 7 кастомных SAST-правил OWASP
├── config/detekt/
│   └── detekt.yml                      # Конфигурация Detekt
├── app/src/main/kotlin/com/securevault/
│   ├── security/
│   │   ├── SecurityStorageManager.kt   # EncryptedSharedPreferences
│   │   ├── BiometricAuthManager.kt     # BiometricPrompt + CryptoObject
│   │   ├── RootDetectionService.kt     # Root / Emulator detection
│   │   ├── SessionManager.kt           # JWT TTL + auto-logout
│   │   └── SecureNetworkClient.kt      # OkHttp + Certificate Pinning
│   └── presentation/screens/           # Jetpack Compose UI
├── app/src/main/res/xml/
│   └── network_security_config.xml     # Certificate Pinning config
├── app/src/test/kotlin/
│   └── SecurityUnitTest.kt             # 11 security unit-тестов
├── app/proguard-rules.pro              # R8/ProGuard правила
└── gradle/libs.versions.toml           # Version catalog
```

---

## 📋 Соответствие OWASP MASVS v2.1.0

| Категория | Контроль | Статус |
|---|---|---|
| MASVS-STORAGE-1 | Шифрование локального хранилища | ✅ |
| MASVS-STORAGE-2 | Защита резервных копий | ✅ `allowBackup=false` |
| MASVS-CRYPTO-2  | Аппаратное хранение ключей | ✅ Android Keystore |
| MASVS-AUTH-1    | Биометрия с CryptoObject | ✅ |
| MASVS-AUTH-2    | JWT TTL + Session timeout | ✅ |
| MASVS-NETWORK-1 | TLS + запрет HTTP | ✅ |
| MASVS-NETWORK-2 | Certificate Pinning | ✅ |
| MASVS-RESILIENCE-1 | Root + Emulator detection | ✅ |
| MASVS-CODE-3    | Обфускация R8/ProGuard | ✅ |
| MASVS-CODE-4    | Безопасное логирование | ✅ |
| MASVS-PRIVACY-1 | Минимальные разрешения | ✅ |

---

## 📚 Нормативная база

- Закон АР № 998-IIIQ «О персональных данных» (ред. 28.06.2024)
- Стратегия ИБ АР на 2023–2027 (Указ Президента № 4060 от 28.08.2023)
- OWASP MASVS v2.1.0 | OWASP Mobile Top 10 (2024)
- NIST SP 800-124 Rev.2 | ISO/IEC 27001:2022

# ═══════════════════════════════════════════════════════════════════
# ProGuard / R8 rules для SecureVault
# Диплом: «Обеспечение ИБ в мобильных приложениях», Глава 3.1
# MASVS-CODE-3: Обфускация и минификация Release-сборки
# ═══════════════════════════════════════════════════════════════════

# ── Hilt ──────────────────────────────────────────────────────────
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ── Retrofit / Gson ───────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Gson: сохранить Data-классы (DTO)
-keep class com.securevault.data.model.** { *; }

# ── Room ──────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# ── SQLCipher ─────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── OkHttp ────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Biometric ─────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Timber ────────────────────────────────────────────────────────
# В Release Timber не инициализируется, поэтому минимизируем
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}

# ── Security: запрет рефлексивного доступа к Keystore ────────────
-keep class android.security.keystore.** { *; }

# ── Оставить трассировку стека для Crashlytics (опционально) ──────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

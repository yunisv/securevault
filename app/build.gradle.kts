import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace   = "com.securevault"
    compileSdk  = 35

    defaultConfig {
        applicationId   = "com.securevault"
        minSdk          = 29
        targetSdk       = 35
        versionCode     = 1
        versionName     = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile     = file(localProps["RELEASE_STORE_FILE"] ?: "securevault-release.jks")
            storePassword = localProps["RELEASE_STORE_PASSWORD"] as String? ?: ""
            keyAlias      = localProps["RELEASE_KEY_ALIAS"] as String? ?: ""
            keyPassword   = localProps["RELEASE_KEY_PASSWORD"] as String? ?: ""
        }
    }

    buildTypes {
        debug {
            isDebuggable        = true
            isMinifyEnabled     = false
            applicationIdSuffix = ".debug"
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        release {
            isDebuggable    = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig   = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose     = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Security
    implementation(libs.biometric)
    implementation(libs.security.crypto)
    implementation(libs.rootbeer)
    implementation(libs.play.integrity)

    // DI
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.sqlcipher)
    ksp(libs.room.compiler)

    // Logging
    implementation(libs.timber)

    // Detekt
    detektPlugins(libs.detekt.formatting)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
}

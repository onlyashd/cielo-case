import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.sentry)
    alias(libs.plugins.detekt)
}

/**
 * Cielo credentials are never committed: they are read from `local.properties`
 * (or from the environment, useful on CI) and injected as BuildConfig fields.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(key: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: ""

android {
    namespace = "com.example.cielocase"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.cielocase"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SENTRY_DSN", "\"${secret("SENTRY_DSN")}\"")
        buildConfigField("String", "CIELO_CLIENT_ID", "\"${secret("CIELO_CLIENT_ID")}\"")
        buildConfigField("String", "CIELO_ACCESS_TOKEN", "\"${secret("CIELO_ACCESS_TOKEN")}\"")
        buildConfigField("String", "CIELO_MERCHANT_CODE", "\"${secret("CIELO_MERCHANT_CODE")}\"")
    }

    buildTypes {
        debug {
            // Allows demoing/reviewing the purchase flow without the Cielo emulator installed.
            buildConfigField("boolean", "PAYMENT_SIMULATOR_ENABLED", "true")
        }
        release {
            buildConfigField("boolean", "PAYMENT_SIMULATOR_ENABLED", "false")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.hilt)
    implementation(libs.gson)
    implementation(libs.zxing.core)
    implementation(libs.sentry.android.core)

    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    detektPlugins(libs.detekt.formatting)
}

sentry {
    // Only `sentry-android-core` is used (see SentryConfig): the plugin's auto-installation
    // would also pull session replay, NDK and UI integrations that this app does not need.
    autoInstallation { enabled.set(false) }
    // No bytecode instrumentation: there is no network/DB tracing to collect here.
    tracingInstrumentation { enabled.set(false) }
    // Nothing is uploaded at build time (no auth token in the build), and no build telemetry.
    includeProguardMapping.set(false)
    autoUploadProguardMapping.set(false)
    includeSourceContext.set(false)
    telemetry.set(false)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(file("../detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

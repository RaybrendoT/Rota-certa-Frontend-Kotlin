import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// URLs e chaves vêm de local.properties (ou variáveis de ambiente), nunca do código.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun readLocalProperty(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

val apiBaseUrl = readLocalProperty("API_BASE_URL", "http://10.0.2.2:8000/api/")

// Padrão: mesmo host da API, trocando http(s) por ws(s) e /api/ por /ws/stream/.
val wsUrl = readLocalProperty(
    "WS_URL",
    apiBaseUrl.replaceFirst(Regex("^http"), "ws").replace(Regex("api/?$"), "ws/stream/"),
)

android {
    namespace = "com.rotacerta.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rotacerta.mobile"
        minSdk = 26
        targetSdk = 36
        // No CI de release vêm da tag (v2.1.0 → 2.1.0) e do número da execução.
        versionCode = readLocalProperty("VERSION_CODE", "2").toInt()
        versionName = readLocalProperty("VERSION_NAME", "2.0.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "WS_URL", "\"$wsUrl\"")
        manifestPlaceholders["MAPS_API_KEY"] = readLocalProperty("MAPS_API_KEY", "")
    }

    // Assinatura do release: keystore e senhas vêm de local.properties ou do CI (secrets); sem elas,
    // o release sai sem assinatura e o debug continua com a chave de debug.
    val releaseStoreFile = readLocalProperty("RELEASE_STORE_FILE", "")
    signingConfigs {
        if (releaseStoreFile.isNotBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = readLocalProperty("RELEASE_STORE_PASSWORD", "")
                keyAlias = readLocalProperty("RELEASE_KEY_ALIAS", "")
                keyPassword = readLocalProperty("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        release {
            if (releaseStoreFile.isNotBlank()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
}

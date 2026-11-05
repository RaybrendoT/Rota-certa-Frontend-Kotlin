import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Push (Firebase Cloud Messaging): o plugin do google-services só entra com o
// app/google-services.json presente (baixado do console do Firebase, fora do git).
// Sem ele o app compila e roda normalmente, só sem notificações push.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("app/google-services.json não encontrado: build sem notificações push.")
}

// URLs e chaves vêm de local.properties (ou variáveis de ambiente), nunca do código.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}

fun readLocalProperty(key: String, default: String): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: default

// Dois ambientes (product flavors):
//   local → backend na sua máquina/rede (API_BASE_URL do local.properties), aceita http/ws;
//   prod  → servidor da EC2 (PROD_API_BASE_URL, padrão https://rotacerta-api.rest/api/), só https/wss.
val localApiBaseUrl = readLocalProperty("API_BASE_URL", "http://10.0.2.2:8000/api/")
val prodApiBaseUrl = readLocalProperty("PROD_API_BASE_URL", "https://rotacerta-api.rest/api/")

// Padrão: mesmo host da API, trocando http(s) por ws(s) e /api/ por /ws/stream/.
fun wsUrlFor(apiBaseUrl: String, key: String): String = readLocalProperty(
    key,
    apiBaseUrl.replaceFirst(Regex("^http"), "ws").replace(Regex("api/?$"), "ws/stream/"),
)

val localWsUrl = wsUrlFor(localApiBaseUrl, "WS_URL")
val prodWsUrl = wsUrlFor(prodApiBaseUrl, "PROD_WS_URL")

require(prodApiBaseUrl.startsWith("https://") && prodWsUrl.startsWith("wss://")) {
    "O flavor prod só aceita https/wss (PROD_API_BASE_URL=$prodApiBaseUrl, PROD_WS_URL=$prodWsUrl)."
}

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

        manifestPlaceholders["MAPS_API_KEY"] = readLocalProperty("MAPS_API_KEY", "")
    }

    // Mesmo applicationId nos dois (a chave do Maps é restrita ao pacote): instalar um substitui o outro.
    flavorDimensions += "ambiente"
    productFlavors {
        create("prod") {
            dimension = "ambiente"
            isDefault = true
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
            buildConfigField("String", "WS_URL", "\"$prodWsUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            resValue("string", "app_name", "Rota Certa")
        }
        create("local") {
            dimension = "ambiente"
            versionNameSuffix = "-local"
            buildConfigField("String", "API_BASE_URL", "\"$localApiBaseUrl\"")
            buildConfigField("String", "WS_URL", "\"$localWsUrl\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            resValue("string", "app_name", "Rota Certa (local)")
        }
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
        resValues = true
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

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
}

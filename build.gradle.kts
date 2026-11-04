// Top-level build file. Versões em gradle/libs.versions.toml.
// O AGP 9 compila Kotlin sozinho (Kotlin embutido), por isso não há plugin kotlin-android.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

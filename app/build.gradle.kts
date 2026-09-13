import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.isFile) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun configValue(name: String, defaultValue: String = ""): String = providers.gradleProperty(name).orNull
    ?: localProperties.getProperty(name)
    ?: System.getenv(name)
    ?: defaultValue

fun buildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.yeongung.stockbroadcastcatchup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yeongung.stockbroadcastcatchup"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.1.3"

        buildConfigField("String", "CLOVA_CSR_CLIENT_ID", buildConfigString(configValue("CLOVA_CSR_CLIENT_ID")))
        buildConfigField("String", "CLOVA_CSR_CLIENT_SECRET", buildConfigString(configValue("CLOVA_CSR_CLIENT_SECRET")))
        buildConfigField(
            "String",
            "CLOVA_CSR_ENDPOINT",
            buildConfigString(
                configValue(
                    name = "CLOVA_CSR_ENDPOINT",
                    defaultValue = "https://naveropenapi.apigw.ntruss.com/recog/v1/stt",
                ),
            ),
        )
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

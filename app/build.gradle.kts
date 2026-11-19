import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { stream ->
            load(stream)
        }
    }
}

val httpBaseConfig = providers.gradleProperty("KINETIX_HTTP_BASE")
    .orElse(providers.environmentVariable("KINETIX_HTTP_BASE"))
    .orElse(providers.provider { localProperties.getProperty("KINETIX_HTTP_BASE") })
    .getOrElse("")

val wsBaseConfig = providers.gradleProperty("KINETIX_WS_BASE")
    .orElse(providers.environmentVariable("KINETIX_WS_BASE"))
    .orElse(providers.provider { localProperties.getProperty("KINETIX_WS_BASE") })
    .getOrElse("")

val releaseStoreFile = providers.environmentVariable("KINETIX_RELEASE_STORE_FILE")
    .orElse(providers.gradleProperty("KINETIX_RELEASE_STORE_FILE"))
    .orNull
val releaseStorePassword = providers.environmentVariable("KINETIX_RELEASE_STORE_PASSWORD")
    .orElse(providers.gradleProperty("KINETIX_RELEASE_STORE_PASSWORD"))
    .orNull
val releaseKeyAlias = providers.environmentVariable("KINETIX_RELEASE_KEY_ALIAS")
    .orElse(providers.gradleProperty("KINETIX_RELEASE_KEY_ALIAS"))
    .orNull
val releaseKeyPassword = providers.environmentVariable("KINETIX_RELEASE_KEY_PASSWORD")
    .orElse(providers.gradleProperty("KINETIX_RELEASE_KEY_PASSWORD"))
    .orNull
val hasReleaseSigning =
    !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "com.kinetix.shop"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kinetix.shop"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "HTTP_BASE",
            "\"$httpBaseConfig\""
        )
        buildConfigField(
            "String",
            "WS_BASE",
            "\"$wsBaseConfig\""
        )
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
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

    testOptions {
        unitTests.all {
            it.testLogging {
                events(
                    "passed",
                    "skipped",
                    "failed",
                )
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

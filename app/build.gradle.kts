import java.net.URI
import java.net.URISyntaxException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
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

ksp {
    arg("room.generateKotlin", "true")
    arg("room.schemaLocation", "$projectDir/schemas")
}

val verifyReleaseConfig = tasks.register("verifyReleaseConfig") {
    description = ""
    val httpBase = httpBaseConfig.trim()
    val wsBase = wsBaseConfig.trim()
    doLast {
        val problems = mutableListOf<String>()
        val entries = listOf(
            Triple("KINETIX_HTTP_BASE", httpBase, "https"),
            Triple("KINETIX_WS_BASE", wsBase, "wss"),
        )
        for ((key, raw, expectedScheme) in entries) {
            if (raw.isEmpty()) {
                problems.add("$key is missing")
                continue
            }
            val uri = try {
                URI(raw)
            } catch (_: URISyntaxException) {
                problems.add("$key is not a valid URI")
                continue
            }
            if (uri.scheme?.lowercase() != expectedScheme) {
                problems.add("$key must use $expectedScheme://")
            }
            if (uri.host.isNullOrBlank()) {
                problems.add("$key must include a host")
            }
            val path = uri.rawPath.orEmpty()
            if (path.isNotEmpty() && path != "/") {
                problems.add("$key must not contain a path")
            }
            if (uri.rawQuery != null || uri.rawFragment != null) {
                problems.add("$key must not contain query or fragment parameters")
            }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Invalid release configuration: ${problems.joinToString("; ")}")
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseConfig)
}

dependencyLocking {
    lockAllConfigurations()
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        all {
            val module = requested as? ModuleComponentSelector
            if (module != null && module.group == "org.bouncycastle" && module.version == "1.80.2") {
                useTarget("${module.group}:${module.module}:1.85", "Mitigate CVE-2026-8763")
            }
        }
    }
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
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.osmdroid.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

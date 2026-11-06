plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing material is injected by GitHub Actions from repository secrets.
// Locally (or on forked PRs) the values are absent, so release builds fall
// back to the debug signing config instead of failing configuration.
val signingKeystoreFile = System.getenv("SIGNING_KEYSTORE")?.takeIf { it.isNotBlank() }
val signingStorePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val signingKeyAlias = System.getenv("SIGNING_KEY_ALIAS")?.takeIf { it.isNotBlank() }
val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val hasReleaseSigning = signingKeystoreFile != null &&
    signingStorePassword != null && signingKeyAlias != null && signingKeyPassword != null &&
    file(signingKeystoreFile).isFile

android {
    namespace = "com.xingmihai.sukisu.themeeditor"
    // API 36 is the newest stable platform shipped by the Android SDK on CI.
    // Apps built against 36 run unchanged on Android 17 (API 37) devices.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xingmihai.sukisu.themeeditor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(signingKeystoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
                storeType = "JKS"
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // R8 code shrinking + unused resource shrinking (Compose keep rules
            // are merged automatically by AGP; see proguard-rules.pro).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

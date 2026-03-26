plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ndjc.app"
    compileSdk = (System.getenv("NDJC_COMPILE_SDK") ?: "34").toInt()

    defaultConfig {
        // NDJC-AUTO-APPID-START
applicationId = "com.ndjc.apps.niudaige.id12ab3c"
// NDJC-AUTO-APPID-END

        minSdk = (System.getenv("NDJC_MIN_SDK") ?: "24").toInt()
        targetSdk = (System.getenv("NDJC_TARGET_SDK") ?: "34").toInt()

        // NDJC-AUTO-VERSION-START
versionCode = 1
        versionName = "1.0.0"
// NDJC-AUTO-VERSION-END
    }

    // NDJC-AUTO-SIGNING-START
    signingConfigs {
        // Upload key for Google Play (Play App Signing)
        create("release") {
            val ksPath = System.getenv("NDJC_KEYSTORE_PATH") ?: ""
            if (ksPath.isNotBlank()) {
                storeFile = file(ksPath)
            }
            storePassword = System.getenv("NDJC_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("NDJC_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("NDJC_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        getByName("release") {
            // Keep stable for MVP; enable R8/Proguard later if needed
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    // NDJC-AUTO-SIGNING-END

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // 仅 App 层控制可见：导航 Compose（提供 NavHostController 类型）
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-messaging")
    // 骨架 & 示例模块（装配用：UI 包不依赖它们）
    // NDJC-AUTO-DEPS-START
    implementation(project(":core-skeleton"))
    implementation(project(":feature-showcase"))
  
// NDJC-AUTO-DEPS-END
}

plugins {
    id("com.android.library")
    id("kotlin-kapt")
    kotlin("android")
}

android {
    namespace = "com.ndjc.feature.showcase"
    compileSdk = (System.getenv("NDJC_COMPILE_SDK") ?: "34").toInt()

    defaultConfig {
        minSdk = (System.getenv("NDJC_MIN_SDK") ?: "24").toInt()
    }
   kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // ✅ 与 m3-standard 对齐
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // ✅ 与 BOM 与骨架模块一致
    api(project(":core-skeleton"))

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // ✅ Firebase Cloud Messaging（逻辑模块内注册 token / 调 SDK）
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-messaging")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ✅ 正确的 M2 Material 依赖，里面包含 pullRefresh / PullRefreshIndicator 等 API
    implementation("androidx.compose.material:material")

    implementation("androidx.navigation:navigation-compose:2.8.3")

    // 依赖的 UI 包（其内部已有 api 暴露 compose 依赖）
}

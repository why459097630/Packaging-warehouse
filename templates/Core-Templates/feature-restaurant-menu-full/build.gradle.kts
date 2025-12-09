plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.ndjc.feature.restaurant"
    compileSdk = (System.getenv("NDJC_COMPILE_SDK") ?: "34").toInt()

    defaultConfig {
        minSdk = (System.getenv("NDJC_MIN_SDK") ?: "24").toInt()
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

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ✅ 正确的 M2 Material 依赖，里面包含 pullRefresh / PullRefreshIndicator 等 API
    implementation("androidx.compose.material:material")

    implementation("androidx.navigation:navigation-compose:2.8.3")

    // 依赖的 UI 包（其内部已有 api 暴露 compose 依赖）
    implementation(project(":ui-pack-neumorph"))
}

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.ndjc.core.skeleton"
    compileSdk = (System.getenv("NDJC_COMPILE_SDK") ?: "34").toInt()

    defaultConfig {
        minSdk = (System.getenv("NDJC_MIN_SDK") ?: "24").toInt()
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("com.google.code.gson:gson:2.11.0")

    // ▼ 新增：让 core-skeleton 可以直接使用 feature 模块里的 Screen
    // 如果以后在 CoreNavHost 里再接入更多模块，这里按需继续追加即可
}

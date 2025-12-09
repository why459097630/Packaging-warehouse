plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ndjc.ui.neu"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    kotlinOptions { jvmTarget = "17" }

    // ✅ 和 m3-standard 对齐：Java 17 编译参数
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // ✅ 和 m3-standard 对齐：用 api 暴露
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    api(composeBom)
    androidTestApi(composeBom)


    // Material3 / Compose UI
    api("androidx.compose.material3:material3")          // 含 tabIndicatorOffset
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.compose.material:material-icons-extended")
    api(project(":core-skeleton"))
    implementation("androidx.compose.material:material")
    implementation("io.coil-kt:coil-compose:2.1.0")

    // 可选：Demo/预览
    debugApi("androidx.compose.ui:ui-tooling")

    // 可选：动效/约束布局（按需保留）
    // api("androidx.compose.animation:animation")
    // api("androidx.constraintlayout:constraintlayout-compose:1.0.1")
}

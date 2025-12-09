// NDJC Packaging — Root build.gradle.kts
// Contract: 本工程遵循 NDJC Contract-UI v1（冻结版）进行构建与装配
// 目标：统一 Java/Kotlin 编译到 JDK 17，消除 “Inconsistent JVM-target” 问题

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    kotlin("android") version "1.9.24" apply false
}

/**
 * 统一所有子模块（:app / :core-skeleton / :ui-pack-* / :feature-*）的 JVM 目标：
 * - Kotlin 使用 JDK 17 toolchain & JVM target 17
 * - Android（Application/Library）compileOptions 统一为 Java 17
 */
subprojects {

    // ---- Kotlin: 统一到 JDK 17 / JVM target 17 ----
    plugins.withId("org.jetbrains.kotlin.android") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            jvmToolchain(17)
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
        }
    }

    // ---- Android Application: 统一 Java 17 ----
    plugins.withId("com.android.application") {
        // 注意：Kotlin DSL 的 configure 只传类型，不要再传 "android" 名称
        extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }

    // ---- Android Library: 统一 Java 17 ----
    plugins.withId("com.android.library") {
        extensions.configure<com.android.build.api.dsl.LibraryExtension> {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}

// 可在此追加统一的版本对齐/BOM/仓库等（如有需要）。
// 与契约相关的装配/机审逻辑按 Contract-UI v1 的 Slots/Routes/Tokens/Hooks 等约束在各模块与 assembly 清单中实现。

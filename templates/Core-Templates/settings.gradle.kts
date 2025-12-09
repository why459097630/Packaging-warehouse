pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NDJC-BaseSkeleton"

// NDJC-AUTO-INCLUDE-START
include(
    ":app",
    ":core-skeleton",
    "::feature-restaurant-menu-full",
    ":ui-pack-neumorph"
)
// NDJC-AUTO-INCLUDE-END

// 如果需同时保留旧 UI 包，对比用：把下一行取消注释即可
// include(":ui-pack-m3-standard")
// include(":feature-about-basic")

// ▲▲▲ 下图这些映射只有在模块目录不在根级时才需要，默认请保持注释。
// project(":ui-pack-neumorph").projectDir = file("ui-pack-neumorph")
// project(":ui-pack-m3-standard").projectDir = file("ui-pack-m3-standard")
// project(":feature-about-basic").projectDir = file("feature-about-basic")

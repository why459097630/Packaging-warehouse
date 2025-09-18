package com.ndjc.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 各类 SDK/初始化
        // NDJC:BLOCK:STARTUP
        // NDJC:BLOCK:STARTUP_INITIALIZERS

        // 从 BuildConfig 读入的文案集合（生成器会注入实际值）
        val startupLibs       = BuildConfig.STARTUP_LIBS        // NDJC:STARTUP_LIBS
        val startupInitializer= BuildConfig.STARTUP_INITIALIZERS // NDJC:STARTUP_INITIALIZERS
        @Suppress("UNUSED_VARIABLE")
        val _keepRefs = arrayOf(startupLibs, startupInitializer) // 防止 被 消掉

        // 埋点 / 崩溃 / 远程配置 / 开关 / 实验
        // NDJC:BLOCK:ANALYTICS
        // NDJC:BLOCK:CRASH
        // NDJC:BLOCK:REMOTE_CONFIG
        // NDJC:BLOCK:FEATURE_FLAGS
        // NDJC:BLOCK:AB_TEST

        // 依赖注入容器
        // NDJC:BLOCK:DI_CONTAINER
    }
}

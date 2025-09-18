package com.ndjc.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 各类 SDK/初始化
        // NDJC:ANALYTICS_SDK
        // NDJC:CRASH_SDK
        // NDJC:PUSH_SDK

        // NDJC:BLOCK:STARTUP
        // NDJC:BLOCK:STARTUP_INITIALIZERS

        // 从 BuildConfig 读入（保持引用避免被 R8 清理）
        val startupLibs        = BuildConfig.STARTUP_LIBS         // NDJC:STARTUP_LIBS
        val startupInitializers= BuildConfig.STARTUP_INITIALIZERS // NDJC:STARTUP_INITIALIZERS
        @Suppress("UNUSED_VARIABLE")
        val _keepRefs = arrayOf(startupLibs, startupInitializers)

        // 远程配置/开关/实验
        // NDJC:BLOCK:REMOTE_CONFIG
        // NDJC:BLOCK:FEATURE_FLAGS
        // NDJC:BLOCK:AB_TEST

        // 依赖注入容器
        // NDJC:DI_CONTAINER
        // NDJC:BLOCK:DI_CONTAINER
    }
}

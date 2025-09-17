package com.ndjc.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 各类 SDK/初始化
        // NDJC:BLOCK:STARTUP
        // NDJC:BLOCK:STARTUP_INITIALIZERS
        // NDJC:BLOCK:ANALYTICS
        // NDJC:BLOCK:CRASH
        // NDJC:BLOCK:REMOTE_CONFIG
        // NDJC:BLOCK:FEATURE_FLAGS
        // NDJC:BLOCK:AB_TEST
        // 依赖注入容器
        // NDJC:BLOCK:DI_CONTAINER
    }
}

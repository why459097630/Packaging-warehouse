package com.ndjc.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 各类 SDK / 初始化

        // 框架化启动钩子

        // 从 BuildConfig 读入（保持引用避免被 R8 清理）
        val startupLibs         = BuildConfig.STARTUP_LIBS          //
        val startupInitializers = BuildConfig.STARTUP_INITIALIZERS  //
        @Suppress("UNUSED_VARIABLE")
        val _keepRefs = arrayOf(startupLibs, startupInitializers)

        // 远程配置 / 开关 / 实验

        // 依赖注入容器

    }
}

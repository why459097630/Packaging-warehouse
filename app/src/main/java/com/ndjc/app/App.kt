package com.ndjc.app // com.xiutao.restaurant

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 这里可插 HOOK:AFTER_INSTALL 类似的初始化逻辑（如需一次性任务可加标记）
    }
}

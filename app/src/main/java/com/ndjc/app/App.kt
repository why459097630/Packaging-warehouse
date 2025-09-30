package com.ndjc.app // com.ndjc.demo.core

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // 这里可插  类似的初始化逻辑（如需一次性任务可加标记）
    }
}

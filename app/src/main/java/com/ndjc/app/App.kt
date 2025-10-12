package com.ndjc.app // com.txrestaurant.menu

import android.app.Application
import com.ndjc.app.data.AppCtx

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 将 Application 上下文注入到全局容器，供仓库/工具类获取资源等
        AppCtx.app = this

        // HOOK:AFTER_INSTALL
        // END_HOOK
    }
}

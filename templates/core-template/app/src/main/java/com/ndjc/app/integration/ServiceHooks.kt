package com.ndjc.app.integration

// NDJC:INTEGRATION_IMPORTS

object ServiceHooks {
    fun initOnAppStart(/* ctx: Context */) {
        // NDJC:INTEGRATION_INIT_ON_START   // 各 SDK 初始化、日志开关、加固动态配置…
    }

    fun onUserLogin(/* uid: String */) {
        // NDJC:INTEGRATION_USER_LOGIN
    }

    fun onUserLogout() {
        // NDJC:INTEGRATION_USER_LOGOUT
    }

    // NDJC:INTEGRATION_EXTRA_FUNCS
}

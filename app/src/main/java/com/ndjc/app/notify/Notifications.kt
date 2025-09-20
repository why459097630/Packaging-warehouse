package com.ndjc.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object Notifications {
    // ===== 文本锚点（供生成器注入） =====

    // ===== 业务锚点（落位点） =====

    @Suppress("unused")
    fun ensureDefaultChannel(context: Context) {
        // 这里留空，生成器可在 NOTIFICATION_CHANNELS 锚点处注入具体实现
        // 例如：
        // val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // val ch = NotificationChannel(BuildConfig.CHANNEL_ID, BuildConfig.CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        // mgr.createNotificationChannel(ch)
    }
}

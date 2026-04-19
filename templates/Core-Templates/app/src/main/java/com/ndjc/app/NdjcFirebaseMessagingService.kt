package com.ndjc.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ndjc.feature.showcase.ShowcaseChatCloudRepository
import com.ndjc.feature.showcase.ShowcaseChatRepository
import com.ndjc.feature.showcase.ShowcaseCloudConfig
import com.ndjc.feature.showcase.ShowcaseCloudRepository
import com.ndjc.feature.showcase.ShowcaseRuntimeState
import com.ndjc.feature.showcase.ShowcaseStoreSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class NdjcFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("NDJC_PUSH", "FCM token=$token")

        val freshToken = token.trim()
        if (freshToken.isBlank()) {
            Log.e("NDJC_PUSH", "onNewToken ignored because token is blank")
            return
        }

        serviceScope.launch {
            try {
                val repository = ShowcaseCloudRepository()
                val storeId = ShowcaseStoreSession.requireStoreId()

                val announcementOk = repository.upsertPushDevice(
                    ShowcaseCloudRepository.PushDeviceUpsert(
                        storeId = storeId,
                        audience = "announcement_subscriber",
                        token = freshToken,
                        conversationId = null,
                        clientId = null,
                        appVersion = null
                    )
                )
                Log.d("NDJC_PUSH", "onNewToken announcement_subscriber upsert result=$announcementOk")

                if (ShowcaseStoreSession.isMerchantLoggedIn()) {
                    val merchantOk = repository.upsertPushDevice(
                        ShowcaseCloudRepository.PushDeviceUpsert(
                            storeId = storeId,
                            audience = "chat_merchant",
                            token = freshToken,
                            conversationId = null,
                            clientId = null,
                            appVersion = null
                        )
                    )
                    Log.d("NDJC_PUSH", "onNewToken chat_merchant upsert result=$merchantOk")
                }
            } catch (t: Throwable) {
                Log.e("NDJC_PUSH", "onNewToken re-register failed", t)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val payloadTitle = message.data["title"]
            ?: message.notification?.title
            ?: "NDJC"

        val body = message.data["body"]
            ?: message.notification?.body
            ?: "You have a new update"

        val pushType = message.data["type"]
            ?: message.data["push_type"]

        val conversationId = message.data["conversation_id"]
            ?: message.data["conversationId"]

        val announcementId = message.data["announcement_id"]
            ?: message.data["announcementId"]

        val openAs = message.data["open_as"]
            ?.trim()
            ?.lowercase()

        val normalizedPushType = pushType?.trim()?.lowercase()
        val normalizedConversationId = conversationId?.trim()

        // ✅ 当前前台正在看同一条 chat 会话，或者刚刚看完这条会话且仍在宽限窗口内：直接吞掉，不展示推送条
        if (normalizedPushType == "chat" &&
            ShowcaseRuntimeState.shouldSuppressChatPush(normalizedConversationId)
        ) {
            Log.d(
                "NDJC_PUSH",
                "suppress chat push conversationId=$normalizedConversationId"
            )
            return
        }

        createChannelIfNeeded()

        val finalTitle = runBlocking {
            resolveNotificationTitle(
                payloadTitle = payloadTitle,
                pushType = pushType,
                conversationId = conversationId,
                openAs = openAs
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("push_type", pushType)
            putExtra("conversation_id", conversationId)
            putExtra("announcement_id", announcementId)
            putExtra("open_as", openAs)
        }

        val requestCode = (
                (pushType ?: "") +
                        "|" +
                        (conversationId ?: "") +
                        "|" +
                        (announcementId ?: "") +
                        "|" +
                        System.currentTimeMillis().toString()
                ).hashCode()

        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon =
            when (pushType?.trim()?.lowercase()) {
                "announcement" -> com.ndjc.feature.showcase.R.drawable.ic_stat_announcement
                "chat" -> com.ndjc.feature.showcase.R.drawable.ic_stat_chat
                else -> com.ndjc.feature.showcase.R.drawable.ic_stat_chat
            }

        val notification = NotificationCompat.Builder(this, "ndjc_general_push")
            .setSmallIcon(smallIcon)
            .setContentTitle(finalTitle)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val hasPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            NotificationManagerCompat.from(this)
                .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
        }
    }

    private suspend fun resolveNotificationTitle(
        payloadTitle: String,
        pushType: String?,
        conversationId: String?,
        openAs: String?
    ): String = withContext(Dispatchers.IO) {
        val normalizedPushType = pushType?.trim()?.lowercase().orEmpty()
        val normalizedOpenAs = openAs?.trim()?.lowercase().orEmpty()
        val normalizedConversationId = conversationId?.trim().orEmpty()

        if (normalizedPushType != "chat") {
            return@withContext payloadTitle
        }

        if (normalizedOpenAs != "merchant") {
            return@withContext payloadTitle
        }

        if (normalizedConversationId.isBlank()) {
            return@withContext payloadTitle
        }

        return@withContext runCatching {
            val storeId = ShowcaseStoreSession.requireStoreId()
            val repository = ShowcaseChatRepository(ShowcaseChatCloudRepository())
            repository.resolveMerchantThreadPushDisplayName(
                context = applicationContext,
                storeId = storeId,
                conversationId = normalizedConversationId
            )
        }.getOrDefault(payloadTitle)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "ndjc_general_push",
            "General Push",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Chat and announcement notifications"
        }
        manager.createNotificationChannel(channel)
    }
}
package com.ndjc.feature.showcase

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShowcasePushRoute(
    val pushType: String,
    val conversationId: String?,
    val announcementId: String?,
    val openAs: String?
)

object ShowcasePushRouter {

    private val _pendingRoute = MutableStateFlow<ShowcasePushRoute?>(null)
    val pendingRoute: StateFlow<ShowcasePushRoute?> = _pendingRoute.asStateFlow()

    fun dispatchFromIntent(intent: Intent?) {
        val route = parseIntent(intent) ?: return
        _pendingRoute.value = route

        intent?.removeExtra("push_type")
        intent?.removeExtra("conversation_id")
        intent?.removeExtra("announcement_id")
        intent?.removeExtra("open_as")
    }

    fun consume(route: ShowcasePushRoute?) {
        if (_pendingRoute.value == route) {
            _pendingRoute.value = null
        }
    }

    private fun parseIntent(intent: Intent?): ShowcasePushRoute? {
        val pushType = intent?.getStringExtra("push_type")?.trim()?.lowercase().orEmpty()
        val conversationId = intent?.getStringExtra("conversation_id")?.trim()?.ifBlank { null }
        val announcementId = intent?.getStringExtra("announcement_id")?.trim()?.ifBlank { null }
        val openAs = intent?.getStringExtra("open_as")?.trim()?.lowercase()?.ifBlank { null }

        if (pushType.isBlank()) return null

        return ShowcasePushRoute(
            pushType = pushType,
            conversationId = conversationId,
            announcementId = announcementId,
            openAs = openAs
        )
    }
}
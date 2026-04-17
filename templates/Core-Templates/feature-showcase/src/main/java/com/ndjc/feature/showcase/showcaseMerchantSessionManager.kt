package com.ndjc.feature.showcase

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ShowcaseMerchantSessionManager {
    private const val PREF_NAME = "Showcase_admin_prefs"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_ADMIN_LOGGED_IN = "admin_logged_in"
    private const val KEY_MERCHANT_LOGIN_NAME = "merchant_login_name"
    private const val KEY_MERCHANT_ACCESS_TOKEN = "merchant_access_token"
    private const val KEY_MERCHANT_REFRESH_TOKEN = "merchant_refresh_token"
    private const val KEY_MERCHANT_AUTH_USER_ID = "merchant_auth_user_id"
    private const val KEY_MERCHANT_EXPIRES_AT = "merchant_expires_at"

    private val refreshLock = Any()

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun persistCurrentSession() {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_MERCHANT_LOGIN_NAME, ShowcaseStoreSession.currentMerchantLoginName())
                .putString(KEY_MERCHANT_ACCESS_TOKEN, ShowcaseStoreSession.currentMerchantAccessToken())
                .putString(KEY_MERCHANT_REFRESH_TOKEN, ShowcaseStoreSession.currentMerchantRefreshToken())
                .putString(KEY_MERCHANT_AUTH_USER_ID, ShowcaseStoreSession.currentMerchantAuthUserId())
                .putLong(KEY_MERCHANT_EXPIRES_AT, ShowcaseStoreSession.currentMerchantExpiresAt() ?: 0L)
                .putBoolean(KEY_ADMIN_LOGGED_IN, true)
                .apply()
        } catch (t: Throwable) {
            Log.e("ShowcaseSession", "persistCurrentSession failed", t)
        }
    }

    fun clearPersistedSession(clearRememberMe: Boolean) {
        val context = appContext ?: return
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
                .remove(KEY_MERCHANT_LOGIN_NAME)
                .remove(KEY_MERCHANT_ACCESS_TOKEN)
                .remove(KEY_MERCHANT_REFRESH_TOKEN)
                .remove(KEY_MERCHANT_AUTH_USER_ID)
                .remove(KEY_MERCHANT_EXPIRES_AT)
                .putBoolean(KEY_ADMIN_LOGGED_IN, false)

            if (clearRememberMe) {
                editor.putBoolean(KEY_REMEMBER_ME, false)
            }

            editor.apply()
        } catch (t: Throwable) {
            Log.e("ShowcaseSession", "clearPersistedSession failed", t)
        }
    }

    fun restoreSessionFromDisk(): Boolean {
        val context = appContext ?: return false
        return try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
            val loggedIn = prefs.getBoolean(KEY_ADMIN_LOGGED_IN, false)
            val loginName = prefs.getString(KEY_MERCHANT_LOGIN_NAME, null)?.trim().orEmpty()
            val accessToken = prefs.getString(KEY_MERCHANT_ACCESS_TOKEN, null)?.trim().orEmpty()
            val refreshToken = prefs.getString(KEY_MERCHANT_REFRESH_TOKEN, null)?.trim()?.ifBlank { null }
            val authUserId = prefs.getString(KEY_MERCHANT_AUTH_USER_ID, null)?.trim().orEmpty()
            val expiresAt = prefs.getLong(KEY_MERCHANT_EXPIRES_AT, 0L)

            val canResume = rememberMe &&
                    loggedIn &&
                    loginName.isNotBlank() &&
                    accessToken.isNotBlank() &&
                    authUserId.isNotBlank()

            if (canResume) {
                ShowcaseStoreSession.setMerchantSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    authUserId = authUserId,
                    loginName = loginName,
                    expiresAt = expiresAt
                )
                true
            } else {
                ShowcaseStoreSession.clearMerchantSession()
                false
            }
        } catch (t: Throwable) {
            Log.e("ShowcaseSession", "restoreSessionFromDisk failed", t)
            false
        }
    }

    fun ensureValidMerchantAccessToken(): String? {
        synchronized(refreshLock) {
            val currentToken = ShowcaseStoreSession.currentMerchantAccessToken()?.trim().orEmpty()
            if (currentToken.isNotBlank() && !ShowcaseStoreSession.shouldRefresh()) {
                return currentToken
            }
            return refreshMerchantSessionLocked()
        }
    }

    fun forceRefreshMerchantSession(): Boolean {
        synchronized(refreshLock) {
            return !refreshMerchantSessionLocked().isNullOrBlank()
        }
    }

    private fun refreshMerchantSessionLocked(): String? {
        val refreshToken = ShowcaseStoreSession.currentMerchantRefreshToken()?.trim().orEmpty()
        val loginName = ShowcaseStoreSession.currentMerchantLoginName()?.trim().orEmpty()
        val currentAuthUserId = ShowcaseStoreSession.currentMerchantAuthUserId()?.trim().orEmpty()

        if (refreshToken.isBlank()) {
            Log.w("ShowcaseSession", "refreshMerchantSessionLocked: refresh token is blank")
            return null
        }

        return try {
            val url = ShowcaseCloudConfig.authUrl("token?grant_type=refresh_token")
            val payload = JSONObject().apply {
                put("refresh_token", refreshToken)
            }.toString()

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
                setRequestProperty("apikey", ShowcaseCloudConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${ShowcaseCloudConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val body = readBody(conn, code)

            if (code !in 200..299 || body.isNullOrBlank()) {
                Log.w("ShowcaseSession", "refreshMerchantSessionLocked failed code=$code body=$body")
                return null
            }

            val obj = JSONObject(body)
            val newAccessToken = obj.optString("access_token", "").trim()
            val newRefreshToken = obj.optString("refresh_token", "").trim().ifBlank { refreshToken }
            val expiresIn = obj.optLong("expires_in", 3600L).coerceAtLeast(1L)
            val expiresAt = (System.currentTimeMillis() / 1000L) + expiresIn
            val newAuthUserId = obj.optJSONObject("user")
                ?.optString("id", "")
                ?.trim()
                .orEmpty()
                .ifBlank { currentAuthUserId }

            if (newAccessToken.isBlank() || newAuthUserId.isBlank()) {
                Log.w("ShowcaseSession", "refreshMerchantSessionLocked invalid payload")
                return null
            }

            ShowcaseStoreSession.setMerchantSession(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                authUserId = newAuthUserId,
                loginName = loginName,
                expiresAt = expiresAt
            )

            persistCurrentSession()
            Log.d("ShowcaseSession", "refreshMerchantSessionLocked success expiresAt=$expiresAt")
            newAccessToken
        } catch (t: Throwable) {
            Log.e("ShowcaseSession", "refreshMerchantSessionLocked failed", t)
            null
        }
    }

    private fun readBody(conn: HttpURLConnection, code: Int): String? {
        return try {
            (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
        } catch (_: Throwable) {
            null
        }
    }
}
package com.ndjc.feature.showcase

object ShowcaseStoreSession {
    private const val FIXED_TEST_STORE_ID = "store_test_001"

    @Volatile
    private var merchantAccessToken: String? = null

    @Volatile
    private var merchantRefreshToken: String? = null

    @Volatile
    private var merchantAuthUserId: String? = null

    @Volatile
    private var merchantLoginName: String? = null

    @Volatile
    private var merchantExpiresAt: Long? = null

    fun currentStoreId(): String {
        return FIXED_TEST_STORE_ID
    }

    fun requireStoreId(): String {
        val value = currentStoreId().trim()
        require(value.isNotBlank()) { "storeId is required" }
        return value
    }

    fun setMerchantSession(
        accessToken: String,
        refreshToken: String?,
        authUserId: String,
        loginName: String,
        expiresAt: Long
    ) {
        merchantAccessToken = accessToken.trim()
        merchantRefreshToken = refreshToken?.trim()
        merchantAuthUserId = authUserId.trim()
        merchantLoginName = loginName.trim()
        merchantExpiresAt = expiresAt
    }

    fun clearMerchantSession() {
        merchantAccessToken = null
        merchantRefreshToken = null
        merchantAuthUserId = null
        merchantLoginName = null
        merchantExpiresAt = null
    }

    fun isMerchantLoggedIn(): Boolean {
        return !merchantAccessToken.isNullOrBlank() && !merchantAuthUserId.isNullOrBlank()
    }

    fun requireMerchantAccessToken(): String {
        val value = merchantAccessToken?.trim().orEmpty()
        require(value.isNotBlank()) { "Merchant access token is required" }
        return value
    }

    fun currentMerchantAccessToken(): String? {
        return merchantAccessToken?.trim()
    }

    fun currentMerchantRefreshToken(): String? {
        return merchantRefreshToken?.trim()
    }

    fun currentMerchantAuthUserId(): String? {
        return merchantAuthUserId?.trim()
    }

    fun currentMerchantLoginName(): String? {
        return merchantLoginName?.trim()
    }

    fun currentMerchantExpiresAt(): Long? {
        return merchantExpiresAt
    }

    fun shouldRefresh(): Boolean {
        val expiresAt = merchantExpiresAt ?: return true
        val now = System.currentTimeMillis() / 1000L
        return now >= (expiresAt - 120L)
    }

    fun updateMerchantLoginName(loginName: String) {
        merchantLoginName = loginName.trim()
    }
}
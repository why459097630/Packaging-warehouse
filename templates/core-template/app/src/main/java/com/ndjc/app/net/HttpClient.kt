package com.ndjc.app.net

import com.ndjc.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpClient {

    // 基础配置来自 BuildConfig（文本锚点）
    private const val BASE_URL: String = BuildConfig.API_BASE                 // NDJC:HTTP_BASE_URL
    private const val TIMEOUT_MS: Long = BuildConfig.HTTP_TIMEOUT_MS.toLong() // NDJC:HTTP_TIMEOUT
    private const val LOG_LEVEL: Int   = BuildConfig.HTTP_LOG_LEVEL           // NDJC:HTTP_LOG_LEVEL

    val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = when (LOG_LEVEL) {
                0 -> HttpLoggingInterceptor.Level.NONE
                1 -> HttpLoggingInterceptor.Level.BASIC
                2 -> HttpLoggingInterceptor.Level.HEADERS
                else -> HttpLoggingInterceptor.Level.BODY
            }
            // NDJC:BLOCK:ERROR_MAPPER
        }

        OkHttpClient.Builder()
            .addInterceptor(logger)
            // 自定义网络拦截器等
            // NDJC:BLOCK:NETWORK_INTERCEPTORS
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            // NDJC:BLOCK:RETRY_POLICY
            // NDJC:BLOCK:CACHE_POLICY
            .build()
    }

    // 统一返回包装
    // NDJC:BLOCK:RESULT_WRAPPER

    // Service 定义处
    // NDJC:BLOCK:HTTP_CLIENT

    @Suppress("unused")
    fun baseUrl(): String = BASE_URL
}

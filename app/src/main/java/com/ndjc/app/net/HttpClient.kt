package com.ndjc.app.net

import com.ndjc.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpClient {

    // 基础配置来自 BuildConfig（文本锚点）
    private const val BASE_URL: String = BuildConfig.API_BASE                 //
    private const val TIMEOUT_MS: Long = BuildConfig.HTTP_TIMEOUT_MS.toLong() //
    private const val LOG_LEVEL: Int   = BuildConfig.HTTP_LOG_LEVEL           //

    val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = when (LOG_LEVEL) {
                0 -> HttpLoggingInterceptor.Level.NONE
                1 -> HttpLoggingInterceptor.Level.BASIC
                2 -> HttpLoggingInterceptor.Level.HEADERS
                else -> HttpLoggingInterceptor.Level.BODY
            }

        }

        OkHttpClient.Builder()
            .addInterceptor(logger)
            // 自定义网络拦截器等

            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)

            .build()
    }

    // 统一返回包装

    // Service 定义处

    @Suppress("unused")
    fun baseUrl(): String = BASE_URL
}

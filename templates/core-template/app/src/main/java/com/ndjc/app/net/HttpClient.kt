package com.ndjc.app.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object HttpClient {
    val client: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            // NDJC:BLOCK:ERROR_MAPPER
        }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            // NDJC:BLOCK:NETWORK_INTERCEPTORS
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            // NDJC:BLOCK:RETRY_POLICY
            // NDJC:BLOCK:CACHE_POLICY
            .build()
    }

    // 统一返回包装
    // NDJC:BLOCK:RESULT_WRAPPER
    // Service 定义处
    // NDJC:BLOCK:HTTP_CLIENT
}

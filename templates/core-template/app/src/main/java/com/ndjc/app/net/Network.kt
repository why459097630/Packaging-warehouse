package com.ndjc.app.net

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Network {

  // 文本锚点：基础域名
  val baseUrl = /* NDJC:BASE_URL */ "https://api.example.com/" /* NDJC:END */

  // 拦截器插槽
  private fun okHttp(): OkHttpClient = OkHttpClient.Builder()
    // NDJC:BLOCK(OKHTTP_INTERCEPTORS)
    // .addInterceptor(YourInterceptor())
    // NDJC:END
    .build()

  fun retrofit(): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    // 头/适配器扩展
    // NDJC:SERDE_MODULES
    .client(okHttp())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
}

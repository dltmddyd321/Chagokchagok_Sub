package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val TIMEOUT_SECONDS = 20L

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // 서버 전체가 Basic 인증. 계정은 local.properties → BuildConfig
            .apply {
                if (BuildConfig.CHAGOK_API_USERNAME.isNotBlank()) {
                    val credential = Credentials.basic(BuildConfig.CHAGOK_API_USERNAME, BuildConfig.CHAGOK_API_PASSWORD)
                    addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().header("Authorization", credential).build())
                    }
                }
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactHeader("Authorization") // 로그에 계정이 남지 않게
                        },
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl(BuildConfig.CHAGOK_API_BASE_URL))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val chagokApi: ChagokApi by lazy { retrofit.create(ChagokApi::class.java) }

    /**
     * Retrofit은 baseUrl이 '/'로 끝나야 해서 없으면 붙인다.
     * 경로는 ChagokApi에서 "api/v1/..." (앞 '/' 없이) → https://chagokchagok-api.duckdns.org/api/v1/plans
     */
    internal fun baseUrl(raw: String): String = raw.trim().let { if (it.endsWith("/")) it else "$it/" }
}

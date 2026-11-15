package com.windrr.hyundaesub.ui.component

import android.content.Context
import coil.ImageLoader
import com.windrr.hyundaesub.data.remote.ApiClient

/** 짐 사진용 Coil 로더. 서버 사진(photo_url)은 팀 인증이 필요해서 서버와 같은 OkHttp(Basic 인증)를 쓴다. 기기 사진(file://)도 그대로 열림 */
object ChagokImageLoader {

    @Volatile
    private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .okHttpClient { ApiClient.okHttpClient }
            .crossfade(true)
            .build()
            .also { loader = it }
    }
}

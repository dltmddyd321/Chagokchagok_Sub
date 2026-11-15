package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import com.windrr.hyundaesub.data.remote.model.ItemCreateRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO

/**
 * 실제 서버: 내 짐 등록 → 사진 올리기 → 목록 photo_url → 사진 열기(팀 인증).
 * ⚠ 서버에 "[테스트] 사진 업로드" 내 짐이 남는다 (삭제 API 없음) → CHAGOK_RUN_ITEM_FLOW=1 일 때만.
 * ./gradlew :app:testDebugUnitTest --tests "*ItemPhotoApiLiveTest*"
 */
class ItemPhotoApiLiveTest {

    @Test
    fun itemPhotoFlow_onRealServer() = runBlocking {
        assumeTrue("서버 계정이 local.properties에 없어 건너뜀", BuildConfig.CHAGOK_API_USERNAME.isNotBlank())
        assumeTrue("CHAGOK_RUN_ITEM_FLOW=1 일 때만 (서버에 내 짐이 남음)", System.getenv("CHAGOK_RUN_ITEM_FLOW") == "1")
        val api = ApiClient.chagokApi

        val created = apiCall { api.createItem(UUID.randomUUID().toString(), ItemCreateRequest("[테스트] 사진 업로드", 40.0, 30.0, 20.0, dimensionSource = "AR")) }
        println("POST items → $created")
        val id = (created as ApiResult.Success).data.savedItemId

        val jpeg = ByteArrayOutputStream().also { ImageIO.write(BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB), "jpg", it) }.toByteArray()
        val photo = apiCall { api.uploadItemPhoto(UUID.randomUUID().toString(), id, jpeg.toRequestBody("image/jpeg".toMediaType())) }
        println("PUT items/$id/photo → $photo")
        val photoUrl = (photo as ApiResult.Success).data.photoUrl

        val listed = (apiCall { api.getItems() } as ApiResult.Success).data.items.orEmpty().firstOrNull { it.savedItemId == id }
        println("GET items → photo_url=${listed?.photoUrl}")
        assertNotNull(listed?.photoUrl)

        val url = ApiClient.absoluteUrl(photoUrl)!!
        ApiClient.okHttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            println("GET $url → ${response.code} ${response.header("Content-Type")} ${response.body?.bytes()?.size}B")
            assertEquals(200, response.code)
            assertTrue(response.header("Content-Type").orEmpty().startsWith("image/"))
        }
    }
}

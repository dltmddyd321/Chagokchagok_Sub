package com.windrr.hyundaesub.data.remote

import android.content.Context
import android.net.Uri
import com.windrr.hyundaesub.data.remote.model.ItemCreateRequest
import com.windrr.hyundaesub.data.remote.model.ItemListResponse
import com.windrr.hyundaesub.data.remote.model.ItemPhotoResponse
import com.windrr.hyundaesub.data.remote.model.ItemResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

/** 내 짐 API: 등록 → 사진 올리기, 목록 */
class ItemRemoteRepository(private val api: ChagokApi = ApiClient.chagokApi) {

    suspend fun create(request: ItemCreateRequest, idempotencyKey: String = UUID.randomUUID().toString()): ApiResult<ItemResponse> =
        apiCall { api.createItem(idempotencyKey, request) }

    suspend fun list(): ApiResult<ItemListResponse> = apiCall { api.getItems() }

    /**
     * [photoUri](file://)의 JPEG/PNG 바이트를 올린다. 파일을 못 읽으면 null.
     * 서버 한도: 5 MiB, 2천만 픽셀 (앱 사진은 긴 변 1440px JPEG라 한참 작다)
     */
    suspend fun uploadPhoto(
        context: Context,
        savedItemId: String,
        photoUri: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ApiResult<ItemPhotoResponse>? {
        val bytes = runCatching {
            context.contentResolver.openInputStream(Uri.parse(photoUri))!!.use { it.readBytes() }
        }.getOrNull() ?: return null
        val type = if (photoUri.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
        return apiCall { api.uploadItemPhoto(idempotencyKey, savedItemId, bytes.toRequestBody(type.toMediaType())) }
    }
}

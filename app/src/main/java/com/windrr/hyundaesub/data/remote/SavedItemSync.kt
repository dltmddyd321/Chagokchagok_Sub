package com.windrr.hyundaesub.data.remote

import android.content.Context
import android.util.Log
import com.windrr.hyundaesub.data.local.AppDatabase
import com.windrr.hyundaesub.data.local.entity.DimensionSource
import com.windrr.hyundaesub.data.local.entity.Orientation
import com.windrr.hyundaesub.data.local.entity.SavedItemEntity
import com.windrr.hyundaesub.data.remote.model.ItemCreateRequest
import com.windrr.hyundaesub.data.remote.model.ItemResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import kotlin.math.roundToInt

/**
 * 내 짐을 서버와 맞춘다.
 * - 등록: POST /items → 서버 saved_item_id를 기기 내 짐 id로 쓴다 (배치 요청에 saved_item_id로 보낼 수 있게)
 * - 사진: 등록 뒤 PUT /items/{id}/photo. 화면을 떠나도 끝나도록 앱 범위에서 올린다
 * - 목록: GET /items로 서버에만 있는 내 짐을 기기에 채우고, 사진 경로(photo_url)를 갱신
 * 사용자 동작 한 번에 요청 한 번씩, 실패해도 자동으로 다시 보내지 않는다 (기기에는 그대로 남음).
 */
object SavedItemSync {

    private const val TAG = "SavedItemSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val remote = ItemRemoteRepository()

    /** 서버 등록. 성공하면 서버 응답, 실패하면 null (기기에만 저장) */
    suspend fun register(item: SavedItemEntity): ItemResponse? {
        val request = ItemCreateRequest(
            name = item.name,
            widthCm = item.widthCm.toDouble(),
            depthCm = item.depthCm.toDouble(),
            heightCm = item.heightCm.toDouble(),
            defaultQuantity = item.defaultQuantity,
            allowedOrientations = item.allowedOrientations.map(Orientation::name).takeIf { it.size < Orientation.entries.size },
            dimensionSource = when (item.dimensionSource) {
                DimensionSource.AR -> "AR"
                DimensionSource.MANUAL -> "MANUAL"
            },
        )
        return when (val result = remote.create(request)) {
            is ApiResult.Success -> result.data
            else -> {
                Log.w(TAG, "내 짐 등록 실패 (기기에만 저장): $result")
                null
            }
        }
    }

    /** 사진 올리기 (백그라운드). 성공하면 서버 사진 경로 저장 */
    fun uploadPhotoLater(context: Context, savedItemId: String, photoUri: String) {
        val app = context.applicationContext
        scope.launch {
            when (val result = remote.uploadPhoto(app, savedItemId, photoUri)) {
                is ApiResult.Success -> AppDatabase.getInstance(app).savedItemDao().updatePhotoUrl(savedItemId, result.data.photoUrl)
                null -> Log.w(TAG, "사진 파일을 읽지 못함: $photoUri")
                else -> Log.w(TAG, "사진 업로드 실패: $result")
            }
        }
    }

    /** 서버 내 짐 목록을 기기에 반영 (서버에만 있으면 추가, 있으면 사진 경로만 갱신). 기기 사진·수량은 그대로 */
    suspend fun pull(context: Context) {
        val dao = AppDatabase.getInstance(context).savedItemDao()
        val result = remote.list()
        if (result !is ApiResult.Success) {
            Log.w(TAG, "내 짐 목록 조회 실패: $result")
            return
        }
        val items = result.data.items.orEmpty()
        val local = dao.all().associateBy { it.savedItemId }
        val merged = items.map { server ->
            local[server.savedItemId]?.copy(serverSynced = true, photoUrl = server.photoUrl) ?: SavedItemEntity(
                savedItemId = server.savedItemId,
                name = server.name,
                widthCm = server.widthCm.roundToInt(),
                depthCm = server.depthCm.roundToInt(),
                heightCm = server.heightCm.roundToInt(),
                defaultQuantity = server.defaultQuantity ?: 1,
                allowedOrientations = server.allowedOrientations.orEmpty()
                    .mapNotNull { code -> Orientation.entries.firstOrNull { it.name == code } }
                    .ifEmpty { Orientation.ALL },
                dimensionSource = if (server.dimensionSource == "AR") DimensionSource.AR else DimensionSource.MANUAL,
                createdAt = server.createdAt?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
                    ?: System.currentTimeMillis(),
                serverSynced = true,
                photoUrl = server.photoUrl,
            )
        }
        if (merged.isNotEmpty()) dao.upsert(*merged.toTypedArray())
    }
}

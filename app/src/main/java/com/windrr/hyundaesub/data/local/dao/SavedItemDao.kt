package com.windrr.hyundaesub.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.windrr.hyundaesub.data.local.entity.SavedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {

    /** 최근 등록 순 */
    @Query("SELECT * FROM saved_item ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SavedItemEntity>>

    @Query("SELECT * FROM saved_item WHERE saved_item_id = :savedItemId")
    suspend fun get(savedItemId: String): SavedItemEntity?

    @Query("SELECT COUNT(*) FROM saved_item")
    suspend fun count(): Int

    /** 서버 목록 동기화·수정 모두 같은 id면 덮어쓴다 */
    @Upsert
    suspend fun upsert(vararg items: SavedItemEntity)

    @Query("UPDATE saved_item SET photo_url = :photoUrl WHERE saved_item_id = :savedItemId")
    suspend fun updatePhotoUrl(savedItemId: String, photoUrl: String?)

    @Query("SELECT * FROM saved_item")
    suspend fun all(): List<SavedItemEntity>

    @Query("DELETE FROM saved_item WHERE saved_item_id = :savedItemId")
    suspend fun delete(savedItemId: String)
}

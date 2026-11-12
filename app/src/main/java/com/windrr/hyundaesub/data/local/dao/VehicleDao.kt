package com.windrr.hyundaesub.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.windrr.hyundaesub.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class VehicleDao {

    @Upsert
    abstract suspend fun upsert(vararg vehicles: VehicleEntity)

    @Query("SELECT * FROM vehicle WHERE vehicle_id = :vehicleId")
    abstract suspend fun get(vehicleId: String): VehicleEntity?

    /** 대표 차량 먼저, 나머지는 이름순 */
    @Query("SELECT * FROM vehicle ORDER BY is_main DESC, name")
    abstract fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicle WHERE is_main = 1 LIMIT 1")
    abstract fun observeMain(): Flow<VehicleEntity?>

    /** 대표 차량을 바꾼다. 나머지는 모두 대표 해제 */
    @Transaction
    open suspend fun setMain(vehicleId: String) {
        clearMain()
        markMain(vehicleId)
    }

    @Query("UPDATE vehicle SET is_main = 0 WHERE is_main = 1")
    protected abstract suspend fun clearMain()

    @Query("UPDATE vehicle SET is_main = 1 WHERE vehicle_id = :vehicleId")
    protected abstract suspend fun markMain(vehicleId: String)
}

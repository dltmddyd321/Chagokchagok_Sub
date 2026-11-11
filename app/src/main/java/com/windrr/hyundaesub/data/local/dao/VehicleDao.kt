package com.windrr.hyundaesub.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.windrr.hyundaesub.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Upsert
    suspend fun upsert(vararg vehicles: VehicleEntity)

    @Query("SELECT * FROM vehicle WHERE vehicle_id = :vehicleId")
    suspend fun get(vehicleId: String): VehicleEntity?

    @Query("SELECT * FROM vehicle ORDER BY name")
    fun observeAll(): Flow<List<VehicleEntity>>
}

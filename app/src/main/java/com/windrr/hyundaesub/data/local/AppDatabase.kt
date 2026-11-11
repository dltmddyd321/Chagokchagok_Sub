package com.windrr.hyundaesub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windrr.hyundaesub.data.local.dao.PlanDao
import com.windrr.hyundaesub.data.local.dao.VehicleDao
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        PlanEntity::class,
        PlanItemEntity::class,
        PlanCarSeatEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    abstract fun planDao(): PlanDao

    companion object {
        private const val NAME = "load_plan.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                    .addCallback(SeedVehicles)
                    .build()
                    .also { instance = it }
            }
    }
}

/** 1차 프로토타입 지원 차량 */
object DefaultVehicles {
    val AVANTE = VehicleEntity(vehicleId = "avante_cn8", name = "5인승 세단 · 아반떼")
    val ALL = listOf(AVANTE)
}

/** DB 최초 생성 시 기본 차량을 넣는다 */
private object SeedVehicles : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultVehicles.ALL.forEach {
            db.execSQL("INSERT OR IGNORE INTO vehicle (vehicle_id, name) VALUES (?, ?)", arrayOf(it.vehicleId, it.name))
        }
    }
}

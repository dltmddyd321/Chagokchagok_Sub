package com.windrr.hyundaesub.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windrr.hyundaesub.data.local.converter.Converters
import com.windrr.hyundaesub.data.local.dao.CalculationDao
import com.windrr.hyundaesub.data.local.dao.PlanDao
import com.windrr.hyundaesub.data.local.dao.SavedItemDao
import com.windrr.hyundaesub.data.local.dao.VehicleDao
import com.windrr.hyundaesub.data.local.entity.ArrangementEntity
import com.windrr.hyundaesub.data.local.entity.CalculationEntity
import com.windrr.hyundaesub.data.local.entity.PackingSpaceEntity
import com.windrr.hyundaesub.data.local.entity.PlacementEntity
import com.windrr.hyundaesub.data.local.entity.PlanCarSeatEntity
import com.windrr.hyundaesub.data.local.entity.PlanEntity
import com.windrr.hyundaesub.data.local.entity.PlanItemEntity
import com.windrr.hyundaesub.data.local.entity.SavedItemEntity
import com.windrr.hyundaesub.data.local.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        PlanEntity::class,
        PlanItemEntity::class,
        PlanCarSeatEntity::class,
        CalculationEntity::class,
        ArrangementEntity::class,
        PackingSpaceEntity::class,
        PlacementEntity::class,
        SavedItemEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    abstract fun planDao(): PlanDao

    abstract fun calculationDao(): CalculationDao

    abstract fun savedItemDao(): SavedItemDao

    companion object {
        /** 스키마 버전. 출시 전이라 올리면 기존 로컬 데이터는 지워진다 */
        const val VERSION = 7

        private const val NAME = "load_plan.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME)
                    .addCallback(SeedVehicles)
                    // 출시 전이라 스키마가 바뀌면 기존 로컬 데이터는 버리고 새로 만든다
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}

/** 1차 프로토타입은 이 차량 하나만 쓴다 */
object DefaultVehicles {
    val AVANTE = VehicleEntity(
        vehicleId = "avante_cn8",
        isMain = true,
        name = "더 뉴 아반떼 하이브리드",
        desc = "5인승 SUV",
    )
    val ALL = listOf(AVANTE)
}

/** DB 최초 생성 시 기본 차량을 넣는다 */
private object SeedVehicles : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultVehicles.ALL.forEach {
            db.execSQL(
                "INSERT OR IGNORE INTO vehicle (vehicle_id, is_main, name, description) VALUES (?, ?, ?, ?)",
                arrayOf(it.vehicleId, if (it.isMain) 1 else 0, it.name, it.desc),
            )
        }
    }
}

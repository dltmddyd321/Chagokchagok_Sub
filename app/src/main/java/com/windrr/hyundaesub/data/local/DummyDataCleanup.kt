package com.windrr.hyundaesub.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 예전 디버그 빌드가 넣어 둔 더미 계획·내 짐을 지운다 (id가 "dummy-"로 시작하는 행만).
 * 사용자가 만든 데이터는 건드리지 않는다. 더미가 남은 기기가 없어지면 이 파일은 지워도 된다.
 */
object DummyDataCleanup {

    suspend fun run(db: AppDatabase) = withContext(Dispatchers.IO) {
        runCatching {
            db.runInTransaction {
                val sql = db.openHelper.writableDatabase
                // 계획을 지우면 짐·카시트·계산 결과는 CASCADE로 함께 지워진다
                sql.execSQL("DELETE FROM `plan` WHERE plan_id LIKE 'dummy-%'")
                sql.execSQL("DELETE FROM saved_item WHERE saved_item_id LIKE 'dummy-%'")
            }
        }
    }
}

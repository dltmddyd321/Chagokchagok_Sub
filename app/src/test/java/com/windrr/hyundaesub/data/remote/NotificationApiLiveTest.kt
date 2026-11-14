package com.windrr.hyundaesub.data.remote

import com.windrr.hyundaesub.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID

/**
 * 실제 서버 호출 확인: 기기 등록(PUT) → 해제(DELETE).
 * 테스트용 device_id·가짜 토큰을 쓰고 끝나면 바로 해제한다.
 * local.properties에 서버 계정이 있을 때만 돈다.
 * ./gradlew :app:testDebugUnitTest --tests "*NotificationApiLiveTest*"
 */
class NotificationApiLiveTest {

    @Test
    fun registerThenUnregisterDevice_onRealServer() = runBlocking {
        assumeTrue("서버 계정이 local.properties에 없어 건너뜀", BuildConfig.CHAGOK_API_USERNAME.isNotBlank())
        val repository = NotificationRemoteRepository()
        val deviceId = UUID.randomUUID().toString()

        val registered = repository.registerDevice(deviceId, token = "live-test-token-$deviceId")
        println("PUT devices/$deviceId → $registered")
        assertTrue("등록 실패: $registered", registered is ApiResult.Success)
        assertEquals(true, (registered as ApiResult.Success).data.active)

        val unregistered = repository.unregisterDevice(deviceId)
        println("DELETE devices/$deviceId → $unregistered")
        assertTrue("해제 실패: $unregistered", unregistered is ApiResult.Success)
        assertEquals(false, (unregistered as ApiResult.Success).data.active)
    }
}

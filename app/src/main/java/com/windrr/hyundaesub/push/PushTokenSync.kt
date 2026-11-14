package com.windrr.hyundaesub.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.windrr.hyundaesub.data.remote.ApiResult
import com.windrr.hyundaesub.data.remote.NotificationRemoteRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * FCM 토큰 받아오기 + 서버 등록(PUT /notifications/devices/{device_id}).
 * 앱 시작 시 [refresh], 토큰이 새로 발급되면 [onNewToken]을 부른다.
 */
object PushTokenSync {

    private const val TAG = "PushTokenSync"

    private val remote by lazy { NotificationRemoteRepository() }

    /** 현재 토큰을 받아 저장하고, 아직 서버에 안 올렸으면 등록한다 */
    suspend fun refresh(context: Context) {
        val token = fetchToken() ?: return
        onNewToken(context, token)
    }

    /** 앱 시작(refresh)과 토큰 발급(onNewToken)이 겹쳐도 등록은 한 번씩 차례로 */
    private val mutex = Mutex()

    suspend fun onNewToken(context: Context, token: String): Unit = mutex.withLock {
        val store = PushTokenStore(context)
        store.save(token)
        if (store.isSynced) return@withLock
        when (val result = remote.registerDevice(store.deviceId, token)) {
            is ApiResult.Success -> {
                store.markSynced()
                Log.d(TAG, "FCM 토큰 서버 등록 완료 (active=${result.data.active})")
            }
            else -> Log.w(TAG, "FCM 토큰 서버 등록 실패 → 다음 실행 때 재시도: $result")
        }
    }

    /** 푸시를 끌 때(예: 알림 설정 해제) 서버에서 이 기기를 뺀다 */
    suspend fun unregister(context: Context): Boolean {
        val store = PushTokenStore(context)
        val result = remote.unregisterDevice(store.deviceId)
        if (result is ApiResult.Success) store.markUnsynced() else Log.w(TAG, "FCM 토큰 서버 해제 실패: $result")
        return result is ApiResult.Success
    }

    private suspend fun fetchToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                cont.resume(task.result)
            } else {
                Log.w(TAG, "FCM 토큰 받기 실패", task.exception)
                cont.resume(null)
            }
        }
    }
}

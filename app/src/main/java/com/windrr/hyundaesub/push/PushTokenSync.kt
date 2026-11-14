package com.windrr.hyundaesub.push

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FCM 토큰 받아오기 + 서버 등록.
 * 앱 시작 시 [refresh], 토큰이 새로 발급되면 [onNewToken]을 부른다.
 */
object PushTokenSync {

    private const val TAG = "PushTokenSync"

    /** 현재 토큰을 받아 저장하고, 아직 서버에 안 올렸으면 등록한다 */
    suspend fun refresh(context: Context) {
        val token = fetchToken() ?: return
        onNewToken(context, token)
    }

    suspend fun onNewToken(context: Context, token: String) {
        val store = PushTokenStore(context)
        store.save(token)
        if (store.isSynced) return
        if (uploadToServer(token)) store.markSynced()
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

    /**
     * TODO: 백엔드 알림 기기 등록 API가 정해지면 연결.
     * 지금은 로그만 남기고 false를 돌려줘 다음 실행 때 다시 시도하게 둔다.
     */
    @Suppress("RedundantSuspendModifier")
    private suspend fun uploadToServer(token: String): Boolean {
        Log.d(TAG, "FCM 토큰 준비됨 (서버 등록 API 연결 전): ${token.take(12)}…")
        return false
    }
}

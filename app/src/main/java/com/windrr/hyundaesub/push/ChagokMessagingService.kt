package com.windrr.hyundaesub.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * FCM 수신.
 * - notification 메시지는 앱이 백그라운드면 시스템이 바로 띄우고(기본 채널 사용), 포그라운드면 여기로 온다.
 * - data 메시지는 항상 여기로 온다. data의 title/body 키를 제목·본문으로 쓴다.
 */
class ChagokMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch { PushTokenSync.onNewToken(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        PushNotifier.show(applicationContext, title, body, message.data)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

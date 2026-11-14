package com.windrr.hyundaesub.push

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

/**
 * FCM 토큰 + 서버에 등록할 기기 ID 보관.
 * [isSynced]가 false면(토큰이 바뀌었거나 등록 실패) 다음 실행 때 다시 등록한다.
 */
class PushTokenStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 서버 device_id. 처음 한 번 만들고 앱을 지우기 전까지 같은 값을 쓴다 */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also { id ->
            prefs.edit { putString(KEY_DEVICE_ID, id) }
        }

    val token: String? get() = prefs.getString(KEY_TOKEN, null)
    val isSynced: Boolean get() = prefs.getBoolean(KEY_SYNCED, false)

    /** 토큰이 바뀌었으면 저장하고 서버 등록 상태를 초기화한다. 바뀌었으면 true */
    fun save(newToken: String): Boolean {
        if (newToken == token) return false
        prefs.edit {
            putString(KEY_TOKEN, newToken)
            putBoolean(KEY_SYNCED, false)
        }
        return true
    }

    fun markSynced() = prefs.edit { putBoolean(KEY_SYNCED, true) }

    /** 서버에서 해제했으면 다음 실행 때 다시 등록되도록 표시 */
    fun markUnsynced() = prefs.edit { putBoolean(KEY_SYNCED, false) }

    private companion object {
        const val PREFS = "push_token"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_SYNCED = "synced"
        const val KEY_DEVICE_ID = "device_id"
    }
}

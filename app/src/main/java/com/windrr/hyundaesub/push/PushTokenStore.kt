package com.windrr.hyundaesub.push

import android.content.Context
import androidx.core.content.edit

/**
 * FCM 토큰 보관. 서버 등록 API가 붙기 전까지는 기기에만 저장해 두고,
 * [isSynced]가 false면 다음 실행 때 다시 등록을 시도한다.
 */
class PushTokenStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

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

    private companion object {
        const val PREFS = "push_token"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_SYNCED = "synced"
    }
}

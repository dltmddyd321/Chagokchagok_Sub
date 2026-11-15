package com.windrr.hyundaesub.ui.item.confirm

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

/** 짐 대표 사진을 앱 내부 저장소(files/item_photos)에 복사해 둔다. 캐시 정리·갤러리 권한 만료에도 남도록 */
object ItemPhotoStore {

    /** [source] (file:// 또는 content://)를 복사하고 새 file:// URI를 돌려준다. 실패하면 null (기본 이미지) */
    fun keep(context: Context, source: String, savedItemId: String): String? = runCatching {
        val dir = File(context.filesDir, "item_photos").apply { mkdirs() }
        val target = File(dir, "$savedItemId.jpg")
        context.contentResolver.openInputStream(Uri.parse(source))!!.use { input ->
            target.outputStream().use { input.copyTo(it) }
        }
        Uri.fromFile(target).toString()
    }.onFailure { Log.w("ItemPhotoStore", "사진 저장 실패: $source", it) }.getOrNull()
}

package com.windrr.hyundaesub.push

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.windrr.hyundaesub.MainActivity
import com.windrr.hyundaesub.R

/** FCM 푸시 알림 채널·표시 */
object PushNotifier {

    /** AndroidManifest의 default_notification_channel_id와 같아야 한다 */
    const val CHANNEL_PUSH = "push_default"

    /** 알림을 눌러 앱이 열렸을 때 data 페이로드를 담아 넘기는 extra 접두사 */
    const val EXTRA_PREFIX = "push."

    fun createChannel(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_PUSH, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("차곡차곡 알림")
                .build(),
        )
    }

    @SuppressLint("MissingPermission") // 아래에서 직접 확인
    fun show(context: Context, title: String?, body: String?, data: Map<String, String>) {
        if (title.isNullOrBlank() && body.isNullOrBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)
        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .apply { data.forEach { (key, value) -> putExtra(EXTRA_PREFIX + key, value) } }
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_PUSH)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}

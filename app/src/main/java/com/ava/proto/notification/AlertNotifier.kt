package com.ava.proto.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ava.proto.data.SessionEntity

private const val CHANNEL_ID = "scam_alerts"

/**
 * 세션 상태 변화에 따라 로컬 알림을 띄운다.
 *
 * 단일 채널에서 강한 신호가 잡힌 경우([notifySuspected])도 알린다 — 다채널 격상을
 * "알릴지 말지 결정하는 문"으로 쓰지 않는다. 여러 채널이 겹치면([notifyEscalation])
 * 그보다 더 급하다는 걸 알려줄 뿐이다.
 */
class AlertNotifier(private val context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "보이스피싱 의심 경보",
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun notifySuspected(session: SessionEntity, channels: Set<String>) {
        post(
            session = session,
            title = "주의가 필요한 정황이 감지됐습니다",
            text = "${channels.joinToString(", ")} 채널에서 위험 신호가 확인됐습니다.",
        )
    }

    fun notifyEscalation(session: SessionEntity, channels: Set<String>) {
        post(
            session = session,
            title = "보이스피싱 의심 정황이 강하게 감지됐습니다",
            text = "${channels.joinToString(", ")} 채널에서 연관된 신호가 겹쳐 확인됐습니다.",
        )
    }

    private fun post(session: SessionEntity, title: String, text: String) {
        // areNotificationsEnabled() 는 버전 분기 없이도 Android 13+ 런타임 권한과, 그 이전
        // 버전에서 사용자가 시스템 설정으로 앱 알림 자체를 꺼둔 경우를 함께 잡아준다 —
        // POST_NOTIFICATIONS 런타임 권한만 보던 이전 코드는 후자를 놓치고 있었다.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(session.id.toInt(), notification)
    }
}

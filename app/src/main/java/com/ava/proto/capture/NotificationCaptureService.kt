package com.ava.proto.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ava.proto.ProtoApplication
import com.ava.proto.notification.AlertNotifier
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val TAG = "NotificationCapture"

/** 같은 알림이 내용 변경 없이 갱신되는 경우를 걸러내는 용도 — 무한정 쌓이지 않게 상한을 둔다. */
private const val MAX_DEDUP_ENTRIES = 200

/**
 * SMS와 카카오톡을 같은 메커니즘으로 잡는다 — 문자 앱도 카톡도 새 메시지가 오면 시스템 알림을
 * 띄우므로, RECEIVE_SMS/READ_SMS(기본 문자 앱만 쓸 수 있는 제한 권한) 없이 알림만 읽으면 된다.
 *
 * 허용 목록 밖 패키지의 알림은 onNotificationPosted 진입 즉시 버려지고 어디에도 기록되지 않는다.
 * 이 서비스는 텍스트를 받아적기만 하고 분류는 하지 않는다 — 판정은 [com.ava.proto.pipeline.DetectionPipeline] 몫이다.
 */
class NotificationCaptureService : NotificationListenerService() {

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "캡처 처리 중 예상치 못한 예외", e)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    // 알림 갱신(같은 메시지가 내용 변경 없이 다시 posted)과 진짜 새 메시지를 구분한다.
    private val recentlySeen = LinkedHashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val allowlist = TargetPackages.allowlist(this)
        if (sbn.packageName !in allowlist) return

        // 우리가 띄운 **경보**는 되잡지 않는다.
        //
        // 자기 앱을 감시 대상에 넣은 건 데모 버튼이 실제 경로를 그대로 타게 하려던 것인데,
        // 그 통로로 AlertNotifier의 경보까지 되돌아온다. 통화 한 건을 넣었더니 경보 → SMS
        // 이벤트 → 다시 경보로 3초 간격 세 바퀴가 돌았고, 이벤트가 1건에서 4건으로 늘었다.
        // 세션이 ESCALATED에 닿아 우연히 멈췄을 뿐 막힌 게 아니다.
        //
        // 데모 알림은 다른 채널(demo_messages)을 쓰므로 경보 채널만 걸러내면 데모는 그대로
        // 살아남는다.
        if (sbn.packageName == TargetPackages.PROTO_APP &&
            sbn.notification.channelId == AlertNotifier.CHANNEL_ID
        ) {
            return
        }

        // Proto 앱 자체 알림(데모용)은 extras의 "demo_channel" 힌트로 채널을 구분한다.
        // 실제 카카오톡·문자 앱은 패키지명으로 구분한다.
        val channel = when {
            sbn.packageName == TargetPackages.KAKAO_TALK -> Channel.KAKAO
            sbn.packageName == TargetPackages.PROTO_APP ->
                if (sbn.notification.extras.getString("demo_channel") == "KAKAO") Channel.KAKAO else Channel.SMS
            else -> Channel.SMS
        }

        // 펼친 알림의 전체 텍스트(BigText)가 있으면 그걸 쓰고, 없으면 한 줄 미리보기로 대체한다.
        // 발신 앱이 알림 미리보기를 숨겨두면 둘 다 비어 있을 수 있다 — 이 경우는 그냥 건너뛴다.
        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return

        // 시스템은 기존 알림이 갱신될 때도 이 콜백을 다시 부른다 — 같은 알림 키에 같은
        // 내용이면 새 메시지가 아니라 단순 갱신이므로 건너뛴다.
        if (!markIfNew("${sbn.key}:$text")) return

        val counterpart = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        val app = application as ProtoApplication
        scope.launch {
            app.detectionPipeline.process(
                CapturedEvent(
                    channel = channel,
                    capturedAt = sbn.postTime,
                    sourceLabel = sbn.packageName,
                    text = text,
                    counterpart = counterpart,
                ),
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 알림 접근 설정 변경 등으로 서비스가 재생성될 때, 이전 인스턴스의 진행 중이던
        // 코루틴이 스코프 없이 계속 살아남지 않게 여기서 확실히 끊는다.
        scope.cancel()
    }

    private fun markIfNew(key: String): Boolean {
        if (key in recentlySeen) return false
        recentlySeen.add(key)
        if (recentlySeen.size > MAX_DEDUP_ENTRIES) {
            recentlySeen.remove(recentlySeen.first())
        }
        return true
    }
}

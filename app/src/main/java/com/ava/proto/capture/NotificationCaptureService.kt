package com.ava.proto.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ava.proto.ProtoApplication
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

        val channel = if (sbn.packageName == TargetPackages.KAKAO_TALK) Channel.KAKAO else Channel.SMS

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

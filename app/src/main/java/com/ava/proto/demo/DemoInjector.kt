package com.ava.proto.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val TAG = "DemoInjector"

// 데모 알림 전용 채널 — 실제 경보 채널(scam_alerts)과 구분
private const val DEMO_NOTIF_CHANNEL_ID = "demo_messages"

// 오탐 검증용 정상 메시지 목록 (피싱 키워드 없음)
private val KAKAO_NORMAL_MESSAGES = listOf(
    "친구" to "오늘 저녁 같이 먹어요~ 6시 홍대 어때요?",
    "팀장님" to "자료 확인했어요! 내일 회의 때 공유할게요",
    "배달 알림" to "주문하신 치킨이 배달 출발했습니다. 20분 후 도착 예정",
)
private val SMS_NORMAL_MESSAGES = listOf(
    "[카드사]" to "전월 카드이용금액 320,000원이 정상 청구됩니다. 문의 고객센터 1588-0000",
    "[CJ대한통운]" to "고객님 택배(운송장 1234567890)가 배송 완료되었습니다.",
    "[국민은행]" to "급여 2,850,000원이 입금되었습니다. 잔액 5,120,000원.",
)

/**
 * 채널별 데모 이벤트 주입기.
 *
 * 카카오톡·SMS 데모는 Proto 앱 이름으로 Android 알림을 직접 발생시킨다.
 * NotificationCaptureService가 이 알림을 수신해 실제 탐지 파이프라인을 타므로,
 * 실제 카카오톡·문자 수신 경로와 동일한 코드 경로를 검증할 수 있다.
 *
 * **파이프라인을 직접 들고 있지 않은 것이 이 클래스의 요점이다.** 알림을 띄우는 것 외에
 * 탐지 계층으로 가는 통로가 없어야, 데모 편의를 위해 파이프라인을 우회하는 코드가
 * 나중에 슬그머니 끼어들 수 없다.
 *
 * 통화 채널에는 주입할 것이 없다 — 폴더에 파일을 넣으면 `CallRecordingWatcher`가 잡는다.
 */
class DemoInjector(private val context: Context) {

    private var kakaoNormalIndex = 0
    private var smsNormalIndex = 0

    // minSdk 26 이 곧 채널 도입 버전(O)이라 버전 분기가 필요 없다.
    init {
        val channel = NotificationChannel(
            DEMO_NOTIF_CHANNEL_ID,
            "데모 메시지",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    /**
     * 카카오톡 피싱 메시지를 시뮬레이션한다.
     *
     * Proto 앱이 알림을 발생 → NotificationCaptureService.onNotificationPosted()가
     * "demo_channel=KAKAO" 힌트를 읽어 Channel.KAKAO로 처리한다.
     */
    fun injectKakao() {
        postDemoNotification(
            id = System.currentTimeMillis().toInt(),
            demoChannel = "KAKAO",
            title = "금융감독원",
            text = "금융감독원입니다. 귀하의 계좌가 범죄에 연루되어 즉시 조치가 필요합니다. 협조하지 않으면 구속영장이 발부됩니다.",
        )
    }

    /** 오탐 검증용 — 피싱 키워드가 없는 정상 카카오톡 메시지를 순환 발송한다. */
    fun injectKakaoNormal() {
        val (title, text) = KAKAO_NORMAL_MESSAGES[kakaoNormalIndex % KAKAO_NORMAL_MESSAGES.size]
        kakaoNormalIndex++
        postDemoNotification(
            id = System.currentTimeMillis().toInt(),
            demoChannel = "KAKAO",
            title = title,
            text = text,
        )
    }

    /**
     * SMS 피싱 문자를 시뮬레이션한다.
     *
     * Proto 앱이 알림을 발생 → NotificationCaptureService.onNotificationPosted()가
     * "demo_channel=SMS" 힌트를 읽어 Channel.SMS로 처리한다.
     */
    fun injectSms() {
        postDemoNotification(
            id = System.currentTimeMillis().toInt(),
            demoChannel = "SMS",
            title = "010-9999-0000",
            text = "명의도용 의심 계좌 동결 예정. 안전계좌로 즉시 이체하지 않으면 구속영장이 발부됩니다.",
        )
    }

    /** 오탐 검증용 — 피싱 키워드가 없는 정상 SMS를 순환 발송한다. */
    fun injectSmsNormal() {
        val (title, text) = SMS_NORMAL_MESSAGES[smsNormalIndex % SMS_NORMAL_MESSAGES.size]
        smsNormalIndex++
        postDemoNotification(
            id = System.currentTimeMillis().toInt(),
            demoChannel = "SMS",
            title = title,
            text = text,
        )
    }

    private fun postDemoNotification(id: Int, demoChannel: String, title: String, text: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "알림 권한 없음 — 데모 알림을 발생시킬 수 없음")
            return
        }

        val extras = Bundle().apply { putString("demo_channel", demoChannel) }
        val notification = NotificationCompat.Builder(context, DEMO_NOTIF_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .addExtras(extras)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
        Log.d(TAG, "데모 알림 발생 [$demoChannel] → NotificationCaptureService 수신 대기")
    }
}

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

    /**
     * 카드배달 사칭의 **후속 문자**를 시뮬레이션한다.
     *
     * 앞선 통화만으로는 규칙이 1단계(명의도용 언급)까지밖에 못 간다 — 사기범이 협박도
     * 계좌 요구도 하지 않고 "확인해보세요"로 끊기 때문이다. 사용자에게는 친절한 안내로
     * 읽힌다. 이 문자가 10분 안에 도착해야 2단계(주소 접속·정보 입력)가 되고, 두 조각이
     * 한 사건으로 묶인다.
     *
     * **택배기사가 이 문자를 보내는 것이 아니다.** 통화는 "금융감독원에 확인해보세요"까지만
     * 하고 끊는다 — 씨를 뿌리는 역할이다.
     *
     * 그리고 **문자는 스스로 온 이유를 댄다** — "귀하 명의로 신규 카드가 발급된 정황이
     * 확인되었습니다". 피해자가 아직 아무 데도 신고하지 않았는데 기관이 먼저 연락하는 것이
     * 어색하지 않으려면 이 한 줄이 있어야 한다. 방금 통화에서 들은 얘기를 기관이 그대로
     * 확인해주는 모양이 되어, 두 연락이 서로를 뒷받침한다 — 피해자가 믿는 이유가 그것이고,
     * 우리가 두 조각을 이어야 하는 이유도 같다. 통화 쪽 전사본에서
     * "문자 보내드릴게요" 같은 말을 넣으면 두 발신자가 한 사람이 되어 시나리오가 깨진다.
     *
     * **다채널이 필요한 이유를 한 시나리오로 보여주는 자리다.** 기존 카톡·SMS 데모는
     * 양쪽 다 협박 문구를 담고 있어 단독으로도 잡히고, 그래서 격상이 무엇을 더 해주는지
     * 드러나지 않는다.
     */
    fun injectCardDeliveryFollowUp() {
        postDemoNotification(
            id = System.currentTimeMillis().toInt(),
            demoChannel = "SMS",
            title = "1544-0000",
            text = "[금융감독원] 귀하 명의로 신규 카드가 발급된 정황이 확인되었습니다. " +
                "아래 설치 링크에서 본인확인 후 피해 접수를 진행해 주세요. https://fss-report.co.kr",
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

package com.ava.proto.capture

import android.content.Context
import android.provider.Telephony

/**
 * 알림 리스너가 감시할 패키지 허용 목록. 이 목록 밖 알림은 서비스에서 즉시 버려지고
 * 저장되지 않는다.
 */
object TargetPackages {
    const val KAKAO_TALK = "com.kakao.talk"

    /**
     * 데모 알림 자가 수신용 — Proto 앱이 직접 발생시킨 알림을
     * NotificationCaptureService가 가로채 실제 파이프라인을 타도록 한다.
     */
    const val PROTO_APP = "com.ava.proto"

    /**
     * 문자 앱 패키지명은 기기·제조사마다 다르므로(구글 메시지, 삼성 메시지 등),
     * 사용자에게 고르게 하는 대신 현재 기본 문자 앱을 시스템에 직접 물어본다.
     */
    fun defaultSmsPackage(context: Context): String? = Telephony.Sms.getDefaultSmsPackage(context)

    fun allowlist(context: Context): Set<String> = setOfNotNull(KAKAO_TALK, PROTO_APP, defaultSmsPackage(context))
}

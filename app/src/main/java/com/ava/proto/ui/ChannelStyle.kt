package com.ava.proto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.ava.proto.R
import com.ava.proto.capture.Channel

/**
 * 채널의 색과 아이콘.
 *
 * **색은 팀이 만든 브랜드 시트에서 그대로 가져왔다.** 시트의 `핵심 아이디어` 다이어그램이
 * 초록 전화 · 파란 말풍선 · 노란 말풍선으로 세 채널을 그리고 있어서, 여기서 새로 정할 것이
 * 없었다. 노랑은 카카오톡 브랜드색이기도 해서 설명 없이 읽힌다.
 *
 * 이 파일이 있는 이유는 예전에 네 탭이 각자 색을 정하고 있었기 때문이다 — 같은 통화 채널이
 * 기록 탭에서는 빨강, 설정 탭에서는 보라, 대시보드에서는 파랑이었다. 탭을 오가며 찍은
 * 스크린샷을 슬라이드에 나란히 붙이면 그 어긋남이 그대로 보인다.
 *
 * **빨강은 채널에 쓰지 않는다.** 위험 표시가 빨강·주황을 쓰고 있어서, 카드 안에 빨강이 두 개
 * 뜨면 "통화라서 빨강"인지 "위험해서 빨강"인지 구분되지 않는다. 여기 셋은 전부 위험도와
 * 무관한 신원 표시다.
 *
 * 다크 모드 값을 따로 두는 이유는 시트 색이 흰 배경 기준이라 어두운 배경에서 탁해지기
 * 때문이다. 색상(hue)은 유지하고 밝기만 올린다 — 채널을 알아보는 단서가 색상이라서다.
 */
private val CallLight = Color(0xFF1E8C55)
private val CallDark = Color(0xFF6BE3A0)
private val SmsLight = Color(0xFF2F62D8)
private val SmsDark = Color(0xFF9CBBFF)
private val KakaoLight = Color(0xFFB07A00)
private val KakaoDark = Color(0xFFFECB3A)

@Composable
internal fun channelColor(channel: Channel): Color {
    val dark = isSystemInDarkTheme()
    return when (channel) {
        Channel.CALL -> if (dark) CallDark else CallLight
        Channel.SMS -> if (dark) SmsDark else SmsLight
        Channel.KAKAO -> if (dark) KakaoDark else KakaoLight
    }
}

/**
 * 카카오톡만 직접 그린 말풍선을 쓴다(`ic_channel_kakao.xml`). 기본 아이콘 세트의 종이비행기는
 * "보내기"라서 아무 메신저로나 읽혔다.
 *
 * `@Composable` 인 이유는 벡터 리소스를 읽기 때문이다 — 호출부가 전부 컴포저블 안이라
 * 부담이 없다.
 */
@Composable
internal fun channelIcon(channel: Channel): ImageVector = when (channel) {
    Channel.CALL -> Icons.Filled.Call
    Channel.SMS -> Icons.Filled.Email
    Channel.KAKAO -> ImageVector.vectorResource(R.drawable.ic_channel_kakao)
}

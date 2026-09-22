package com.ava.proto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
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
 * 세 채널 모두 직접 그린 글리프다(`ic_glyph_call` · `ic_glyph_sms` · `ic_channel_kakao`).
 * 기호 세트의 수화기·말풍선은 획이 얇고 각져서 통통한 타일 위에서 빈약해 보였다. 카카오톡을 알아보게 하는 것은 왼쪽 아래로 흐르는 꼬리가 달린
 * 둥근 말풍선 실루엣이라 기호 세트의 말풍선으로는 대신할 수 없다.
 *
 * 문자에 편지봉투를 쓰지 않는다 — 봉투는 이메일이고, 문자는 말풍선이다. 두 말풍선을
 * 점(문자)과 꼬리 모양(카카오톡), 그리고 색으로 가른다.
 */
@Composable
internal fun channelIcon(channel: Channel): ImageVector = when (channel) {
    Channel.CALL -> ImageVector.vectorResource(R.drawable.ic_glyph_call)
    Channel.SMS -> ImageVector.vectorResource(R.drawable.ic_glyph_sms)
    Channel.KAKAO -> ImageVector.vectorResource(R.drawable.ic_channel_kakao)
}

/**
 * 채널을 **앱 아이콘풍 타일**로 그린다 — 노란 사각형에 갈색 말풍선이면 설명 없이 카카오톡이고,
 * 초록 사각형에 흰 수화기면 전화다. 토스가 은행을 로고 타일로 보여주는 것과 같은 문법이다.
 *
 * 옅은 배경에 색 글리프를 얹던 [IconBubble] 방식은 어느 앱에나 있는 목록 아이콘이라
 * 채널이 "항목"으로만 읽혔다. 타일은 라이트·다크에서 색을 바꾸지 않는다 — 앱 아이콘이
 * 테마를 따라 변하지 않듯이.
 *
 * 카카오의 공식 로고 파일은 쓰지 않는다. 노랑 바탕과 말풍선 실루엣만 빌린다.
 * 꺼진 채널은 회색 타일이다 — 색 타일은 곧 "감시 중"이라는 뜻이어야 한다.
 */
@Composable
internal fun ChannelTile(channel: Channel, size: Int = 44, active: Boolean = true) {
    // 위가 밝고 아래가 진한 두 톤. 단색 평면은 마스코트의 젤리 질감 옆에서 딱딱해 보였다.
    val (top, bottom, glyph) = when {
        !active -> Triple(
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.outline,
        )
        channel == Channel.CALL -> Triple(Color(0xFF5BDC8A), Color(0xFF22B45C), Color(0xFFFFFFFF))
        channel == Channel.SMS -> Triple(Color(0xFF5C97FF), Color(0xFF2A6CEB), Color(0xFFFFFFFF))
        else -> Triple(Color(0xFFFFEE4D), Color(0xFFF9D000), Color(0xFF3C1E1E))
    }
    val shape = RoundedCornerShape((size * 0.36f).dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(Brush.verticalGradient(listOf(top, bottom)), shape)
            // 위쪽 광택 한 줄 — 유리처럼 보이지 않을 만큼만.
            .background(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.22f),
                    0.45f to Color.Transparent,
                ),
                shape,
            ),
    ) {
        Icon(
            channelIcon(channel),
            contentDescription = channel.label,
            tint = glyph,
            modifier = Modifier.size((size * 0.55f).dp),
        )
    }
}

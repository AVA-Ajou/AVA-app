package com.ava.proto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 색 체계는 **중립 회색 바탕 + 보라 하나 + 의미색 셋**으로 줄였다.
 *
 * 예전에는 배경부터 카드 테두리까지 전부 라벤더 계열이라 화면 어디를 봐도 보라가 깔려 있었고,
 * 그 위에 올린 경보 빨강·주황이 같은 무게로 섞여 **무엇이 강조인지 색으로 읽히지 않았다.**
 * 토스·뱅크샐러드류 금융 앱이 바탕을 회색으로 비워두는 이유가 그것이다 — 색이 나오는 자리가
 * 곧 봐야 할 자리가 된다.
 *
 * 보라는 브랜드 시트의 `#6D4FDE` 하나만 남겼다. 버튼·선택 탭·링크가 전부 이 값이고,
 * 진보라 워드마크색(`#452A9B`)은 보라 위에 얹는 글자에만 쓴다.
 *
 * ```
 * 바탕      #F3F3F8   카드 위에 회색 바탕이 살짝 비쳐 카드 경계가 선 없이 읽힌다
 * 카드      #FFFFFF
 * 본문      #191A2E   순검정이 아니라 보라 쪽으로 아주 조금 기울였다 — 브랜드와 붙는다
 * 보조 글자 #6B6B80
 * 힌트 글자 #A0A0B4
 * ```
 *
 * 다크 모드는 색상(hue)을 유지하고 명도만 뒤집는다. 특히 **의미색은 다크에서 채도를 올린 원색을
 * 글자로 쓰고 바탕은 알파로 깐다** — 어두운 빨강 판 위에 연한 빨강 글자를 얹던 방식은 대비가
 * 나오지 않아 기록 탭 칩이 읽히지 않았다.
 *
 * 다이나믹 컬러(Material You)는 쓰지 않는다. 기기 배경색에 따라 위험/정상 색이 흔들리면
 * 경보 강도를 색으로 읽을 수 없고, 브랜드 보라도 사라진다.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF6D4FDE),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FC),
    onPrimaryContainer = Color(0xFF452A9B),
    inversePrimary = Color(0xFFA996FF),
    secondary = Color(0xFF7656E5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDE9FC),
    onSecondaryContainer = Color(0xFF452A9B),
    // 초록은 "연결됨·정상"에만 쓴다. 채널색(통화)과 같은 계열이지만 자리가 겹치지 않는다.
    tertiary = Color(0xFF0BA360),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3F7EC),
    onTertiaryContainer = Color(0xFF06643B),
    error = Color(0xFFF04452),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFDE8EA),
    onErrorContainer = Color(0xFFB3261E),
    background = Color(0xFFF3F3F8),
    onBackground = Color(0xFF191A2E),
    surface = Color(0xFFF3F3F8),
    onSurface = Color(0xFF191A2E),
    surfaceVariant = Color(0xFFECECF3),
    onSurfaceVariant = Color(0xFF6B6B80),
    surfaceTint = Color(0xFF6D4FDE),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE1E1EA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9FC),
    surfaceContainer = Color(0xFFF1F1F6),
    surfaceContainerHigh = Color(0xFFE9E9F0),
    surfaceContainerHighest = Color(0xFFE1E1EA),
    outline = Color(0xFFA0A0B4),
    outlineVariant = Color(0xFFE6E6EE),
    inverseSurface = Color(0xFF2B2B38),
    inverseOnSurface = Color(0xFFF3F3F8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA996FF),
    onPrimary = Color(0xFF23125E),
    primaryContainer = Color(0xFF2C2450),
    onPrimaryContainer = Color(0xFFDCD3FF),
    inversePrimary = Color(0xFF6D4FDE),
    secondary = Color(0xFFB8A8FF),
    onSecondary = Color(0xFF2A1A6B),
    secondaryContainer = Color(0xFF2C2450),
    onSecondaryContainer = Color(0xFFDCD3FF),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF14332A),
    onTertiaryContainer = Color(0xFF8CFFBC),
    error = Color(0xFFFF6B72),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF3A1A1E),
    onErrorContainer = Color(0xFFFFB4B8),
    background = Color(0xFF111114),
    onBackground = Color(0xFFECECF2),
    surface = Color(0xFF111114),
    onSurface = Color(0xFFECECF2),
    surfaceVariant = Color(0xFF26262E),
    onSurfaceVariant = Color(0xFFA3A3B5),
    surfaceTint = Color(0xFFA996FF),
    surfaceBright = Color(0xFF34343D),
    surfaceDim = Color(0xFF111114),
    surfaceContainerLowest = Color(0xFF1B1B21),
    surfaceContainerLow = Color(0xFF17171C),
    surfaceContainer = Color(0xFF232329),
    surfaceContainerHigh = Color(0xFF2B2B33),
    surfaceContainerHighest = Color(0xFF34343D),
    outline = Color(0xFF6E6E82),
    outlineVariant = Color(0xFF2E2E38),
    inverseSurface = Color(0xFFECECF2),
    inverseOnSurface = Color(0xFF2B2B38),
)

/**
 * 확정되지 않은 경고에 쓰는 주황색. Material 3 스킴에 자리가 없어 따로 둔다.
 *
 * `error`(빨강)와 구분하려고 만든 값이다 — 빨강은 **규칙이 근거를 찾은** 단계 표시에 쓰고,
 * 주황은 모델만 위험하다고 보고 규칙은 아무 신호도 못 찾은 `경보우려`에 쓴다.
 * 라이트/다크 값을 쌍으로 두는 이유는 `error` 와 같다 — 한쪽만 정하면 다크 모드에서 묻힌다.
 */
private val CautionLight = Color(0xFFE0701A)
private val CautionDark = Color(0xFFFFA65C)

val caution: Color
    @Composable get() = if (isSystemInDarkTheme()) CautionDark else CautionLight

/**
 * 예보에 쓰는 노란색. [caution]보다 한 칸 약한 자리다.
 *
 * 주황과 갈라놓은 이유는 **켜진 판정기의 수가 다르기 때문이다** — 주황(주의보·경보우려)은
 * 모델이나 규칙 중 한쪽이 위험하다고 본 자리고, 노랑은 어느 쪽도 그러지 않았는데 모델
 * 점수만 바닥을 넘은 자리다. 같은 주황을 쓰면 "한 명이라도 손을 들었나"가 색에서 사라진다.
 *
 * 라이트 값을 황토색 쪽으로 누른 것은 순한 노랑이 흰 배경에서 글자로 읽히지 않아서다.
 */
private val ForecastLight = Color(0xFFB57F00)
private val ForecastDark = Color(0xFFF5C542)

val forecast: Color
    @Composable get() = if (isSystemInDarkTheme()) ForecastDark else ForecastLight

@Composable
fun ProtoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AvamonTypography,
        content = content,
    )
}

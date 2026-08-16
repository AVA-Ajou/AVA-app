package com.ava.proto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 색은 전부 **팀이 만든 브랜드 시트에서 뽑은 값**이다. 눈대중으로 고르지 않고 시트 이미지의
 * 픽셀을 직접 샘플링했다 — 앱과 발표 자료가 같은 보라를 써야 폰 목업을 슬라이드에 얹었을 때
 * 배경과 붙는다.
 *
 * ```
 * 워드마크 진보라  #452A9B      배지·버튼 보라  #6D4FDE
 * 보조 보라        #7656E5      연보라          #B6A7EE
 * 배경 라벤더      #F7F4FD
 * ```
 *
 * 개별 컴포저블에 색을 하드코딩하지 않고 스킴에 몰아넣은 이유는, 카드 배경과 테두리의 대비가
 * 라이트/다크에서 쌍으로 움직여야 하기 때문이다 — 한쪽만 고치면 카드가 배경에 묻힌다.
 *
 * 다이나믹 컬러(Material You)는 쓰지 않는다. 기기 배경색에 따라 위험/정상 색이 흔들리면
 * 데모에서 경보 강도를 색으로 읽을 수 없고, 브랜드 보라도 사라진다.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF452A9B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6D4FDE),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFC7B6FF),
    secondary = Color(0xFF7656E5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB6A7EE),
    onSecondaryContainer = Color(0xFF231152),
    // 초록은 채널색(통화)과 "무해함" 표시에 함께 쓰인다. 시트의 #57D383 은 흰 배경에서
    // 글자로 쓰기엔 옅어서, 같은 계열을 어둡게 눌러 본문 대비를 확보했다.
    tertiary = Color(0xFF1E8C55),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF57D383),
    onTertiaryContainer = Color(0xFF00351B),
    error = Color(0xFFD92D20),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF912018),
    background = Color(0xFFF7F4FD),
    onBackground = Color(0xFF221A38),
    surface = Color(0xFFF7F4FD),
    onSurface = Color(0xFF221A38),
    surfaceVariant = Color(0xFFEBE5FA),
    onSurfaceVariant = Color(0xFF605A78),
    surfaceTint = Color(0xFF6D4FDE),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE3DBF7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF9FE),
    surfaceContainer = Color(0xFFF2EEFC),
    surfaceContainerHigh = Color(0xFFEBE5FA),
    surfaceContainerHighest = Color(0xFFE3DBF7),
    outline = Color(0xFF8B80B5),
    outlineVariant = Color(0xFFDED5F5),
    inverseSurface = Color(0xFF2E2647),
    inverseOnSurface = Color(0xFFF3F0FB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7B6FF),
    onPrimary = Color(0xFF25105F),
    primaryContainer = Color(0xFF5B3FD0),
    onPrimaryContainer = Color(0xFFE8E0FF),
    inversePrimary = Color(0xFF6D4FDE),
    secondary = Color(0xFFCBBDFF),
    onSecondary = Color(0xFF33206B),
    secondaryContainer = Color(0xFF4A34A8),
    onSecondaryContainer = Color(0xFFE6DEFF),
    tertiary = Color(0xFF6BE3A0),
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF00522F),
    onTertiaryContainer = Color(0xFF8CFFBC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF14111F),
    onBackground = Color(0xFFE8E3F5),
    surface = Color(0xFF14111F),
    onSurface = Color(0xFFE8E3F5),
    surfaceVariant = Color(0xFF2A2440),
    onSurfaceVariant = Color(0xFFC0B8D8),
    surfaceTint = Color(0xFFC7B6FF),
    surfaceBright = Color(0xFF3A3355),
    surfaceDim = Color(0xFF14111F),
    surfaceContainerLowest = Color(0xFF100D19),
    surfaceContainerLow = Color(0xFF1A1628),
    surfaceContainer = Color(0xFF201B30),
    surfaceContainerHigh = Color(0xFF2A2440),
    surfaceContainerHighest = Color(0xFF342D4E),
    outline = Color(0xFF8A80A8),
    outlineVariant = Color(0xFF3B3355),
    inverseSurface = Color(0xFFE8E3F5),
    inverseOnSurface = Color(0xFF2E2647),
)

/**
 * 확정되지 않은 경고에 쓰는 주황색. Material 3 스킴에 자리가 없어 따로 둔다.
 *
 * `error`(빨강)와 구분하려고 만든 값이다 — 빨강은 **규칙이 근거를 찾은** 단계 표시에 쓰고,
 * 주황은 모델만 위험하다고 보고 규칙은 아무 신호도 못 찾은 `경보우려`에 쓴다.
 * 라이트/다크 값을 쌍으로 두는 이유는 `error` 와 같다 — 한쪽만 정하면 다크 모드에서 묻힌다.
 */
private val CautionLight = Color(0xFFB35309)
private val CautionDark = Color(0xFFFFB77C)

val caution: Color
    @Composable get() = if (isSystemInDarkTheme()) CautionDark else CautionLight

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

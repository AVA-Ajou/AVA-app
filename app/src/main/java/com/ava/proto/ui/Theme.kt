package com.ava.proto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 시안(Material 3 토큰)을 그대로 옮긴 값이다. 개별 컴포저블에 색을 하드코딩하지 않고
 * 스킴에 몰아넣은 이유는, 카드 배경(surfaceContainerLowest)과 테두리(outlineVariant)의
 * 대비가 라이트/다크에서 쌍으로 움직여야 하기 때문이다 — 한쪽만 고치면 카드가 배경에 묻힌다.
 *
 * 다이나믹 컬러(Material You)는 쓰지 않는다. 기기 배경색에 따라 위험/정상 색이 흔들리면
 * 데모에서 경보 강도를 색으로 읽을 수 없다.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF24389C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF3F51B5),
    onPrimaryContainer = Color(0xFFCACFFF),
    inversePrimary = Color(0xFFBAC3FF),
    secondary = Color(0xFF0061A4),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF33A0FD),
    onSecondaryContainer = Color(0xFF00355C),
    tertiary = Color(0xFF004E1C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF006928),
    onTertiaryContainer = Color(0xFF4CEF74),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFE1E3E4),
    onSurfaceVariant = Color(0xFF454652),
    surfaceTint = Color(0xFF4355B9),
    surfaceBright = Color(0xFFF8F9FA),
    surfaceDim = Color(0xFFD9DADB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F4F5),
    surfaceContainer = Color(0xFFEDEEEF),
    surfaceContainerHigh = Color(0xFFE7E8E9),
    surfaceContainerHighest = Color(0xFFE1E3E4),
    outline = Color(0xFF757684),
    outlineVariant = Color(0xFFC5C5D4),
    inverseSurface = Color(0xFF2E3132),
    inverseOnSurface = Color(0xFFF0F1F2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF00105C),
    primaryContainer = Color(0xFF293CA0),
    onPrimaryContainer = Color(0xFFDEE0FF),
    inversePrimary = Color(0xFF4355B9),
    secondary = Color(0xFF9ECAFF),
    onSecondary = Color(0xFF001D36),
    secondaryContainer = Color(0xFF00497D),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFF3CE36A),
    onTertiary = Color(0xFF002108),
    tertiaryContainer = Color(0xFF00531E),
    onTertiaryContainer = Color(0xFF69FF87),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111415),
    onBackground = Color(0xFFE1E3E4),
    surface = Color(0xFF111415),
    onSurface = Color(0xFFE1E3E4),
    surfaceVariant = Color(0xFF454652),
    onSurfaceVariant = Color(0xFFC5C5D4),
    surfaceTint = Color(0xFFBAC3FF),
    surfaceBright = Color(0xFF373A3B),
    surfaceDim = Color(0xFF111415),
    surfaceContainerLowest = Color(0xFF0C0F10),
    surfaceContainerLow = Color(0xFF191C1D),
    surfaceContainer = Color(0xFF1D2021),
    surfaceContainerHigh = Color(0xFF282B2C),
    surfaceContainerHighest = Color(0xFF333637),
    outline = Color(0xFF8F909E),
    outlineVariant = Color(0xFF454652),
    inverseSurface = Color(0xFFE1E3E4),
    inverseOnSurface = Color(0xFF2E3132),
)

/**
 * 확정되지 않은 경고에 쓰는 주황색. Material 3 스킴에 자리가 없어 따로 둔다.
 *
 * `error`(빨강)와 구분하려고 만든 값이다 — 빨강은 **규칙이 근거를 찾은** 단계 표시에 쓰고,
 * 주황은 모델만 위험하다고 보고 규칙은 아무 신호도 못 찾은 `주의 필요`에 쓴다.
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
        content = content,
    )
}

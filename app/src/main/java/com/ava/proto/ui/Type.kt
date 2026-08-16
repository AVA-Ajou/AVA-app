package com.ava.proto.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * 앱 전체가 쓰는 글꼴.
 *
 * **여기 한 줄만 바꾸면 앱 전체 글꼴이 갈린다.** 컴포저블마다 `fontFamily` 를 지정하지 않는
 * 이유가 그것이다 — 화면이 늘어난 뒤에 글꼴을 바꾸려면 전부 찾아다녀야 한다.
 *
 * 지금은 시스템 기본(안드로이드는 Roboto + 한글은 본고딕 대체)이다. 브랜드 시트의 둥근
 * 서체로 맞추려면 `res/font/` 에 파일을 넣고 `FontFamily(Font(R.font.…))` 로 바꾼다.
 */
val AvamonFont: FontFamily = FontFamily.Default

/**
 * 기본 Material 3 타이포그래피보다 **제목을 크고 굵게, 자간을 좁게** 잡았다.
 *
 * 화면이 폰 목업으로 발표 자료에 들어가기 때문이다 — 슬라이드에서 목업은 실제 크기의
 * 30~40%로 줄어드는데, 기본 `headlineLarge`(32sp/자간 0)는 그 크기에서 본문과 구분되지
 * 않는다. 제목만 키우고 본문은 그대로 두면 위계가 생긴다.
 *
 * 한글은 라틴보다 자간이 넓어 보여서 제목에 음수 자간을 준다. 본문에는 주지 않는다 —
 * 작은 글자에서 자간을 좁히면 받침이 붙어 읽기 어려워진다.
 *
 * `trim = None` 은 한글의 위아래 여백이 잘리지 않게 한다. 기본값은 라틴 기준이라
 * 큰 제목에서 한글 받침이 눌린다.
 */
private val KoreanLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun heading(size: Int, lineHeight: Int, tracking: Float) = TextStyle(
    fontFamily = AvamonFont,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking.sp,
    lineHeightStyle = KoreanLineHeight,
)

private fun body(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = AvamonFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    lineHeightStyle = KoreanLineHeight,
)

val AvamonTypography = Typography(
    displaySmall = heading(38, 46, -1.0f),
    headlineLarge = heading(32, 42, -0.8f),
    headlineMedium = heading(27, 36, -0.6f),
    headlineSmall = heading(23, 31, -0.4f),
    titleLarge = heading(21, 28, -0.4f),
    titleMedium = body(17, 24, FontWeight.SemiBold),
    titleSmall = body(15, 21, FontWeight.SemiBold),
    bodyLarge = body(16, 25),
    bodyMedium = body(14, 22),
    bodySmall = body(13, 20),
    labelLarge = body(14, 19, FontWeight.SemiBold),
    labelMedium = body(12, 17, FontWeight.Medium),
    labelSmall = body(11, 15, FontWeight.Medium),
)

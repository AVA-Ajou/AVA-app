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
 * 지금은 시스템 기본(안드로이드는 Roboto + 한글은 본고딕 대체)이다. Pretendard 같은 서체로
 * 맞추려면 `res/font/` 에 파일을 넣고 `FontFamily(Font(R.font.…))` 로 바꾼다.
 */
val AvamonFont: FontFamily = FontFamily.Default

/**
 * 위계는 **크기 차이보다 굵기와 색으로** 낸다.
 *
 * 예전에는 화면 제목이 32sp였는데, 하단 탭에 같은 단어가 이미 있어 그 크기가 정보가 아니라
 * 자리만 차지했다. 제목을 한 단계 내리고(24sp) 대신 본문은 15sp로 반 단계 올려, 제목과
 * 본문 사이의 층이 셋(제목 · 항목 이름 · 보조 글자)으로 또렷이 갈리게 했다.
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
    displaySmall = heading(34, 42, -1.0f),
    headlineLarge = heading(28, 36, -0.7f),
    headlineMedium = heading(24, 32, -0.5f),
    headlineSmall = heading(21, 28, -0.4f),
    titleLarge = heading(19, 26, -0.3f),
    titleMedium = body(16, 22, FontWeight.SemiBold),
    titleSmall = body(15, 20, FontWeight.SemiBold),
    bodyLarge = body(16, 24),
    bodyMedium = body(15, 22),
    bodySmall = body(13, 19),
    labelLarge = body(14, 18, FontWeight.SemiBold),
    labelMedium = body(13, 17, FontWeight.Medium),
    labelSmall = body(12, 16, FontWeight.Medium),
)

package com.ava.proto.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.ava.proto.R

/**
 * 앱 전체가 쓰는 글꼴 — Pretendard (SIL OFL, `docs/licenses/PRETENDARD_LICENSE.txt`).
 *
 * **여기만 바꾸면 앱 전체 글꼴이 갈린다.** 컴포저블마다 `fontFamily` 를 지정하지 않는
 * 이유가 그것이다 — 화면이 늘어난 뒤에 글꼴을 바꾸려면 전부 찾아다녀야 한다.
 *
 * 시스템 기본(Roboto + 본고딕 대체)을 버린 이유는 두 글꼴이 **한 줄 안에서 섞이기** 때문이다.
 * `0507-1234-5678` 같은 숫자와 그 옆의 한글이 서로 다른 굵기·높이로 놓여, 어떤 앱을 봐도
 * 같은 "기본 안드로이드 앱" 인상을 준다. Pretendard 는 라틴·숫자·한글을 한 벌로 그려
 * 그 이음새가 없다.
 *
 * 가변 폰트가 아니라 정적 4벌을 넣었다. 굵기를 넷만 쓰기도 하고, 정적 파일이 구형 기기와
 * 에뮬레이터에서 렌더링이 더 고르다.
 */
val AvamonFont: FontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold),
)

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

package com.ava.proto.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.ava.proto.R

/**
 * 앱이 쓰는 기호 아이콘. 전부 **Material Symbols Rounded** 한 세트에서 가져왔다
 * (`res/drawable/ic_sym_*.xml`, Apache 2.0).
 *
 * `Icons.Filled.*`(material-icons-core)를 버린 이유는 그 세트가 2014년 머티리얼 디자인의
 * 각진 아이콘이라서다. 전화기와 편지봉투가 모든 튜토리얼 앱에 똑같이 들어가 있어, 그 두 개만
 * 보여도 "기본 안드로이드 앱"으로 읽힌다. Rounded 세트는 획 끝이 둥글고 굵기가 고르며
 * Pretendard 의 둥근 종성과 한 결로 놓인다.
 *
 * 탭 아이콘은 두 벌이다 — 선택된 탭만 채운 모양이 되어, 색을 못 가리는 눈에도 어느 탭인지
 * 형태로 읽힌다.
 *
 * `@Composable` 게터인 이유는 벡터 리소스를 읽기 때문이다. 호출부가 전부 컴포저블이라
 * 부담이 없다.
 */
internal object AppIcons {
    val call: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_call)
    val sms: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_sms)
    val home: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_home)
    val homeFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_home_fill)
    val history: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_history)
    val historyFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_history_fill)
    val lab: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_lab)
    val labFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_lab_fill)
    val settings: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_settings)
    val settingsFilled: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_settings_fill)
    val refresh: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_refresh)
    val warning: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_warning)
    val info: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_info)
    val chevronRight: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.ic_sym_chevron_right)
}

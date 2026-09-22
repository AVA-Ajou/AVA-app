package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 하단 탭. 화면이 늘어나도 진입점은 이 enum 하나로 유지한다. */
private enum class HomeTab(val label: String) {
    DASHBOARD("홈"),
    HISTORY("기록"),
    SIMULATION("시뮬레이션"),
    SETTINGS("설정");

    /** 선택된 탭은 채운 모양, 나머지는 선 모양 — 색 없이도 형태로 갈린다. */
    @Composable
    fun icon(selected: Boolean) = when (this) {
        DASHBOARD -> if (selected) AppIcons.homeFilled else AppIcons.home
        HISTORY -> if (selected) AppIcons.historyFilled else AppIcons.history
        SIMULATION -> if (selected) AppIcons.labFilled else AppIcons.lab
        SETTINGS -> if (selected) AppIcons.settingsFilled else AppIcons.settings
    }
}

/**
 * 탭 셸. 상태는 여전히 전부 위(MainActivity·HomeViewModel)에서 내려오고, 이 화면은
 * 어느 탭을 보여줄지만 스스로 기억한다 — 탭 선택은 화면 밖에서 알 필요가 없는 정보다.
 *
 * **공용 앱바가 없다.** 워드마크는 홈 탭의 머리에만 있고, 나머지 탭은 제목 한 줄이 머리다.
 * 앱바에 있던 종 아이콘은 눌러도 아무 일이 없는 표시등이었다 — 알림 접근 상태는 홈 탭의
 * `보호 중 / 설정 필요` 칩이 맡고, 그 칩은 누르면 설정 탭으로 간다.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    defaultSmsPackage: String?,
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onScanNow: () -> Unit,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCardFollowUp: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 화면 회전이나 프로세스 재생성 후에도 보던 탭에 남아 있어야 데모 흐름이 끊기지 않는다.
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.DASHBOARD) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // 위쪽 실선 한 줄이 바와 본문을 가른다. 그림자 대신 선을 쓰는 것은 바탕이
                // 회색이라 그림자가 탁하게 번지기 때문이다.
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 0.dp,
                ) {
                    HomeTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    tab.icon(selected = selectedTab == tab),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                // 선택 표시는 색으로만 낸다. 알약 배경까지 깔면 탭 넷 중
                                // 하나가 버튼처럼 떠서 바가 무거워진다.
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.outline,
                                unselectedTextColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
        ) {
            when (selectedTab) {
                HomeTab.DASHBOARD -> DashboardTab(
                    events = uiState.events,
                    recordingFolderUri = recordingFolderUri,
                    notificationAccessGranted = notificationAccessGranted,
                    onScanNow = onScanNow,
                    onViewAllEvents = { selectedTab = HomeTab.HISTORY },
                    onOpenSettings = { selectedTab = HomeTab.SETTINGS },
                )

                HomeTab.HISTORY -> HistoryTab(
                    events = uiState.events,
                    escalatedSessionIds = uiState.escalatedSessionIds,
                )

                HomeTab.SIMULATION -> SimulationTab(
                    isBusy = isBusy,
                    callDemoStep = callDemoStep,
                    callDemoResult = callDemoResult,
                    autoTestRunning = autoTestRunning,
                    autoTestStatus = autoTestStatus,
                    onDemoKakao = onDemoKakao,
                    onDemoSms = onDemoSms,
                    onDemoCardFollowUp = onDemoCardFollowUp,
                    onDemoCall = onDemoCall,
                    onCancelCallDemo = onCancelCallDemo,
                    onStartAutoTestKakao = onStartAutoTestKakao,
                    onStartAutoTestSms = onStartAutoTestSms,
                    onStopAutoTest = onStopAutoTest,
                )

                HomeTab.SETTINGS -> SettingsTab(
                    recordingFolderUri = recordingFolderUri,
                    notificationAccessGranted = notificationAccessGranted,
                    defaultSmsPackage = defaultSmsPackage,
                    onConnectFolder = onConnectFolder,
                    onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                    onScanNow = onScanNow,
                )
            }
        }
    }
}

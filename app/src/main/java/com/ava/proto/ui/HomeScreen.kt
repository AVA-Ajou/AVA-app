package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ava.proto.R

/** 하단 탭. 화면이 늘어나도 진입점은 이 enum 하나로 유지한다. */
private enum class HomeTab(val label: String, val icon: ImageVector) {
    DASHBOARD("대시보드", Icons.Filled.Home),
    HISTORY("기록", Icons.AutoMirrored.Filled.List),
    SIMULATION("시뮬레이션", Icons.Filled.PlayArrow),
    SETTINGS("설정", Icons.Filled.Settings),
}

/**
 * 탭 셸. 상태는 여전히 전부 위(MainActivity·HomeViewModel)에서 내려오고, 이 화면은
 * 어느 탭을 보여줄지만 스스로 기억한다 — 탭 선택은 화면 밖에서 알 필요가 없는 정보다.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 마스코트는 여러 색(보라 몸통·흰 방패·분홍 볼)이 형태를 이루므로
                        // `tint` 를 걸지 않는다 — 한 색으로 칠하면 실루엣만 남는다.
                        // 원본의 흰 배경을 따내 투명으로 만들었기 때문에 라이트·다크
                        // 어느 쪽에도 그대로 올라간다.
                        Image(
                            painterResource(R.drawable.ic_avamon_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Avamon",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    // 알림 접근이 꺼져 있으면 카톡·SMS 채널이 통째로 죽으므로 앱바에서 먼저 알린다.
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = if (notificationAccessGranted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(end = 16.dp).size(22.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
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
                    escalatedSessionIds = uiState.escalatedSessionIds,
                    recordingFolderUri = recordingFolderUri,
                    notificationAccessGranted = notificationAccessGranted,
                    onScanNow = onScanNow,
                    onViewAllEvents = { selectedTab = HomeTab.HISTORY },
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

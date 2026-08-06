package com.ava.proto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 데모와 오탐 검증을 한 탭에 묶었다. 둘 다 "신호를 일부러 만들어 파이프라인을 통과시키는"
 * 같은 성격이고, 실행 중에는 서로를 막아야 해서(enabled 조건 공유) 떨어뜨리면 상태가 꼬인다.
 */
@Composable
fun SimulationTab(
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val callBusy = callDemoStep != CallDemoStep.IDLE
    val idle = !isBusy && !callBusy && !autoTestRunning

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle("시뮬레이션", "피싱 신호를 직접 발생시켜 탐지 동작을 확인합니다.")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("채널별 데모", color = MaterialTheme.colorScheme.primary)
            CleanCard {
                Column {
                    SimulationRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        title = "카카오톡 피싱",
                        description = "기관 사칭 메시지가 카카오톡으로 도착한 상황을 재현합니다.",
                        enabled = !isBusy && !autoTestRunning,
                        onRun = onDemoKakao,
                    )
                    RowDivider()
                    SimulationRow(
                        icon = Icons.Filled.Email,
                        tint = MaterialTheme.colorScheme.secondaryContainer,
                        title = "SMS 피싱",
                        description = "카카오톡 데모 직후 실행하면 같은 세션이 ESCALATED로 격상됩니다.",
                        enabled = !isBusy && !autoTestRunning,
                        onRun = onDemoSms,
                    )
                    RowDivider()
                    SimulationRow(
                        icon = Icons.Filled.Call,
                        tint = MaterialTheme.colorScheme.error,
                        title = "통화 녹음",
                        description = "샘플 음성을 폴더에 복사해 STT와 분류까지 실제로 태웁니다.",
                        enabled = idle,
                        onRun = onDemoCall,
                    )
                }
            }

            when (callDemoStep) {
                CallDemoStep.COPYING -> ProgressPanel(text = "음성 파일 복사 중...")
                CallDemoStep.ANALYZING -> ProgressPanel(
                    text = "AI가 녹음을 분석 중... (수 초~수십 초 소요)",
                    emphasize = true,
                    onCancel = onCancelCallDemo,
                )
                CallDemoStep.IDLE -> {}
            }

            if (callDemoResult == CallDemoResult.NO_FOLDER) {
                ErrorBanner("녹음 폴더가 연결되지 않았습니다. 설정 탭에서 폴더를 먼저 연결해주세요.")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("오탐 검증 (자동 순환)")
            Text(
                "정상 메시지와 피싱 메시지를 번갈아 발송해 오탐/미탐을 확인합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!autoTestRunning) {
                CleanCard {
                    Column {
                        SimulationRow(
                            icon = Icons.Filled.Refresh,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            title = "카카오톡 순환",
                            description = "정상 ↔ 피싱 카카오톡 메시지를 번갈아 발송합니다.",
                            enabled = !isBusy && !callBusy,
                            runLabel = "시작",
                            onRun = onStartAutoTestKakao,
                        )
                        RowDivider()
                        SimulationRow(
                            icon = Icons.Filled.Refresh,
                            tint = MaterialTheme.colorScheme.secondaryContainer,
                            title = "SMS 순환",
                            description = "정상 ↔ 피싱 문자를 번갈아 발송합니다.",
                            enabled = !isBusy && !callBusy,
                            runLabel = "시작",
                            onRun = onStartAutoTestSms,
                        )
                    }
                }
            } else {
                ProgressPanel(text = autoTestStatus, emphasize = true, onCancel = onStopAutoTest)
            }
        }
    }
}

@Composable
private fun SimulationRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    enabled: Boolean,
    onRun: () -> Unit,
    runLabel: String = "실행",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon, tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedPillButton(text = runLabel, enabled = enabled, onClick = onRun)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

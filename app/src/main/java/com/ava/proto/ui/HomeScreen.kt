package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

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
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("다채널 피싱 탐지", style = MaterialTheme.typography.headlineMedium)
            Text(
                "통화·문자·카카오톡 중 두 채널 이상에서 위험 신호가 겹치면 알려드립니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            ChannelSetupSection(
                recordingFolderUri = recordingFolderUri,
                notificationAccessGranted = notificationAccessGranted,
                defaultSmsPackage = defaultSmsPackage,
                onConnectFolder = onConnectFolder,
                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                onScanNow = onScanNow,
            )

            HorizontalDivider()

            DemoSection(
                isBusy = isBusy,
                callDemoStep = callDemoStep,
                callDemoResult = callDemoResult,
                hasFolderConnected = recordingFolderUri != null,
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

            HorizontalDivider()

            Text("최근 이벤트 (${uiState.events.size})", style = MaterialTheme.typography.titleMedium)
            uiState.events.forEach { EventRow(it) }
        }
    }
}

@Composable
private fun DemoSection(
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    hasFolderConnected: Boolean,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
) {
    val callBusy = callDemoStep != CallDemoStep.IDLE

    Text("채널별 데모", style = MaterialTheme.typography.titleMedium)
    Text(
        "버튼을 누르면 해당 채널로 피싱 신호가 들어온 것을 시뮬레이션합니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Button(
            onClick = onDemoKakao,
            enabled = !isBusy,
            modifier = Modifier.weight(1f),
        ) {
            Text("카카오톡", style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = onDemoSms,
            enabled = !isBusy,
            modifier = Modifier.weight(1f),
        ) {
            Text("SMS", style = MaterialTheme.typography.labelMedium)
        }
        Button(
            onClick = onDemoCall,
            enabled = !isBusy && !callBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
            ),
            modifier = Modifier.weight(1f),
        ) {
            Text("통화 녹음", style = MaterialTheme.typography.labelMedium)
        }
    }

    // 통화 녹음 진행 상태 표시
    when (callDemoStep) {
        CallDemoStep.COPYING -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                "음성 파일 복사 중...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CallDemoStep.ANALYZING -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "AI가 녹음을 분석 중... (수 초~수십 초 소요)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedButton(onClick = onCancelCallDemo) {
                    Text("중단", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        CallDemoStep.IDLE -> {}
    }

    if (callDemoResult == CallDemoResult.NO_FOLDER) {
        Text(
            "녹음 폴더가 연결되지 않았습니다. 위에서 폴더를 먼저 연결해주세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }

    // ── 오탐 검증: 정상 ↔ 피싱 자동 순환 ───────────────────────────────────
    HorizontalDivider()
    Text("오탐 검증 (자동 순환)", style = MaterialTheme.typography.titleSmall)
    Text(
        "정상 메시지와 피싱 메시지를 번갈아 발송해 오탐/미탐을 확인합니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    if (!autoTestRunning) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = onStartAutoTestKakao,
                enabled = !isBusy && callDemoStep == CallDemoStep.IDLE,
                modifier = Modifier.weight(1f),
            ) {
                Text("카카오톡 순환", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = onStartAutoTestSms,
                enabled = !isBusy && callDemoStep == CallDemoStep.IDLE,
                modifier = Modifier.weight(1f),
            ) {
                Text("SMS 순환", style = MaterialTheme.typography.labelMedium)
            }
        }
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                autoTestStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onStopAutoTest) {
                Text("중단", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ChannelSetupSection(
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    defaultSmsPackage: String?,
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onScanNow: () -> Unit,
) {
    Text("채널 연결", style = MaterialTheme.typography.titleMedium)

    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("통화 녹음", style = MaterialTheme.typography.titleSmall)
            Text(
                recordingFolderUri?.lastPathSegment ?: "연결된 폴더가 없습니다",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "갤럭시: 내부저장소 › Recordings › Call",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConnectFolder) {
                    Text(if (recordingFolderUri == null) "녹음 폴더 연결" else "폴더 변경")
                }
                if (recordingFolderUri != null) {
                    OutlinedButton(onClick = onScanNow) {
                        Text("지금 스캔")
                    }
                }
            }
        }
    }

    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("문자 · 카카오톡", style = MaterialTheme.typography.titleSmall)
            Text(
                "알림 접근 권한 하나로 두 채널을 함께 감시합니다"
                    + (defaultSmsPackage?.let { " (문자 앱: $it)" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (notificationAccessGranted) "상태: 허용됨" else "상태: 거부됨",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = onOpenNotificationAccessSettings) {
                Text("알림 접근 설정 열기")
            }
        }
    }
}

@Composable
private fun EventRow(event: EventEntity) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("[${event.channel}] ${event.sourceLabel}", style = MaterialTheme.typography.bodyMedium)
                Text(formatTime(event.capturedAt), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                event.text ?: "(STT 대기 중)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (event.riskSignal == RiskSignal.HIGH) {
                Text(
                    "위험 신호: ${event.matchedPhrase}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

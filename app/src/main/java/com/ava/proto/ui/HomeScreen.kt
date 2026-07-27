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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.ava.proto.data.SessionEntity
import com.ava.proto.data.SessionState
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
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
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
            )

            HorizontalDivider()

            Text("세션 (${uiState.sessions.size})", style = MaterialTheme.typography.titleMedium)
            if (uiState.sessions.isEmpty()) {
                Text(
                    "아직 위험 신호가 감지된 세션이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            uiState.sessions.forEach { SessionCard(it) }

            HorizontalDivider()

            Text("최근 이벤트 (${uiState.events.size})", style = MaterialTheme.typography.titleMedium)
            uiState.events.forEach { EventRow(it) }
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
            Button(onClick = onConnectFolder) {
                Text(if (recordingFolderUri == null) "녹음 폴더 연결" else "폴더 변경")
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
private fun SessionCard(session: SessionEntity) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (session.state) {
                SessionState.SUSPECTED -> MaterialTheme.colorScheme.surfaceVariant
                SessionState.ESCALATED, SessionState.ALERT -> MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.state.name, style = MaterialTheme.typography.titleSmall)
                Text(formatTime(session.updatedAt), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "관련 채널: ${session.channelsInvolved.joinToString(", ").ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
            )
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

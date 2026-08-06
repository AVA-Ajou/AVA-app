package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal

/**
 * 랜딩 화면. 숫자는 전부 실제 이벤트에서 계산한다 — 데모용 고정값을 박아두면
 * 탐지가 동작하는지 화면만 보고는 알 수 없게 된다.
 */
@Composable
fun DashboardTab(
    events: List<EventEntity>,
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    onScanNow: () -> Unit,
    onViewAllEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val riskyCount = events.count { it.riskSignal == RiskSignal.HIGH }
    val safePercent = if (events.isEmpty()) 100 else (events.size - riskyCount) * 100 / events.size

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenTitle("AVA", "실시간으로 감시하고 있습니다.")

        StatusCard(
            safePercent = safePercent,
            riskyCount = riskyCount,
            totalCount = events.size,
            scanEnabled = recordingFolderUri != null,
            onScanNow = onScanNow,
        )

        ChannelStatusCard(
            callActive = recordingFolderUri != null,
            messagingActive = notificationAccessGranted,
        )

        RecentActivityCard(events = events.take(3), onViewAllEvents = onViewAllEvents)
    }
}

@Composable
private fun StatusCard(
    safePercent: Int,
    riskyCount: Int,
    totalCount: Int,
    scanEnabled: Boolean,
    onScanNow: () -> Unit,
) {
    val safe = riskyCount == 0
    CleanCard {
        Column(
            // fillMaxWidth 가 없으면 Column 이 가장 넓은 자식(설명 문구)만큼만 차지해
            // 카드 왼쪽에 붙는다 — 중앙 정렬은 그 좁은 폭 안에서만 일어난다.
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreRing(percent = safePercent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (safe) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (safe) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        "$safePercent%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "안전",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                when {
                    totalCount == 0 -> "감시 대기 중"
                    safe -> "시스템 안전"
                    else -> "위험 신호 ${riskyCount}건 감지"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (safe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                when {
                    totalCount == 0 -> "아직 수신된 이벤트가 없습니다. 시뮬레이션 탭에서 신호를 발생시켜 보세요."
                    safe -> "수신한 ${totalCount}건 모두 무해한 것으로 분류됐습니다."
                    else -> "전체 ${totalCount}건 중 ${riskyCount}건이 피싱으로 분류됐습니다."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            PrimaryPillButton(
                text = "지금 스캔",
                icon = Icons.Filled.Refresh,
                enabled = scanEnabled,
                onClick = onScanNow,
            )
            if (!scanEnabled) {
                Text(
                    "녹음 폴더를 연결하면 스캔할 수 있습니다 (설정 탭)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ChannelStatusCard(callActive: Boolean, messagingActive: Boolean) {
    CleanCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel("감시 중인 채널", color = MaterialTheme.colorScheme.primary)
            ChannelRow(
                icon = Icons.Filled.Call,
                title = "통화 녹음",
                // 알림 리스너와 달리 통화는 폴더 연결이 전제라 상태 근거가 다르다.
                status = if (callActive) "감시 중 · SAF 폴더 연결됨" else "폴더 미연결",
                active = callActive,
            )
            ChannelRow(
                icon = Icons.Filled.Email,
                title = "문자 (SMS)",
                status = if (messagingActive) "감시 중 · 알림 접근 허용" else "알림 접근 거부됨",
                active = messagingActive,
            )
            ChannelRow(
                icon = Icons.AutoMirrored.Filled.Send,
                title = "카카오톡",
                status = if (messagingActive) "감시 중 · 알림 접근 허용" else "알림 접근 거부됨",
                active = messagingActive,
            )
        }
    }
}

@Composable
private fun ChannelRow(icon: ImageVector, title: String, status: String, active: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        IconBubble(
            icon,
            if (active) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.outline
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusDot(active)
    }
}

@Composable
private fun RecentActivityCard(events: List<EventEntity>, onViewAllEvents: () -> Unit) {
    CleanCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel("최근 활동", color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onViewAllEvents, shape = PillShape) {
                    Text("전체 보기", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (events.isEmpty()) {
                Text(
                    "기록된 활동이 없습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            events.forEach { ActivityRow(it) }
        }
    }
}

@Composable
private fun ActivityRow(event: EventEntity) {
    val risky = event.riskSignal == RiskSignal.HIGH
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconBubble(
            if (risky) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            if (risky) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiaryContainer,
            size = 32,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (risky) "피싱 의심 신호 탐지" else "정상 메시지 확인",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                event.text ?: "(STT 대기 중)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Text(
            formatTime(event.capturedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

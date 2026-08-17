package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ava.proto.R
import com.ava.proto.capture.Channel
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal

/**
 * 랜딩 화면. 숫자는 전부 실제 이벤트에서 계산한다 — 데모용 고정값을 박아두면
 * 탐지가 동작하는지 화면만 보고는 알 수 없게 된다.
 *
 * **마스코트는 평온할 때만 나온다.** 웃는 캐릭터를 `위험 신호 2건` 옆에 두면 화면이
 * 말하는 것과 그림이 말하는 것이 어긋난다. 위험할 때는 캐릭터 자리를 경고 카드가 대신한다.
 */
@Composable
fun DashboardTab(
    events: List<EventEntity>,
    escalatedSessionIds: Set<Long> = emptySet(),
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    onScanNow: () -> Unit,
    onViewAllEvents: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 경보우려까지만 센다. 알림은 예보에서부터 나가지만, 이 화면 맨 위의 큰 숫자는 **지금
    // 당장 봐야 하는 건수**를 뜻한다. 판정기 어느 쪽도 확신하지 않은 예보까지 여기 넣으면
    // 그 숫자가 무엇을 세는지 흐려진다 — 등급별 내역은 기록 탭의 칩이 보여준다.
    val riskyCount = events.count {
        it.riskSignal == RiskSignal.HIGH || it.riskSignal == RiskSignal.HIGH_UNBACKED
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        StatusHero(
            riskyCount = riskyCount,
            totalCount = events.size,
            scanEnabled = recordingFolderUri != null,
            onScanNow = onScanNow,
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("감시 중인 채널")
            ChannelCard(
                callActive = recordingFolderUri != null,
                messagingActive = notificationAccessGranted,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("최근 활동", actionLabel = "전체 보기", onAction = onViewAllEvents)
            RecentActivityCard(events = events.take(3), escalatedSessionIds = escalatedSessionIds)
        }
    }
}

/**
 * 화면의 주인공. 상태에 따라 **그림이 통째로 갈린다** — 색만 바꾸면 목업 크기에서 두 상태가
 * 같아 보인다.
 */
@Composable
private fun StatusHero(
    riskyCount: Int,
    totalCount: Int,
    scanEnabled: Boolean,
    onScanNow: () -> Unit,
) {
    val safe = riskyCount == 0

    CleanCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .background(
                            if (safe) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.24f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            },
                            CircleShape,
                        ),
                )
                if (safe) {
                    Image(
                        painterResource(R.drawable.ic_avamon_mascot),
                        contentDescription = null,
                        modifier = Modifier.size(124.dp),
                    )
                } else {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(62.dp),
                    )
                }
            }

            Text(
                when {
                    totalCount == 0 -> "감시를 시작했어요"
                    safe -> "지금은 안전해요"
                    else -> "위험 신호 ${riskyCount}건"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = if (safe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
            Text(
                when {
                    totalCount == 0 -> "통화 녹음 · 문자 · 카카오톡을 함께 봅니다.\n시뮬레이션 탭에서 신호를 만들어볼 수 있어요."
                    safe -> "확인한 연락 ${totalCount}건에서 위험 신호가 없었어요."
                    else -> "확인한 ${totalCount}건 중 ${riskyCount}건이 피싱으로 분류됐어요."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (scanEnabled) {
                PrimaryPillButton(
                    text = "지금 스캔",
                    icon = Icons.Filled.Refresh,
                    onClick = onScanNow,
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(callActive: Boolean, messagingActive: Boolean) {
    CleanCard {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            ChannelRow(
                channel = Channel.CALL,
                // 알림 리스너와 달리 통화는 폴더 연결이 전제라 상태 근거가 다르다.
                status = if (callActive) "녹음 폴더 연결됨" else "폴더 미연결 (설정 탭)",
                active = callActive,
            )
            RowDivider()
            ChannelRow(
                channel = Channel.SMS,
                status = if (messagingActive) "알림 접근 허용됨" else "알림 접근 거부됨",
                active = messagingActive,
            )
            RowDivider()
            ChannelRow(
                channel = Channel.KAKAO,
                status = if (messagingActive) "알림 접근 허용됨" else "알림 접근 거부됨",
                active = messagingActive,
            )
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, status: String, active: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        IconBubble(
            channelIcon(channel),
            // 꺼진 채널까지 채널색으로 칠하면 "감시 중"과 구분이 안 된다.
            if (active) channelColor(channel) else MaterialTheme.colorScheme.outline,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                channel.label,
                style = MaterialTheme.typography.titleMedium,
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
private fun RecentActivityCard(events: List<EventEntity>, escalatedSessionIds: Set<Long> = emptySet()) {
    CleanCard {
        if (events.isEmpty()) {
            Text(
                "기록된 활동이 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
            return@CleanCard
        }
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            events.forEachIndexed { index, event ->
                if (index > 0) RowDivider()
                ActivityRow(
                    event = event,
                    isMultiChannel = event.sessionId != null && event.sessionId in escalatedSessionIds,
                )
            }
        }
    }
}

@Composable
private fun ActivityRow(event: EventEntity, isMultiChannel: Boolean = false) {
    // 주의보를 경보와 같은 빨강으로 묶지 않는다. 이 구간은 모델이 애매하다고 본 자리라
    // 실제로 정상이 섞여 들어온다 — 색까지 같으면 사용자가 둘을 구분할 방법이 없다.
    // 예보는 그보다 한 칸 더 약해 노랑으로 둔다. 어느 판정기도 위험하다고 하지 않았다.
    val risky = event.riskSignal == RiskSignal.HIGH
    val unbacked = event.riskSignal == RiskSignal.HIGH_UNBACKED
    val cautious = event.riskSignal == RiskSignal.CAUTION
    val watch = event.riskSignal == RiskSignal.FORECAST
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        IconBubble(
            if (risky || unbacked || cautious || watch) Icons.Filled.Warning else Icons.Filled.CheckCircle,
            when {
                risky -> MaterialTheme.colorScheme.error
                unbacked || cautious -> caution
                watch -> forecast
                else -> MaterialTheme.colorScheme.tertiaryContainer
            },
            size = 32,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    event.channel.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    formatTime(event.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when {
                    risky -> "피싱 의심 신호 탐지"
                    unbacked -> "피싱 경보우려"
                    cautious -> "피싱 주의보"
                    watch -> "피싱 예보"
                    else -> "정상 메시지 확인"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                event.text ?: "(STT 대기 중)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (risky || isMultiChannel) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (risky) {
                        Text(
                            "피싱 의심",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (isMultiChannel) {
                        MultiChannelTag()
                    }
                }
            }
        }
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 18.dp),
    )
}

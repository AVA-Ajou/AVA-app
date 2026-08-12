package com.ava.proto.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ava.proto.capture.Channel
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
fun HistoryTab(events: List<EventEntity>, modifier: Modifier = Modifier) {
    val riskyCount = events.count { it.riskSignal == RiskSignal.HIGH }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("기록", "캡처된 이벤트와 분류 결과입니다.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                "전체 ${events.size}",
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            StatusBadge(
                "위험 $riskyCount",
                content = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            )
        }

        if (events.isEmpty()) {
            Text(
                "아직 수신된 이벤트가 없습니다. 시뮬레이션 탭에서 신호를 발생시켜 보세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        events.forEach { EventCard(it) }
    }
}

@Composable
internal fun EventCard(event: EventEntity) {
    CleanCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    val color = channelColor(event.channel)
                    StatusBadge(
                        event.channel.name,
                        content = color,
                        container = color.copy(alpha = 0.14f),
                    )
                    Text(
                        event.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatTime(event.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                event.text ?: "(STT 대기 중)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // **위험도 숫자는 화면에 내지 않는다.** 값이 사실상 0 아니면 100으로 갈려
            // "위험도 100.0"이 "피싱임"과 같은 말이 되고, 사용자가 할 행동을 정하는 것은
            // 점수가 아니라 단계다 — 같은 100점이어도 압박 단계면 끊으면 되고 이체 지시
            // 단계면 몇 분 안에 돈이 나간다.
            //
            // 지우는 게 아니라 화면에서만 뺀다. 원본 값은 `EventEntity.risk` 와
            // `BackendClassification` 로그에 그대로 남는다 — 59.8 같은 경계선 오탐은
            // 숫자로만 보이기 때문에 개발 중에는 볼 수 있어야 한다.
            //
            // 근거 문장(`matchedPhrase`)도 화면에 내지 않는다. 모델이 인용은 정확히 하지만
            // **가장 결정적인 문구를 못 고른다** — 계좌번호를 부르는 대목 대신 "통화가
            // 녹취됩니다"를 뽑아오는 것을 두 번 확인했다. DB에는 그대로 남는다.
            //
            // 그래서 카드에 남는 판정 표시는 단계 배지 하나뿐이고, 예전 경고 줄이 쓰던
            // 생김새(느낌표 + 빨간 글씨 + 테두리)를 그 배지로 옮겼다.
            if (event.riskSignal == RiskSignal.HIGH) {
                val stage = event.stage
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        // 규칙이 신호를 하나도 못 찾으면 단계가 null이다. 없는 근거로 1단계를
                        // 찍지 않는다 — 피싱이라는 사실만 전한다.
                        if (stage != null) "${stage}단계 ${event.stageLabel.orEmpty()}".trim()
                        else "피싱 의심",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun channelColor(channel: Channel): Color = when (channel) {
    Channel.KAKAO -> MaterialTheme.colorScheme.primaryContainer
    Channel.SMS -> MaterialTheme.colorScheme.secondaryContainer
    Channel.CALL -> MaterialTheme.colorScheme.error
}

internal fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

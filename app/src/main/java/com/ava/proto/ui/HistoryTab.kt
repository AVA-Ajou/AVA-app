package com.ava.proto.ui

import androidx.compose.foundation.background
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
            if (event.riskSignal == RiskSignal.HIGH) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "위험 신호: ${event.matchedPhrase}",
                        style = MaterialTheme.typography.bodySmall,
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

package com.ava.proto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenTitle("기록", "캡처된 연락과 분류 결과입니다.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                "전체 ${events.size}",
                content = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            StatusBadge(
                "위험 $riskyCount",
                content = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
            )
        }

        if (events.isEmpty()) {
            CleanCard {
                Text(
                    "아직 수신된 연락이 없습니다.\n시뮬레이션 탭에서 신호를 발생시켜 보세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        events.forEach { EventCard(it) }
    }
}

@Composable
internal fun EventCard(event: EventEntity) {
    CleanCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBubble(channelIcon(event.channel), channelColor(event.channel), size = 40)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        event.channel.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        event.sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    formatTime(event.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 본문은 연보라 판 위에 올린다. 흰 카드에 그대로 두면 목업 크기에서 글자 벽으로만
            // 보이고, 판을 깔면 "받은 내용"과 "우리 판정"이 시각적으로 갈린다.
            SoftBlock {
                Text(
                    event.text ?: "(전사 대기 중)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }

            // **위험도 숫자는 화면에 내지 않는다.** 값이 사실상 0 아니면 100으로 갈려
            // "위험도 100.0"이 "피싱임"과 같은 말이 되고, 사용자가 할 행동을 정하는 것은
            // 점수가 아니라 단계다 — 같은 100점이어도 압박 단계면 끊으면 되고 이체 지시
            // 단계면 몇 분 안에 돈이 나간다. 지우는 게 아니라 화면에서만 뺀 것이라
            // 원본은 EventEntity.risk 와 BackendClassification 로그에 그대로 남는다.
            //
            // 근거 문장(matchedPhrase)도 내지 않는다. 모델이 인용은 정확히 하지만 **가장
            // 결정적인 문구를 못 고른다** — 계좌번호를 부르는 대목 대신 "통화가 녹취됩니다"를
            // 뽑아오는 것을 두 번 확인했다.
            //
            // **색이 두 가지인 이유** — 규칙이 신호를 하나도 못 찾으면 단계가 null이다.
            // 없는 근거로 1단계를 찍지 않고 `주의 필요`를 주황으로 띄운다. 모델만 위험하다고
            // 본 상태이기 때문이다. 반대로 진짜 피싱의 19%도 여기 걸리므로 걸러내지는 않는다.
            if (event.riskSignal == RiskSignal.HIGH) {
                val stage = event.stage
                val tint = if (stage != null) MaterialTheme.colorScheme.error else caution
                StageBadge(
                    tint = tint,
                    text = if (stage != null) {
                        "${stage}단계 ${event.stageLabel.orEmpty()}".trim()
                    } else {
                        "주의 필요"
                    },
                )
            }
        }
    }
}

@Composable
private fun StageBadge(tint: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(tint.copy(alpha = 0.12f), PillShape)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
    }
}

internal fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

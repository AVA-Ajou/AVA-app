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
    val cautionCount = events.count { it.riskSignal == RiskSignal.CAUTION }

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
                "경보 $riskyCount",
                content = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            )
            // 경보와 합쳐 세지 않는다. 두 등급은 근거가 다르고(모델 단독 / 두 판정기의 교집합)
            // 합치면 어느 쪽이 늘었는지 안 보인다 — 학습셋을 보강했을 때 볼 값이 이것이다.
            if (cautionCount > 0) {
                StatusBadge(
                    "주의보 $cautionCount",
                    content = caution,
                    container = caution.copy(alpha = 0.15f),
                )
            }
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
            //
            // **주황이 두 자리에서 나온다.** 둘 다 "한쪽 판정기만 위험하다고 본 상태"다.
            //
            //   경보(빨강)   위험도 66 이상 + 단계 있음 — 둘 다 위험하다고 봤다
            //   경보(주황)   위험도 66 이상 + 단계 없음 — 모델만 봤다. `경보 · 주의`
            //   주의보(주황) 위험도 33~66 + 단계 있음   — 규칙만 봤다. `주의보 · N단계`
            //
            // 모델만 본 쪽을 빨강으로 올리지 않는 이유는 건강보험공단 환급금 안내(정상)가
            // 위험도 99.0을 받은 일이 있어서다. 규칙은 그 통화에서 정보 요구도 이체 지시도
            // 못 찾았고, 실제로 없었다. 반대로 진짜 피싱의 19%도 여기 걸리므로(검증셋 실측)
            // **걸러내지는 않는다** — 미탐은 돈이 나가고 오탐은 짜증에 그친다.
            //
            // 주의보 쪽을 문구로 구분하는 이유는 원인이 정반대라서다. 화면에서는 같은 주황을
            // 쓰지만, 로그를 되짚을 때 어느 판정기가 켰는지 모르면 오판을 못 쫓아간다.
            if (event.riskSignal != RiskSignal.NONE) {
                val stage = event.stage
                val alert = event.riskSignal == RiskSignal.HIGH
                val tint = if (alert && stage != null) MaterialTheme.colorScheme.error else caution
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .border(1.dp, tint, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        when {
                            // 등급 없이 단계만 띄우면 빨강·주황이 색으로만 갈려, 색을 못 가리는
                            // 눈에는 둘이 같은 배지가 된다. 등급 이름을 앞에 붙여 글로도 읽히게 한다.
                            //
                            // 단계 없는 경보를 `경보 · 주의`로 낮춰 부르는 이유 — 모델만 위험하다고
                            // 본 상태라 규칙이 뒷받침하지 않는다. 정상 통화가 위험도 99.0을 받은
                            // 일이 실제로 있었다. 등급은 경보로 두되(진짜 피싱의 19%가 여기 있다)
                            // 말로는 단정하지 않는다.
                            stage == null -> "경보 · 주의"
                            alert -> "경보 · ${stage}단계 ${event.stageLabel.orEmpty()}".trim()
                            else -> "주의보 · ${stage}단계 ${event.stageLabel.orEmpty()}".trim()
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
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

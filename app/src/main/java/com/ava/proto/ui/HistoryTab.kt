package com.ava.proto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun HistoryTab(
    events: List<EventEntity>,
    escalatedSessionIds: Set<Long> = emptySet(),
    modifier: Modifier = Modifier,
) {
    // 아래 카드가 붙이는 배지와 같은 갈래로 센다. 둘을 한 숫자에 뭉치면 규칙이 뒷받침한
    // 경보와 모델만 본 `경보우려`가 섞여, 요약과 카드가 서로 다른 말을 한다.
    val riskyCount = events.count { it.riskSignal == RiskSignal.HIGH }
    val unbackedCount = events.count { it.riskSignal == RiskSignal.HIGH_UNBACKED }
    val cautionCount = events.count { it.riskSignal == RiskSignal.CAUTION }
    val forecastCount = events.count { it.riskSignal == RiskSignal.FORECAST }

    // 등급 안내는 접어둔다. 배지 이름만으로 순서(예보 < 주의보 < 경보우려 < 경보)를 알 수
    // 없다는 것이 이 버튼을 만든 이유인데, 그렇다고 다섯 줄짜리 설명을 늘 펴두면 정작
    // 목록이 아래로 밀린다. 알고 싶을 때만 열게 한다.
    var guideOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ScreenTitle("기록", "캡처된 연락과 분류 결과입니다.", modifier = Modifier.weight(1f))
            GuideToggle(open = guideOpen, onToggle = { guideOpen = !guideOpen })
        }

        if (guideOpen) TierGuide()

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                "전체 ${events.size}",
                content = MaterialTheme.colorScheme.primary,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            StatusBadge(
                "경보 $riskyCount",
                content = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.errorContainer,
            )
            // 아래 세 칩은 0이면 접는다. 등급을 늘 다 띄우면 대부분의 화면이 `0`을 세 개 달고
            // 있게 되고, 실제로 뭔가 잡힌 날의 숫자가 그만큼 덜 보인다.
            // 경보는 0이어도 남긴다 — 이 화면이 무엇을 세는 화면인지 알리는 기준점이다.
            if (unbackedCount > 0) {
                StatusBadge(
                    "경보우려 $unbackedCount",
                    content = caution,
                    container = caution.copy(alpha = 0.15f),
                )
            }
            // 주의보를 경보 쪽에 합쳐 세지 않는다. 두 등급은 근거가 다르고(모델 단독 /
            // 두 판정기의 교집합) 합치면 어느 쪽이 늘었는지 안 보인다 — 학습셋을 보강했을 때
            // 볼 값이 이것이다.
            if (cautionCount > 0) {
                StatusBadge(
                    "주의보 $cautionCount",
                    content = caution,
                    container = caution.copy(alpha = 0.15f),
                )
            }
            // 예보를 정상 쪽으로 밀어 세지 않는다. 이 숫자가 크다는 것은 모델이 애매해한
            // 통화가 그만큼 많았다는 뜻이고, 학습셋을 보강할 자리를 가리키는 값이 이것이다.
            if (forecastCount > 0) {
                StatusBadge(
                    "예보 $forecastCount",
                    content = forecast,
                    container = forecast.copy(alpha = 0.15f),
                )
            }
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
        events.forEach { event ->
            EventCard(
                event = event,
                isMultiChannel = event.sessionId != null && event.sessionId in escalatedSessionIds,
            )
        }
    }
}

/** 제목 옆의 여닫이 버튼. 열려 있을 때 화살표가 뒤집혀, 다시 누르면 접힌다는 것을 알린다. */
@Composable
private fun GuideToggle(open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(
            Icons.Filled.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "등급 설명",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (open) "접기" else "펼치기",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * 다섯 등급을 센 것부터 약한 것 순으로 늘어놓는 안내. 사용자가 이 카드를 여는 때는 방금 본
 * 배지가 무엇인지 궁금할 때이고, 그 배지는 대개 위쪽 등급이다.
 *
 * **위험도 숫자도 진행 단계도 적지 않는다.** 화면 어디에도 내보내지 않기로 한 값을 안내에만
 * 적으면, 사용자가 배지에서 찾을 수 없는 기준을 머릿속에 들고 목록을 보게 된다. 대신 각
 * 등급이 무엇을 근거로 켜졌는지를 적는다 — 그것이 등급 사이의 실제 차이다.
 */
@Composable
private fun TierGuide() {
    CleanCard {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "판정 등급",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "AVA는 통화 내용을 두 갈래로 봅니다. " +
                    "학습된 모델이 매기는 위험도와 사기 진행 문형을 찾는 규칙입니다. " +
                    "둘 중 몇 개가 위험하다고 봤는지에 따라 아래 다섯 등급이 갈립니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TierRow(
                color = MaterialTheme.colorScheme.error,
                name = "경보",
                detail = "둘 다 위험하다고 봤거나, 모델이 혼자서 단정할 만큼 높습니다.",
            )
            TierRow(
                color = caution,
                name = "경보우려",
                detail = "모델만 위험하다고 보고 있으며, 뚜렷한 규칙은 나타나지 않습니다.",
            )
            TierRow(
                color = caution,
                name = "주의보",
                detail = "모델이 일부 우려를 나타내고 있으며, 규칙은 위험하다고 봅니다.",
            )
            TierRow(
                color = forecast,
                name = "예보",
                detail = "모델이 일부 우려를 나타내고 있으며, 뚜렷한 규칙은 나타나지 않습니다.",
            )
            TierRow(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                name = "정상",
                detail = "위험 신호가 없습니다.",
            )
        }
    }
}

@Composable
private fun TierRow(color: Color, name: String, detail: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // 색 점을 첫 줄 높이에 맞춰 띄운다. 세로 가운데에 두면 설명이 두 줄일 때 등급 이름과
        // 어긋나 어느 줄에 붙은 점인지 흐려진다.
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .background(color, CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun EventCard(event: EventEntity, isMultiChannel: Boolean = false) {
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
                    event.text ?: "(음성 변환 대기 중)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(14.dp),
                )
            }

            // **화면에 나가는 것은 등급 이름 하나뿐이다.** 위험도 숫자도, 진행 단계도,
            // 근거 문장도 내지 않는다.
            //
            //   위험도    값이 사실상 0 아니면 100으로 갈려 "위험도 100.0"이 "피싱임"과
            //             같은 말이 된다. 숫자를 보여줄 이유가 없다
            //   단계      **등급을 가르는 재료로만 쓴다.** 규칙이 정답지 36건에서 91.7%로
            //             맞히지만 그건 "단계가 있다/없다"를 가르는 정확도지, 1단계인지
            //             2단계인지까지 맞다는 뜻이 아니다. 틀린 단계를 띄우면 사용자가
            //             "아직 2단계니까 괜찮다"고 읽는다 — 등급만 내보내면 그 오독이 없다
            //   근거 문장  모델이 인용은 정확히 하지만 **가장 결정적인 문구를 못 고른다** —
            //             계좌번호를 부르는 대목 대신 "통화가 녹취됩니다"를 뽑아오는 것을
            //             두 번 확인했다
            //
            // 셋 다 지우는 게 아니라 화면에서만 뺀 것이라 EventEntity 와 로그에는 남는다.
            //
            // **색은 켜진 판정기의 수를 뜻한다.** 빨강은 둘 다, 주황은 한쪽만, 노랑은 어느
            // 쪽도 위험하다고 하지 않았다는 뜻이다. 등급 이름을 함께 쓰는 이유는 색을 못
            // 가리는 눈에는 배지가 전부 같은 모양이기 때문이다.
            //
            //   경보(빨강)   위험도 80 이상 또는 50 이상+단계 — 단정할 만하다
            //   경보우려(주황) 위험도 50~80 + 단계 없음        — 모델만 봤다
            //   주의보(주황) 위험도 20~50 + 단계 있음         — 규칙만 봤다
            //   예보(노랑)   위험도 20~50 + 단계 없음         — 어느 쪽도 확신하지 않았다
            //
            // 주의보와 경보우려가 같은 주황인데도 이름을 갈라두는 이유는 원인이 정반대라서다.
            // 로그를 되짚을 때 어느 판정기가 켰는지 모르면 오판을 못 쫓아간다.
            //
            // **등급 판단을 여기서 다시 하지 않는다.** 위험도 문턱은 전부
            // `BackendClassificationClient` 에 있고, 이 화면은 [RiskSignal]에 이름과 색만 붙인다.
            if (event.riskSignal != RiskSignal.NONE || isMultiChannel) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (event.riskSignal != RiskSignal.NONE) {
                        val alert = event.riskSignal == RiskSignal.HIGH
                        val unbacked = event.riskSignal == RiskSignal.HIGH_UNBACKED
                        val watch = event.riskSignal == RiskSignal.FORECAST
                        val tint = when {
                            alert -> MaterialTheme.colorScheme.error
                            watch -> forecast
                            else -> caution
                        }
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
                                    watch -> "예보"
                                    unbacked -> "경보우려"
                                    alert -> "경보"
                                    else -> "주의보"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = tint,
                            )
                        }
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

/**
 * 같은 시간 창 안에 다른 채널에서도 신호가 잡혀 세션이 ESCALATED로 격상됐음을 표시하는 태그.
 *
 * 위험도 배지와 나란히 붙는다. 위험도는 "이 메시지 자체가 얼마나 위험한가"를 뜻하고,
 * 이 태그는 "다른 채널과 엮여 있다"는 맥락 정보다 — 둘은 다른 층위의 정보라 함께 보여도
 * 충돌하지 않는다. 섞이지 않도록 보라 계열 색을 써서 위험도 배지(빨강·주황·노랑)와 구분한다.
 */
@Composable
internal fun MultiChannelTag() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "다채널 탐지",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

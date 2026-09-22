package com.ava.proto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ava.proto.R
import com.ava.proto.capture.Channel
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/** 본문을 접어둘 때 보이는 줄 수. 통화 전사본은 여덟 줄이 넘어가 카드 하나가 화면을 다 먹었다. */
private const val COLLAPSED_LINES = 3

@Composable
fun HistoryTab(
    events: List<EventEntity>,
    escalatedSessionIds: Set<Long>,
    modifier: Modifier = Modifier,
) {
    // 아래 카드가 붙이는 칩과 같은 갈래로 센다. 둘을 한 숫자에 뭉치면 규칙이 뒷받침한
    // 경보와 모델만 본 `경보우려`가 섞여, 요약과 카드가 서로 다른 말을 한다.
    val riskyCount = events.count { it.riskSignal == RiskSignal.HIGH }
    val unbackedCount = events.count { it.riskSignal == RiskSignal.HIGH_UNBACKED }
    val cautionCount = events.count { it.riskSignal == RiskSignal.CAUTION }
    val forecastCount = events.count { it.riskSignal == RiskSignal.FORECAST }

    // 등급 안내는 접어둔다. 칩 이름만으로 순서(예보 < 주의보 < 경보우려 < 경보)를 알 수
    // 없다는 것이 이 버튼을 만든 이유인데, 그렇다고 다섯 줄짜리 설명을 늘 펴두면 정작
    // 목록이 아래로 밀린다. 알고 싶을 때만 열게 한다.
    var guideOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageHeader("기록") {
            GuideToggle(open = guideOpen, onToggle = { guideOpen = !guideOpen })
        }

        if (guideOpen) TierGuide()

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            Chip("전체 ${events.size}", MaterialTheme.colorScheme.onSurfaceVariant)
            // 사건 수가 아니라 **다채널로 격상된 사건 수**다. 이벤트 건수와 따로 세는 이유는
            // 두 숫자가 다른 것을 재기 때문이다 — 이벤트는 몇 건이 들어왔나이고, 이쪽은
            // 몇 번의 시도가 경로를 갈아타며 이어졌나다.
            if (escalatedSessionIds.isNotEmpty()) {
                Chip("다채널 ${escalatedSessionIds.size}", MaterialTheme.colorScheme.error, strong = true)
            }
            // 경보는 0이어도 남긴다 — 이 화면이 무엇을 세는 화면인지 알리는 기준점이다.
            // 아래 세 칩은 0이면 접는다. 등급을 늘 다 띄우면 대부분의 화면이 `0`을 세 개 달고
            // 있게 되고, 실제로 뭔가 잡힌 날의 숫자가 그만큼 덜 보인다.
            Chip("경보 $riskyCount", MaterialTheme.colorScheme.error)
            if (unbackedCount > 0) Chip("경보우려 $unbackedCount", caution)
            // 주의보를 경보 쪽에 합쳐 세지 않는다. 두 등급은 근거가 다르고(모델 단독 /
            // 두 판정기의 교집합) 합치면 어느 쪽이 늘었는지 안 보인다.
            if (cautionCount > 0) Chip("주의보 $cautionCount", caution)
            // 예보를 정상 쪽으로 밀어 세지 않는다. 이 숫자가 크다는 것은 모델이 애매해한
            // 통화가 그만큼 많았다는 뜻이고, 학습셋을 보강할 자리를 가리키는 값이 이것이다.
            if (forecastCount > 0) Chip("예보 $forecastCount", forecast)
        }

        if (events.isEmpty()) {
            EmptyState(
                image = R.drawable.ic_avamon_sleep,
                title = "아직 기록이 없어요",
                detail = "시뮬레이션 탭에서 신호를 만들어볼 수 있어요",
            )
        }

        // 딱지는 **합류한 쪽에만** 붙인다. 세션을 연 첫 연락은 그때까지 채널이 하나뿐이라
        // 다채널이 아니었고, 뒤이어 다른 경로로 들어온 연락이 사건을 다채널로 만든다.
        // 둘 다 붙이면 "처음부터 다채널이었다"로 읽혀, 단독으로는 확정되지 않았다는
        // 이 화면의 요점이 흐려진다.
        val joinedEventIds = events
            .filter { it.sessionId != null && it.sessionId in escalatedSessionIds }
            .groupBy { it.sessionId }
            .values
            .flatMap { group -> group.sortedBy { it.capturedAt }.drop(1) }
            .mapTo(mutableSetOf()) { it.id }

        events.forEach { EventCard(it, multiChannel = it.id in joinedEventIds) }
    }
}

/** 제목 옆의 여닫이 버튼. 열려 있을 때 화살표가 뒤집혀, 다시 누르면 접힌다는 것을 알린다. */
@Composable
private fun GuideToggle(open: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(PillShape)
            .clickable(onClick = onToggle)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(start = 10.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
    ) {
        Icon(
            AppIcons.info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            "등급 안내",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Icon(
            if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (open) "접기" else "펼치기",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * 다섯 등급을 센 것부터 약한 것 순으로 늘어놓는 안내. 사용자가 이 카드를 여는 때는 방금 본
 * 칩이 무엇인지 궁금할 때이고, 그 칩은 대개 위쪽 등급이다.
 *
 * **위험도 숫자도 진행 단계도 적지 않는다.** 화면 어디에도 내보내지 않기로 한 값을 안내에만
 * 적으면, 사용자가 칩에서 찾을 수 없는 기준을 머릿속에 들고 목록을 보게 된다. 대신 각
 * 등급이 무엇을 근거로 켜졌는지를 적는다 — 그것이 등급 사이의 실제 차이다.
 */
@Composable
private fun TierGuide() {
    CleanCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Avamon은 연락을 두 갈래로 봅니다. 학습된 모델이 매기는 위험도와 사기 진행 문형을 " +
                    "찾는 규칙입니다. 둘 중 몇 개가 위험하다고 봤는지에 따라 등급이 갈립니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TierRow(RiskSignal.HIGH, "둘 다 위험하다고 봤거나, 모델이 혼자서 단정할 만큼 높습니다.")
            TierRow(RiskSignal.HIGH_UNBACKED, "모델만 위험하다고 보고, 뚜렷한 사기 문형은 없습니다.")
            TierRow(RiskSignal.CAUTION, "모델은 일부 우려를 보이고, 규칙이 사기 문형을 찾았습니다.")
            TierRow(RiskSignal.FORECAST, "모델이 일부 우려를 보이지만, 뚜렷한 문형은 없습니다.")
            TierRow(RiskSignal.NONE, "위험 신호가 없습니다.")
        }
    }
}

@Composable
private fun TierRow(signal: RiskSignal, detail: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 칩을 실제 카드에 붙는 것과 같은 모양으로 그린다 — 안내에서 본 것을 목록에서
        // 그대로 찾을 수 있어야 한다. 칸 폭을 고정하는 것은 `경보우려`와 `예보`의 폭 차이로
        // 설명 줄의 시작점이 들쭉날쭉해지지 않게 하기 위해서다.
        Box(modifier = Modifier.width(76.dp)) { TierChip(signal) }
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(top = 3.dp),
        )
    }
}

/**
 * 기록 한 건.
 *
 * **등급 칩은 맨 위 오른쪽이다.** 카드에서 가장 중요한 정보인데 예전에는 본문 아래에 있어,
 * 통화 전사본처럼 본문이 긴 카드에서는 스크롤 밖으로 밀렸다.
 *
 * 제목은 상대방이다. 발신처를 모르는 통화만 채널 이름이 제목이 되고, 그 경우 파일명을 부제에
 * 적어 어느 녹음인지 알 수 있게 한다. 문자·카카오톡의 `sourceLabel` 은 **패키지명**이라
 * 화면에 내지 않는다 — 사용자가 볼 화면에 `com.…` 식별자가 나오면 미완성으로 읽힌다.
 */
@Composable
internal fun EventCard(event: EventEntity, multiChannel: Boolean = false) {
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        event.counterpart ?: event.channel.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildString {
                            // 제목이 이미 채널 이름이면(상대방을 모르는 통화) 부제에서 한 번
                            // 더 적지 않는다 — 같은 단어가 두 줄 연속으로 오면 그만큼 파일명이
                            // 잘린다.
                            if (event.counterpart != null) {
                                append(event.channel.label)
                                append(" · ")
                            }
                            append(formatTime(event.capturedAt))
                            if (event.channel == Channel.CALL) {
                                append(" · ")
                                append(event.sourceLabel)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 다채널 딱지는 등급 칩 옆이다. 이 카드가 **혼자가 아니라는 것**을 알려주는
                // 표시라 등급과 나란히 읽혀야 하되, 채워진 색으로 그려 등급과 구분한다.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (multiChannel) Chip("다채널", MaterialTheme.colorScheme.error, strong = true)
                    // 정상은 칩을 달지 않는다 — 배지가 없는 것이 곧 "볼 것 없음"이다.
                    if (event.riskSignal != RiskSignal.NONE) TierChip(event.riskSignal)
                }
            }

            // 본문은 회색 판 위에 올린다. 흰 카드에 그대로 두면 글자 벽으로만 보이고,
            // 판을 깔면 "받은 내용"과 "우리 판정"이 시각적으로 갈린다.
            SoftBlock {
                ExpandableText(
                    text = event.text ?: "음성 변환 대기 중",
                    key = event.id,
                )
            }
        }
    }
}

/**
 * 세 줄을 넘는 본문은 접고 `더보기`로 편다. 펼침 상태는 이벤트 id 로 기억해 목록이
 * 갱신돼도 읽던 카드가 다시 접히지 않는다.
 */
@Composable
private fun ExpandableText(text: String, key: Long) {
    var expanded by rememberSaveable(key) { mutableStateOf(false) }
    var overflows by remember(key) { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            Text(
                if (expanded) "접기" else "더보기",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 2.dp),
            )
        }
    }
}

internal fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

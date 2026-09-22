package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ava.proto.R
import com.ava.proto.capture.Channel
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal

/** 홈의 큰 숫자가 세는 기간. 기간 없는 숫자는 "오늘"인지 "전체"인지 읽을 수 없다. */
private const val HERO_WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

/**
 * 랜딩 화면. 숫자는 전부 실제 이벤트에서 계산한다 — 데모용 고정값을 박아두면
 * 탐지가 동작하는지 화면만 보고는 알 수 없게 된다.
 *
 * **마스코트는 평온할 때만 나온다.** 웃는 캐릭터를 `위험 신호 2건` 옆에 두면 화면이
 * 말하는 것과 그림이 말하는 것이 어긋난다. 위험할 때는 캐릭터 자리를 경고 아이콘이 대신한다.
 */
@Composable
fun DashboardTab(
    events: List<EventEntity>,
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    onScanNow: () -> Unit,
    onViewAllEvents: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val since = System.currentTimeMillis() - HERO_WINDOW_MILLIS
    val recent = events.filter { it.capturedAt >= since }
    // 경보우려까지만 센다. 알림은 예보에서부터 나가지만, 이 화면 맨 위의 큰 숫자는 **지금
    // 당장 봐야 하는 건수**를 뜻한다. 판정기 어느 쪽도 확신하지 않은 예보까지 여기 넣으면
    // 그 숫자가 무엇을 세는지 흐려진다 — 등급별 내역은 기록 탭의 칩이 보여준다.
    val riskyCount = recent.count {
        it.riskSignal == RiskSignal.HIGH || it.riskSignal == RiskSignal.HIGH_UNBACKED
    }
    val allConnected = recordingFolderUri != null && notificationAccessGranted

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrandHeader(protected = allConnected, onOpenSettings = onOpenSettings)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusHero(
                riskyCount = riskyCount,
                totalCount = recent.size,
                neverSeen = events.isEmpty(),
                scanEnabled = recordingFolderUri != null,
                onScanNow = onScanNow,
            )
            if (!allConnected) SetupBanner(onOpenSettings)
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("감시 채널")
            ChannelCard(
                callActive = recordingFolderUri != null,
                messagingActive = notificationAccessGranted,
                onOpenSettings = onOpenSettings,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("최근 활동", actionLabel = "전체 보기", onAction = onViewAllEvents)
            RecentActivityCard(events = events.take(4))
        }
    }
}

/**
 * 홈의 머리. 다른 탭의 제목 자리에 워드마크가 온다 — 네 탭의 시작선이 같아야 탭을 오갈 때
 * 화면이 위아래로 튀지 않는다.
 *
 * 오른쪽 칩은 앱바의 종 아이콘을 대신한다. 아이콘은 버튼처럼 생겼는데 눌러도 아무 일이
 * 없었고, 색으로만 상태를 실어 무슨 뜻인지 설명이 없었다. 칩은 글자로 상태를 말하고 누르면
 * 고칠 수 있는 곳(설정)으로 간다.
 */
@Composable
private fun BrandHeader(protected: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 마스코트는 여러 색(보라 몸통·흰 방패·분홍 볼)이 형태를 이루므로 `tint` 를 걸지
        // 않는다 — 한 색으로 칠하면 실루엣만 남는다.
        Image(
            painterResource(R.drawable.ic_avamon_logo),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Avamon",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.weight(1f))
        val tint = if (protected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
        Text(
            if (protected) "보호 중" else "설정 필요",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = Modifier
                .clip(PillShape)
                .clickable(onClick = onOpenSettings)
                .background(tint.copy(alpha = 0.12f))
                .padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

/**
 * 화면의 주인공. 글이 왼쪽, 그림이 오른쪽인 가로 카드다.
 *
 * 예전에는 지름 148dp 원 안에 마스코트를 넣고 그 아래 글을 세로로 쌓아 화면의 40%를 썼는데,
 * 그 넓이가 전하는 정보는 "1건" 하나였다. 가로로 눕히면 같은 정보가 1/3 높이에 들어가고
 * 감시 채널까지 첫 화면에 보인다.
 *
 * 위험할 때의 그림은 라이트·다크 모두 **알파 틴트 위의 아이콘**이다. 다크에서만
 * `errorContainer` 를 꽉 채우던 때는 같은 화면이 라이트에서는 연분홍, 다크에서는 핏빛이었다.
 */
@Composable
private fun StatusHero(
    riskyCount: Int,
    totalCount: Int,
    neverSeen: Boolean,
    scanEnabled: Boolean,
    onScanNow: () -> Unit,
) {
    val safe = riskyCount == 0
    val error = MaterialTheme.colorScheme.error

    CleanCard {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "최근 7일",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        buildAnnotatedString {
                            when {
                                // 기록이 아예 없을 때만 "시작" 문구다. 7일 밖의 기록만 있으면
                                // 아래 최근 활동에 경보가 보이는데 위에서 "시작했어요"라고
                                // 하면 두 줄이 서로 다른 말을 한다.
                                neverSeen -> append("감시를 시작했어요")
                                safe -> append("위험 신호 없음")
                                else -> {
                                    append("위험 신호 ")
                                    withStyle(SpanStyle(color = error)) { append("${riskyCount}건") }
                                }
                            }
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        when {
                            neverSeen -> "통화 · 문자 · 카카오톡을 함께 봅니다"
                            totalCount == 0 -> "이번 주에 새로 확인한 연락이 없어요"
                            safe -> "확인한 연락 ${totalCount}건 · 모두 정상"
                            else -> "확인한 ${totalCount}건 중 ${riskyCount}건이 피싱으로 분류됐어요"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(14.dp))
                if (safe) {
                    Image(
                        painterResource(R.drawable.ic_avamon_mascot),
                        contentDescription = null,
                        modifier = Modifier.size(78.dp),
                    )
                } else {
                    IconBubble(Icons.Filled.Warning, error, size = 60)
                }
            }

            if (scanEnabled) {
                Spacer(Modifier.height(18.dp))
                TonalButton(
                    text = "지금 스캔",
                    icon = Icons.Filled.Refresh,
                    onClick = onScanNow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 채널이 하나라도 끊겨 있으면 히어로 바로 아래에서 알린다. 누르면 설정으로 간다. */
@Composable
private fun SetupBanner(onOpenSettings: () -> Unit) {
    CleanCard(onClick = onOpenSettings) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
        ) {
            IconBubble(Icons.Filled.Settings, MaterialTheme.colorScheme.primary, size = 40)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "채널 연결이 필요해요",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "연결되지 않은 채널은 감시되지 않아요",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ChannelCard(callActive: Boolean, messagingActive: Boolean, onOpenSettings: () -> Unit) {
    CleanCard {
        Column(modifier = Modifier.padding(vertical = 5.dp)) {
            ChannelRow(
                channel = Channel.CALL,
                // 알림 리스너와 달리 통화는 폴더 연결이 전제라 상태 근거가 다르다.
                status = if (callActive) "녹음 폴더 연결됨" else "설정에서 폴더를 연결하세요",
                active = callActive,
                onOpenSettings = onOpenSettings,
            )
            ChannelRow(
                channel = Channel.SMS,
                status = if (messagingActive) "알림 접근 허용됨" else "알림 접근을 허용하세요",
                active = messagingActive,
                onOpenSettings = onOpenSettings,
            )
            ChannelRow(
                channel = Channel.KAKAO,
                status = if (messagingActive) "알림 접근 허용됨" else "알림 접근을 허용하세요",
                active = messagingActive,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, status: String, active: Boolean, onOpenSettings: () -> Unit) {
    ListRow(
        title = channel.label,
        subtitle = status,
        leading = {
            IconBubble(
                channelIcon(channel),
                // 꺼진 채널까지 채널색으로 칠하면 "감시 중"과 구분이 안 된다.
                if (active) channelColor(channel) else MaterialTheme.colorScheme.outline,
            )
        },
        trailing = { StatusChip(active, activeText = "감시 중") },
        // 끊긴 채널만 누를 수 있다. 연결된 채널을 눌러 갈 곳이 없다.
        onClick = if (active) null else onOpenSettings,
    )
}

@Composable
private fun RecentActivityCard(events: List<EventEntity>) {
    CleanCard {
        if (events.isEmpty()) {
            Text(
                "아직 기록이 없어요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            )
            return@CleanCard
        }
        Column(modifier = Modifier.padding(vertical = 5.dp)) {
            events.forEach { ActivityRow(it) }
        }
    }
}

/**
 * 최근 활동 한 줄. 제목은 **상대방**이고 채널은 아이콘 색으로 읽힌다 — 문자 세 건이 전부
 * "문자"라는 제목을 달면 목록이 같은 줄의 반복이 된다.
 */
@Composable
private fun ActivityRow(event: EventEntity) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        IconBubble(channelIcon(event.channel), channelColor(event.channel), size = 40)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                event.counterpart ?: event.channel.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                event.text ?: "음성 변환 대기 중",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TierChip(event.riskSignal)
            Text(
                formatTime(event.capturedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

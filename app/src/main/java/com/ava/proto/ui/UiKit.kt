package com.ava.proto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ava.proto.data.RiskSignal

/**
 * 탭이 넷으로 갈라져도 같은 앱처럼 보이려면 모서리·카드·버튼 규격이 한 곳에서 나와야 한다.
 * 탭마다 각자 Card를 조립하면 반드시 값이 어긋난다.
 *
 * 모서리는 카드 20dp · 버튼 14dp · 칩은 알약이다. 버튼을 알약에서 둥근 사각형으로 바꾼 것은
 * 알약이 칩과 같은 모양이라 **누르는 것과 읽는 것이 한 형태였기 때문이다** — 형태가 갈리면
 * 설명 없이도 구분된다.
 */
internal val CardShape = RoundedCornerShape(20.dp)
internal val ButtonShape = RoundedCornerShape(14.dp)
internal val BubbleShape = RoundedCornerShape(14.dp)
internal val PillShape = RoundedCornerShape(percent = 50)

/**
 * 기본 카드. 회색 바탕 위의 흰 판이다.
 *
 * 그림자도 테두리도 없다. 바탕이 흰색이 아니라 회색이라 그것만으로 경계가 읽히고, 그림자를
 * 얹으면 카드가 여럿일 때 화면이 무거워진다.
 */
@Composable
internal fun CleanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        content()
    }
}

/** 카드 안에서 한 번 더 묶을 때 쓰는 회색 판. 흰 위에 흰을 얹으면 경계가 사라진다. */
@Composable
internal fun SoftBlock(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(tint, BubbleShape),
    ) {
        content()
    }
}

/**
 * 화면 머리. 제목 한 줄과 오른쪽 동작 하나로 끝낸다.
 *
 * 부제를 없앴다 — 하단 탭에 같은 단어가 있는데 제목 아래 설명까지 두면 첫 화면의 1/4이
 * 이미 아는 말로 찬다. 대시보드도 같은 높이의 머리(워드마크)를 가져 네 탭의 시작선이 맞는다.
 */
@Composable
internal fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

/** 섹션 이름. 작은 회색 글자 한 줄 — 카드 위에 붙는 라벨이지 카드가 아니다. */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/** 옅게 깔린 둥근 사각형 안의 아이콘. 목록 항목의 왼쪽 손잡이다. */
@Composable
internal fun IconBubble(icon: ImageVector, tint: Color, size: Int = 44) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.12f), BubbleShape),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

/**
 * 색 칩. **바탕은 알파, 글자는 원색**이다.
 *
 * 라이트·다크 어느 쪽에서도 같은 식이 통한다 — 원색 글자가 흰 바탕에서도 검은 바탕에서도
 * 읽히고, 알파 바탕은 밑색을 따라간다. 스킴의 `errorContainer` 를 쓰던 때는 다크에서
 * 어두운 빨강 위에 연한 빨강이 올라가 읽히지 않았다.
 */
@Composable
internal fun Chip(text: String, tint: Color, strong: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = if (strong) MaterialTheme.colorScheme.onPrimary else tint,
        modifier = Modifier
            .background(if (strong) tint else tint.copy(alpha = 0.12f), PillShape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

/** 채널 연결 여부. 색 점 대신 글자로 적는다 — 색을 못 가리는 눈에도 읽혀야 한다. */
@Composable
internal fun StatusChip(active: Boolean, activeText: String = "연결됨", inactiveText: String = "미연결") {
    if (active) {
        Chip(activeText, MaterialTheme.colorScheme.tertiary)
    } else {
        Chip(inactiveText, MaterialTheme.colorScheme.outline)
    }
}

// ── 등급 표기 ─────────────────────────────────────────────────────────────────
//
// **화면에 나가는 것은 등급 이름 하나뿐이다.** 위험도 숫자도, 진행 단계도, 근거 문장도
// 내지 않는다 (CLAUDE.md Critical Rules). 등급 판단은 `BackendClassificationClient` 에
// 있고 여기서는 [RiskSignal]에 이름과 색만 붙인다.
//
// 색은 켜진 판정기의 수를 뜻한다 — 빨강은 둘 다(또는 모델 혼자 단정), 주황은 한쪽만,
// 노랑은 어느 쪽도 위험하다고 하지 않았다. 이름을 함께 쓰는 이유는 색을 못 가리는 눈에는
// 칩이 전부 같은 모양이기 때문이다.

internal fun tierLabel(signal: RiskSignal): String = when (signal) {
    RiskSignal.HIGH -> "경보"
    RiskSignal.HIGH_UNBACKED -> "경보우려"
    RiskSignal.CAUTION -> "주의보"
    RiskSignal.FORECAST -> "예보"
    RiskSignal.NONE -> "정상"
}

@Composable
internal fun tierColor(signal: RiskSignal): Color = when (signal) {
    RiskSignal.HIGH -> MaterialTheme.colorScheme.error
    RiskSignal.HIGH_UNBACKED, RiskSignal.CAUTION -> caution
    RiskSignal.FORECAST -> forecast
    RiskSignal.NONE -> MaterialTheme.colorScheme.tertiary
}

@Composable
internal fun TierChip(signal: RiskSignal) {
    Chip(tierLabel(signal), tierColor(signal))
}

// ── 버튼 ─────────────────────────────────────────────────────────────────────

@Composable
internal fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 보조 버튼. 테두리 대신 옅은 보라 판 — 선 버튼은 회색 바탕에서 칩과 구분되지 않았다. */
@Composable
internal fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 15.dp),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 목록 항목 오른쪽에 붙는 작은 버튼. 설명이 두 줄로 감기지 않게 폭을 최소로 잡는다. */
@Composable
internal fun CompactButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 34.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ── 목록 ─────────────────────────────────────────────────────────────────────

/**
 * 카드 안 항목 한 줄. 구분선을 긋지 않고 위아래 여백으로 갈라놓는다 — 선이 있으면 카드
 * 하나가 표처럼 보이고, 없으면 항목이 각자 숨 쉴 자리를 가진다.
 */
@Composable
internal fun ListRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 13.dp),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/** 카드 안의 이름-값 한 줄. 설정 탭의 정보 항목처럼 값이 한 단어인 자리에 쓴다. */
@Composable
internal fun KeyValueRow(key: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── 상태 패널 ─────────────────────────────────────────────────────────────────

/** 진행 중 상태는 취소 가능 여부만 다르므로 한 컴포저블로 합쳤다. */
@Composable
internal fun ProgressPanel(
    text: String,
    onCancel: (() -> Unit)? = null,
) {
    CleanCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, PillShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, PillShape),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onCancel != null) {
                    Spacer(Modifier.width(12.dp))
                    CompactButton("중단", onClick = onCancel)
                }
            }
        }
    }
}

@Composable
internal fun ErrorBanner(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f), CardShape)
            .padding(16.dp),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

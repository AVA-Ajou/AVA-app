package com.ava.proto.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 탭이 넷으로 갈라져도 같은 앱처럼 보이려면 모서리·카드·버튼 규격이 한 곳에서 나와야 한다.
 * 탭마다 각자 Card를 조립하면 반드시 값이 어긋난다.
 *
 * 모서리를 크게(22dp) 잡은 것은 발표 자료의 폰 목업을 전제로 한 선택이다 — 목업은 슬라이드에서
 * 화면을 30~40%로 줄여 쓰는데, 12dp 는 그 크기에서 거의 직각으로 보여 카드가 서로 붙어 보인다.
 */
internal val CardShape = RoundedCornerShape(22.dp)
internal val PillShape = RoundedCornerShape(percent = 50)

/**
 * 기본 카드. 라벤더 배경 위의 흰 판이다.
 *
 * 테두리를 빼고 아주 옅은 그림자만 남겼다 — 브랜드 시트의 카드가 선 없이 떠 있는 모양이고,
 * 배경이 흰색이 아니라 라벤더라 테두리 없이도 카드 경계가 읽힌다.
 */
@Composable
internal fun CleanCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        content()
    }
}

/** 카드 안에서 한 번 더 묶을 때 쓰는 연보라 판. 흰 위에 흰을 얹으면 경계가 사라진다. */
@Composable
internal fun SoftBlock(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(tint, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

/**
 * 섹션 머리. 브랜드 시트가 `핵심 아이디어`·`주요 기능`을 **보라로 채운 알약**에 흰 글씨로
 * 얹어 쓰는데, 그게 시트에서 가장 눈에 띄는 반복 요소라 그대로 가져왔다.
 */
@Composable
internal fun SectionPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, PillShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/** 섹션 머리 한 줄 — 알약 + (선택) 오른쪽 링크. */
@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionPill(title)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, shape = PillShape) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 옅은 원 안의 아이콘. 시트가 기능 아이콘을 전부 이 모양으로 쓴다. */
@Composable
internal fun IconBubble(icon: ImageVector, tint: Color, size: Int = 46) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.14f), CircleShape),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size * 0.48f).dp),
        )
    }
}

@Composable
internal fun StatusBadge(text: String, content: Color, container: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = content,
        modifier = Modifier
            .background(container, PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** 채널 활성 여부를 점 하나로 요약한다 — 목록에서 문장보다 빨리 읽힌다. */
@Composable
internal fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(9.dp)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                CircleShape,
            ),
    )
}

@Composable
internal fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
internal fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondaryContainer),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** 진행 중 상태는 취소 가능 여부만 다르므로 한 컴포저블로 합쳤다. */
@Composable
internal fun ProgressPanel(
    text: String,
    emphasize: Boolean = false,
    onCancel: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
            .padding(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(6.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, PillShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, PillShape),
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
                color = if (emphasize) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
            if (onCancel != null) {
                TextButton(onClick = onCancel, shape = PillShape) {
                    Text("중단", style = MaterialTheme.typography.labelMedium)
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
            .background(MaterialTheme.colorScheme.errorContainer, CardShape)
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
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

package com.ava.proto.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 탭이 넷으로 갈라져도 같은 앱처럼 보이려면 모서리·카드·버튼 규격이 한 곳에서 나와야 한다.
 * 탭마다 각자 Card를 조립하면 반드시 값이 어긋난다.
 */
internal val CardShape = RoundedCornerShape(12.dp)
internal val PillShape = RoundedCornerShape(percent = 50)

@Composable
internal fun CleanCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
internal fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SectionLabel(
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = color,
    )
}

@Composable
internal fun IconBubble(icon: ImageVector, tint: Color, size: Int = 40) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size((size / 2).dp),
        )
    }
}

@Composable
internal fun StatusBadge(text: String, content: Color, container: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = content,
        modifier = Modifier
            .background(container, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** 채널 활성 여부를 점 하나로 요약한다 — 목록에서 문장보다 빨리 읽힌다. */
@Composable
internal fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                if (active) {
                    MaterialTheme.colorScheme.tertiaryContainer
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
            .padding(12.dp),
    ) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, CardShape)
            .padding(12.dp),
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

/**
 * 점수를 원호로 그린다. 시안의 SVG 링과 달리 Canvas로 직접 그리는 이유는,
 * 벡터 리소스로 두면 각도가 고정돼 이벤트 수에 따라 움직일 수 없기 때문이다.
 */
@Composable
internal fun ScoreRing(
    percent: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val gradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.primary,
        ),
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(160.dp)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = 14f, cap = StrokeCap.Round)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                brush = gradient,
                startAngle = -90f,
                sweepAngle = 360f * percent / 100f,
                useCenter = false,
                style = stroke,
            )
        }
        content()
    }
}

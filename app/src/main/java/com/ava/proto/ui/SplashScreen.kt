package com.ava.proto.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ava.proto.R
import kotlinx.coroutines.delay

/**
 * 인트로.
 *
 * **진행 막대는 실제 작업량이 아니라 시간으로 찬다.** 앱의 실제 기동은 첫 프레임까지
 * 255~285ms로 측정됐다(`am start -W`). 그대로 두면 화면이 스치듯 지나가서 인트로가 성립하지
 * 않는다. 여기 적힌 단계는 앱이 **평소에** 하는 일의 이름이지 지금 그 일을 하고 있다는
 * 뜻이 아니다.
 *
 * **총 0.9초를 넘기지 말 것.** 처음에 2.4초로 잡았다가 앱이 느려진 것처럼 느껴진다는
 * 지적을 받아 두 번 줄였다 — 기동이 270ms인 앱을 2.7초 기다리게 만들고 있었다.
 * 단계별 시간이 고르지 않은 것은 처음과 마지막 문구만 읽히면 되기 때문이다.
 * 화면을 누르면 즉시 건너뛴다.
 */
private val STAGES = listOf(
    "보안을 준비하고 있어요" to 250,
    "신호 수집 중" to 150,
    "신호 분석 중" to 150,
    "위험 여부 판단 중" to 150,
    "보호 준비 완료" to 200,
)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        STAGES.forEachIndexed { index, (_, millis) ->
            step = index
            delay(millis.toLong())
        }
        onFinished()
    }

    val progress by animateFloatAsState(
        targetValue = (step + 1f) / STAGES.size,
        animationSpec = tween(durationMillis = 160),
        label = "splash-progress",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 물결·그림자 없이 탭만 받는다 — 인트로에 버튼처럼 보이는 것을 두고 싶지 않다.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp),
        ) {
            // 마스코트 뒤의 옅은 원 — 회색 바탕 위에 캐릭터가 떠 보이게 하는 장치다.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape),
                )
                Image(
                    // 시트의 기본 포즈를 쓴다. 예전 원본 PNG는 바닥 그림자가 밝은 회색으로 구워져 있어
                    // 다크 모드에서 캐릭터 밑에 흰 얼룩이 떴다.
                    painterResource(R.drawable.ic_avamon_guard),
                    contentDescription = null,
                    modifier = Modifier.size(156.dp),
                )
            }

            Spacer(Modifier.height(36.dp))

            Text(
                "Avamon",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "통화 · 문자 · 카카오톡의 신호를 모아\n보이스피싱을 먼저 알려드려요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(56.dp))

            // 기본 LinearProgressIndicator 를 쓰지 않는다 — 모서리와 두께가 여기 막대와 달라
            // 이 한 줄만 다른 앱처럼 보인다.
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, PillShape),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.primary, PillShape),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                STAGES[step].first,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

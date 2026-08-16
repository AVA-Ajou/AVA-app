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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 브랜드 시트의 `로딩 화면 흐름`을 옮긴 인트로.
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
    "보호 준비 완료!" to 200,
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
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Text(
                "Avamon",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "여러 채널의 신호를 모아\n당신을 지켜주는 스마트 경고",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(44.dp))

            // 마스코트 뒤의 옅은 원 — 시트에서 캐릭터를 띄워 보이게 하는 장치다.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f),
                            CircleShape,
                        ),
                )
                Image(
                    painterResource(R.drawable.ic_avamon_mascot),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp),
                )
            }

            Spacer(Modifier.height(48.dp))

            // 기본 LinearProgressIndicator 를 쓰지 않는다 — 모서리와 두께가 시트의 막대와
            // 달라서, 목업으로 나란히 놓으면 이 한 줄만 다른 앱처럼 보인다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        RoundedCornerShape(50),
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(50),
                        ),
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                STAGES[step].first,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

    }
}

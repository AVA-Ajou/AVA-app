package com.ava.proto.ui

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ava.proto.R
import com.ava.proto.capture.Channel

/**
 * 첫 실행 여부. 온보딩을 끝냈거나 건너뛴 뒤에는 다시 뜨지 않는다.
 *
 * DB가 아니라 SharedPreferences 인 이유는 DB가 파괴적 마이그레이션으로 지워질 때 온보딩이
 * 되살아나면 안 되기 때문이다 — 권한과 폴더는 그대로인데 "처음이세요?"라고 물으면 이상하다.
 */
object OnboardingPrefs {
    private const val FILE = "onboarding"
    private const val KEY_DONE = "done"

    fun isDone(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_DONE, false)

    fun markDone(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_DONE, true).apply()
    }
}

/** 세 단계 + 완료. 순서는 앱이 채널을 켜는 데 필요한 순서다 — 권한 하나가 채널 둘을 연다. */
private enum class Step { WELCOME, MESSAGING, CALL, DONE }

/**
 * 첫 실행 온보딩. 예전에는 첫 화면이 바로 홈이었고 권한·폴더 연결은 설정 탭 안에 있어서,
 * 사용자가 아무것도 켜지 않은 채 "감시를 시작했어요"를 보게 됐다. 채널이 꺼진 감시 앱은
 * 아무것도 하지 않는다 — 켜는 절차를 첫 화면으로 끌어올렸다.
 *
 * 각 단계는 **시스템 설정에서 돌아오면 상태가 바뀐다**. 권한 여부는 MainActivity 가 ON_RESUME
 * 마다 다시 읽어 내려보내므로, 여기서는 값만 보고 "허용됨"으로 바꾼다. 건너뛰기를 막지
 * 않는다 — 막으면 권한을 못 주는 기기(에뮬레이터·회사폰)에서 앱에 들어갈 수 없다.
 */
@Composable
fun OnboardingScreen(
    notificationAccessGranted: Boolean,
    recordingFolderUri: Uri?,
    onOpenNotificationAccessSettings: () -> Unit,
    onConnectFolder: () -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val current = Step.entries[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        // 단계 표시. 환영과 완료는 세지 않는다 — 사용자가 "해야 할 일"은 둘뿐이다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StepDots(total = 3, active = step.coerceAtMost(2))
            if (current == Step.MESSAGING || current == Step.CALL) {
                Text(
                    "나중에 할게요",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable { step++ }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInHorizontally { it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
            },
            label = "onboarding-step",
            modifier = Modifier.weight(1f),
        ) { s ->
            when (s) {
                Step.WELCOME -> StepBody(
                    image = R.drawable.ic_avamon_wave,
                    title = "안녕하세요,\nAvamon이에요",
                    body = "통화 · 문자 · 카카오톡을 함께 지켜보다가\n보이스피싱 신호가 보이면 먼저 알려드려요.\n두 가지만 연결하면 바로 시작돼요.",
                )
                Step.MESSAGING -> StepBody(
                    image = R.drawable.ic_avamon_notify,
                    title = "문자와 카카오톡을\n지켜볼게요",
                    body = "알림 접근을 허용하면 새 문자와 카카오톡 메시지를 읽고 위험한지 판단해요. 메시지는 기기 밖으로 나가지 않아요.",
                    status = if (notificationAccessGranted) "허용됨" else null,
                    channels = listOf(Channel.SMS, Channel.KAKAO),
                )
                Step.CALL -> StepBody(
                    image = R.drawable.ic_avamon_monitor,
                    title = "통화 녹음도\n함께 볼게요",
                    body = "통화 녹음이 저장되는 폴더를 연결하면 새 녹음을 자동으로 분석해요.\n갤럭시: 내부저장소 › Recordings › Call",
                    status = if (recordingFolderUri != null) "연결됨" else null,
                    channels = listOf(Channel.CALL),
                )
                Step.DONE -> StepBody(
                    image = R.drawable.ic_avamon_goodjob,
                    title = "준비됐어요",
                    body = when {
                        notificationAccessGranted && recordingFolderUri != null ->
                            "세 채널을 모두 지켜보고 있어요.\n위험한 신호가 보이면 바로 알려드릴게요."
                        else ->
                            "아직 연결하지 않은 채널은 설정에서 언제든 켤 수 있어요."
                    },
                )
            }
        }

        // 아래 버튼. 단계마다 **하나**다 — 권한 화면에서는 "허용하기"가 곧 다음이고,
        // 허용된 뒤에야 "다음"이 된다. 버튼 둘을 나란히 두면 어느 쪽이 진행인지 흐려진다.
        Column(
            modifier = Modifier.padding(bottom = 20.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (current) {
                Step.WELCOME -> PrimaryButton("시작하기", onClick = { step++ }, modifier = Modifier.fillMaxWidth())
                Step.MESSAGING ->
                    if (notificationAccessGranted) {
                        PrimaryButton("다음", onClick = { step++ }, modifier = Modifier.fillMaxWidth())
                    } else {
                        PrimaryButton("알림 접근 허용하기", onClick = onOpenNotificationAccessSettings, modifier = Modifier.fillMaxWidth())
                    }
                Step.CALL ->
                    if (recordingFolderUri != null) {
                        PrimaryButton("다음", onClick = { step++ }, modifier = Modifier.fillMaxWidth())
                    } else {
                        PrimaryButton("녹음 폴더 연결하기", onClick = onConnectFolder, modifier = Modifier.fillMaxWidth())
                    }
                Step.DONE -> PrimaryButton("홈으로", onClick = onFinish, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StepDots(total: Int, active: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (i == active) 22.dp else 6.dp)
                    .background(
                        if (i <= active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        PillShape,
                    ),
            )
        }
    }
}

/**
 * 단계 본문. 그림 → 제목 → 설명 → (상태 칩) 순서로 세로 중앙에 놓는다.
 * 그림이 화면의 주인공이다 — 권한 설명은 읽히지 않아도 캐릭터가 뭘 하는지는 읽힌다.
 */
@Composable
private fun StepBody(
    image: Int,
    title: String,
    body: String,
    status: String? = null,
    channels: List<Channel> = emptyList(),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (channels.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                channels.forEach { ChannelTile(it, size = 40) }
                if (status != null) {
                    Spacer(Modifier.width(2.dp))
                    Chip(status, MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

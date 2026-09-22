package com.ava.proto.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ava.proto.capture.Channel

/**
 * 데모와 오탐 검증을 한 탭에 묶었다. 둘 다 "신호를 일부러 만들어 파이프라인을 통과시키는"
 * 같은 성격이고, 실행 중에는 서로를 막아야 해서(enabled 조건 공유) 떨어뜨리면 상태가 꼬인다.
 *
 * 버튼 동사는 섹션 안에서 하나다 — 데모는 전부 `실행`, 순환은 전부 `시작`. 통화만 `분석`이던
 * 때는 왜 그것만 다른지 화면에서 설명되지 않았다. 설명문은 한 줄로 끝낸다. 두 줄로 감기면
 * `카카오톡으로 도 / 착한` 같은 자리에서 줄이 꺾인다.
 */
@Composable
fun SimulationTab(
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCardFollowUp: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val callBusy = callDemoStep != CallDemoStep.IDLE
    val idle = !isBusy && !callBusy && !autoTestRunning

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PageHeader("시뮬레이션")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("채널 데모")
            CleanCard {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    SimulationRow(
                        leading = { ChannelTile(Channel.KAKAO) },
                        title = "카카오톡 피싱",
                        description = "기관 사칭 메시지 수신을 재현",
                        enabled = !isBusy && !autoTestRunning,
                        onRun = onDemoKakao,
                    )
                    SimulationRow(
                        leading = { ChannelTile(Channel.SMS) },
                        title = "문자 피싱",
                        description = "직전 카카오톡과 같은 사건으로 묶임",
                        enabled = !isBusy && !autoTestRunning,
                        onRun = onDemoSms,
                    )
                    SimulationRow(
                        leading = { ChannelTile(Channel.CALL) },
                        title = "통화 전사본 분석",
                        description = "녹음 폴더의 .txt를 다시 판정",
                        enabled = idle,
                        onRun = onDemoCall,
                    )
                    SimulationRow(
                        leading = { ChannelTile(Channel.SMS) },
                        title = "카드배달 후속 문자",
                        description = "통화 분석 직후면 한 사건으로 묶임",
                        enabled = !isBusy && !autoTestRunning,
                        onRun = onDemoCardFollowUp,
                    )
                }
            }

            when (callDemoStep) {
                CallDemoStep.ANALYZING -> ProgressPanel(
                    text = "통화 내용을 분석하고 있어요 · 파일당 수십 초",
                    onCancel = onCancelCallDemo,
                )
                CallDemoStep.IDLE -> {}
            }

            if (callDemoResult == CallDemoResult.NO_FOLDER) {
                ErrorBanner("녹음 폴더가 연결되지 않았어요. 설정 탭에서 폴더를 먼저 연결해주세요.")
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("오탐 검증")
            if (!autoTestRunning) {
                CleanCard {
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        SimulationRow(
                            leading = { IconBubble(AppIcons.refresh, channelColor(Channel.KAKAO)) },
                            title = "카카오톡 순환",
                            description = "정상 ↔ 피싱 메시지를 번갈아 발송",
                            enabled = !isBusy && !callBusy,
                            runLabel = "시작",
                            onRun = onStartAutoTestKakao,
                        )
                        SimulationRow(
                            leading = { IconBubble(AppIcons.refresh, channelColor(Channel.SMS)) },
                            title = "문자 순환",
                            description = "정상 ↔ 피싱 문자를 번갈아 발송",
                            enabled = !isBusy && !callBusy,
                            runLabel = "시작",
                            onRun = onStartAutoTestSms,
                        )
                    }
                }
            } else {
                ProgressPanel(text = autoTestStatus, onCancel = onStopAutoTest)
            }
        }
    }
}

@Composable
private fun SimulationRow(
    leading: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean,
    onRun: () -> Unit,
    runLabel: String = "실행",
) {
    ListRow(
        title = title,
        subtitle = description,
        leading = leading,
        trailing = { CompactButton(runLabel, enabled = enabled, onClick = onRun) },
    )
}

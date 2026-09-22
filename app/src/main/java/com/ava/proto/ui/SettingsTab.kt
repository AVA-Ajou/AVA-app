package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ava.proto.BuildConfig
import com.ava.proto.capture.Channel

/**
 * 채널을 켜고 끄는 토글은 두지 않는다 — 이 앱에서 채널의 on/off 는 앱 설정이 아니라
 * 시스템 권한(알림 접근)과 SAF 폴더 연결 여부로 결정되기 때문이다.
 *
 * 두 채널 카드의 상태 표기는 **같은 칩 하나**다. 예전에는 통화가 오른쪽 위 알약, 문자가
 * 초록 점 + `상태: 허용됨` 글자라 같은 뜻을 두 방식으로 쓰고 있었다.
 */
@Composable
fun SettingsTab(
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    defaultSmsPackage: String?,
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onScanNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PageHeader("설정")

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("채널 연결")

            CleanCard {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    ListRow(
                        title = Channel.CALL.label,
                        subtitle = recordingFolderUri?.lastPathSegment?.substringAfterLast(':')
                            ?: "폴더를 연결하면 감시가 시작돼요",
                        modifier = Modifier.padding(top = 5.dp),
                        leading = { ChannelTile(Channel.CALL) },
                        trailing = { StatusChip(recordingFolderUri != null) },
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                    ) {
                        if (recordingFolderUri == null) {
                            PrimaryButton(
                                text = "녹음 폴더 연결",
                                onClick = onConnectFolder,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            TonalButton(
                                text = "폴더 변경",
                                onClick = onConnectFolder,
                                modifier = Modifier.weight(1f),
                            )
                            PrimaryButton(
                                text = "지금 스캔",
                                onClick = onScanNow,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            CleanCard {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    ListRow(
                        title = "문자 · 카카오톡",
                        subtitle = "알림 접근 하나로 두 채널을 감시해요",
                        modifier = Modifier.padding(top = 5.dp),
                        leading = { ChannelTile(Channel.SMS) },
                        trailing = { StatusChip(notificationAccessGranted, activeText = "허용됨", inactiveText = "거부됨") },
                    )
                    if (notificationAccessGranted) {
                        TonalButton(
                            text = "알림 접근 설정",
                            onClick = onOpenNotificationAccessSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                        )
                    } else {
                        PrimaryButton(
                            text = "알림 접근 허용하기",
                            onClick = onOpenNotificationAccessSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                        )
                    }
                }
            }
        }

        // 진단 정보. 서버가 붙었는지는 판정 결과에 직접 영향을 주므로(없으면 키워드 대역)
        // 화면 어딘가에는 보여야 한다 — 예전에는 로그를 봐야만 알 수 있었다.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader("정보")
            CleanCard {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    val serverConfigured = BuildConfig.DETECTION_SERVER_URL.isNotBlank()
                    KeyValueRow(
                        "판정 서버",
                        if (serverConfigured) "연결 설정됨" else "미설정 · 키워드 판정",
                        valueColor = if (serverConfigured) MaterialTheme.colorScheme.tertiary else caution,
                    )
                    KeyValueRow("녹음 위치 (갤럭시)", "내부저장소 › Recordings › Call")
                    if (defaultSmsPackage != null) {
                        KeyValueRow("기본 문자 앱", defaultSmsPackage.substringAfterLast('.'))
                    }
                    KeyValueRow("앱 버전", BuildConfig.VERSION_NAME)
                }
            }
        }
    }
}

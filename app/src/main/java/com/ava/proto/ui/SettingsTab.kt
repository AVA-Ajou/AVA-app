package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ava.proto.capture.Channel

/**
 * 채널을 켜고 끄는 토글은 두지 않는다 — 이 앱에서 채널의 on/off 는 앱 설정이 아니라
 * 시스템 권한(알림 접근)과 SAF 폴더 연결 여부로 결정되기 때문이다.
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("설정", "채널을 연결해야 감시가 시작됩니다.")

        SectionHeader("채널 연결")

        CleanCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBubble(channelIcon(Channel.CALL), channelColor(Channel.CALL))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "통화 녹음",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            if (recordingFolderUri != null) {
                                StatusBadge(
                                    "연결됨",
                                    content = MaterialTheme.colorScheme.tertiary,
                                    container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                                )
                            } else {
                                StatusBadge(
                                    "미연결",
                                    content = MaterialTheme.colorScheme.onSurfaceVariant,
                                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                            }
                        }
                        Text(
                            recordingFolderUri?.lastPathSegment ?: "연결된 폴더가 없습니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "갤럭시: 내부저장소 › Recordings › Call",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (recordingFolderUri == null) {
                        PrimaryPillButton(
                            text = "녹음 폴더 연결",
                            onClick = onConnectFolder,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        OutlinedPillButton(
                            text = "폴더 변경",
                            onClick = onConnectFolder,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryPillButton(
                            text = "지금 스캔",
                            onClick = onScanNow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 좌측 강조 바 — 알림 접근 하나가 두 채널을 동시에 좌우한다는 걸 시각적으로 묶는다.
        CleanCard {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(channelColor(Channel.SMS)),
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconBubble(channelIcon(Channel.SMS), channelColor(Channel.SMS))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "문자 · 카카오톡",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "알림 접근 권한 하나로 두 채널을 함께 감시합니다"
                                    + (defaultSmsPackage?.let { " (문자 앱: $it)" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                StatusDot(notificationAccessGranted)
                                Text(
                                    if (notificationAccessGranted) "상태: 허용됨" else "상태: 거부됨",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (notificationAccessGranted) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                            }
                        }
                    }
                    OutlinedPillButton(
                        text = "알림 접근 설정 열기",
                        onClick = onOpenNotificationAccessSettings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

package com.ava.proto.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ava.proto.capture.Channel
import com.ava.proto.data.EventEntity
import com.ava.proto.data.RiskSignal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

/** 카드 모서리를 한 곳에서 정한다 — 값이 갈라지면 섹션마다 다른 앱처럼 보인다. */
private val CardShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(percent = 50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    defaultSmsPackage: String?,
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onScanNow: () -> Unit,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "AVA-Detection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                actions = {
                    // 알림 접근이 꺼져 있으면 카톡·SMS 채널이 통째로 죽으므로 앱바에서 먼저 알린다.
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = if (notificationAccessGranted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(end = 16.dp).size(22.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "다채널 피싱 탐지",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "통화·문자·카카오톡 중 두 채널 이상에서 위험 신호가 겹치면 알려드립니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ChannelSetupSection(
                recordingFolderUri = recordingFolderUri,
                notificationAccessGranted = notificationAccessGranted,
                defaultSmsPackage = defaultSmsPackage,
                onConnectFolder = onConnectFolder,
                onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                onScanNow = onScanNow,
            )

            SectionDivider()

            DemoSection(
                isBusy = isBusy,
                callDemoStep = callDemoStep,
                callDemoResult = callDemoResult,
                onDemoKakao = onDemoKakao,
                onDemoSms = onDemoSms,
                onDemoCall = onDemoCall,
                onCancelCallDemo = onCancelCallDemo,
            )

            SectionDivider()

            AutoTestSection(
                isBusy = isBusy,
                callDemoStep = callDemoStep,
                autoTestRunning = autoTestRunning,
                autoTestStatus = autoTestStatus,
                onStartAutoTestKakao = onStartAutoTestKakao,
                onStartAutoTestSms = onStartAutoTestSms,
                onStopAutoTest = onStopAutoTest,
            )

            SectionDivider()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("최근 이벤트 (${uiState.events.size})")
                if (uiState.events.isEmpty()) {
                    Text(
                        "아직 수신된 이벤트가 없습니다. 위 데모 버튼으로 신호를 발생시켜 보세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                uiState.events.forEach { EventRow(it) }
            }
        }
    }
}

// ── 채널 연결 ────────────────────────────────────────────────────────────────

@Composable
private fun ChannelSetupSection(
    recordingFolderUri: Uri?,
    notificationAccessGranted: Boolean,
    defaultSmsPackage: String?,
    onConnectFolder: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onScanNow: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("채널 연결", color = MaterialTheme.colorScheme.primary)

        CleanCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBubble(Icons.Filled.Call, MaterialTheme.colorScheme.primaryContainer)
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
                                    "Active",
                                    content = MaterialTheme.colorScheme.tertiaryContainer,
                                    container = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.16f),
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

        // 시안의 좌측 강조 바 — 알림 접근 하나가 두 채널을 좌우한다는 걸 시각적으로 묶는다.
        CleanCard {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconBubble(Icons.Filled.Email, MaterialTheme.colorScheme.secondaryContainer)
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
                                val statusColor = if (notificationAccessGranted) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(statusColor, CircleShape),
                                )
                                Text(
                                    if (notificationAccessGranted) "상태: 허용됨" else "상태: 거부됨",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = statusColor,
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

// ── 채널별 데모 ──────────────────────────────────────────────────────────────

@Composable
private fun DemoSection(
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    callDemoResult: CallDemoResult?,
    onDemoKakao: () -> Unit,
    onDemoSms: () -> Unit,
    onDemoCall: () -> Unit,
    onCancelCallDemo: () -> Unit,
) {
    val callBusy = callDemoStep != CallDemoStep.IDLE

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("채널별 데모")
        Text(
            "버튼을 누르면 해당 채널로 피싱 신호가 들어온 것을 시뮬레이션합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton(
                text = "카카오톡",
                icon = Icons.AutoMirrored.Filled.Send,
                iconTint = MaterialTheme.colorScheme.primaryContainer,
                enabled = !isBusy,
                onClick = onDemoKakao,
            )
            DemoButton(
                text = "SMS",
                icon = Icons.Filled.Email,
                iconTint = MaterialTheme.colorScheme.secondaryContainer,
                enabled = !isBusy,
                onClick = onDemoSms,
            )
            DemoButton(
                text = "통화 녹음",
                icon = Icons.Filled.Warning,
                iconTint = MaterialTheme.colorScheme.error,
                enabled = !isBusy && !callBusy,
                danger = true,
                onClick = onDemoCall,
            )
        }

        when (callDemoStep) {
            CallDemoStep.COPYING -> ProgressPanel(text = "음성 파일 복사 중...")
            CallDemoStep.ANALYZING -> ProgressPanel(
                text = "AI가 녹음을 분석 중... (수 초~수십 초 소요)",
                emphasize = true,
                onCancel = onCancelCallDemo,
            )
            CallDemoStep.IDLE -> {}
        }

        if (callDemoResult == CallDemoResult.NO_FOLDER) {
            ErrorBanner("녹음 폴더가 연결되지 않았습니다. 위에서 폴더를 먼저 연결해주세요.")
        }
    }
}

// ── 오탐 검증 ────────────────────────────────────────────────────────────────

@Composable
private fun AutoTestSection(
    isBusy: Boolean,
    callDemoStep: CallDemoStep,
    autoTestRunning: Boolean,
    autoTestStatus: String,
    onStartAutoTestKakao: () -> Unit,
    onStartAutoTestSms: () -> Unit,
    onStopAutoTest: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("오탐 검증 (자동 순환)")
        Text(
            "정상 메시지와 피싱 메시지를 번갈아 발송해 오탐/미탐을 확인합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!autoTestRunning) {
            val enabled = !isBusy && callDemoStep == CallDemoStep.IDLE
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StackedButton(
                    text = "카카오톡 순환",
                    iconTint = MaterialTheme.colorScheme.primaryContainer,
                    enabled = enabled,
                    onClick = onStartAutoTestKakao,
                )
                StackedButton(
                    text = "SMS 순환",
                    iconTint = MaterialTheme.colorScheme.secondaryContainer,
                    enabled = enabled,
                    onClick = onStartAutoTestSms,
                )
            }
        } else {
            ProgressPanel(text = autoTestStatus, emphasize = true, onCancel = onStopAutoTest)
        }
    }
}

// ── 이벤트 목록 ──────────────────────────────────────────────────────────────

@Composable
private fun EventRow(event: EventEntity) {
    CleanCard {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    val channelColor = channelColor(event.channel)
                    StatusBadge(
                        event.channel.name,
                        content = channelColor,
                        container = channelColor.copy(alpha = 0.14f),
                    )
                    Text(
                        event.sourceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    formatTime(event.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                event.text ?: "(STT 대기 중)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (event.riskSignal == RiskSignal.HIGH) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "위험 신호: ${event.matchedPhrase}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun channelColor(channel: Channel): Color = when (channel) {
    Channel.KAKAO -> MaterialTheme.colorScheme.primaryContainer
    Channel.SMS -> MaterialTheme.colorScheme.secondaryContainer
    Channel.CALL -> MaterialTheme.colorScheme.error
}

// ── 공용 조각 ────────────────────────────────────────────────────────────────

@Composable
private fun CleanCard(content: @Composable () -> Unit) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = color,
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
private fun IconBubble(icon: ImageVector, tint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .background(tint.copy(alpha = 0.12f), CircleShape),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatusBadge(text: String, content: Color, container: Color) {
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

@Composable
private fun PrimaryPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun OutlinedPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = PillShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
private fun RowScope.DemoButton(
    text: String,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (danger) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (danger) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = if (danger) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = Modifier.weight(1f),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowScope.StackedButton(
    text: String,
    iconTint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CardShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp),
        modifier = Modifier.weight(1f),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** 진행 중 상태는 취소 가능 여부만 다르므로 한 컴포저블로 합쳤다. */
@Composable
private fun ProgressPanel(
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
private fun ErrorBanner(text: String) {
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

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

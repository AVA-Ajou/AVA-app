package com.readingshield.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.readingshield.audio.AudioFile
import com.readingshield.audio.AudioInput
import com.readingshield.audio.FoundRecording
import java.util.Locale

@Composable
fun AnalysisScreen(
    input: AudioInput,
    folderUri: Uri?,
    recordings: List<FoundRecording>,
    onPickFile: () -> Unit,
    onConnectFolder: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("리딩쉴드", style = MaterialTheme.typography.headlineMedium)

            FolderSection(
                folderUri = folderUri,
                recordings = recordings,
                onConnectFolder = onConnectFolder,
                onRescan = onRescan,
            )

            HorizontalDivider()

            Text("파일 하나만 확인", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onPickFile) {
                Text("녹음 파일 선택")
            }

            when (input) {
                AudioInput.Empty -> Unit
                is AudioInput.Loaded -> FileCard(input.file)
                is AudioInput.Failed -> MessageCard("파일을 읽지 못했습니다", input.message)
            }
        }
    }
}

@Composable
private fun FolderSection(
    folderUri: Uri?,
    recordings: List<FoundRecording>,
    onConnectFolder: () -> Unit,
    onRescan: () -> Unit,
) {
    Text("녹음 폴더", style = MaterialTheme.typography.titleMedium)

    if (folderUri == null) {
        Text(
            "통화 녹음이 저장되는 폴더를 한 번 지정하면, 이후에는 앱이 새 녹음을 직접 찾습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onConnectFolder) {
            Text("녹음 폴더 연결")
        }
        return
    }

    Text(
        folderUri.lastPathSegment ?: folderUri.toString(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onRescan) { Text("다시 훑기") }
        OutlinedButton(onClick = onConnectFolder) { Text("폴더 변경") }
    }

    if (recordings.isEmpty()) {
        Text(
            "폴더에서 파일을 찾지 못했습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text("발견한 파일 ${recordings.size}건", style = MaterialTheme.typography.bodyMedium)

    recordings.forEach { recording ->
        Card(colors = CardDefaults.elevatedCardColors()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(recording.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${formatSize(recording.sizeBytes)} · ${recording.mimeType ?: "형식 미상"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FileCard(file: AudioFile) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(file.displayName, style = MaterialTheme.typography.titleSmall)
            InfoRow("길이", file.durationMs?.let(::formatDuration) ?: "확인 불가")
            InfoRow("크기", file.sizeBytes?.let(::formatSize) ?: "확인 불가")
            InfoRow("형식", file.mimeType ?: "확인 불가")

            Text(
                "STT · 분류 파이프라인은 아직 연결되지 않았습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun MessageCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return String.format(Locale.US, "%d분 %02d초", totalSeconds / 60, totalSeconds % 60)
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
}

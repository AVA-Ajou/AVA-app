package com.readingshield

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import com.readingshield.audio.AudioInput
import com.readingshield.audio.FoundRecording
import com.readingshield.audio.readAudioFile
import com.readingshield.audio.recordingFolder
import com.readingshield.audio.saveRecordingFolder
import com.readingshield.audio.scanRecordingFolder
import com.readingshield.ui.AnalysisScreen
import com.readingshield.ui.ReadingShieldTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 공유 시트/파일 열기로 들어왔다면 인텐트에 대상 URI 가 실려 있다.
        val sharedUri = intent?.extractAudioUri()

        setContent {
            ReadingShieldTheme {
                val context = LocalContext.current

                var input by remember { mutableStateOf<AudioInput>(AudioInput.Empty) }
                var folderUri by remember { mutableStateOf(context.recordingFolder()) }
                var recordings by remember { mutableStateOf(emptyList<FoundRecording>()) }

                // 이미 폴더를 연결해 둔 상태라면 진입할 때 바로 훑는다.
                LaunchedEffect(folderUri) {
                    recordings = context.scanRecordingFolder()
                }

                LaunchedEffect(sharedUri) {
                    if (sharedUri != null) input = context.loadInput(sharedUri)
                }

                val filePicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri -> if (uri != null) input = context.loadInput(uri) }

                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri ->
                    if (uri != null) {
                        context.saveRecordingFolder(uri)
                        folderUri = uri
                    }
                }

                AnalysisScreen(
                    input = input,
                    folderUri = folderUri,
                    recordings = recordings,
                    onPickFile = { filePicker.launch(arrayOf("audio/*")) },
                    onConnectFolder = { folderPicker.launch(null) },
                    onRescan = { recordings = context.scanRecordingFolder() },
                )
            }
        }
    }
}

private fun Intent.extractAudioUri(): Uri? = when (action) {
    Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
    Intent.ACTION_VIEW -> data
    else -> null
}

private fun Context.loadInput(uri: Uri): AudioInput =
    runCatching { AudioInput.Loaded(readAudioFile(uri)) }
        .getOrElse { error ->
            AudioInput.Failed(
                when (error) {
                    is SecurityException ->
                        "이 파일을 읽을 권한이 없습니다. 파일 관리자에서 다시 공유해 주세요."
                    else -> error.message ?: "알 수 없는 오류"
                },
            )
        }

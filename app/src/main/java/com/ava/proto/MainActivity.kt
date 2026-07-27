package com.ava.proto

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ava.proto.capture.RecordingFolder
import com.ava.proto.capture.TargetPackages
import com.ava.proto.ui.HomeScreen
import com.ava.proto.ui.HomeViewModel
import com.ava.proto.ui.ProtoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProtoTheme {
                val context = LocalContext.current
                val app = context.applicationContext as ProtoApplication

                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.database))
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                var folderUri by remember { mutableStateOf(RecordingFolder.get(context)) }
                val notificationAccessGranted by rememberNotificationAccessGranted()

                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri ->
                    if (uri != null) {
                        RecordingFolder.save(context, uri)
                        folderUri = uri
                    }
                }

                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                HomeScreen(
                    uiState = uiState,
                    recordingFolderUri = folderUri,
                    notificationAccessGranted = notificationAccessGranted,
                    defaultSmsPackage = TargetPackages.defaultSmsPackage(context),
                    onConnectFolder = { folderPicker.launch(null) },
                    onOpenNotificationAccessSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                )
            }
        }
    }
}

/**
 * 알림 접근은 시스템 설정에서 켜고 끄기 때문에, 화면으로 돌아올 때(ON_RESUME)마다
 * 다시 확인해야 최신 상태를 보여줄 수 있다.
 */
@Composable
private fun rememberNotificationAccessGranted(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(isNotificationAccessGranted(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.value = isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return state
}

private fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

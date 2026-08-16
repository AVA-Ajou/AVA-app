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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ava.proto.ui.SplashScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ProtoTheme {
                // 인트로는 Activity 가 아니라 여기서 상태 하나로 가린다 — 별도 Activity 를
                // 두면 진입점이 둘이 되고, 알림을 눌러 들어오는 경로에서도 인트로가 끼어든다.
                var showSplash by rememberSaveable { mutableStateOf(true) }

                val context = LocalContext.current
                val app = context.applicationContext as ProtoApplication

                // ViewModel 에는 Activity 가 아니라 Application 을 넘긴다 — ViewModel 은 화면
                // 회전을 넘어 살아남으므로 Activity 를 들고 있으면 그대로 누수가 된다.
                val viewModel: HomeViewModel =
                    viewModel(factory = HomeViewModel.Factory(app.database, app.demoInjector, app))
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
                val callDemoStep by viewModel.callDemoStep.collectAsStateWithLifecycle()
                val callDemoResult by viewModel.callDemoResult.collectAsStateWithLifecycle()
                val autoTestRunning by viewModel.autoTestRunning.collectAsStateWithLifecycle()
                val autoTestStatus by viewModel.autoTestStatus.collectAsStateWithLifecycle()

                var folderUri by remember { mutableStateOf(RecordingFolder.get(context)) }
                val notificationAccessGranted by rememberNotificationAccessGranted()

                val folderPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocumentTree(),
                ) { uri ->
                    if (uri != null) {
                        RecordingFolder.save(context, uri)
                        folderUri = uri
                        app.restartWatcher()
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

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                    return@ProtoTheme
                }

                HomeScreen(
                    uiState = uiState,
                    recordingFolderUri = folderUri,
                    notificationAccessGranted = notificationAccessGranted,
                    defaultSmsPackage = TargetPackages.defaultSmsPackage(context),
                    isBusy = isBusy,
                    callDemoStep = callDemoStep,
                    callDemoResult = callDemoResult,
                    autoTestRunning = autoTestRunning,
                    autoTestStatus = autoTestStatus,
                    onConnectFolder = { folderPicker.launch(null) },
                    onOpenNotificationAccessSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onScanNow = viewModel::scanNow,
                    onDemoKakao = viewModel::demoKakao,
                    onDemoSms = viewModel::demoSms,
                    onDemoCall = viewModel::rescanCallFolder,
                    onCancelCallDemo = viewModel::cancelCallDemo,
                    onStartAutoTestKakao = { viewModel.startAutoTest(isKakao = true) },
                    onStartAutoTestSms = { viewModel.startAutoTest(isKakao = false) },
                    onStopAutoTest = viewModel::stopAutoTest,
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

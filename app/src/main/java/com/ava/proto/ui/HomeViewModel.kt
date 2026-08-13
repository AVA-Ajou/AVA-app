package com.ava.proto.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.ava.proto.capture.RecordingScanWorker
import com.ava.proto.data.AppDatabase
import com.ava.proto.data.EventEntity
import com.ava.proto.demo.DemoInjector
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ava.proto.capture.RecordingFolder
import kotlinx.coroutines.withTimeoutOrNull

data class HomeUiState(
    val events: List<EventEntity> = emptyList(),
)

/** 통화 전사본 분석의 진행 상태. */
enum class CallDemoStep {
    IDLE,
    /** 녹음 폴더의 통화 파일을 판정하는 중 */
    ANALYZING,
}

enum class CallDemoResult { NO_FOLDER }

/** 진행 표시를 강제로 내리는 시간. 모델 판정이 파일당 수십 초 걸릴 수 있어 넉넉히 잡는다. */
private const val SCAN_TIMEOUT_MILLIS = 3 * 60 * 1000L

/**
 * `AndroidViewModel`을 쓰는 이유는 Context 가 필요해서다 —
 * `RecordingFolder`(SharedPreferences) 와 `WorkManager` 가 요구한다.
 *
 * 평범한 `ViewModel`에 Context 를 필드로 들고 있으면 Activity 를 넘겨받는 순간 그대로
 * 누수가 된다. ViewModel 은 화면 회전을 넘어 살아남기 때문이다.
 */
class HomeViewModel(
    private val database: AppDatabase,
    private val demoInjector: DemoInjector,
    application: Application,
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>()

    val uiState: StateFlow<HomeUiState> = database.eventDao().observeRecent()
        .map { events -> HomeUiState(events = events) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _callDemoStep = MutableStateFlow(CallDemoStep.IDLE)
    val callDemoStep: StateFlow<CallDemoStep> = _callDemoStep.asStateFlow()

    /** 진행 표시를 끝낼 때 쓰는 작업 감시. 새 분석을 시작할 때마다 갈아끼운다. */
    private var scanWatchJob: Job? = null

    private val _callDemoResult = MutableStateFlow<CallDemoResult?>(null)
    val callDemoResult: StateFlow<CallDemoResult?> = _callDemoResult.asStateFlow()

    fun demoKakao() { demoInjector.injectKakao() }

    fun demoSms() { demoInjector.injectSms() }

    // ── 오탐 검증: 정상 ↔ 피싱 자동 순환 ─────────────────────────────────────

    private val _autoTestRunning = MutableStateFlow(false)
    val autoTestRunning: StateFlow<Boolean> = _autoTestRunning.asStateFlow()

    private val _autoTestStatus = MutableStateFlow("")
    val autoTestStatus: StateFlow<String> = _autoTestStatus.asStateFlow()

    private var autoTestJob: Job? = null

    fun startAutoTest(isKakao: Boolean) {
        autoTestJob?.cancel()
        autoTestJob = viewModelScope.launch {
            _autoTestRunning.value = true
            try {
                val channelLabel = if (isKakao) "카카오톡" else "SMS"
                while (true) {
                    // 정상 메시지 발송
                    _autoTestStatus.value = "[$channelLabel] 정상 메시지 발송 중..."
                    val countBefore = uiState.value.events.size
                    if (isKakao) demoInjector.injectKakaoNormal() else demoInjector.injectSmsNormal()

                    _autoTestStatus.value = "[$channelLabel] 정상 메시지 → 탐지 결과 대기 중..."
                    withTimeoutOrNull(8_000) {
                        uiState.filter { it.events.size > countBefore }.first()
                    }
                    delay(2_000)

                    // 피싱 메시지 발송
                    _autoTestStatus.value = "[$channelLabel] 피싱 메시지 발송 중..."
                    val countBefore2 = uiState.value.events.size
                    if (isKakao) demoInjector.injectKakao() else demoInjector.injectSms()

                    _autoTestStatus.value = "[$channelLabel] 피싱 메시지 → 탐지 결과 대기 중..."
                    withTimeoutOrNull(8_000) {
                        uiState.filter { it.events.size > countBefore2 }.first()
                    }
                    delay(2_000)
                }
            } finally {
                _autoTestRunning.value = false
                _autoTestStatus.value = ""
            }
        }
    }

    fun stopAutoTest() {
        autoTestJob?.cancel()
    }

    /**
     * 녹음 폴더에 넣어둔 통화 파일을 지금 분석한다. **이미 판정한 파일도 다시 본다.**
     *
     * 예전에는 앱에 박힌 샘플 음성을 폴더에 복사해 STT를 태웠는데, 지금 확인하려는 것은
     * 모델의 탐지 성능이고 STT는 별개 단계다. 사용자가 폴더에 `.txt`를 넣어두고 이 버튼을
     * 누르면 그 내용을 그대로 판정한다.
     */
    fun rescanCallFolder() = launchDemo {
        if (RecordingFolder.get(context) == null) {
            _callDemoResult.value = CallDemoResult.NO_FOLDER
            return@launchDemo
        }
        // 폴더를 연결한 뒤 다시 누르면 이전 오류 배너를 내린다. 이 한 줄이 없으면 한 번 뜬
        // "폴더 미연결"이 연결을 마친 뒤에도 화면에 그대로 남는다 — 배너를 닫는 다른
        // 경로가 없기 때문이다.
        _callDemoResult.value = null

        val request = RecordingScanWorker.enqueueNow(context, force = true)
        _callDemoStep.value = CallDemoStep.ANALYZING
        watchScan(request.id)
    }

    /**
     * 작업이 끝나면 진행 표시를 내린다.
     *
     * 예전에는 "분석 시작 시각 이후의 CALL 이벤트가 생겼는가"로 판단했는데, 그 시각은 파일의
     * **수정 시각**이라 과거에 넣어둔 파일을 다시 볼 때는 영원히 조건에 맞지 않았다.
     * 작업 상태를 직접 보면 그런 어긋남이 없다. 그래도 안 끝나는 경우를 대비해 시간 제한을 둔다 —
     * 표시가 영원히 남으면 앱을 지우기 전에는 벗어날 방법이 없다.
     */
    private fun watchScan(id: java.util.UUID) {
        scanWatchJob?.cancel()
        scanWatchJob = viewModelScope.launch {
            withTimeoutOrNull(SCAN_TIMEOUT_MILLIS) {
                WorkManager.getInstance(context).getWorkInfoByIdFlow(id)
                    .first { it == null || it.state.isFinished }
            }
            _callDemoStep.value = CallDemoStep.IDLE
        }
    }

    fun cancelCallDemo() {
        scanWatchJob?.cancel()
        WorkManager.getInstance(context).cancelAllWorkByTag(RecordingScanWorker.TAG_IMMEDIATE)
        _callDemoStep.value = CallDemoStep.IDLE
    }

    /** 15분 주기를 기다리지 않고 녹음 폴더를 즉시 스캔한다 (수동 확인용). */
    fun scanNow() {
        RecordingScanWorker.enqueueNow(context)
    }

    private fun launchDemo(block: suspend () -> Unit) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            block()
            _isBusy.value = false
        }
    }

    class Factory(
        private val database: AppDatabase,
        private val demoInjector: DemoInjector,
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(database, demoInjector, application) as T
    }
}

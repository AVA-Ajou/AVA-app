package com.ava.proto.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ava.proto.capture.Channel
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
import kotlinx.coroutines.withTimeoutOrNull

data class HomeUiState(
    val events: List<EventEntity> = emptyList(),
)

/** 통화 녹음 데모의 단계별 진행 상태. */
enum class CallDemoStep {
    IDLE,
    /** 음성 파일을 SAF 폴더로 복사 중 */
    COPYING,
    /** FileObserver가 감지 → Gemini STT + 분류 중 */
    ANALYZING,
}

enum class CallDemoResult { NO_FOLDER }

class HomeViewModel(
    private val database: AppDatabase,
    private val demoInjector: DemoInjector,
    private val context: Context,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = database.eventDao().observeRecent()
        .map { events -> HomeUiState(events = events) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _callDemoStep = MutableStateFlow(CallDemoStep.IDLE)
    val callDemoStep: StateFlow<CallDemoStep> = _callDemoStep.asStateFlow()

    // ANALYZING 상태가 시작된 시각 — 이 이후에 CALL 이벤트가 들어오면 완료로 판단
    private var analyzingStartedAt = 0L

    init {
        // 워커가 완료돼 DB에 새 CALL 이벤트가 생기면 자동으로 IDLE 전환
        viewModelScope.launch {
            database.eventDao().observeRecent().collect { events ->
                if (_callDemoStep.value == CallDemoStep.ANALYZING) {
                    val done = events.any { it.channel == Channel.CALL && it.capturedAt >= analyzingStartedAt }
                    if (done) _callDemoStep.value = CallDemoStep.IDLE
                }
            }
        }
    }

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

    fun demoCall() = launchDemo {
        _callDemoStep.value = CallDemoStep.COPYING
        val ok = demoInjector.injectCallRecording()
        if (ok) {
            analyzingStartedAt = System.currentTimeMillis()
            _callDemoStep.value = CallDemoStep.ANALYZING
        } else {
            _callDemoStep.value = CallDemoStep.IDLE
            _callDemoResult.value = CallDemoResult.NO_FOLDER
        }
    }

    fun cancelCallDemo() {
        WorkManager.getInstance(context).cancelAllWorkByTag(RecordingScanWorker.TAG_IMMEDIATE)
        _callDemoStep.value = CallDemoStep.IDLE
    }

    fun clearCallDemoResult() { _callDemoResult.value = null }

    /** 15분 주기를 기다리지 않고 녹음 폴더를 즉시 스캔한다 (수동 확인용). */
    fun scanNow() {
        WorkManager.getInstance(context)
            .enqueue(
                OneTimeWorkRequestBuilder<RecordingScanWorker>()
                    .addTag(RecordingScanWorker.TAG_IMMEDIATE)
                    .build(),
            )
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
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(database, demoInjector, context) as T
    }
}

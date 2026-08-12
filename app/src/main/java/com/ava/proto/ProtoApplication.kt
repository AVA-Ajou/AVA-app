package com.ava.proto

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ava.proto.capture.CallRecordingWatcher
import com.ava.proto.capture.RecordingScanWorker
import com.ava.proto.classification.BackendClassificationClient
import com.ava.proto.classification.ClassificationClient
import com.ava.proto.classification.LocalKeywordClassificationClient
import com.ava.proto.data.AppDatabase
import com.ava.proto.demo.DemoInjector
import com.ava.proto.notification.AlertNotifier
import com.ava.proto.pipeline.DetectionPipeline
import com.ava.proto.session.SessionEngine
import com.ava.proto.stt.AudioTranscriber
import com.ava.proto.stt.ServerAudioTranscriber
import java.util.concurrent.TimeUnit

/**
 * 수동 DI 컨테이너. 이 시점 규모에서는 Hilt를 끌어들일 이유가 없어 Application이 직접
 * 싱글턴을 들고 있는다.
 */
class ProtoApplication : Application() {

    val database by lazy { AppDatabase.get(this) }
    val alertNotifier by lazy { AlertNotifier(this) }
    val sessionEngine by lazy {
        SessionEngine(
            eventDao = database.eventDao(),
            sessionDao = database.sessionDao(),
            alertNotifier = alertNotifier,
        )
    }

    /**
     * 판정기 선택.
     *
     * 서버 주소가 없으면 키워드 대역으로 떨어진다 — 설정이 덜 된 상태에서도 앱은 떠야 한다.
     * 예전에는 그 사이에 Groq LLM 단계가 있었는데, 서버가 위험도를 **보정된 확률**로 주는
     * 지금은 쓸 이유가 없어 걷어냈다.
     */
    val classificationClient: ClassificationClient by lazy {
        val serverUrl = BuildConfig.DETECTION_SERVER_URL
        if (serverUrl.isNotBlank()) BackendClassificationClient(serverUrl.trimEnd('/'))
        else LocalKeywordClassificationClient()
    }

    val detectionPipeline by lazy {
        DetectionPipeline(
            classificationClient = classificationClient,
            sessionEngine = sessionEngine,
            eventDao = database.eventDao(),
        )
    }

    /**
     * 통화 녹음 STT. 판정과 **같은 서버, 같은 모델**이 한다 — 어댑터를 끄면 전사기다.
     *
     * 서버 주소가 없으면 null 이고, [RecordingScanWorker]가 PENDING_TRANSCRIPTION 으로
     * 기록만 남긴다. 예전에는 Groq Whisper·Gemini 를 썼는데, 통화 음성이 외부 업체로
     * 나가는 것이 이 앱의 권한 정책과 맞지 않아 걷어냈다.
     */
    val audioTranscriber: AudioTranscriber? by lazy {
        BuildConfig.DETECTION_SERVER_URL.takeIf { it.isNotBlank() }
            ?.let { ServerAudioTranscriber(this, it.trimEnd('/')) }
    }

    val demoInjector by lazy { DemoInjector(this, detectionPipeline) }

    val callRecordingWatcher by lazy { CallRecordingWatcher(this) }

    override fun onCreate() {
        super.onCreate()
        WorkManager.getInstance(this).cancelAllWorkByTag(RecordingScanWorker.TAG_IMMEDIATE)
        scheduleRecordingScan()
        callRecordingWatcher.restart()
    }

    /** 폴더 변경 후 호출해 감시 대상을 갱신한다. */
    fun restartWatcher() = callRecordingWatcher.restart()

    private fun scheduleRecordingScan() {
        val request = PeriodicWorkRequestBuilder<RecordingScanWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecordingScanWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

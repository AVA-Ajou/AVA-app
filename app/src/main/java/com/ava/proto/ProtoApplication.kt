package com.ava.proto

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ava.proto.capture.CallRecordingWatcher
import com.ava.proto.capture.RecordingScanWorker
import com.ava.proto.classification.BackendClassificationClient
import com.ava.proto.classification.ClassificationClient
import com.ava.proto.classification.GroqClassificationClient
import com.ava.proto.classification.LocalKeywordClassificationClient
import com.ava.proto.data.AppDatabase
import com.ava.proto.demo.DemoInjector
import com.ava.proto.notification.AlertNotifier
import com.ava.proto.pipeline.DetectionPipeline
import com.ava.proto.session.SessionEngine
import com.ava.proto.stt.AudioTranscriber
import com.ava.proto.stt.GeminiAudioTranscriber
import com.ava.proto.stt.GroqAudioTranscriber
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
     * 판정기 선택. 아래로 갈수록 약한 대역이다.
     *
     *   1. 파인튜닝 모델 서버 — 위험도를 확률에서 읽고 진행 단계까지 준다. 주소가 있으면
     *   2. Groq LLM — 키워드 없이 문맥을 읽지만 위험도는 이분법이다
     *   3. 키워드 — API 키가 없어도 앱은 동작해야 하므로 남겨둔다
     */
    val classificationClient: ClassificationClient by lazy {
        val serverUrl = BuildConfig.DETECTION_SERVER_URL
        val groqKey = BuildConfig.GROQ_API_KEY
        when {
            serverUrl.isNotBlank() -> BackendClassificationClient(serverUrl.trimEnd('/'))
            groqKey.isNotBlank() -> GroqClassificationClient(groqKey)
            else -> LocalKeywordClassificationClient()
        }
    }

    val detectionPipeline by lazy {
        DetectionPipeline(
            classificationClient = classificationClient,
            sessionEngine = sessionEngine,
            eventDao = database.eventDao(),
        )
    }

    /**
     * 통화 녹음 STT.
     * Groq Whisper를 우선 사용하고, 키가 없으면 Gemini로 폴백.
     * 둘 다 없으면 null — [RecordingScanWorker]가 PENDING_TRANSCRIPTION으로 기록한다.
     */
    val audioTranscriber: AudioTranscriber? by lazy {
        val groqKey = BuildConfig.GROQ_API_KEY
        val geminiKey = BuildConfig.GEMINI_API_KEY
        when {
            groqKey.isNotBlank() -> GroqAudioTranscriber(this, groqKey)
            geminiKey.isNotBlank() -> GeminiAudioTranscriber(this, geminiKey)
            else -> null
        }
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

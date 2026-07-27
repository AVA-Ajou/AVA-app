package com.ava.proto

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ava.proto.capture.RecordingScanWorker
import com.ava.proto.classification.ClassificationClient
import com.ava.proto.classification.LocalKeywordClassificationClient
import com.ava.proto.data.AppDatabase
import com.ava.proto.notification.AlertNotifier
import com.ava.proto.pipeline.DetectionPipeline
import com.ava.proto.session.SessionEngine
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

    // 백엔드(Gemini)가 준비되면 이 한 줄만 실제 구현체로 바꾸면 된다.
    val classificationClient: ClassificationClient by lazy { LocalKeywordClassificationClient() }

    val detectionPipeline by lazy {
        DetectionPipeline(
            classificationClient = classificationClient,
            sessionEngine = sessionEngine,
        )
    }

    override fun onCreate() {
        super.onCreate()
        scheduleRecordingScan()
    }

    private fun scheduleRecordingScan() {
        val request = PeriodicWorkRequestBuilder<RecordingScanWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            RecordingScanWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

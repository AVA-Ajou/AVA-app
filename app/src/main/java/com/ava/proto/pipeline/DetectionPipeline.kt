package com.ava.proto.pipeline

import android.util.Log
import com.ava.proto.capture.CapturedEvent
import com.ava.proto.classification.ClassificationClient
import com.ava.proto.classification.ClassificationVerdict
import com.ava.proto.data.EventDao
import com.ava.proto.data.EventEntity
import com.ava.proto.data.EventStatus
import com.ava.proto.data.RiskSignal
import com.ava.proto.session.SessionEngine

private const val TAG = "DetectionPipeline"

/**
 * 캡처 -> 분류 -> 세션 융합을 잇는 유일한 조립 지점.
 *
 * 캡처 계층(NotificationCaptureService, RecordingScanWorker)은 이 클래스만 알면 되고,
 * 분류를 누가 하는지(모델 서버인지 키워드 대역인지)는 몰라도 된다. 반대로 분류·세션
 * 로직도 캡처 방식(알림 리스너인지 SAF 폴더 스캔인지)을 몰라도 된다 — 각 계층이 서로를
 * 모르게 유지하려고 이 조립 지점을 따로 뒀다.
 */
class DetectionPipeline(
    private val classificationClient: ClassificationClient,
    private val sessionEngine: SessionEngine,
    private val eventDao: EventDao,
) {
    suspend fun process(captured: CapturedEvent): EventEntity {
        val (status, verdict) = resolveVerdict(captured)

        // 같은 파일을 다시 분석하는 경우 기존 행을 이어받는다. 새 행을 넣으면 재분석할 때마다
        // 이벤트가 쌓여 탐지율 같은 숫자가 부풀려진다. 세션은 그대로 두고 판정만 갱신한다.
        val existing = eventDao.findBySource(captured.channel, captured.sourceLabel)

        val event = EventEntity(
            id = existing?.id ?: 0,
            sessionId = existing?.sessionId,
            channel = captured.channel,
            capturedAt = captured.capturedAt,
            sourceLabel = captured.sourceLabel,
            text = captured.text,
            status = status,
            riskSignal = verdict.riskSignal,
            matchedPhrase = verdict.matchedPhrase,
            audioUri = captured.audioUri,
            counterpart = captured.counterpart,
            // 모델이 준 값은 그대로 싣는다. 등급으로 접기 전 원본이라 화면에 보여줄 수 있다.
            risk = verdict.risk,
            stage = verdict.stage,
            stageLabel = verdict.stageLabel,
        )

        return sessionEngine.ingest(event)
    }

    private suspend fun resolveVerdict(captured: CapturedEvent): Pair<EventStatus, ClassificationVerdict> {
        val text = captured.text
            // 분석할 텍스트가 없으면 분류를 시도하지 않고 그대로 기록만 한다.
            ?: return EventStatus.PENDING_TRANSCRIPTION to ClassificationVerdict(RiskSignal.NONE, null)

        // 텍스트가 있는 이벤트는 내용과 무관하게 예외 없이 분류를 거친다 — 게이트 없음.
        return try {
            EventStatus.ANALYZED to classificationClient.classify(text)
        } catch (e: Exception) {
            // 분류 실패를 RiskSignal.NONE으로만 남기면 "무해하다고 확인됨"과 구분이 안 된다.
            // CLASSIFICATION_FAILED로 명시해서, 게이트 시절의 "NONE = 분석 안 됨" 모호함이
            // 되돌아오지 않게 한다. 이벤트 자체는 여기서 유실시키지 않고 기록은 남긴다.
            Log.e(TAG, "분류 실패, CLASSIFICATION_FAILED로 기록", e)
            EventStatus.CLASSIFICATION_FAILED to ClassificationVerdict(RiskSignal.NONE, null)
        }
    }
}
